#!/usr/bin/env bash
# Deploy one service to an OVH VPS over SSH: Docker container behind Caddy,
# with an auto-renewing Let's Encrypt certificate for the server's bare IP.
# Several services can share one server: each gets its own local port, and
# Caddy routes /api/<service>/* to it.
# Usage: scripts/deploy-ovh.sh <prayer|calendar|profile|life|knowledge|planner|finance|live> <server-ip> [ssh-user]
# Database settings must already exist on the server in /etc/your-life-one/db.env
# (set them with scripts/ovh-set-db-env.sh, which restarts the service); this
# script never reads them. Every service writes through the server's RabbitMQ,
# which this script starts once per server.
set -euo pipefail

usage="usage: $0 <prayer|calendar|profile|life|knowledge|planner|finance|live> <server-ip> [ssh-user]"
service="${1:?$usage}"
ip="${2:?$usage}"
user="${3:-ubuntu}"
case "$service" in
  prayer|calendar) port=8080 ;;
  # profile shares the prayer server and life the calendar server, each on the second port.
  profile|life) port=8081 ;;
  # knowledge shares the prayer server on the third port.
  knowledge) port=8082 ;;
  # the live-update gateway runs on every server, streaming that server's change events.
  live) port=8083 ;;
  # planner shares the calendar server.
  planner) port=8084 ;;
  # finance shares the prayer server on the fifth port.
  finance) port=8085 ;;
  *) echo "$usage" >&2; exit 1 ;;
esac

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
target="$user@$ip"
name="your-life-one-$service-service"

src_dir="your-life-one-services-$service"
rsync -az --delete --exclude .git --exclude '**/target' "$repo_root/" "$target:~/$src_dir/"

# In CI the database settings arrive as environment variables (fetched from
# Bitwarden Secrets Manager); stream them straight into the server's env file.
if [[ -n "${SPRING_DATASOURCE_URL:-}" ]]; then
  # Each service writes its own temporary file, so services sharing a server never race.
  tmp_env="/etc/your-life-one/db.env.$service.tmp"
  printf 'SPRING_DATASOURCE_URL=%s\nSPRING_DATASOURCE_USERNAME=%s\nSPRING_DATASOURCE_PASSWORD=%s\n' \
    "$SPRING_DATASOURCE_URL" "${SPRING_DATASOURCE_USERNAME:?}" "${SPRING_DATASOURCE_PASSWORD:?}" |
    ssh "$target" "sudo install -d -m 700 /etc/your-life-one &&
      sudo tee '$tmp_env' >/dev/null && sudo chmod 600 '$tmp_env' &&
      sudo mv '$tmp_env' /etc/your-life-one/db.env"
fi

# Grafana Cloud metrics settings, when the CI provides them, go to /etc/your-life-one/metrics.env.
if [[ -n "${GRAFANA_OTLP_AUTHORIZATION:-}" ]]; then
  tmp_metrics="/etc/your-life-one/metrics.env.$service.tmp"
  printf 'GRAFANA_METRICS_ENABLED=true\nGRAFANA_OTLP_METRICS_URL=%s\nGRAFANA_OTLP_AUTHORIZATION=%s\n' \
    "${GRAFANA_OTLP_METRICS_URL:?}" "$GRAFANA_OTLP_AUTHORIZATION" |
    ssh "$target" "sudo install -d -m 700 /etc/your-life-one &&
      sudo tee '$tmp_metrics' >/dev/null && sudo chmod 600 '$tmp_metrics' &&
      sudo mv '$tmp_metrics' /etc/your-life-one/metrics.env"
fi

# The calendar service holds the Google OAuth client; when the CI provides it, it goes to
# /etc/your-life-one/google-oauth.env, which only the calendar container reads.
if [[ "$service" == calendar && -n "${GOOGLE_OAUTH_CLIENT_ID:-}" ]]; then
  tmp_oauth="/etc/your-life-one/google-oauth.env.$service.tmp"
  printf 'GOOGLE_OAUTH_CLIENT_ID=%s\nGOOGLE_OAUTH_CLIENT_SECRET=%s\n' \
    "$GOOGLE_OAUTH_CLIENT_ID" "${GOOGLE_OAUTH_CLIENT_SECRET:?}" |
    ssh "$target" "sudo install -d -m 700 /etc/your-life-one &&
      sudo tee '$tmp_oauth' >/dev/null && sudo chmod 600 '$tmp_oauth' &&
      sudo mv '$tmp_oauth' /etc/your-life-one/google-oauth.env"
