#!/usr/bin/env python3
"""Restore an encrypted product backup to disposable vanilla PostgreSQL; print no records."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

APPROVALS = ['yourlifeone_' + domain + '_oauth_clients' for domain in ['inventory','employment','equity','finance']]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, required=True)
    parser.add_argument('--product', choices=['your-life-one'], required=True)
    args = parser.parse_args()
    os.umask(0o077)
    root = args.directory
    cipher = AESGCM((root/'backup.key').read_bytes())
    def decrypt(suffix):
        name=args.product+'.'+suffix+'.aesgcm'
        raw=(root/name).read_bytes()
        return cipher.decrypt(raw[:12], raw[12:], name.encode())
    canonical=decrypt('pgdump')
    legacy=decrypt('legacy.pgdump')
    assert canonical.startswith(b'PGDMP') and legacy.startswith(b'PGDMP')
    container=args.product+'-retirement-restore'
    diagnostic=root/(args.product+'.restore-private.log')
    def run(command, body=None, check=True):
        result=subprocess.run(command,input=body,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if result.returncode and check:
            descriptor=os.open(diagnostic,os.O_WRONLY|os.O_CREAT|os.O_APPEND,0o600)
            with os.fdopen(descriptor,'ab') as output:output.write(result.stderr+b'\n')
            raise RuntimeError('Disposable restore failed; private diagnostics saved.')
        return result
    def sql(statement):
        return run(['docker','exec','-i',container,'psql','-X','-U','postgres','--set=ON_ERROR_STOP=1','-At'],statement.encode()).stdout
    def restore(archive, section, selected=False):
        command=['docker','exec','-i',container,'pg_restore','--exit-on-error','--no-owner','--no-privileges',
                 '--dbname=postgres','--username=postgres','--section='+section]
        if selected:command+=['--use-list=/tmp/approvals.list']
        run(command,archive)
    started=False
    try:
        env=os.environ.copy();env['POSTGRES_PASSWORD']=os.urandom(24).hex()
        result=subprocess.run(['docker','run','--rm','-d','--name',container,'--label','sdlc.task=supabase-retirement',
                               '--env','POSTGRES_PASSWORD','postgres:18-alpine'],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if result.returncode:raise RuntimeError('Could not start the disposable PostgreSQL18 restore target.')
        started=True
        for attempt in range(60):
            if run(['docker','exec',container,'pg_isready','-U','postgres'],check=False).returncode==0:break
            time.sleep(0.25)
        else:raise RuntimeError('Disposable PostgreSQL did not become ready.')
        sql('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS pgcrypto; '
            'CREATE SCHEMA auth; CREATE TABLE auth.users(id uuid PRIMARY KEY);')
        listing=run(['docker','exec','-i',container,'pg_restore','--list'],legacy).stdout.decode()
        selected='\n'.join(line for line in listing.splitlines() if any(name in line.split() for name in APPROVALS))+'\n'
        if not all('TABLE public '+table+' ' in selected for table in APPROVALS):raise RuntimeError('An approval table is missing from the recovery archive.')
        run(['docker','exec','-i',container,'sh','-c','cat > /tmp/approvals.list'],selected.encode())
        restore(canonical,'pre-data')
        # Replace the legacy Supabase policy/RPC dependency with the already-tested native approval schema.
        approvals_schema=Path(__file__).resolve().parent.parent/'services/access-service/src/main/resources/db/migration/identity/V3__domain_approvals.sql'
        sql(approvals_schema.read_text())
        restore(canonical,'data')
        restore(legacy,'data',True)
        # This compatibility registry supports old foreign keys; native Google identity links own authentication.
        sql("""DO $$ DECLARE target record; BEGIN
          FOR target IN SELECT table_schema,table_name FROM information_schema.columns
            WHERE column_name='user_id' AND data_type='uuid' AND table_schema NOT IN ('pg_catalog','information_schema','auth')
          LOOP EXECUTE format('INSERT INTO auth.users SELECT DISTINCT user_id FROM %I.%I WHERE user_id IS NOT NULL ON CONFLICT DO NOTHING',target.table_schema,target.table_name);
          END LOOP;
        END $$;""")
        restore(canonical,'post-data')
        tables=int(sql("select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema','auth','public') and table_type='BASE TABLE';").decode().strip())
        if tables<20:raise RuntimeError('The archive omitted application-owned schemas.')
        result={'product':args.product,'postgresVersion':18,'applicationTablesRestored':tables,
                'approvalTablesRestored':4,'constraintsRestored':True,'recordsPrinted':False,'cleanupVerified':False}
    finally:
        if started:
            cleanup=run(['docker','rm','-f','-v',container],check=False)
            if cleanup.returncode:raise RuntimeError('Disposable restore cleanup failed.')
    result['cleanupVerified']=True
    receipt=root/(args.product+'.native-restore.json')
    descriptor=os.open(receipt,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(descriptor,'w') as output:json.dump(result,output,indent=2)
    print(json.dumps(result))

if __name__=='__main__':main()
