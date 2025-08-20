#!/bin/bash

# Скрипт для сборки и подписи APK

# Переходим в корневую директорию проекта
cd "$(dirname "$0")/.." || exit 1

echo "Cleaning project..."
./gradlew clean

echo "Building release APK..."
./gradlew assembleRelease

# Проверяем, что сборка прошла успешно
if [ $? -ne 0 ]; then
  echo "Build failed!"
  exit 1
fi

echo "Signing APK..."
ANDROID_SDK_ROOT="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
APKSIGNER="$ANDROID_SDK_ROOT/build-tools/35.0.1/apksigner"

if [ ! -f "$APKSIGNER" ]; then
  echo "apksigner not found at $APKSIGNER"
  echo "Please set ANDROID_HOME or install Android SDK build-tools 35.0.1"
  exit 1
fi

UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
SIGNED_APK="app/build/outputs/apk/release/app-release.apk"

"$APKSIGNER" sign \
  --ks ~/.android/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED_APK" \
  "$UNSIGNED_APK"

if [ $? -ne 0 ]; then
  echo "Signing failed!"
  exit 1
fi

echo "Success! Signed APK is available at: $SIGNED_APK"
