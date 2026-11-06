#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if ! command -v doctl >/dev/null 2>&1; then
  echo "doctl is required. Install the DigitalOcean CLI and authenticate it first." >&2
  exit 1
fi

if ! doctl account get >/dev/null 2>&1; then
  echo "doctl is not authenticated. Run 'doctl auth init' with a DigitalOcean API token, then retry." >&2
  exit 1
fi

doctl apps propose --spec .do/app.yaml

if [[ -n "${DO_APP_ID:-}" ]]; then
  doctl apps update "$DO_APP_ID" --spec .do/app.yaml --update-sources --wait
else
  doctl apps create --spec .do/app.yaml --wait
fi
