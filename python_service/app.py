from flask import Flask, request, jsonify, send_from_directory
from services.data_cleaning import clean_data
from services.graph import build_graph_for_project, graph_json_for_project
import os
import datetime
from flask_cors import CORS
import uuid
import pymysql
import uuid
from datetime import datetime
import json

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

# ============ 静态文件访问 ============ #
UPLOAD_FOLDER = os.path.join(os.getcwd(), 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/uploads/<path:filename>', methods=['GET'])
def download_file(filename):
    """允许 SpringBoot 通过 HTTP 下载清洗后的文件"""
    return send_from_directory(UPLOAD_FOLDER, filename, as_attachment=True)

# ============ 用户注册接口 ============ #
@app.route('/user/register', methods=['POST'])
def register_user():
    """用户注册接口"""
    try:
        data = request.get_json(force=True)
        username = data.get('username')
        password_hash = data.get('passwordHash')
        role = data.get('role', 'user')

        if not username or not password_hash:
            return jsonify({"code": 400, "msg": "用户名或密码不能为空"}), 400

        # 数据库连接
        conn = pymysql.connect(
            host='localhost',
            user='root',
            password='1234',
            database='douyincac_sql',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        cursor = conn.cursor()

        # 检查用户名是否重复
        check_sql = "SELECT COUNT(*) AS cnt FROM user WHERE username=%s"
        cursor.execute(check_sql, (username,))
        exists = cursor.fetchone()['cnt']
        if exists > 0:
            conn.close()
            return jsonify({"code": 409, "msg": "用户名已存在"}), 200

        # 生成 uuid
        user_uuid = str(uuid.uuid4())

        # 插入新用户
        insert_sql = """
                     INSERT INTO user (username, password_hash, role, created_at, uuid)
                     VALUES (%s, %s, %s, %s, %s) \
                     """
        cursor.execute(insert_sql, (username, password_hash, role, datetime.now(), user_uuid))
        conn.commit()
        conn.close()

        return jsonify({
            "code": 0,
            "msg": "注册成功",
            "data": {
                "username": username,
                "role": role,
                "uuid": user_uuid
            }
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"code": 500, "msg": str(e)})


# ============ 用户登录接口 ============ #
@app.route('/user/login', methods=['POST'])
def login_user():
    """用户登录接口"""
    try:
        data = request.get_json(force=True)
        username = data.get('username')
        password_hash = data.get('passwordHash')

        if not username or not password_hash:
            return jsonify({"code": 400, "msg": "用户名或密码不能为空"}), 400

        # 连接数据库
        conn = pymysql.connect(
            host='localhost',
            user='root',
            password='1234',
            database='douyincac_sql',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        cursor = conn.cursor()

        # 查询用户信息
        sql = "SELECT uid, uuid, username, password_hash, role FROM user WHERE username=%s"
        cursor.execute(sql, (username,))
        user = cursor.fetchone()

        if not user:
            conn.close()
            return jsonify({"code": 401, "msg": "用户不存在"}), 200

        # 校验密码（前端已经 md5 加密，后端直接比对哈希）
        if user['password_hash'] != password_hash:
            conn.close()
            return jsonify({"code": 401, "msg": "用户名或密码错误"}), 200

        # 生成 token
        token = str(uuid.uuid4())

        # 更新最后登录时间
        update_sql = "UPDATE user SET last_login=%s WHERE uid=%s"
        cursor.execute(update_sql, (datetime.now(), user['uid']))
        conn.commit()
        conn.close()

        # 返回登录结果
        return jsonify({
            "code": 200,
            "msg": "登录成功",
            "data": {
                "token": token,
                "role": user['role'],
                "uuid": user['uuid']
            }
        })
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"code": 500, "msg": str(e)})

# ============ 主清洗接口 ============ #
@app.route('/clean', methods=['POST'])
@app.route('/clean', methods=['POST'])
def clean():
    """
    上传文件 -> 调用 data_cleaning.clean_data()
    -> 自动保存 + 返回预览与下载路径
    """
    try:
        request.charset = 'utf-8'
        print("🧾 content-type =", request.content_type)
        print("🧾 headers =", dict(request.headers))

        # ---- 验证上传 ----
        if 'file' not in request.files:
            return jsonify({"status": "error", "message": "未检测到上传文件"}), 400

        file = request.files['file']
        if file.filename == '':
            return jsonify({"status": "error", "message": "文件名为空"}), 400

        # ---- 读取表单参数 ----
        project_name = request.form.get('project_name', 'default_project')
        options = request.form.get('options', '[]')
        download_time = request.form.get('download_time')
        print("🧾 表单参数：", project_name, options, download_time)

        # ---- 保存上传文件 ----
        project_folder = os.path.join(UPLOAD_FOLDER, project_name)
        os.makedirs(project_folder, exist_ok=True)
        _, ext = os.path.splitext(file.filename)
        safe_filename = f"{uuid.uuid4().hex}{ext}"
        input_path = os.path.join(project_folder, safe_filename)
        file.save(input_path)

        # ---- 输出路径 ----
        output_path = os.path.join(project_folder, f"cleaned_{os.path.splitext(safe_filename)[0]}.csv")

        # ---- 调用清洗逻辑 ----
        result = clean_data(input_path, output_path, options_json=options, download_time=download_time)

        # ---- 转换路径格式 ----
        if result.get("status") == "success":
            rel_path = os.path.relpath(result["output_path"], os.getcwd()).replace("\\", "/")
            result["output_path"] = rel_path

        # ✅ 最关键：安全序列化 + 日志打印
        from flask import Response
        safe_json = json.dumps(result, ensure_ascii=False, default=str)
        print("✅ Flask 返回 JSON 预览：", safe_json[:200], "...")
        return Response(safe_json, mimetype="application/json")

    except Exception as e:
        import traceback
        print("❌ Flask 后端异常：", e)
        traceback.print_exc()
        return jsonify({"status": "error", "message": str(e)}), 500


# ============ 图构建：POST /graph/build ============ #
@app.route('/graph/build', methods=['POST'])
def graph_build():
    try:
        data = request.get_json(force=True)
        pid = data.get('pid')
        if not pid:
            return jsonify({"status": "fail", "message": "pid is required"}), 400
        result = build_graph_for_project(pid)
        return jsonify(result)
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


# ============ 图导出：GET /graph/project?pid=xxx ============ #
@app.route('/graph/project', methods=['GET'])
def graph_project():
    try:
        pid = request.args.get('pid')
        if not pid:
            return jsonify({"status": "fail", "message": "pid is required"}), 400
        data = graph_json_for_project(pid)
        return jsonify({"status": "success", "data": data})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


# ============ CORS 设置 ============ #
@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
    return response


# ============ 情感分析接口 ============ #
from snownlp import SnowNLP
import pymysql
from datetime import datetime
from tqdm import tqdm

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'douyincac_sql',
    'charset': 'utf8mb4'
}

