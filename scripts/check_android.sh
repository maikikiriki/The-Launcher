#!/usr/bin/env bash
# One complete run avoids duplicate tests and state contamination between runs.
set -u
scope="${ACRONIX_TEST_SCOPE:-full}"
extra_args=()
case "$scope" in
  full) ;;
  families) extra_args=(-Pandroid.testInstrumentationRunnerArguments.class=com.galaxyrio.gracelauncher.ClockStyleEditorModesTest,com.galaxyrio.gracelauncher.ClockRegionRenderingTest,com.galaxyrio.gracelauncher.ClockStyleSettingsTest,com.galaxyrio.gracelauncher.ClockStylePersistenceTest,com.galaxyrio.gracelauncher.IconDesignerTest,com.galaxyrio.gracelauncher.LauncherInteractionTest,com.galaxyrio.gracelauncher.FavoritesEditorTest,com.galaxyrio.gracelauncher.LauncherReloadTest) ;;
  *) exit 2 ;;
esac
suite_status=0
bash gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace "${extra_args[@]}" || suite_status=$?
summary_status=0
python3 scripts/summarize_tests.py --root app/build/outputs/androidTest-results/connected \
  --output app/build/acronix-reports/instrumentation-summary.json || summary_status=$?
if [ "$suite_status" -ne 0 ] || [ "$summary_status" -ne 0 ]; then
  exit 1
fi
