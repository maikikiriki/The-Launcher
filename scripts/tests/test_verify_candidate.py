import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('candidate', Path(__file__).parents[1] / 'verify_candidate.py')
candidate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(candidate)
CERT = 'a' * 64

class CandidateGateTest(unittest.TestCase):
    def run_gate(self, certificate=CERT, package='com.acronix.launcher', version=4, minimum=4):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / 'candidate.apk'
            apk.write_bytes(b'fixture')
            outputs = [f'Signer #1 certificate SHA-256 digest: {certificate}\n',
                       f"package: name='{package}' versionCode='{version}' versionName='test'\n"]
            with patch.object(candidate.subprocess, 'check_output', side_effect=outputs):
                return candidate.verify(apk, CERT, minimum)

    def test_matching_candidate_still_does_not_authorize_installation(self):
        result = self.run_gate()
        self.assertFalse(result['installationAuthorized'])
        self.assertEqual(64, len(result['apkSha256']))

    def test_different_certificate_is_rejected(self):
        with self.assertRaises(ValueError): self.run_gate(certificate='b' * 64)

    def test_grace_package_is_rejected(self):
        with self.assertRaises(ValueError): self.run_gate(package='com.galaxyrio.gracelauncher')

    def test_downgrade_is_rejected(self):
        with self.assertRaises(ValueError): self.run_gate(version=3)
