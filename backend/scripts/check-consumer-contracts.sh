#!/usr/bin/env bash
# Preserve the established entry point; the recursive comparison also covers native MCP contracts.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "$repo_root/scripts/check-consumer-contracts.py"
