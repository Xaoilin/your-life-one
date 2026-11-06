#!/usr/bin/env python3
"""Validate Alloy with EnvironmentFile values passed as data, never shell code."""
import os
from pathlib import Path
import subprocess
import sys


def validate(binary, environment_file, configuration):
    values = dict(line.split('=', 1) for line in Path(environment_file).read_text().splitlines() if '=' in line)
    subprocess.run([binary, 'validate', configuration], env=os.environ | values, check=True)


if __name__ == '__main__':
    validate(*sys.argv[1:])
