#!/usr/bin/env python3
"""Private, encrypted pre-cutover backups. Never prints records, identity links or credentials."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

PRODUCTS = {'your-life-one': 'aeiijptebzenjhgcntup'}
# These application-owned schemas exclude AgentBoard, Storage, Vault and Supabase internals.
SCHEMAS = ['prayer', 'profile', 'calendar', 'activity', 'inventory', 'jobs', 'trips', 'health',
           'knowledge', 'lifestyle', 'projects', 'tasks', 'clock', 'gamification', 'life', 'finance',
           'equity', 'finance_service', 'knowledge_service', 'planner_service']
REMOTE = r'''
import json, os, subprocess, sys
from pathlib import Path
from urllib.parse import urlparse, parse_qs
product, project, schemas, mode = json.loads(sys.argv[1])
def load(name):
    values = {}
    for line in Path('/etc', product, name).read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            values[key] = value.strip().strip('"').strip("'")
    return values
config = load('db.env')
url = urlparse(config['SPRING_DATASOURCE_URL'].removeprefix('jdbc:'))
user = config['SPRING_DATASOURCE_USERNAME']
if project not in (url.hostname or '') and not user.endswith('.' + project):
    raise SystemExit('Source identity did not match the selected product.')
environment = os.environ.copy()
environment.update(PGHOST=url.hostname, PGPORT=str(url.port or 5432), PGDATABASE=url.path.lstrip('/'),
                   PGUSER=user, PGPASSWORD=config['SPRING_DATASOURCE_PASSWORD'], PGSSLMODE='require')
command = ['docker', 'run', '--rm', '--name', product + '-retirement-backup',
           '--label', 'sdlc.task=supabase-retirement-backup']
for key in ['PGHOST','PGPORT','PGDATABASE','PGUSER','PGPASSWORD','PGSSLMODE']:
    command += ['--env', key]
command += ['postgres:17-alpine']
if mode == 'dump':
    command += ['pg_dump', '--format=custom', '--no-owner', '--no-privileges']
    command += ['--schema=' + schema for schema in schemas]
elif mode == 'legacy':
    command += ['pg_dump', '--format=custom', '--no-owner', '--no-privileges',
                '--table=public.yourlifeone_*', '--table=public.product_usage_events']
else:
    owner = load('prayer.env')['APP_PRAYER_COMPETITION_OWNER_ID']
    import uuid
    owner = str(uuid.UUID(owner))
    # Only the product's explicitly configured initial owner receives an enabled native login link.
    query = "COPY (SELECT user_id,provider,coalesce(identity_data->>'sub',provider_id) AS subject,identity_data->>'email' AS email " \
            "FROM auth.identities WHERE provider='google' AND user_id='" + owner + "'::uuid) TO STDOUT WITH CSV HEADER"
    command += ['psql', '-X', '--set=ON_ERROR_STOP=1', '--command', query]
result = subprocess.run(command, env=environment, stdout=sys.stdout.buffer, stderr=subprocess.PIPE)
if result.returncode:
    # Never forward an unexpected SQL/driver diagnostic that may contain account content.
    raise SystemExit('The private source export failed; no database was changed.')
'''

def private_write(path, data):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, 'wb') as stream:
        stream.write(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', choices=PRODUCTS, required=True)
    parser.add_argument('--known-hosts', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    os.umask(0o077)
    args.output.mkdir(parents=True, exist_ok=True, mode=0o700)
    if args.output.stat().st_mode & 0o077:
        raise SystemExit('The backup directory must be private (mode 700).')
    key_path = args.output / 'backup.key'
    if not key_path.exists():
        private_write(key_path, AESGCM.generate_key(bit_length=256))
    if key_path.stat().st_mode & 0o077:
        raise SystemExit('The backup key must be private (mode 600).')
    cipher = AESGCM(key_path.read_bytes())
    import shlex
    receipts = []
    for mode, suffix in [('dump', 'pgdump'), ('legacy', 'legacy.pgdump'), ('identity', 'identity.csv')]:
        request = json.dumps([args.product, PRODUCTS[args.product], SCHEMAS, mode])
        ssh = ['ssh', '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=yes', '-o',
               'UserKnownHostsFile=' + str(args.known_hosts), '-o', 'ConnectTimeout=8',
               'root@192.0.2.50', 'python3 - ' + shlex.quote(request)]
        result = subprocess.run(ssh, input=REMOTE.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode or not result.stdout:
            raise SystemExit('Private export failed for ' + args.product + '/' + mode + '; no database was changed.')
        name = args.product + '.' + suffix + '.aesgcm'
        destination = args.output / name
        nonce = os.urandom(12)
        ciphertext = nonce + cipher.encrypt(nonce, result.stdout, name.encode())
        private_write(destination, ciphertext)
        if cipher.decrypt(ciphertext[:12], ciphertext[12:], name.encode()) != result.stdout:
            raise SystemExit('Encrypted backup verification failed.')
        receipts.append({'file': name, 'bytes': len(ciphertext), 'sha256': hashlib.sha256(ciphertext).hexdigest()})
    receipt = {'product': args.product, 'project': PRODUCTS[args.product],
               'createdAt': datetime.now(timezone.utc).isoformat(), 'files': receipts,
               'encryptedRoundTrip': True, 'restoreVerified': False}
    private_write(args.output / (args.product + '.receipt.json'), json.dumps(receipt, indent=2).encode())
    print(json.dumps(receipt))

if __name__ == '__main__':
    main()
