"""Regression proof for Basic authorization and shell-like EnvironmentFile values."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('validator', Path(__file__).with_name('validate-observability-config.py'))
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class EnvironmentFileTest(unittest.TestCase):
    def test_values_are_passed_literally_to_the_validator(self):
        with tempfile.TemporaryDirectory() as directory:
            marker = Path(directory) / 'must-not-exist'
            authorization = f'Basic synthetic==$(touch {marker})'
            environment = Path(directory) / 'collector.env'
            environment.write_text(f'GRAFANA_OTLP_AUTHORIZATION={authorization}\nYOUR_LIFE_HOST=test-host\n')
            with patch.object(validator.subprocess, 'run') as run:
                validator.validate('/alloy', environment, '/config.alloy')
            args, kwargs = run.call_args
            self.assertEqual(args[0], ['/alloy', 'validate', '/config.alloy'])
            self.assertEqual(kwargs['env']['GRAFANA_OTLP_AUTHORIZATION'], authorization)
            self.assertTrue(kwargs['check'])
            self.assertFalse(marker.exists())


if __name__ == '__main__':
    unittest.main()
