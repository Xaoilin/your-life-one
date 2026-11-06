#!/usr/bin/env python3
"""Verify that browser and service API fixtures are byte-identical."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def fixtures(component):
    directory = root / component / 'contracts'
    return {
        path.relative_to(directory).as_posix(): path.read_bytes()
        for path in directory.rglob('*.json')
        if path.relative_to(directory).parts[0].endswith('-service')
    }

browser = fixtures('web')
backend = fixtures('backend')
if not browser or not backend:
    raise SystemExit('Contract fixtures must exist for both components.')
differences = [name for name in sorted(browser.keys() | backend.keys()) if browser.get(name) != backend.get(name)]
if differences:
    raise SystemExit('Contracts differ: ' + ', '.join(differences))
print(f'PASS: {len(browser)} shared API contract fixtures match.')
