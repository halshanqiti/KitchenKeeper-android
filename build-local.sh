#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
  printf '%s\n' 'Install Gradle 8.13, or use the included GitHub Actions workflow.' >&2
  exit 1
fi
if [[ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" && ! -f local.properties ]]; then
  printf '%s\n' 'Set ANDROID_HOME to your Android SDK, or configure sdk.dir in local.properties.' >&2
  exit 1
fi
gradle --no-daemon lintDebug assembleDebug
printf '%s\n' 'APK: app/build/outputs/apk/debug/app-debug.apk'