fi

ssh "$target" SERVICE="$service" IP="$ip" NAME="$name" PORT="$port" SRC_DIR="$src_dir" 'bash -s' <<'REMOTE'
set -euo pipefail

# A freshly installed VPS runs unattended upgrades that hold the apt lock.
timeout 900 bash -c 'while sudo fuser /var/lib/dpkg/lock-frontend /var/lib/dpkg/lock >/dev/null 2>&1; do sleep 5; done' || true

if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
fi
if ! command -v caddy >/dev/null; then
  sudo apt-get update -qq
  sudo apt-get install -y -qq debian-keyring debian-archive-keyring apt-transport-https curl gpg
  curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/gpg.key | sudo gpg --dearmor --yes -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt | sudo tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
  sudo apt-get update -qq && sudo apt-get install -y -qq caddy
fi

if ! sudo test -s /etc/your-life-one/db.env; then
  echo "WARNING: /etc/your-life-one/db.env is empty; database health will fail until scripts/ovh-set-db-env.sh is run." >&2
  sudo install -d -m 700 /etc/your-life-one && sudo touch /etc/your-life-one/db.env && sudo chmod 600 /etc/your-life-one/db.env
fi

# Writes go through RabbitMQ: one broker per server, reachable only on the private
# your-life-one Docker network. Its password is generated on the server and never leaves it.
# Services sharing a server deploy in parallel, so setup runs under a lock.
service_args=()
if [[ "$SERVICE" == prayer || "$SERVICE" == profile || "$SERVICE" == calendar || "$SERVICE" == life || "$SERVICE" == knowledge || "$SERVICE" == planner || "$SERVICE" == finance || "$SERVICE" == live ]]; then
  sudo install -d -m 700 /etc/your-life-one
  sudo flock /etc/your-life-one/rabbitmq.lock bash -euo pipefail -c '
    docker network inspect your-life-one >/dev/null 2>&1 || docker network create your-life-one >/dev/null
    if [ ! -s /etc/your-life-one/rabbitmq.env ]; then
      password="$(openssl rand -hex 24)"
      umask 077
      printf "RABBITMQ_DEFAULT_USER=yourlife\nRABBITMQ_DEFAULT_PASS=%s\nSPRING_RABBITMQ_HOST=your-life-one-rabbitmq\nSPRING_RABBITMQ_USERNAME=yourlife\nSPRING_RABBITMQ_PASSWORD=%s\n" \
        "$password" "$password" > /etc/your-life-one/rabbitmq.env
    fi
    if ! docker container inspect your-life-one-rabbitmq >/dev/null 2>&1; then
      docker run -d --name your-life-one-rabbitmq --restart unless-stopped --memory 384m --log-opt max-size=10m --log-opt max-file=3 \
        --network your-life-one --hostname your-life-one-rabbitmq \
        --env-file /etc/your-life-one/rabbitmq.env \
        -v your-life-one-rabbitmq:/var/lib/rabbitmq rabbitmq:4-alpine >/dev/null
    fi
    docker start your-life-one-rabbitmq >/dev/null
  '
  service_args=(--network your-life-one --env-file /etc/your-life-one/rabbitmq.env)
fi

# The calendar service encrypts stored Google refresh tokens with a key generated here once; like the
# RabbitMQ password it never leaves the server. Losing it means every user reconnects Google.
calendar_args=()
if [[ "$SERVICE" == calendar ]]; then
  sudo install -d -m 700 /etc/your-life-one
  sudo flock /etc/your-life-one/calendar-credential-key.lock bash -euo pipefail -c '
    if [ ! -s /etc/your-life-one/calendar-credential-key.env ]; then
      umask 077
      printf "CALENDAR_GOOGLE_CREDENTIAL_KEY=%s\n" "$(openssl rand -base64 32)" \
        > /etc/your-life-one/calendar-credential-key.env.tmp
      mv /etc/your-life-one/calendar-credential-key.env.tmp /etc/your-life-one/calendar-credential-key.env
    fi
  '
  calendar_args=(--env-file /etc/your-life-one/calendar-credential-key.env)
  if sudo test -s /etc/your-life-one/google-oauth.env; then
    calendar_args+=(--env-file /etc/your-life-one/google-oauth.env)
  else
    echo "WARNING: /etc/your-life-one/google-oauth.env is missing; Google Calendar stays unavailable until the deploy provides GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET." >&2
  fi
