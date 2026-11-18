import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('summary', Path(__file__).parents[1] / 'summarize_tests.py')
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)

class JUnitSummaryTest(unittest.TestCase):
    def test_counts_nested_suites_without_copying_private_text(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'TEST-sample.xml').write_text('''<testsuites><testsuite>
            <testcase classname="com.galaxyrio.gracelauncher.ExampleTest" name="private label"/>
            <testcase classname="com.galaxyrio.gracelauncher.ExampleTest"><failure>private content</failure></testcase>
            <testcase classname="com.galaxyrio.gracelauncher.OtherTest"><skipped/></testcase>
            <testcase classname="com.galaxyrio.gracelauncher.OtherTest"><error>private content</error></testcase>
            </testsuite></testsuites>''')
            result = summary.summarize(directory)
            self.assertEqual(dict(total=4, passed=1, failed=2, skipped=1), result['totals'])
            self.assertNotIn('private', json.dumps(result))

    def test_missing_reports_fail_instead_of_reporting_zero_successes(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError): summary.summarize(directory)

    def test_unknown_class_is_not_published(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'TEST-sample.xml').write_text('<testsuite><testcase classname="personal data"/></testsuite>')
            with self.assertRaises(ValueError): summary.summarize(directory)
