import pymysql
from snownlp import SnowNLP
from datetime import datetime
from tqdm import tqdm
import traceback
import json
import os

# ================= 数据库配置 =================
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'douyincac_sql',
    'charset': 'utf8mb4'
}

# ================= 工具函数 =================
def get_connection():
    return pymysql.connect(**DB_CONFIG)

# 更新项目状态
def update_project_status(pid, status):
    conn = get_connection()
    cursor = conn.cursor()
    sql = "UPDATE project SET status=%s, update_time=%s WHERE pid=%s"
    cursor.execute(sql, (status, datetime.now(), pid))
    conn.commit()
    conn.close()

# 读取评论数据
def fetch_comments(pid):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    sql = """
          SELECT cid, pid, content
          FROM comment
          WHERE pid = %s AND is_abnormal = 0
            AND content IS NOT NULL AND content != '' \
          """
    cursor.execute(sql, (pid,))
    data = cursor.fetchall()
    conn.close()
    return data

# 情感分析逻辑
def analyze_sentiment(text, pos=None, neg=None, neu=None, alpha=0.05, beta=0.30):
    try:
        s = SnowNLP(text)
        score = float(s.sentiments)
        tl = (text or "").lower()

        # 出现次数计数，避免只修正一次
        def count_hits(word: str) -> int:
            return tl.count(word) if word else 0

        # 正/负向按权重线性叠加
        for w, wt in (pos or {}).items():
            c = count_hits(w)
            if c:
                score += alpha * wt * c

        for w, wt in (neg or {}).items():
            c = count_hits(w)
            if c:
                score -= alpha * wt * c

        # 中性：拉回 0.5（向 0.5 收缩）
        for w, wt in (neu or {}).items():
            c = count_hits(w)
            if c:
                score += (0.5 - score) * (beta * wt * c)

        # 裁剪 + 四舍五入
        score = max(0.0, min(1.0, round(score, 4)))

        # 阈值可微调：>=0.6 正向，<=0.4 负向
        label = 1 if score >= 0.6 else (-1 if score <= 0.4 else 0)
        return label, score

    except Exception:
        return None, 0.0

# 插入分析结果
def insert_sentiment_results(results):
    conn = get_connection()
    cursor = conn.cursor()
    sql = """
          INSERT INTO sentiment (cid, pid, sentiment_label, confidence_score, analysis_time)
          VALUES (%s, %s, %s, %s, %s)
              ON DUPLICATE KEY UPDATE
                                   sentiment_label=VALUES(sentiment_label),
                                   confidence_score=VALUES(confidence_score),
                                   analysis_time=VALUES(analysis_time) \
          """
    cursor.executemany(sql, results)
    conn.commit()
    conn.close()

#情感词典
def load_user_dict(uuid: str):
    if not uuid:
        return {}, {}, {}

    try:
        conn = get_connection()
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
                       SELECT word, sentiment, weight
                       FROM sentiment_dict
                       WHERE uuid = %s
                       """, (uuid,))
        rows = cursor.fetchall()
        conn.close()

        pos, neg, neu = {}, {}, {}
        for r in rows:
            w = (r["word"] or "").strip().lower()
            s = (r["sentiment"] or "").strip().lower()
            wt = float(r.get("weight") or 1.0)  # 默认 1.0
            if not w:
                continue
            if s == "positive":
                pos[w] = wt
            elif s == "negative":
                neg[w] = wt
            elif s == "neutral":
                neu[w] = wt

        print(f"✅ 用户词典：pos={len(pos)} neg={len(neg)} neu={len(neu)}")
        return pos, neg, neu

    except Exception as e:
        print(f"⚠️ 用户词典加载失败: {e}")
        return {}, {}, {}


# ================= 主流程 =================
def main(pid):
    print(f"\n🚀 开始分析项目 {pid} 的评论情感…")
    update_project_status(pid, "running")
    try:
        # 获取项目归属用户
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT uuid FROM project WHERE pid=%s", (pid,))
        row = cursor.fetchone()
        conn.close()

        user_uuid = row[0] if row else None

        # ✅ 加载用户词典
        user_pos, user_neg = load_user_dict(user_uuid)
        print(f"📚 用户自定义词典加载完成: +{len(user_pos)} 正面, +{len(user_neg)} 负面")

        comments = fetch_comments(pid)
        print(f"📦 共获取到 {len(comments)} 条评论。")

        results = []
        for row in tqdm(comments, desc="分析中"):
            cid = row["cid"]
            content = row["content"]
            label, score = analyze_sentiment(content, user_pos, user_neg)
            if label is not None:
                results.append((cid, pid, label, score, datetime.now()))

        if results:
            insert_sentiment_results(results)
            print(f"✅ 已写入 {len(results)} 条情感分析结果。")
            update_project_status(pid, "success")
        else:
            print("⚠️ 没有有效评论被分析。")
            update_project_status(pid, "fail")

    except Exception as e:
        print("❌ 任务执行失败：", str(e))
        traceback.print_exc()
        update_project_status(pid, "fail")

    finally:
        print(f"📅 任务完成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

# ================= 执行入口 =================
if __name__ == "__main__":
    pid_input = input("请输入要分析的项目ID (pid): ").strip()
    # 加载用户词典
    user_pos, user_neg, user_neu = load_user_dict(user_uuid)
    print(f"📚 词典加载完成: +{len(user_pos)} 正面, +{len(user_neg)} 负面, +{len(user_neu)} 中性")
    # 循环分析
    label, score = analyze_sentiment(content, user_pos, user_neg, user_neu)

    main(pid_input)
