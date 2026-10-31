#!/usr/bin/env bash
# Paso 1 — Diagnóstico de SOLO LECTURA del proyector por ADB en red.
#
# Uso:   scripts/01-diagnostico.sh <IP>[:PUERTO]      (puerto por defecto 5555)
#
# No escribe nada en el proyector: no flashea, no instala, no desinstala, no cambia ajustes.
# Única excepción, en tiempo de ejecución: prueba "adb root". Si la build lo permite, adbd se
# reinicia como root; al terminar se deshace con "adb unroot" (y un reinicio también lo deshace).
#
# Salida en diagnostico/<fecha>/
#   privado/      -> todo, en bruto. Ignorado por git: contiene serie, MAC, ajustes.
#   compartible/  -> subconjunto sin serie/MAC/IDs, apto para revisar y subir al repo.
#
# Compatible con el bash 3.2 de macOS.
set -u

if [ $# -lt 1 ]; then
  echo "Uso: $0 <IP>[:PUERTO]" >&2
  exit 1
fi
TARGET="$1"
case "$TARGET" in *:*) ;; *) TARGET="$TARGET:5555" ;; esac

if ! command -v adb >/dev/null 2>&1; then
  echo "No encuentro adb. En el Mac: brew install android-platform-tools" >&2
  exit 1
fi

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$REPO_DIR/diagnostico/$STAMP"
PRIV="$OUT/privado"
PUB="$OUT/compartible"
LOG="$PRIV/_comandos.log"
mkdir -p "$PRIV" "$PUB"

# Claves que se tachan en la versión compartible (getprop y cmdline).
SENSIBLE='serial|sn$|mac|imei|meid|android_id|ssid|psk|passw|uuid|cpuid|chipid|chip_id|deviceid|device_id|ethaddr|btaddr|bt_addr|wifiaddr|wlan_addr|hwaddr|usid|keybox|widevine|hdcp|attest'

A() { adb -s "$TARGET" "$@"; }

esperar_dispositivo() {
  i=0; avisado=0
  while [ $i -lt 40 ]; do
    adb connect "$TARGET" >/dev/null 2>&1
    estado="$(A get-state 2>/dev/null)"
    case "$estado" in
      device) return 0 ;;
      unauthorized)
        if [ $avisado -eq 0 ]; then
          echo "  -> Acepta en la pantalla del proyector el aviso de depuración (marca 'Permitir siempre')."
          avisado=1
        fi ;;
    esac
    sleep 1; i=$((i + 1))
  done
  return 1
}

# run <nombre> <comando a ejecutar en el proyector>
run() {
  nombre="$1"; cmd="$2"
  printf '  %-28s' "$nombre"
  A shell "$cmd" </dev/null >"$PRIV/$nombre.txt" 2>&1
  rc=$?
  printf '%s | exit %s | %s\n' "$nombre" "$rc" "$cmd" >>"$LOG"
  [ $rc -eq 0 ] && echo "ok" || echo "exit $rc (ver $nombre.txt)"
}

echo "== Conectando a $TARGET"
if ! esperar_dispositivo; then
  echo "No se pudo conectar a $TARGET (estado: $(A get-state 2>&1))." >&2
  echo "Comprueba: misma red, 'Depuración ADB por red' activa en el proyector, IP correcta." >&2
  exit 1
fi
adb devices -l >"$PRIV/adb_devices.txt" 2>&1
adb version >"$PRIV/adb_version_mac.txt" 2>&1

echo "== Sistema"
run getprop            'getprop'
run id_shell           'id'
run uname              'uname -a'
run proc_version       'cat /proc/version'
run cpuinfo            'cat /proc/cpuinfo'
run meminfo            'cat /proc/meminfo'
run devicetree         'for f in compatible model; do echo "== $f"; cat /proc/device-tree/$f 2>/dev/null | tr "\0" "\n"; echo; done'
run cmdline            'cat /proc/cmdline'
run selinux            'getenforce'
run su_presente        'for p in /system/bin/su /system/xbin/su /sbin/su /vendor/bin/su; do [ -e "$p" ] && ls -l "$p"; done; which su; true'
run features           'pm list features'
run servicios          'service list'
run procesos           'ps -A 2>/dev/null || ps'
run modulos            'cat /proc/modules; for d in /vendor/lib/modules /system/lib/modules /vendor_dlkm/lib/modules; do [ -d "$d" ] && { echo "== $d"; ls -l "$d"; }; done'
run input              'cat /proc/bus/input/devices'
run pantalla           'wm size; wm density; dumpsys display'
run ip                 'ip addr'

