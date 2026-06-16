#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${SCRIPT_DIR}/sqldm.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "未找到 PID 文件，sqldm 可能未运行"
  exit 0
fi

PID="$(cat "${PID_FILE}")"
if kill -0 "${PID}" 2>/dev/null; then
  kill "${PID}"
  echo "已停止 sqldm (PID ${PID})"
else
  echo "进程 ${PID} 不存在"
fi

rm -f "${PID_FILE}"
