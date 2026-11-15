#!/usr/bin/env bash
# Always run the full suite, even if the focused integration tests fail.
set -u
focused_status=0
bash gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace \
  -Pandroid.testInstrumentationRunnerArguments.class=com.galaxyrio.gracelauncher.AppLaunchRoutingTest,com.galaxyrio.gracelauncher.FoldPlacementUiTest,com.galaxyrio.gracelauncher.BackupExportIntegrityTest \
  || focused_status=$?
full_status=0
bash gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace || full_status=$?
if [ "$focused_status" -ne 0 ] || [ "$full_status" -ne 0 ]; then
  exit 1
fi
