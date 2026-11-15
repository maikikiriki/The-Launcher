#!/usr/bin/env bash
# One complete run avoids duplicate tests and state contamination between runs.
set -u
suite_status=0
bash gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace || suite_status=$?
summary_status=0
python3 scripts/summarize_tests.py --root app/build/outputs/androidTest-results/connected \
  --output app/build/acronix-reports/instrumentation-summary.json || summary_status=$?
if [ "$suite_status" -ne 0 ] || [ "$summary_status" -ne 0 ]; then
  exit 1
fi
