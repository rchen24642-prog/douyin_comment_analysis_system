import pandas as pd
import numpy as np
import json
import os
import re
import uuid
from datetime import timedelta, datetime

def clean_data(input_path, output_path, options_json="[]", download_time=None):
    """
    Douyin 评论数据清洗程序
    - 自动识别一级/二级评论
    - 支持相对时间转换
    - 可选清洗规则
    """

    #文件读取
    ext = os.path.splitext(input_path)[-1].lower()
    if ext in [".xlsx", ".xls"]:
        df_raw = pd.read_excel(input_path, engine="openpyxl")
    else:
        df_raw = pd.read_csv(input_path, encoding="utf-8", low_memory=False)

    print(f"🧾 表单参数： {options_json}")
    print(f"📂 文件共 {len(df_raw)} 条记录")

    #字段自动映射
    colmap = {
        "username": None,
        "comment_time": None,
        "content": None,
        "like_count": None,
        "reply_username": None,
        "reply_time": None,
        "reply_content": None,
        "reply_like": None,
    }

    for col in df_raw.columns:
        if "评论人" in col and "二级" not in col:
            colmap["username"] = col
        elif "评论时间" in col and "二级" not in col:
            colmap["comment_time"] = col
        elif "评论内容" in col and "二级" not in col:
            colmap["content"] = col
        elif "点赞" in col and "二级" not in col:
            colmap["like_count"] = col
        elif "二级评论人" in col:
            colmap["reply_username"] = col
        elif "二级评论时间" in col:
            colmap["reply_time"] = col
        elif "二级评论内容" in col:
            colmap["reply_content"] = col
        elif "二级评论点赞" in col:
            colmap["reply_like"] = col

    print("🧩 字段映射表：", colmap)

    #数据拆平：一级 + 二级评论
    rows = []
    for _, r in df_raw.iterrows():
        top_cid = str(uuid.uuid4())

        # 一级评论
        top = {
            "cid": top_cid,
            "parent_cid": "",
            "comment_type": 0,
            "content": safe_str(r.get(colmap["content"])),
            "comment_time": convert_relative_time(r.get(colmap["comment_time"]), download_time),
            "username": safe_str(r.get(colmap["username"])),
            "like_count": safe_int(r.get(colmap["like_count"])),
            "reply_count": 0
        }
        rows.append(top)

        # 二级评论
        reply_user = r.get(colmap["reply_username"])
        reply_content = r.get(colmap["reply_content"])
        if pd.notna(reply_user) or pd.notna(reply_content):
            sub = {
                "cid": str(uuid.uuid4()),
                "parent_cid": top_cid,
                "comment_type": 1,
                "content": safe_str(reply_content),
                "comment_time": convert_relative_time(r.get(colmap["reply_time"]), download_time),
                "username": safe_str(reply_user),
                "like_count": safe_int(r.get(colmap["reply_like"])),
                "reply_count": 0
            }
            rows.append(sub)

    df = pd.DataFrame(rows, columns=[
        "cid", "parent_cid", "comment_type",
        "content", "comment_time", "username",
        "like_count", "reply_count"
    ])

    print(f"✅ 拆平完成，共 {len(df)} 条；其中二级评论 {sum(df['comment_type']==1)} 条")

    #清洗选项
    options = json.loads(options_json or "[]")

    if "删除缺失行" in options:
        df.dropna(inplace=True)
    elif "填充默认值" in options:
        for col in ["username", "content"]:
            df[col].fillna("未知", inplace=True)
        df.fillna(0, inplace=True)

    if "删除重复行" in options:
        df.drop_duplicates(inplace=True)

    # 日期标准化
    df["comment_time"] = pd.to_datetime(df["comment_time"], errors="coerce").dt.strftime("%Y-%m-%d %H:%M:%S")

    # 网络用语替换
    if "网络用语替换" in options and "content" in df.columns:
        slang = {"yyds": "永远的神", "dbq": "对不起", "awsl": "啊我死了", "xswl": "笑死我了", "233": "哈哈哈"}
        for k, v in slang.items():
            df["content"] = df["content"].astype(str).str.replace(k, v, regex=False)

    #导出
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    df.to_csv(output_path, index=False, encoding="utf-8-sig")

    preview = df.head(50).replace({np.nan: None}).to_dict(orient="records")
    print(f"✅ 清洗完成，输出文件：{output_path}")

    return {
        "status": "success",
        "message": f"清洗完成，共 {len(df)} 条记录",
        "output_path": output_path,
        "preview": preview
    }


# ========== 工具函数 ==========
def safe_str(val):
    return str(val).strip() if pd.notna(val) else ""

def safe_int(val):
    try:
        return int(val)
    except:
        return 0

def convert_relative_time(time_str, download_time):
    """支持相对时间和标准时间的智能转换"""
    if pd.isna(time_str):
        return None
    if not download_time:
        return time_str
    base = pd.to_datetime(download_time)

    s = str(time_str)
    if re.match(r"\d+天前", s):
        return (base - timedelta(days=int(re.findall(r"\d+", s)[0]))).strftime("%Y-%m-%d %H:%M:%S")
    elif re.match(r"\d+小时前", s):
        return (base - timedelta(hours=int(re.findall(r"\d+", s)[0]))).strftime("%Y-%m-%d %H:%M:%S")
    elif re.match(r"\d+分钟前", s):
        return (base - timedelta(minutes=int(re.findall(r"\d+", s)[0]))).strftime("%Y-%m-%d %H:%M:%S")
    elif "刚刚" in s:
        return base.strftime("%Y-%m-%d %H:%M:%S")
    else:
        try:
            return pd.to_datetime(s).strftime("%Y-%m-%d %H:%M:%S")
        except:
            return None