echo "== Almacenamiento y particiones"
run particiones        'cat /proc/partitions'
run by_name            'for d in /dev/block/by-name /dev/block/bootdevice/by-name /dev/block/platform/*/by-name /dev/block/platform/*/*/by-name; do [ -d "$d" ] && { echo "== $d"; ls -l "$d"; }; done'
run mtd                'cat /proc/mtd'
run dev_block          'ls -l /dev/block/'
run mounts             'cat /proc/mounts'
run df                 'df -h 2>/dev/null || df'
run fstab              'for f in /vendor/etc/fstab.* /odm/etc/fstab.* /fstab.* /system/etc/fstab.*; do [ -f "$f" ] && { echo "== $f"; cat "$f"; }; done'
run dirs_unicos        'ls -la / /mnt/vendor /persist /metadata /factory /data/vendor 2>&1'

echo "== Paquetes"
run paquetes_todos     'pm list packages -f -i'
run paquetes_sistema   'pm list packages -s -f'
run paquetes_terceros  'pm list packages -3 -f'
run paquetes_desact    'pm list packages -d'
run dumpsys_package    'dumpsys package'
run dirs_apps          'for d in /system/app /system/priv-app /product/app /product/priv-app /system_ext/app /system_ext/priv-app /vendor/app /vendor/priv-app /odm/app; do [ -d "$d" ] && { echo "== $d"; ls -l "$d"; }; done'
run launchers          'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME; echo "== HOME por defecto"; cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
run usuarios           'pm list users'
run settings           'echo "== global"; settings list global; echo "== system"; settings list system; echo "== secure"; settings list secure'

echo "== Prueba de adb root"
A root >"$PRIV/adb_root.txt" 2>&1
cat "$PRIV/adb_root.txt" | sed 's/^/  /'
ROOT=no
DESHACER_ROOT=no
if grep -qi "already running as root" "$PRIV/adb_root.txt"; then
  ROOT="si (adbd ya corría como root de fábrica)"
elif grep -qi "restarting adbd as root" "$PRIV/adb_root.txt"; then
  sleep 3
  if esperar_dispositivo; then
    if A shell id </dev/null 2>/dev/null | grep -q "uid=0"; then
      ROOT="si (adb root funciona; build debuggable)"
      DESHACER_ROOT=si
    fi
  else
    echo "  !! adbd no volvió tras 'adb root'. Reinicia el proyector y vuelve a activar ADB por red." >&2
  fi
fi
echo "  root: $ROOT"

if [ "$ROOT" != no ]; then
  echo "== Lecturas extra como root (solo lectura)"
  run root_id            'id'
  run root_cmdline       'cat /proc/cmdline'
  run root_dirs_unicos   'ls -laR /mnt/vendor /persist /factory /data/vendor 2>&1 | head -n 2000'
  run root_tam_part      'for p in /dev/block/by-name/*; do printf "%s %s %s\n" "$(basename $p)" "$(readlink -f $p)" "$(blockdev --getsize64 $p 2>/dev/null)"; done'
fi

if [ "$DESHACER_ROOT" = si ]; then
  echo "== Deshaciendo adb root (adb unroot)"
  A unroot >>"$PRIV/adb_root.txt" 2>&1
  sleep 3
  esperar_dispositivo || echo "  !! No volvió tras 'adb unroot'; un reinicio del proyector lo deja como estaba." >&2
fi

# ---------- Versión compartible (sin serie / MAC / IDs) ----------
echo "== Generando versión compartible"
tachar_mac() { sed -E 's/([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}/XX:XX:XX:XX:XX:XX/g'; }

