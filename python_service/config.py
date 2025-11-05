# MySQL
MYSQL_DSN = "mysql+pymysql://root:1234@localhost:3306/douyincac_sql?charset=utf8mb4"

# Neo4j
NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "ririchen0426@"

# 图构建参数
GRAPH_PAGERANK_DAMPING = 0.85
GRAPH_BATCH_SIZE = 1000  # 批量写 Neo4j 的 UNWIND 批大小

