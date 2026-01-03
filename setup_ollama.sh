#!/bin/bash

echo "Checking if Ollama is already installed..."
if command -v ollama >/dev/null 2>&1; then
  echo "Ollama is installed. Skipping install."
else
  echo "Installing Ollama with Homebrew..."
  brew install ollama || { echo "Brew install failed!"; exit 1; }
fi

echo "Pulling the llama3.2 model..."
ollama pull llama3.2 || { echo "Failed to pull model!"; exit 1; }

# Check if something is using port 11434
echo "Checking if port 11434 is busy..."
if lsof -i :11434 >/dev/null 2>&1; then
  echo "Port 11434 is in use! Let's see what's there:"
  lsof -i :11434
  brew services stop ollama || echo "No service to stop, or it failed."
  echo "Killing any processes on port 11434 (be careful!)"
  pkill -f ollama || echo "Nothing to kill."
  sleep 2  # Wait a sec
fi

echo "Starting Ollama in the background..."
OLLAMA_FLASH_ATTENTION="1" OLLAMA_KV_CACHE_TYPE="q8_0" /opt/homebrew/opt/ollama/bin/ollama serve &
OLLAMA_PID=$!
sleep 3  # Give it time to start

# Check if it started okay
if ps -p $OLLAMA_PID >/dev/null 2>&1; then
  echo "Ollama is running! PID: $OLLAMA_PID"
  echo "Testing with 'Hello!'"
  ollama run llama3.2 "Hello!" || echo "Test failed, but Ollama might still be up."
else
  echo "Failed to start Ollama. Check logs or try manually."
  exit 1
fi

echo "All done! Ollama should be good to go."
echo "To stop it later: kill $OLLAMA_PID"