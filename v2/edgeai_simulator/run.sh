#!/bin/bash
set -e

echo "Installing Backend Dependencies..."
pip install -r requirements.txt

echo "Starting FastAPI Server at http://localhost:8000..."
python main.py
