#!/usr/bin/env bash
# Run on the host from the reviewed repository. Secrets are read only from its existing root-only env.
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ "$EUID" == 0 ]] || { echo "Run with sudo" >&2; exit 1; }
install -d -m 700 /etc/your-life-one-observability /var/lib/your-life-one-observability
exec 9>/etc/your-life-one/observability.lock
flock 9
if ! test -x /usr/local/bin/yourlife-alloy || ! /usr/local/bin/yourlife-alloy --version | head -1 | grep -q v1.21.0; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "$tmp_dir"' EXIT
  curl -fsSL --retry 3 https://github.com/grafana/alloy/releases/download/v1.21.0/alloy-linux-amd64.zip -o "$tmp_dir/alloy.zip"
  echo "2348c2342ab09dd865c33135aa28347618c41faf964149649e66b553eb4cbab3  $tmp_dir/alloy.zip" | sha256sum -c -
  python3 - "$tmp_dir" <<'ZIP'
import sys, zipfile
from pathlib import Path
root = Path(sys.argv[1])
with zipfile.ZipFile(root / 'alloy.zip') as archive:
    (root / 'alloy').write_bytes(archive.read('alloy-linux-amd64'))
ZIP
  install -m 755 "$tmp_dir/alloy" /usr/local/bin/yourlife-alloy
fi
before="$(sha256sum /etc/your-life-one-observability/* /etc/systemd/system/your-life-one-observability.service 2>/dev/null || true)"
install -m 600 "$repo_root/observability/config.alloy" "$repo_root/observability/blackbox.yml" /etc/your-life-one-observability/
install -m 644 "$repo_root/observability/your-life-one-observability.service" /etc/systemd/system/
python3 - <<'CONFIG'
import json, os, subprocess
from pathlib import Path
root = Path('/etc/your-life-one-observability')
values = dict(line.split('=', 1) for line in Path('/etc/your-life-one/metrics.env').read_text().splitlines() if '=' in line)
network = json.loads(subprocess.check_output(['docker', 'network', 'inspect', 'your-life-one']))[0]
gateway = network['IPAM']['Config'][0]['Gateway']
host = subprocess.check_output(['hostname'], text=True).strip()
base = values['GRAFANA_OTLP_METRICS_URL'].removesuffix('/v1/metrics')
(root / 'collector.env').write_text(f"YOUR_LIFE_HOST={host}\nYOUR_LIFE_OTLP_LISTEN={gateway}:4318\nGRAFANA_OTLP_ENDPOINT={base}\nGRAFANA_OTLP_AUTHORIZATION={values['GRAFANA_OTLP_AUTHORIZATION']}\n")
(root / 'service.env').write_text(f"GRAFANA_OTLP_METRICS_URL=http://{gateway}:4318/v1/metrics\nGRAFANA_TRACING_ENABLED=true\nGRAFANA_OTLP_TRACES_URL=http://{gateway}:4318/v1/traces\n")
# This expected inventory is independent of container discovery: a missing service stays visible.
ip = os.environ['YOUR_LIFE_PUBLIC_IP']
services = ['prayer', 'profile', 'knowledge', 'finance', 'live'] if ip == '192.0.2.10' else ['calendar', 'life', 'planner', 'live']
def targets(database=False):
    return [dict(name=f"{service}{'-database' if database else ''}", address=f"https://{ip}/api/{service}/health" + ('/database' if database else ''), module='http_health', service=f'{service}-service') for service in services if not database or service != 'live']
probes = targets() + [dict(name='web', address='https://your-life-one.example/yourlifeone/', module='http_page', service='web')]
if ip == '192.0.2.10':
    probes.append(dict(name='supabase-auth', address='https://example-project.supabase.co/auth/v1/health', module='http_supabase_auth', service='supabase-auth'))
(root / 'probes.json').write_text(json.dumps(probes))
(root / 'database-probes.json').write_text(json.dumps(targets(True)))
provider_file = Path('/etc/your-life-one/supabase-metrics.env')
provider = dict(line.split('=', 1) for line in provider_file.read_text().splitlines() if '=' in line) if provider_file.exists() else {}
# JSON strings are valid YAML scalars; credentials cannot alter the module structure.
blackbox = root / 'blackbox.yml'
blackbox.write_text(blackbox.read_text().replace('__YOUR_LIFE_SUPABASE_APIKEY__', json.dumps(provider.get('SUPABASE_METRICS_KEY', ''))))
with (root / 'collector.env').open('a') as output:
    output.write('SUPABASE_METRICS_KEY=' + provider.get('SUPABASE_METRICS_KEY', '') + '\n')
(root / 'provider-targets.json').write_text(json.dumps([{'__address__': 'example-project.supabase.co:443'}] if ip == '192.0.2.10' else []))

for path in root.iterdir():
    path.chmod(0o600)
CONFIG
# Existing broker has this plugin already; enabling is idempotent and needs no restart.
docker exec your-life-one-rabbitmq rabbitmq-plugins enable rabbitmq_prometheus >/dev/null
# EnvironmentFile values are data, not shell code (Basic auth contains a space).
python3 "$repo_root/scripts/validate-observability-config.py" /usr/local/bin/yourlife-alloy \
  /etc/your-life-one-observability/collector.env /etc/your-life-one-observability/config.alloy
systemctl daemon-reload
after="$(sha256sum /etc/your-life-one-observability/* /etc/systemd/system/your-life-one-observability.service)"
if [[ "$before" != "$after" ]] || ! systemctl is-active --quiet your-life-one-observability; then
  systemctl enable your-life-one-observability >/dev/null
  systemctl restart your-life-one-observability
fi
curl -fsS --retry 10 --retry-delay 1 --retry-connrefused http://127.0.0.1:12345/-/ready >/dev/null