def get_connection():
    return pymysql.connect(**DB_CONFIG)

def update_project_status(pid, status):
    conn = get_connection()
    cursor = conn.cursor()
    sql = "UPDATE project SET status=%s, update_time=%s WHERE pid=%s"
    cursor.execute(sql, (status, datetime.now(), pid))
    conn.commit()
    conn.close()

def fetch_comments(pid):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    sql = """
          SELECT cid, pid, content
          FROM comment
          WHERE pid = %s AND is_abnormal = 0
            AND content IS NOT NULL AND content != ''
          """
    cursor.execute(sql, (pid,))
    data = cursor.fetchall()
    conn.close()
    return data

def analyze_sentiment(text):
    try:
        s = SnowNLP(text)
        score = round(float(s.sentiments), 4)
        if score > 0.6:
            label = 1
        elif score < 0.4:
            label = -1
        else:
            label = 0
        return label, score
    except Exception:
        return None, 0.0

def insert_sentiment_results(results):
    conn = get_connection()
    cursor = conn.cursor()
    sql = """
          INSERT INTO sentiment (cid, pid, sentiment_label, confidence_score, analysis_time)
          VALUES (%s, %s, %s, %s, %s)
              ON DUPLICATE KEY UPDATE
                                   sentiment_label=VALUES(sentiment_label),
                                   confidence_score=VALUES(confidence_score),
                                   analysis_time=VALUES(analysis_time)
          """
    cursor.executemany(sql, results)
    conn.commit()
    conn.close()

@app.route('/sentiment/analyze', methods=['POST'])
def analyze_sentiment_api():
    """情感分析接口"""
    try:
        data = request.get_json(force=True)
        pid = data.get('pid')
        if not pid:
            return jsonify({"status": "fail", "message": "pid is required"}), 400

        print(f"🚀 开始分析项目 {pid} 的评论情感…")
        update_project_status(pid, "running")

        comments = fetch_comments(pid)
        if not comments:
            update_project_status(pid, "fail")
            return jsonify({"status": "fail", "message": "无评论数据"}), 200

        results = []
        for row in tqdm(comments, desc=f"项目 {pid} 分析中"):
            cid = row["cid"]
            content = row["content"]
            label, score = analyze_sentiment(content)
            if label is not None:
                results.append((cid, pid, label, score, datetime.now()))

        if results:
            insert_sentiment_results(results)
            update_project_status(pid, "success")
            return jsonify({
                "status": "success",
                "count": len(results),
                "message": f"成功写入 {len(results)} 条情感分析结果"
            })
        else:
            update_project_status(pid, "fail")
            return jsonify({
                "status": "fail",
                "message": "未生成有效结果"
            })

    except Exception as e:
        update_project_status(pid, "fail")
        print("❌ Flask 情感分析异常：", e)
        return jsonify({"status": "error", "message": str(e)}), 500


# ============ 启动服务 ============ #
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
    app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024  # 允许最大100MB上传

