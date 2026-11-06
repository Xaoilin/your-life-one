#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash -n scripts/install-observability.sh scripts/deploy-ovh.sh
python3 -B scripts/test-observability-config.py
docker run --rm \
  -e YOUR_LIFE_OTLP_LISTEN=172.17.0.1:4318 -e YOUR_LIFE_HOST=validation \
  -e GRAFANA_OTLP_ENDPOINT=https://example.invalid/otlp -e GRAFANA_OTLP_AUTHORIZATION=validation \
  -v "$PWD/observability:/etc/your-life-one-observability:ro" \
  grafana/alloy:v1.21.0 validate /etc/your-life-one-observability/config.alloy
