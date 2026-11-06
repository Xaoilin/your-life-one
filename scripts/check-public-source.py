#!/usr/bin/env python3
"""Reject accidental private exports and high-confidence credential formats."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
credentials = re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\bgithub_pat_[A-Za-z0-9_]{30,}|\bgh[pousr]_[A-Za-z0-9]{30,}|\bAKIA[A-Z0-9]{16}\b')
failures = []
count = 0
for name in filter(None, paths):
    path = root / name
    if not path.is_file():
        continue
    count += 1
    if (path.name.startswith('.env') and path.name != '.env.example') or any(part in {'reports', 'node_modules', 'target', 'test-results', 'playwright-report'} for part in path.parts):
        failures.append(name + ': private or generated output')
        continue
    try:
        text = path.read_text()
    except UnicodeDecodeError:
        continue
    if credentials.search(text):
        failures.append(name + ': credential format')
if failures:
    raise SystemExit('\n'.join(failures))
print(f'PASS: {count} tracked files checked for private outputs and credential formats.')
