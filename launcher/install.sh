#!/bin/bash
# Instala el launcher y lo deja como inicio. Reinstalar borra la preferencia, por eso se repite.
# Volver al de fábrica: install.sh --restore <ip:puerto>
set -euo pipefail
if [ "${1:-}" = "--restore" ]; then
  S="$2"
  adb -s "$S" shell 'su 0 setprop persist.jx.launcher.highpriority com.jixin.launcher5; cmd package set-home-activity com.jixin.launcher5/.MainActivity'
  exit 0
fi
S="${1:?ip:puerto}"
adb -s "$S" install -r "$(cd "$(dirname "$0")" && pwd)/build/home.apk"
adb -s "$S" shell 'cmd package set-home-activity com.superprojector.home/.HomeActivity; su 0 setprop persist.jx.launcher.highpriority com.superprojector.home'
