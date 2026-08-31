#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_JAR="$ROOT_DIR/renovatio-api/target/renovatio-api.jar"
MCP_JAR="$ROOT_DIR/renovatio-mcp-server/target/renovatio-mcp-server-0.0.1-SNAPSHOT.jar"
API_PORT=8080
MCP_PORT=8082
API_PID_FILE="/tmp/renovatio-api.pid"
MCP_PID_FILE="/tmp/renovatio-mcp.pid"
API_LOG="/tmp/renovatio-api.log"
MCP_LOG="/tmp/renovatio-mcp.log"

usage() {
  cat <<'EOF'
Usage:
  scripts/dev-stack.sh restart   Rebuild, stop existing servers, and start API + MCP
  scripts/dev-stack.sh start     Start API + MCP without rebuilding
  scripts/dev-stack.sh stop      Stop both servers
  scripts/dev-stack.sh status    Show status and health of both servers
  scripts/dev-stack.sh logs      Tail both log files
EOF
}

stop_port() {
  local port="$1"
  local pids
  pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -n "$pids" ]]; then
    echo "Stopping processes on port ${port}: ${pids//$'\n'/, }"
    kill $pids 2>/dev/null || true
    sleep 2
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    if [[ -n "$pids" ]]; then
      echo "Force-stopping processes on port ${port}: ${pids//$'\n'/, }"
      kill -9 $pids 2>/dev/null || true
    fi
  fi
}

stop_pidfile() {
  local pidfile="$1"
  if [[ -f "$pidfile" ]]; then
    local pid
    pid="$(cat "$pidfile" 2>/dev/null || true)"
    if [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null; then
      echo "Stopping process from ${pidfile}: ${pid}"
      kill "$pid" 2>/dev/null || true
      sleep 2
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null || true
      fi
    fi
    rm -f "$pidfile"
  fi
}

build() {
  echo "Building API and MCP jars (this also rebuilds the UI assets)..."
  mvn -q -pl renovatio-api,renovatio-mcp-server -am package -DskipTests -Djacoco.skip=true
}

start_api() {
  if [[ ! -f "$API_JAR" ]]; then
    echo "Missing API jar: $API_JAR"
    exit 1
  fi

  nohup java -jar "$API_JAR" >"$API_LOG" 2>&1 &
  echo $! > "$API_PID_FILE"
  echo "API started on http://127.0.0.1:${API_PORT} (pid $(cat "$API_PID_FILE"))"
}

start_mcp() {
  if [[ ! -f "$MCP_JAR" ]]; then
    echo "Missing MCP jar: $MCP_JAR"
    exit 1
  fi

  nohup java -jar "$MCP_JAR" >"$MCP_LOG" 2>&1 &
  echo $! > "$MCP_PID_FILE"
  echo "MCP started on http://127.0.0.1:${MCP_PORT} (pid $(cat "$MCP_PID_FILE"))"
}

wait_for_http() {
  local label="$1"
  local url="$2"
  local header="${3:-}"
  local timeout_seconds=90
  local elapsed=0

  while (( elapsed < timeout_seconds )); do
    if [[ -n "$header" ]]; then
      if curl -fsS "$url" -H "$header" >/dev/null 2>&1; then
        echo "$label is ready"
        return 0
      fi
    else
      if curl -fsS "$url" >/dev/null 2>&1; then
        echo "$label is ready"
        return 0
      fi
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "$label did not become ready in time"
  return 1
}

status() {
  echo "API:"
  if lsof -tiTCP:"$API_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "  port ${API_PORT}: listening"
  else
    echo "  port ${API_PORT}: not listening"
  fi
  if curl -fsS -H 'X-Role: ADMIN' "http://127.0.0.1:${API_PORT}/api/projects" >/dev/null 2>&1; then
    echo "  /api/projects: OK"
  else
    echo "  /api/projects: not ready"
  fi

  echo "MCP:"
  if lsof -tiTCP:"$MCP_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "  port ${MCP_PORT}: listening"
  else
    echo "  port ${MCP_PORT}: not listening"
  fi
  if curl -fsS "http://127.0.0.1:${MCP_PORT}/actuator/health" >/dev/null 2>&1; then
    echo "  /actuator/health: OK"
  else
    echo "  /actuator/health: not ready"
  fi
}

tail_logs() {
  touch "$API_LOG" "$MCP_LOG"
  tail -f "$API_LOG" "$MCP_LOG"
}

command="${1:-restart}"

case "$command" in
  restart)
    build
    stop_port "$API_PORT"
    stop_port "$MCP_PORT"
    stop_pidfile "$API_PID_FILE"
    stop_pidfile "$MCP_PID_FILE"
    start_api
    start_mcp
    wait_for_http "API" "http://127.0.0.1:${API_PORT}/api/projects" 'X-Role: ADMIN'
    wait_for_http "MCP" "http://127.0.0.1:${MCP_PORT}/actuator/health"
    echo
    echo "Dashboard: http://127.0.0.1:${API_PORT}"
    echo "MCP health: http://127.0.0.1:${MCP_PORT}/actuator/health"
    echo "Logs:"
    echo "  $API_LOG"
    echo "  $MCP_LOG"
    ;;
  start)
    stop_port "$API_PORT"
    stop_port "$MCP_PORT"
    stop_pidfile "$API_PID_FILE"
    stop_pidfile "$MCP_PID_FILE"
    start_api
    start_mcp
    ;;
  stop)
    stop_port "$API_PORT"
    stop_port "$MCP_PORT"
    stop_pidfile "$API_PID_FILE"
    stop_pidfile "$MCP_PID_FILE"
    ;;
  status)
    status
    ;;
  logs)
    tail_logs
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "Unknown command: $command"
    usage
    exit 1
    ;;
esac
