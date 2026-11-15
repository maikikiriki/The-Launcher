import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPTS = Path(__file__).parents[1]

class AndroidCheckScriptTest(unittest.TestCase):
    def run_script(self, code):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'scripts').mkdir()
            shutil.copy(SCRIPTS / 'summarize_tests.py', root / 'scripts')
            (root / 'gradlew').write_text('''#!/bin/bash
printf 'called\\n' >> calls
mkdir -p app/build/outputs/androidTest-results/connected
cat > app/build/outputs/androidTest-results/connected/TEST-fixture.xml <<'XML'
<testsuite><testcase classname="com.galaxyrio.gracelauncher.FixtureTest"/></testsuite>
XML
exit ''' + str(code) + '\n')
            result = subprocess.run(['bash', str(SCRIPTS / 'check_android.sh')], cwd=root,
                                    capture_output=True, text=True)
            self.assertEqual(['called'], (root / 'calls').read_text().splitlines())
            report = json.loads((root / 'app/build/acronix-reports/instrumentation-summary.json').read_text())
            self.assertEqual(1, report['totals']['total'])
            return result.returncode

    def test_single_complete_run_succeeds_with_reports(self):
        self.assertEqual(0, self.run_script(0))

    def test_gradle_failure_remains_failure_even_when_summary_exists(self):
        self.assertEqual(1, self.run_script(4))
