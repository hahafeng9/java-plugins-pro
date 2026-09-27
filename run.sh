#!/bin/bash
# 伪装进程名启动脚本：让 ps 看到的进程名不再是 "java -jar xxx.jar"
# 用法：./run.sh
# 可通过环境变量 PROC_NAME 自定义伪装名（默认 worker-service）
set -e
PROC_NAME="${PROC_NAME:-worker-service}"
JAR_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$JAR_DIR/EssentialsX-1.21.11.jar"
if [ ! -f "$JAR" ]; then
  echo "找不到 $JAR，请先把 Release 的 jar 和本脚本放在同一目录" >&2
  exit 1
fi
export PROC_NAME
# exec -a 伪装 argv[0]：ps aux 显示为 "$PROC_NAME -jar ..." 而不是 java
exec -a "$PROC_NAME" java -jar "$JAR" "$@"
