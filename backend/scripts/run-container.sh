#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
docker build -f services/prayer-service/Dockerfile -t your-life-one-prayer-service:local .
exec docker run --rm --name your-life-one-prayer-service -p 8080:8080 your-life-one-prayer-service:local
