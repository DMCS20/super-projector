#!/bin/bash
# Compila el launcher sin Gradle para mantener el APK mínimo.
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-35/android.jar"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/build"
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/dex"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$HERE/AndroidManifest.xml"
javac --release 11 -nowarn -classpath "$JAR" -d "$OUT/classes" $(find "$HERE/src" -name "*.java")
"$BT/d8" --release --min-api 33 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -0 "$OUT/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --out "$OUT/home.apk" "$OUT/aligned.apk"
ls -l "$OUT/home.apk"
