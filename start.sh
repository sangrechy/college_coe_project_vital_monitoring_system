#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/v2/interface/backend"
FRONTEND_DIR="$ROOT_DIR/v2/interface/frontend"

# Ensure Node/npm is on PATH (includes local devtools fallback)
if ! command -v npm >/dev/null 2>&1; then
  export PATH="/home/sangrechy/main/devtools/node/bin:$PATH"
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "[ERROR] npm not found. Please install Node.js or add it to PATH."
  exit 1
fi

# Clean shutdown of both processes on Ctrl+C
cleanup() {
  echo ""
  echo "Stopping Backend and Frontend..."
  [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  exit 0
}
trap cleanup INT TERM

echo "================================================="
echo "  Edge AI Wearable Health Monitor — Launcher"
echo "================================================="

# 1. Setup Backend venv if needed
if [ ! -d "$BACKEND_DIR/venv" ]; then
  echo "[1/3] Creating Python virtual environment for backend..."
  python3 -m venv "$BACKEND_DIR/venv"
  "$BACKEND_DIR/venv/bin/pip" install --upgrade pip -q
  "$BACKEND_DIR/venv/bin/pip" install -r "$BACKEND_DIR/requirements.txt"
fi

# 2. Setup Frontend dependencies if needed
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
  echo "[2/3] Installing React frontend dependencies..."
  (cd "$FRONTEND_DIR" && npm install)
fi

# 3. Start FastAPI + BLE Backend
echo "[3/3] Starting FastAPI + BLE Backend (http://localhost:8000)..."
(cd "$BACKEND_DIR" && "$BACKEND_DIR/venv/bin/python3" main.py) &
BACKEND_PID=$!

sleep 2

# 4. Start React Dashboard
echo "Starting React Dashboard (http://localhost:3000)..."
echo "Press Ctrl+C to stop both services."
echo "================================================="
(cd "$FRONTEND_DIR" && npm start) &
FRONTEND_PID=$!

wait "$BACKEND_PID" "$FRONTEND_PID"
