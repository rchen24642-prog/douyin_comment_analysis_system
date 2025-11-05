import math
from typing import Dict, Any, List, Tuple
import pandas as pd
from sqlalchemy import create_engine, text
import networkx as nx

# 社区检测：优先用 label propagation（内置），如需 Louvain 可后续替换
from networkx.algorithms.community import asyn_lpa_communities

from py2neo import Graph as NeoGraph

from config import (
    MYSQL_DSN, NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD,
    GRAPH_PAGERANK_DAMPING, GRAPH_BATCH_SIZE
)

# =========================
# 1) 数据加载
# =========================
def load_comments_df(pid: str) -> pd.DataFrame:
    """
    读取清洗后的评论，最少需要: cid, username, parent_cid, comment_time, pid
    """
    engine = create_engine(MYSQL_DSN)
    sql = text("""
               SELECT cid, username, parent_cid, comment_time, pid
               FROM comment
               WHERE pid = :pid
               """)
    df = pd.read_sql(sql, engine, params={"pid": pid})
    # 兜底：空用户名统一为“未知用户”
    df["username"] = (
        df["username"]
        .fillna("未知用户")
        .astype(str)
        .str.replace(r"[\r\n\t]+", " ", regex=True)  # 删除换行和制表符
        .str.strip()                                # 去前后空格
    )
    return df


# =========================
# 2) 构图（NetworkX DiGraph）
# =========================
def build_digraph(df: pd.DataFrame) -> nx.DiGraph:
    """
    用户为节点；若 parent_cid 存在且有效，则添加边：reply_user -> parent_user
    """
    G = nx.DiGraph()

    # Step 1️⃣ 建立 cid -> username 映射表（用于快速查找父评论作者）
    cid_to_user = dict(zip(df["cid"], df["username"]))

    # Step 2️⃣ 添加节点（所有出现过的用户名）
    for user in df["username"].unique():
        G.add_node(user)

    # Step 3️⃣ 遍历每条评论，构建用户交互边
    for _, row in df.iterrows():
        parent_cid = row.get("parent_cid")
        reply_user = row["username"]

        # 过滤掉空的、异常的 parent_cid
        if pd.isna(parent_cid) or parent_cid not in cid_to_user:
            continue

        parent_user = cid_to_user[parent_cid]
        if not isinstance(reply_user, str) or not isinstance(parent_user, str):
            continue
        if reply_user == parent_user:
            continue  # 自回复过滤

        # 添加或累积边
        if G.has_edge(reply_user, parent_user):
            G[reply_user][parent_user]["weight"] += 1
            G[reply_user][parent_user]["comments"].append(row["cid"])
            G[reply_user][parent_user]["last_ts"] = row.get("comment_time")
        else:
            G.add_edge(
                reply_user,
                parent_user,
                weight=1,
                comments=[row["cid"]],
                last_ts=row.get("comment_time")
            )

    print(f"✅ 构图完成：节点 {G.number_of_nodes()}，边 {G.number_of_edges()}")
    return G



# =========================
# 3) 指标计算（度/PR/社区）
# =========================
def compute_metrics(G: nx.DiGraph) -> None:
    """
    直接给 G 的节点写属性：in_degree, out_degree, pagerank, community
    PageRank: PR(v) = (1-d) + d * Σ[ PR(u) / k_out(u) ], d≈0.85
    社区: 异步标签传播 asyn_lpa_communities
    """
    # 度
    in_deg = dict(G.in_degree())
    out_deg = dict(G.out_degree())
    nx.set_node_attributes(G, in_deg, "in_degree")
    nx.set_node_attributes(G, out_deg, "out_degree")

    # PageRank
    pr = nx.pagerank(G, alpha=GRAPH_PAGERANK_DAMPING)
    nx.set_node_attributes(G, pr, "pagerank")

    # 社区（无监督、快速）
    # 返回的是若干个节点集合
    communities = list(asyn_lpa_communities(G))
    # 映射：node -> community_id
    comm_map: Dict[str, int] = {}
    for cid, comm_nodes in enumerate(communities, start=1):
        for n in comm_nodes:
            comm_map[n] = cid
    nx.set_node_attributes(G, comm_map, "community")


