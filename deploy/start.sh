#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/config.env"
PID_FILE="${SCRIPT_DIR}/sqldm.pid"
LOG_DIR="${SCRIPT_DIR}/logs"
LOG_FILE="${LOG_DIR}/app.log"

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "缺少 config.env，请先复制 config.env.example 并填写数据库连接："
  echo "  cp ${SCRIPT_DIR}/config.env.example ${CONFIG_FILE}"
  exit 1
fi

# shellcheck disable=SC1090
source "${CONFIG_FILE}"

if [[ -f "${SCRIPT_DIR}/sqldm.jar" ]]; then
  JAR="${SCRIPT_DIR}/sqldm.jar"
elif [[ -f "${SCRIPT_DIR}/../target/sqldm-1.0-SNAPSHOT.jar" ]]; then
  JAR="${SCRIPT_DIR}/../target/sqldm-1.0-SNAPSHOT.jar"
else
  echo "未找到 sqldm.jar，请先执行: mvn clean package -DskipTests"
  exit 1
fi

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  echo "sqldm 已在运行 (PID $(cat "${PID_FILE}"))"
  exit 0
fi

mkdir -p "${LOG_DIR}"

export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:?请在 config.env 中设置 SPRING_DATASOURCE_URL}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:?请在 config.env 中设置 SPRING_DATASOURCE_USERNAME}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:?请在 config.env 中设置 SPRING_DATASOURCE_PASSWORD}"
export SERVER_PORT="${SERVER_PORT:-8080}"

nohup java -jar "${JAR}" > "${LOG_FILE}" 2>&1 &
echo $! > "${PID_FILE}"

echo "sqldm 已启动 (PID $(cat "${PID_FILE}"))"
echo "端口: ${SERVER_PORT}"
echo "日志: ${LOG_FILE}"
echo "健康检查: http://localhost:${SERVER_PORT}/actuator/health"
