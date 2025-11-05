@echo off
title Douyin_CAC 服务启动器
color 0a

echo ===========================================
echo     🚀 抖音评论舆情分析系统 一键启动脚本
echo ===========================================

:: --- 启动 Neo4j ---
echo [1/3] 启动 Neo4j...
start cmd /k "cd /d D:\neo4j-community-5.26.12\bin && neo4j.bat console"

:: 等待5秒再启动下一个
timeout /t 5 /nobreak >nul

:: --- 启动 Elasticsearch ---
echo [2/3] 启动 Elasticsearch...
start cmd /k "cd /d D:\elasticsearch\elasticsearch-9.1.5\bin && elasticsearch.bat"

:: 等待10秒确保ES启动
timeout /t 10 /nobreak >nul

:: --- 启动 Flask 服务 ---
echo [3/3] 启动 Flask（Python）服务...
start cmd /k "cd /d D:\MyVue\douyin CAC\python_service && python app.py"

echo -------------------------------------------
echo ✅ 所有服务已启动！
echo 若需停止，请手动关闭对应的命令行窗口。
echo -------------------------------------------
pause