# =========================
# 4) 写入 Neo4j（批量）
# =========================
def get_neo_graph() -> NeoGraph:
    return NeoGraph(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))


def ensure_constraints(graph: NeoGraph):
    """
    为 (:User {name, pid}) 建唯一索引 / 复合索引；(:INTERACTS) 走关系无需索引
    """
    # Neo4j 5.x 语法：IF NOT EXISTS
    graph.run("CREATE CONSTRAINT user_name_pid IF NOT EXISTS FOR (u:User) REQUIRE (u.name, u.pid) IS UNIQUE")


def to_batches(items: List[Dict[str, Any]], batch_size: int):
    for i in range(0, len(items), batch_size):
        yield items[i:i + batch_size]


def write_graph_to_neo4j(G: nx.DiGraph, pid: str) -> Tuple[int, int]:
    """
    将 G 写入 Neo4j：
    节点标签：User {name, pid, in_degree, out_degree, pagerank, community}
    关系：(:User {name, pid})-[:INTERACTS {weight, comments, last_ts}]->(:User {name, pid})
    """
    graph = get_neo_graph()
    ensure_constraints(graph)

    # 1) 节点数据
    nodes = []
    for n, attr in G.nodes(data=True):
        clean_name = str(n).replace("\n", "").replace("\r", "").replace("\t", "").strip()
        nodes.append({
            "name": clean_name,
            "pid": str(pid),
            "in_degree": int(attr.get("in_degree", 0)),
            "out_degree": int(attr.get("out_degree", 0)),
            "pagerank": float(attr.get("pagerank", 0.0)),
            "community": int(attr.get("community", 0))
        })

    # 2) 关系数据
    rels = []
    for u, v, attr in G.edges(data=True):
        rels.append({
            "src": str(u),
            "dst": str(v),
            "pid": str(pid),
            "weight": int(attr.get("weight", 1)),
            "comments": list(map(str, attr.get("comments", []))),
            "last_ts": str(attr.get("last_ts")) if attr.get("last_ts") else None
        })

    # 3) 批量 MERGE 节点
    node_cypher = """
    UNWIND $rows AS row
    MERGE (u:User {name: row.name, pid: row.pid})
    SET u.in_degree = row.in_degree,
        u.out_degree = row.out_degree,
        u.pagerank = row.pagerank,
        u.community = row.community
    """
    # 4) 批量 MERGE 关系
    rel_cypher = """
    UNWIND $rows AS row
    MATCH (a:User {name: row.src, pid: row.pid})
    MATCH (b:User {name: row.dst, pid: row.pid})
    MERGE (a)-[r:INTERACTS]->(b)
    SET r.weight = coalesce(r.weight, 0) + row.weight,
        r.comments = coalesce(r.comments, []) + row.comments,
        r.last_ts = row.last_ts
    """

    # 分批执行
    tx = graph.begin()
    for batch in to_batches(nodes, GRAPH_BATCH_SIZE):
        tx.run(node_cypher, rows=batch)
    for batch in to_batches(rels, GRAPH_BATCH_SIZE):
        tx.run(rel_cypher, rows=batch)
    tx.commit()

    return len(nodes), len(rels)