# Excluidos de compartible: ip, settings, dumpsys_package (en bruto), adb_devices, procesos.
for f in "$PRIV"/*.txt; do
  n="$(basename "$f" .txt)"
  case "$n" in
    _comandos|ip|settings|dumpsys_package|adb_devices|procesos|dirs_unicos|root_dirs_unicos) continue ;;
    getprop)
      awk -v pat="$SENSIBLE" '
        substr($0,1,1)=="[" { e=index($0,"]"); k=substr($0,2,e-2)
          if (tolower(k) ~ pat) { print "[" k "]: [REDACTADO]"; next } }
        { print }' "$f" | tachar_mac >"$PUB/$n.txt" ;;
    cmdline|root_cmdline)
      awk -v pat="$SENSIBLE" '{
          o=""; for (i=1;i<=NF;i++) { t=$i; e=index(t,"=")
            if (e>0 && tolower(substr(t,1,e-1)) ~ pat) t=substr(t,1,e) "REDACTADO"
            o = o (i>1?" ":"") t }
          print o }' "$f" | tachar_mac >"$PUB/$n.txt" ;;
    cpuinfo)
      sed -E 's/^(Serial[[:space:]]*:).*/\1 REDACTADO/' "$f" | tachar_mac >"$PUB/$n.txt" ;;
    *)
      tachar_mac <"$f" >"$PUB/$n.txt" ;;
  esac
done
# Resumen ligero de paquetes (nombre, versión, ruta, flags) a partir de dumpsys package.
grep -E '^  Package \[|versionName=|versionCode=|codePath=|^    flags=|^    pkgFlags=' \
  "$PRIV/dumpsys_package.txt" >"$PUB/paquetes_detalle.txt" 2>/dev/null

# ---------- Resumen ----------
prop() { grep -F "[$1]:" "$PUB/getprop.txt" 2>/dev/null | sed -E 's/^\[[^]]*\]: \[(.*)\]$/\1/'; }
{
  echo "Diagnóstico $STAMP  (objetivo $TARGET)"
  echo
  for k in ro.product.brand ro.product.manufacturer ro.product.model ro.product.device ro.product.name \
           ro.product.board ro.board.platform ro.hardware ro.boot.hardware ro.chipname ro.soc.manufacturer ro.soc.model \
           ro.build.fingerprint ro.build.display.id ro.build.version.release ro.build.version.sdk \
           ro.build.version.security_patch ro.product.first_api_level ro.build.type ro.build.tags \
           ro.debuggable ro.secure ro.adb.secure ro.product.cpu.abilist ro.treble.enabled ro.vndk.version \
           ro.build.ab_update ro.boot.slot_suffix ro.boot.dynamic_partitions ro.boot.verifiedbootstate \
           ro.boot.flash.locked ro.boot.vbmeta.device_state ro.boot.veritymode ro.oem_unlock_supported \
           ro.crypto.state ro.crypto.type ro.bootloader ro.sf.lcd_density ro.com.google.gmsversion; do
    printf '%-34s %s\n' "$k" "$(prop "$k")"
  done
  echo
  echo "adb root: $ROOT"
  echo "SELinux:  $(tr -d '\r' <"$PRIV/selinux.txt")"
  echo "Device tree: $(tr -d '\r\0' <"$PUB/devicetree.txt" | tr '\n' ' ')"
  echo "Paquetes: total $(grep -c '^package:' "$PRIV/paquetes_todos.txt") |" \
       "sistema $(grep -c '^package:' "$PRIV/paquetes_sistema.txt") |" \
       "terceros $(grep -c '^package:' "$PRIV/paquetes_terceros.txt") |" \
       "desactivados $(grep -c '^package:' "$PRIV/paquetes_desact.txt")"
  echo
  echo "Particiones (by-name):"
  sed 's/^/  /' "$PUB/by_name.txt"
} >"$PUB/RESUMEN.txt"

echo
cat "$PUB/RESUMEN.txt"
echo
echo "Listo. Salida en: $OUT"
echo "  privado/      -> NO subir (ignorado por git)"
echo "  compartible/  -> revísalo y, si está bien, súbelo:  git add diagnostico/$STAMP/compartible"
