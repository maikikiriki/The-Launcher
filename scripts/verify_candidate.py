#!/usr/bin/env python3
"""Read-only APK gate. Does not install, sign, request secrets or contact a device."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path


def verify(apk, expected_certificate, minimum_version_code, apksigner='apksigner', aapt='aapt',
           application_id='com.acronix.launcher'):
    if application_id not in {'com.acronix.launcher', 'com.acronix.launcher.preview'}:
        raise ValueError('Only explicitly recognized Acronix package identifiers are allowed')
    expected = expected_certificate.replace(':', '').lower()
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        raise ValueError('Expected public certificate SHA-256 is required')
    if minimum_version_code < 1:
        raise ValueError('Minimum version code must be positive')
    signature = subprocess.check_output([apksigner, 'verify', '--print-certs', str(apk)], text=True)
    certificates = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', signature, re.M)
    if len(certificates) != 1 or certificates[0].lower() != expected:
        raise ValueError('Certificate mismatch or unsupported signer count; do not install')
    badging = subprocess.check_output([aapt, 'dump', 'badging', str(apk)], text=True)
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)'", badging, re.M)
    if not package or package[1] != application_id:
        raise ValueError('Unexpected applicationId; do not install')
    version = int(package[2])
    if version < minimum_version_code:
        raise ValueError('Version code below required baseline; do not install')
    digest = hashlib.sha256()
    with Path(apk).open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            digest.update(block)
    return dict(applicationId=package[1], versionCode=version, certificateSha256=expected,
                apkSha256=digest.hexdigest(), installationAuthorized=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--expected-certificate', required=True)
    parser.add_argument('--minimum-version-code', type=int, required=True)
    parser.add_argument('--application-id', default='com.acronix.launcher',
                        choices=['com.acronix.launcher', 'com.acronix.launcher.preview'])
    parser.add_argument('--apksigner', default='apksigner')
    parser.add_argument('--aapt', default='aapt')
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.apk, args.expected_certificate, args.minimum_version_code,
                                args.apksigner, args.aapt, args.application_id), indent=2))
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f'Candidate rejected: {type(error).__name__}\n')


if __name__ == '__main__':
    main()