# =========================
# 5) 导出前端可视化 JSON
# =========================
def graph_json_for_project(pid: str, limit: int = 5000) -> Dict[str, Any]:

    # === 新增：查询每个用户的代表性评论（取该用户在该 pid 下的最近一条） ===
    engine = create_engine(MYSQL_DSN)

    # 用窗口函数拿“每个 username 的最近一条 content”，避免 GROUP_CONCAT 限制/排序异常
    content_sql = text("""
                       SELECT username, content
                       FROM (
                                SELECT
                                    username,
                                    content,
                                    ROW_NUMBER() OVER (PARTITION BY username ORDER BY comment_time DESC) AS rn
                                FROM comment
                                WHERE pid = :pid
                                  AND username IS NOT NULL AND username <> ''
                                  AND content  IS NOT NULL AND content  <> ''
                            ) t
                       WHERE rn = 1
                       """)
    content_df = pd.read_sql(content_sql, engine, params={"pid": pid})

    # 统一两侧 key：去首尾空格，保证和 Neo4j 节点名匹配
    user_to_content = {str(u).strip(): str(c)
                        for u, c in zip(content_df["username"], content_df["content"])}

    # 方便定位问题：打印一下映射规模
    print(f"🧩 [DEBUG] user_to_content size = {len(user_to_content)} for pid={pid}")


    """
    为 Vue/ECharts 返回 nodes/links JSON；默认最多 5000 条边/点
    """
    graph = get_neo_graph()
    # 取前 N 个最重要节点（按 PR），及其出入边（控制体量）
    nodes_query = """
    MATCH (u:User {pid: $pid})
    RETURN u.name AS id, u.pagerank AS pr, u.in_degree AS indeg, u.out_degree AS outdeg, u.community AS comm
    ORDER BY pr DESC
    LIMIT $limit
    """
    nodes_res = graph.run(nodes_query, pid=pid, limit=limit).data()
    keep = set([row["id"] for row in nodes_res])

    rels_query = """
    MATCH (a:User {pid: $pid})-[r:INTERACTS]->(b:User {pid: $pid})
    WHERE a.name IN $keep AND b.name IN $keep
    RETURN a.name AS src, b.name AS dst, r.weight AS w
    LIMIT $limit
    """
    rels_res = graph.run(rels_query, pid=pid, keep=list(keep), limit=limit).data()

    nodes = []
    for r in nodes_res:
        uname = str(r["id"]).strip()  # ✅ 与映射同样的标准化
        content_text = user_to_content.get(uname)

        # 兜底：若没命中（比如编码/空格问题），再对该用户做一次单独查询
        if not content_text:
            try:
                fallback_sql = text("""
                                    SELECT content
                                    FROM comment
                                    WHERE pid = :pid AND username = :uname
                                      AND content IS NOT NULL AND content <> ''
                                    ORDER BY comment_time DESC
                                        LIMIT 1
                                    """)
                fb = pd.read_sql(fallback_sql, engine, params={"pid": pid, "uname": uname})
                print(f"🔍 [DEBUG] content_df rows = {len(content_df)}")
                if not fb.empty:
                    content_text = str(fb.iloc[0]["content"])
            except Exception as e:
                print(f"⚠️ [DEBUG] fallback query failed for {uname}: {e}")

        nodes.append({
            "id": uname,
            "name": uname,
            "pagerank": r["pr"],
            "in_degree": r["indeg"],
            "out_degree": r["outdeg"],
            "community": r["comm"],
            "content": content_text or "(无评论内容)"   # ✅ 前端已能显示
        })
    links = [{"source": r["src"], "target": r["dst"], "weight": r["w"]} for r in rels_res]
    print("🧪 user_to_content keys sample:", list(user_to_content.keys())[:5])
    print("🧪 first node name sample:", [r["id"] for r in nodes_res[:5]])
    return {"nodes": nodes, "links": links}


# =========================
# 6) 对外的总控函数
# =========================
def build_graph_for_project(pid: str) -> Dict[str, Any]:
    print(f"🚀 [DEBUG] 正在构建图: pid={pid}")

    df = load_comments_df(pid)
    print(f"📊 [DEBUG] 评论数据行数: {len(df)}")
    print(df.head().to_string())
    if df.empty:
        return {"status": "fail", "message": f"无评论数据，pid={pid}"}

    G = build_digraph(df)
    print(f"🕸️ [DEBUG] 构图完成: 节点数={G.number_of_nodes()}，边数={G.number_of_edges()}")
    compute_metrics(G)
    n_nodes, n_edges = write_graph_to_neo4j(G, pid)

    print(f"💾 [DEBUG] Neo4j写入: 节点={n_nodes}, 边={n_edges}")

    # Top-10 关键用户（按 PR）
    pr = nx.get_node_attributes(G, "pagerank")
    top = sorted(pr.items(), key=lambda x: x[1], reverse=True)[:10]
    top_list = [{"username": u, "pagerank": float(v)} for u, v in top]

    return {
        "status": "success",
        "pid": pid,
        "nodes": n_nodes,
        "edges": n_edges,
        "top_pagerank": top_list
    }
