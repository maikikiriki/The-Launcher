#!/usr/bin/env python3
"""Export numeric JUnit results only. Never copy messages, traces or screenshots."""
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET


def summarize(root):
    classes = {}
    files = sorted(Path(root).rglob('TEST-*.xml'))
    if not files:
        raise ValueError('No JUnit reports found')
    for file in files:
        if file.stat().st_size > 16 * 1024 * 1024:
            raise ValueError('Oversized JUnit report')
        document = ET.parse(file)
        for case in document.iter('testcase'):
            name = case.get('classname', '')
            if not re.fullmatch(r'com\.galaxyrio\.gracelauncher\.[A-Za-z0-9_.$]+', name):
                raise ValueError('Unrecognized test class')
            row = classes.setdefault(name, dict(total=0, passed=0, failed=0, skipped=0))
            row['total'] += 1
            status = 'failed' if case.find('failure') is not None or case.find('error') is not None else (
                'skipped' if case.find('skipped') is not None else 'passed')
            row[status] += 1
    totals = {key: sum(row[key] for row in classes.values()) for key in ('total', 'passed', 'failed', 'skipped')}
    if totals['total'] == 0:
        raise ValueError('JUnit reports contained no test cases')
    return dict(totals=totals, classes=dict(sorted(classes.items())))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    summary = summarize(args.root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps(summary['totals']))


if __name__ == '__main__':
    main()
