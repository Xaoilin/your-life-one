#!/usr/bin/env python3
"""Exercise the pinned collector with synthetic secrets; no production credentials or traffic."""
import gzip
import http.server
import json
import os
from pathlib import Path
import platform
import socket
import subprocess
import sys
import tempfile
import threading
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
IMAGE = 'grafana/alloy:v1.21.0'
received = []
SHARED_HOST = '--shared-host' in sys.argv

class Sink(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        # The shared-host mode exercises the real product filter with content-free fixture series.
        body = ('postgresql_pg_stat_database_xact_commit{datname="your_life_one",application_name="NEVER_EXPORT"} 17\n'
                'postgresql_pg_stat_database_xact_commit{datname="partner_one",application_name="NEVER_EXPORT"} 42\n').encode()
        self.send_response(200)
        self.send_header('Content-Type', 'text/plain; version=0.0.4')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        if self.headers.get('Content-Encoding') == 'gzip':
            body = gzip.decompress(body)
        received.append((self.path, json.loads(body)))
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(b'{}')

    def log_message(self, *_args):
        pass

def command(*args):
    return subprocess.check_output(args, text=True).strip()

def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]

sink = http.server.ThreadingHTTPServer(('0.0.0.0', 0), Sink)
threading.Thread(target=sink.serve_forever, daemon=True).start()
name = 'yourlife-observability-proof-' + uuid.uuid4().hex[:8]
input_port = free_port()
admin_port = free_port()
with tempfile.TemporaryDirectory() as directory:
    folder = Path(directory)
    source_directory = ROOT / ('infrastructure/digitalocean' if SHARED_HOST else 'observability')
    for file in source_directory.iterdir():
        if file.is_file():
            (folder / file.name).write_bytes(file.read_bytes())
    source_name = 'collector.alloy' if SHARED_HOST else 'config.alloy'
    source = (folder / source_name).read_text()
    if SHARED_HOST:
        for name in ['probes.json', 'database-probes.json']:
            (folder / name).write_text('[]')
        (folder / 'managed-postgres-targets.json').write_text(json.dumps([{
            '__address__': f'host.docker.internal:{sink.server_port}', 'instance': 'synthetic-postgres',
        }]))
        # TLS is independently proven against the actual managed endpoint. This fixture tests relabel/export.
        source = source.replace('job_name = "yourlife-postgres"\n  scheme = "https"', 'job_name = "yourlife-postgres"\n  scheme = "http"')
        source = source.replace('metrics_path = "/metrics"\n  scrape_interval = "5m"\n  scrape_timeout = "10s"', 'metrics_path = "/metrics"\n  scrape_interval = "1s"\n  scrape_timeout = "1s"')
    source = source.replace('otelcol.exporter.otlphttp "cloud" {', 'otelcol.exporter.otlphttp "cloud" {\n  encoding = "json"')
    source = source.replace('/var/log/caddy/yourlife-access.log', '/etc/your-life-one-observability/edge.log')
    # Exercise actual embedded-exporter labels without waiting a production scrape interval.
    source = source.replace('job_name = "yourlife-collector"\n  scrape_interval = "60s"', 'job_name = "yourlife-collector"\n  scrape_interval = "15s"')
    (folder / source_name).write_text(source)
    (folder / 'edge.log').write_text('')
    try:
        extra = ['--add-host=host.docker.internal:host-gateway'] if platform.system() == 'Linux' else []
        command('docker', 'run', '-d', '--name', name, *extra,
                '-p', f'127.0.0.1:{input_port}:4318', '-p', f'127.0.0.1:{admin_port}:12345',
                '-e', 'YOUR_LIFE_OTLP_LISTEN=0.0.0.0:4318', '-e', 'YOUR_LIFE_HOST=synthetic-proof',
                '-e', f'GRAFANA_OTLP_ENDPOINT=http://host.docker.internal:{sink.server_port}',
                '-e', 'GRAFANA_OTLP_AUTHORIZATION=synthetic',
                '-e', 'DO_DATABASE_METRICS_USERNAME=synthetic', '-e', 'DO_DATABASE_METRICS_PASSWORD=synthetic',
                '-e', 'DO_DATABASE_CA_FILE=/etc/ssl/certs/ca-certificates.crt',
                '-v', f'{folder}:/etc/your-life-one-observability',
                IMAGE, 'run', '--server.http.listen-addr=0.0.0.0:12345', f'/etc/your-life-one-observability/{source_name}')
        import urllib.request
        deadline = time.monotonic() + 30
        while True:
            try:
                urllib.request.urlopen(f'http://127.0.0.1:{admin_port}/-/ready', timeout=1).read()
                break
            except Exception:
                if time.monotonic() > deadline:
                    raise AssertionError(command('docker', 'logs', name))
                time.sleep(.25)
        now = time.time_ns()
        payload = {'resourceSpans': [{'resource': {'attributes': [
            {'key': 'service.name', 'value': {'stringValue': 'synthetic-service'}},
            {'key': 'private', 'value': {'stringValue': 'NEVER_EXPORT'}}]},
            'scopeSpans': [{'spans': [{'traceId': '1' * 32, 'spanId': '2' * 16,
                'name': 'NEVER_EXPORT', 'kind': 2, 'startTimeUnixNano': str(now),
                'endTimeUnixNano': str(now + 1000000),
                'attributes': [{'key': 'db.statement', 'value': {'stringValue': 'NEVER_EXPORT'}},
                    {'key': 'uri', 'value': {'stringValue': '/safe/{id}'}},
                    {'key': 'status', 'value': {'stringValue': '503'}}],
                'status': {'code': 0, 'message': 'NEVER_EXPORT'},
                'events': [{'timeUnixNano': str(now), 'name': 'exception', 'attributes': [
                    {'key': 'exception.message', 'value': {'stringValue': 'NEVER_EXPORT'}},
                    {'key': 'exception.type', 'value': {'stringValue': 'SyntheticException'}}]}]}]}]}]}
        request = urllib.request.Request(f'http://127.0.0.1:{input_port}/v1/traces',
            data=json.dumps(payload).encode(), headers={'Content-Type': 'application/json'})
        urllib.request.urlopen(request, timeout=3).read()
        (folder / 'edge.log').write_text(json.dumps({'status': 503, 'duration': 1.25, 'size': 42,
            'request': {'uri': '/NEVER_EXPORT', 'headers': {'Authorization': 'NEVER_EXPORT'}}}) + '\n')
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            if all(any(path == expected for path, _ in received) for expected in ['/v1/traces', '/v1/logs', '/v1/metrics']):
                break
            time.sleep(.2)
        traces = [body for path, body in received if path == '/v1/traces']
        logs = [body for path, body in received if path == '/v1/logs']
        metrics = [body for path, body in received if path == '/v1/metrics']
        assert traces and logs and metrics, command('docker', 'logs', name)
        collector_resources = [resource for batch in metrics for resource in batch['resourceMetrics']
            if any(attribute['key'] == 'service.name' and attribute['value'].get('stringValue') == 'yourlife-collector'
                for attribute in resource['resource']['attributes'])]
        assert any(metric['name'] == 'up' for resource in collector_resources
            for scope in resource['scopeMetrics'] for metric in scope['metrics']), 'Embedded exporter job must match dashboard/alert selectors'
        assert 'NEVER_EXPORT' not in json.dumps(received), 'Private fields escaped the collector'
        if SHARED_HOST:
            assert 'your_life_one' in json.dumps(metrics), 'Native PostgreSQL series were not exported'
            assert 'partner_one' not in json.dumps(metrics), 'Sibling database metrics escaped the product boundary'
        assert '/safe/{id}' in json.dumps(traces)
        assert 'SyntheticException' in json.dumps(traces)
        exported_spans = [span for batch in traces for resource in batch['resourceSpans']
            for scope in resource['scopeSpans'] for span in scope['spans']]
        assert exported_spans[0]['status']['code'] == 2, 'Native HTTP 5xx must be retained as errors'
        assert '503' in json.dumps(logs)
        assert 'service.name' in json.dumps(logs) and 'caddy' in json.dumps(logs)
        print(json.dumps({'result': 'passed', 'collector': IMAGE, 'trace_batches': len(traces),
            'log_batches': len(logs), 'metric_batches': len(metrics), 'collector_job': 'yourlife-collector', 'private_fields_exported': False, 'shared_host': SHARED_HOST}))
    finally:
        subprocess.run(['docker', 'rm', '-f', name], check=False, stdout=subprocess.DEVNULL)
        sink.shutdown()