fi

cd ~/"$SRC_DIR"
# Collector setup is serialized per host and validated before any service uses it.
if sudo test -s /etc/your-life-one/metrics.env; then
  sudo env YOUR_LIFE_PUBLIC_IP="$IP" scripts/install-observability.sh
fi
# Each build runs a full Maven reactor; services sharing a server deploy in parallel, and several builds at
# once exhausted a 4 GB server's memory (the kernel killed a running service, 2026-09-27). Build one at a time.
sudo flock /etc/your-life-one/docker-build.lock \
  docker build -q -f "$(readlink -f "services/$SERVICE-service/Dockerfile")" -t "$NAME:latest" .
sudo docker rm -f "$NAME" >/dev/null 2>&1 || true
# The memory cap lets several JVMs share one server (heap = 75% of the cap).
metrics_args=()
if sudo test -s /etc/your-life-one/metrics.env; then
  metrics_args=(--env-file /etc/your-life-one/metrics.env)
  if sudo test -s /etc/your-life-one-observability/service.env; then
    metrics_args+=(--env-file /etc/your-life-one-observability/service.env)
  fi
fi
sudo docker run -d --name "$NAME" --restart unless-stopped --memory 1g --log-opt max-size=10m --log-opt max-file=3 \
  --env-file /etc/your-life-one/db.env "${service_args[@]}" "${metrics_args[@]}" "${calendar_args[@]}" \
  -e APP_CORS_ALLOWED_ORIGINS=https://your-life-one.example \
  -e APP_SERVICE_INSTANCE_ID="$SERVICE-$IP" \
  -p "127.0.0.1:$PORT:8080" "$NAME:latest" >/dev/null

# One route file per service on this server; the site imports them all. The live-update gateway streams
# server-sent events, so Caddy passes each one on at once instead of buffering the response.
sudo install -d /etc/caddy/your-life-one
if [[ "$SERVICE" == live ]]; then
  printf 'handle /api/%s/* {\n  reverse_proxy 127.0.0.1:%s {\n    flush_interval -1\n  }\n}\n' "$SERVICE" "$PORT"
else
  printf 'handle /api/%s/* {\n  reverse_proxy 127.0.0.1:%s\n}\n' "$SERVICE" "$PORT"
fi | sudo tee "/etc/caddy/your-life-one/$SERVICE.caddy.tmp" >/dev/null
sudo mv "/etc/caddy/your-life-one/$SERVICE.caddy.tmp" "/etc/caddy/your-life-one/$SERVICE.caddy"

# Let's Encrypt IP certificates are only issued under the short-lived profile;
# Caddy renews them automatically.
sudo tee "/etc/caddy/Caddyfile.$SERVICE.tmp" >/dev/null <<CADDY
{
  metrics
}
https://$IP {
  log {
    output file /var/log/caddy/yourlife-access.log {
      roll_size 10MiB
      roll_keep 3
      roll_keep_for 72h
    }
    format filter {
      wrap json
      fields {
        request>headers delete
        request>uri delete
        request>remote_ip delete
        request>remote_port delete
        request>client_ip delete
        resp_headers delete
      }
    }
  }
  tls {
    issuer acme {
      profile shortlived
    }
  }
  import /etc/caddy/your-life-one/*.caddy
  handle {
    respond 404
  }
}
CADDY
sudo mv "/etc/caddy/Caddyfile.$SERVICE.tmp" /etc/caddy/Caddyfile
sudo systemctl reload caddy || sudo systemctl restart caddy

if command -v ufw >/dev/null && sudo ufw status | grep -q active; then
  sudo ufw allow 80/tcp && sudo ufw allow 443/tcp
fi
REMOTE

echo "Deployed $name to https://$ip"
