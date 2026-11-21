#!/usr/bin/env bash
set -euo pipefail

# Hermetic ADB instrumented test runner & ADB helper for Biodontos.
# Enforces Android testing & ADB best practices:
#   - Uses `am instrument -w -r` wrapped in `timeout` (never `adb -t`, which is transport_id)
#   - Never gates on `$?` of `am instrument` (always exits 0); parses INSTRUMENTATION_STATUS_CODE
#     where -1 (error) and -2 (failure) fail, while -3 (ignored) and -4 (assumption failure) skip
#   - Hermetic preamble: wait-for-device + sys.boot_completed, pm clear, am force-stop, animations=0
#   - Cleanup via `trap cleanup EXIT` restoring animation scales and clearing forwards/reverses
#   - Capture-on-failure namespaced by serial: screencap via exec-out, logcat (main/crash/events),
#     dumpsys activity, and app private data via exec-out run-as + tar

APP_PKG="com.example.biodontos"
TEST_PKG="com.example.biodontos.test"
TEST_RUNNER="${TEST_PKG}/androidx.test.runner.AndroidJUnitRunner"
ORCHESTRATOR_RUNNER="androidx.test.orchestrator/.AndroidTestOrchestrator"

DEFAULT_APP_APK="app/build/outputs/apk/debug/app-debug.apk"
DEFAULT_TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

TIMEOUT_CMD="timeout"
if ! command -v timeout >/dev/null 2>&1; then
  if command -v gtimeout >/dev/null 2>&1; then
    TIMEOUT_CMD="gtimeout"
  else
    TIMEOUT_CMD=""
  fi
fi

usage() {
  cat <<'EOF'
Usage:
  run-instrumented-adb.sh [run] [OPTIONS]
  run-instrumented-adb.sh forward [-s SERIAL] <LOCAL> <REMOTE>
  run-instrumented-adb.sh reverse [-s SERIAL] <REMOTE> <LOCAL>
  run-instrumented-adb.sh wifi-pair <HOST:PAIR_PORT> <PAIR_CODE> [HOST:CONNECT_PORT]
  run-instrumented-adb.sh devices

Run Options:
  -s, --serial <SERIAL>       Target specific device serial (overrides ANDROID_SERIAL)
      --parallel              Shard across all attached 'device'-state transports in parallel
      --num-shards <N>        Explicit -e numShards <N> (with single serial)
      --shard-index <i>       Explicit -e shardIndex <i> (0-based)
      --size <small|medium|large>
                              Filter by @SmallTest/@MediumTest/@LargeTest (-e size)
      --class <fqcn[#method]> Filter to test class or class#method (-e class)
      --package <pkg>         Filter to Java package (-e package)
      --orchestrator          Wrap execution via Android Test Orchestrator (-e clearPackageData true)
      --install               Install debug APK (-r -g) and androidTest APK (-r -t -g) before run
      --app-apk <path>        App APK path (default: app/build/outputs/apk/debug/app-debug.apk)
      --test-apk <path>       Test APK path (default: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk)
      --orchestrator-apk <p>  Optional orchestrator APK to install with --force-queryable
      --test-services-apk <p> Optional test-services APK to install with --force-queryable
      --extra-arg <k=v>       Additional instrumentation arg (-e k v), repeatable
      --timeout <secs>        Per-device am instrument timeout in seconds (default: 600)
      --boot-timeout <secs>   Max seconds to wait for sys.boot_completed=1 (default: 120)
      --artifacts-dir <dir>   Failure artifacts root (default: artifacts)
  -h, --help                  Show this help

Port Forwarding Semantics (opposite argument order!):
  forward LOCAL REMOTE        Host -> Device  (e.g. tcp:9222 localabstract:chrome_devtools_remote)
  reverse REMOTE LOCAL        Device -> Host  (e.g. tcp:9000 tcp:8080)

Wireless Debugging (Android 11+):
  wifi-pair <IP:PAIR_PORT> <CODE> [IP:CONNECT_PORT]
    Pairs via `adb pair` using the one-shot pairing port, inspects `adb mdns services`
    for `_adb-tls-connect._tcp`, and connects on the separate connect port.
EOF
}

sanitize_serial() {
  printf '%s' "$1" | sed 's/[:_\/]/-/g'
}

retry_adb() {
  local attempt=0 max=3 delay=2
  until "$@"; do
    attempt=$((attempt + 1))
    if [[ "$attempt" -ge "$max" ]]; then
      echo "[retry] Command failed after $max attempts: $*" >&2
      return 1
    fi
    echo "[retry] Transient adb error (attempt $attempt/$max); restarting adb server..." >&2
    adb kill-server >/dev/null 2>&1 || true
    adb start-server >/dev/null 2>&1 || true
    sleep "$delay"
  done
}

wait_for_boot() {
  local serial="$1"
  local max_wait="$2"
  local elapsed=0

  echo "[$serial] Waiting for device transport..."
  retry_adb adb -s "$serial" wait-for-device

  echo "[$serial] Polling sys.boot_completed == 1 (timeout ${max_wait}s)..."
  while true; do
    local boot_val
    boot_val="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n' || true)"
    if [[ "$boot_val" == "1" ]]; then
      break
    fi
    if [[ "$elapsed" -ge "$max_wait" ]]; then
      echo "[$serial] ERROR: Device did not complete boot within ${max_wait}s." >&2
      return 1
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  adb -s "$serial" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
}

hermetic_preamble() {
  local serial="$1"
  echo "[$serial] Applying hermetic preamble (pm clear, force-stop, animation scales=0)..."
  retry_adb adb -s "$serial" shell pm clear "$APP_PKG" >/dev/null
  adb -s "$serial" shell pm clear "$TEST_PKG" >/dev/null 2>&1 || true
  adb -s "$serial" shell am force-stop "$APP_PKG" >/dev/null 2>&1 || true
  retry_adb adb -s "$serial" shell settings put global window_animation_scale 0
  retry_adb adb -s "$serial" shell settings put global transition_animation_scale 0
  retry_adb adb -s "$serial" shell settings put global animator_duration_scale 0
  adb -s "$serial" logcat -c >/dev/null 2>&1 || true
}

capture_on_failure() {
  local serial="$1"
  local out_dir="$2"
  mkdir -p "$out_dir"
  echo "[$serial] Capturing failure diagnostics into $out_dir ..."

  # Always use exec-out for binary streams (PNG, tar) to prevent PTY LF->CRLF corruption
  adb -s "$serial" exec-out screencap -p > "$out_dir/fail.png" 2>/dev/null || true
  adb -s "$serial" logcat -d -v threadtime > "$out_dir/logcat.txt" 2>/dev/null || true
  adb -s "$serial" logcat -d -b crash -v threadtime > "$out_dir/crash.txt" 2>/dev/null || true
  adb -s "$serial" logcat -d -b events -v descriptive > "$out_dir/events.txt" 2>/dev/null || true
  adb -s "$serial" shell dumpsys activity > "$out_dir/dumpsys-activity.txt" 2>/dev/null || true

  # Debuggable app private storage grab via exec-out run-as + tar
  mkdir -p "$out_dir/app-data"
  if command -v tar >/dev/null 2>&1; then
    adb -s "$serial" exec-out run-as "$APP_PKG" tar cf - . 2>/dev/null \
      | tar xf - -C "$out_dir/app-data" 2>/dev/null || true
  else
    adb -s "$serial" exec-out run-as "$APP_PKG" tar cf - . > "$out_dir/app-data.tar" 2>/dev/null || true
  fi
}

cleanup_device() {
  local serial="$1"
  adb -s "$serial" shell am force-stop "$APP_PKG" >/dev/null 2>&1 || true
  adb -s "$serial" shell pm clear "$APP_PKG" >/dev/null 2>&1 || true
  adb -s "$serial" shell settings put global window_animation_scale 1 >/dev/null 2>&1 || true
  adb -s "$serial" shell settings put global transition_animation_scale 1 >/dev/null 2>&1 || true
  adb -s "$serial" shell settings put global animator_duration_scale 1 >/dev/null 2>&1 || true
  adb -s "$serial" forward --remove-all >/dev/null 2>&1 || true
  adb -s "$serial" reverse --remove-all >/dev/null 2>&1 || true
}

TARGET_SERIALS=()
cleanup() {
  local s
  if [[ "${#TARGET_SERIALS[@]}" -gt 0 ]]; then
    for s in "${TARGET_SERIALS[@]}"; do
      [[ -n "$s" ]] && cleanup_device "$s"
    done
  fi
  adb forward --remove-all >/dev/null 2>&1 || true
  adb reverse --remove-all >/dev/null 2>&1 || true
}

cmd_forward() {
  local serial=""
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -s|--serial) serial="${2:?missing serial}"; shift 2 ;;
      *) break ;;
    esac
  done
  if [[ $# -ne 2 ]]; then
    echo "Usage: run-instrumented-adb.sh forward [-s SERIAL] <LOCAL> <REMOTE>" >&2
    exit 2
  fi
  local local_spec="$1" remote_spec="$2"
  echo "Setting up adb forward (HOST -> DEVICE): LOCAL=$local_spec REMOTE=$remote_spec"
  if [[ -n "$serial" ]]; then
    adb -s "$serial" forward "$local_spec" "$remote_spec"
  else
    adb forward "$local_spec" "$remote_spec"
  fi
}

cmd_reverse() {
  local serial=""
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -s|--serial) serial="${2:?missing serial}"; shift 2 ;;
      *) break ;;
    esac
  done
  if [[ $# -ne 2 ]]; then
    echo "Usage: run-instrumented-adb.sh reverse [-s SERIAL] <REMOTE> <LOCAL>" >&2
    exit 2
  fi
  local remote_spec="$1" local_spec="$2"
  echo "Setting up adb reverse (DEVICE -> HOST): REMOTE=$remote_spec LOCAL=$local_spec"
  if [[ -n "$serial" ]]; then
    adb -s "$serial" reverse "$remote_spec" "$local_spec"
  else
    adb reverse "$remote_spec" "$local_spec"
  fi
}

cmd_wifi_pair() {
  if [[ $# -lt 2 || $# -gt 3 ]]; then
    echo "Usage: run-instrumented-adb.sh wifi-pair <HOST:PAIR_PORT> <PAIR_CODE> [HOST:CONNECT_PORT]" >&2
    exit 2
  fi
  local pair_addr="$1"
  local pair_code="$2"
  local connect_addr="${3:-}"

  echo "Checking mDNS daemon status..."
  adb mdns check || true
  echo "Discovered mDNS services:"
  adb mdns services || true

  echo "Pairing with $pair_addr ..."
  adb pair "$pair_addr" "$pair_code"

  if [[ -z "$connect_addr" ]]; then
    local pair_ip="${pair_addr%%:*}"
    connect_addr="$(adb mdns services 2>/dev/null | awk -v ip="$pair_ip" '$2 == "_adb-tls-connect._tcp" && index($3, ip ":") == 1 { print $3; exit }')"
  fi

  if [[ -n "$connect_addr" ]]; then
    echo "Connecting to separate debug port $connect_addr ..."
    adb connect "$connect_addr"
    adb devices -l
  else
    echo "Paired successfully. Find the separate connect port under Wireless debugging and run:"
    echo "  adb connect <HOST:CONNECT_PORT>"
  fi
}

cmd_devices() {
  adb devices -l
  echo ""
  adb mdns services || true
}

SUBCMD="run"
if [[ $# -gt 0 ]]; then
  case "$1" in
    run|forward|reverse|wifi-pair|devices)
      SUBCMD="$1"
      shift
      ;;
  esac
fi

case "$SUBCMD" in
  forward) cmd_forward "$@"; exit $? ;;
  reverse) cmd_reverse "$@"; exit $? ;;
  wifi-pair) cmd_wifi_pair "$@"; exit $? ;;
  devices) cmd_devices; exit $? ;;
esac

# --- Subcommand: run ---
SERIAL="${ANDROID_SERIAL:-}"
PARALLEL=false
NUM_SHARDS=""
SHARD_INDEX=""
TEST_SIZE=""
TEST_CLASS=""
TEST_PACKAGE=""
USE_ORCHESTRATOR=false
DO_INSTALL=false
APP_APK="$REPO_ROOT/$DEFAULT_APP_APK"
TEST_APK="$REPO_ROOT/$DEFAULT_TEST_APK"
ORCH_APK=""
TEST_SERVICES_APK=""
TIMEOUT_SECS=600
BOOT_TIMEOUT_SECS=120
ARTIFACTS_DIR="$REPO_ROOT/artifacts"
EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--serial) SERIAL="${2:?missing value for $1}"; shift 2 ;;
    --parallel) PARALLEL=true; shift ;;
    --num-shards) NUM_SHARDS="${2:?missing value for $1}"; shift 2 ;;
    --shard-index) SHARD_INDEX="${2:?missing value for $1}"; shift 2 ;;
    --size) TEST_SIZE="${2:?missing value for $1}"; shift 2 ;;
    --class) TEST_CLASS="${2:?missing value for $1}"; shift 2 ;;
    --package) TEST_PACKAGE="${2:?missing value for $1}"; shift 2 ;;
    --orchestrator) USE_ORCHESTRATOR=true; shift ;;
    --install) DO_INSTALL=true; shift ;;
    --app-apk) APP_APK="${2:?missing value for $1}"; shift 2 ;;
    --test-apk) TEST_APK="${2:?missing value for $1}"; shift 2 ;;
    --orchestrator-apk) ORCH_APK="${2:?missing value for $1}"; shift 2 ;;
    --test-services-apk) TEST_SERVICES_APK="${2:?missing value for $1}"; shift 2 ;;
    --extra-arg) EXTRA_ARGS+=("${2:?missing value for $1}"); shift 2 ;;
    --timeout) TIMEOUT_SECS="${2:?missing value for $1}"; shift 2 ;;
    --boot-timeout) BOOT_TIMEOUT_SECS="${2:?missing value for $1}"; shift 2 ;;
    --artifacts-dir) ARTIFACTS_DIR="${2:?missing value for $1}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

adb start-server >/dev/null 2>&1 || true

if [[ -n "$SERIAL" && "$PARALLEL" == "false" ]]; then
  TARGET_SERIALS=("$SERIAL")
else
  while IFS= read -r _ser; do
    [[ -n "$_ser" ]] && TARGET_SERIALS+=("$_ser")
  done < <(adb devices | awk 'NR>1 && $2=="device"{print $1}')
  if [[ -n "$SERIAL" ]]; then
    TARGET_SERIALS=("$SERIAL")
  fi
fi

if [[ "${#TARGET_SERIALS[@]}" -eq 0 ]]; then
  echo "ERROR: No devices in 'device' state found. Run 'adb devices -l'." >&2
  exit 1
fi

if [[ "${#TARGET_SERIALS[@]}" -gt 1 && "$PARALLEL" == "false" && -z "$SERIAL" ]]; then
  echo "ERROR: Multiple devices attached (${TARGET_SERIALS[*]})." >&2
  echo "Specify -s <SERIAL> or pass --parallel to shard across all devices." >&2
  exit 1
fi

trap cleanup EXIT

# Serialize boot gate, optional APK install, and hermetic preamble per device
for s in "${TARGET_SERIALS[@]}"; do
  wait_for_boot "$s" "$BOOT_TIMEOUT_SECS"
  if [[ "$DO_INSTALL" == "true" ]]; then
    if [[ ! -f "$APP_APK" ]]; then
      echo "ERROR: App APK not found: $APP_APK" >&2
      exit 1
    fi
    if [[ ! -f "$TEST_APK" ]]; then
      echo "ERROR: Test APK not found: $TEST_APK" >&2
      exit 1
    fi
    echo "[$s] Installing app APK (-r -g) and androidTest APK (-r -t -g)..."
    retry_adb adb -s "$s" install -r -g "$APP_APK"
    retry_adb adb -s "$s" install -r -t -g "$TEST_APK"
    if [[ -n "$ORCH_APK" ]]; then
      retry_adb adb -s "$s" install --force-queryable -r "$ORCH_APK"
    fi
    if [[ -n "$TEST_SERVICES_APK" ]]; then
      retry_adb adb -s "$s" install --force-queryable -r "$TEST_SERVICES_APK"
    fi
  fi
  hermetic_preamble "$s"
done

run_single_device() {
  local serial="$1"
  local shard_idx="${2:-}"
  local total_shards="${3:-}"

  local s_clean
  s_clean="$(sanitize_serial "$serial")"
  local dev_dir="$ARTIFACTS_DIR/$s_clean"
  mkdir -p "$dev_dir"
  local log_file="$dev_dir/instrument.log"

  local -a args=(-w -r)
  [[ -n "$TEST_SIZE" ]] && args+=(-e size "$TEST_SIZE")
  [[ -n "$TEST_CLASS" ]] && args+=(-e class "$TEST_CLASS")
  [[ -n "$TEST_PACKAGE" ]] && args+=(-e package "$TEST_PACKAGE")
  if [[ -n "$total_shards" && -n "$shard_idx" ]]; then
    args+=(-e numShards "$total_shards" -e shardIndex "$shard_idx")
  fi
  if [[ "${#EXTRA_ARGS[@]}" -gt 0 ]]; then
    for kv in "${EXTRA_ARGS[@]}"; do
      [[ -z "$kv" ]] && continue
      local k="${kv%%=*}"
      local v="${kv#*=}"
      args+=(-e "$k" "$v")
    done
  fi

  if [[ "$USE_ORCHESTRATOR" == "true" ]]; then
    args+=(-e clearPackageData true -e targetInstrumentation "$TEST_RUNNER" "$ORCHESTRATOR_RUNNER")
  else
    args+=("$TEST_RUNNER")
  fi

  echo "[$serial] Executing: adb -s $serial shell am instrument ${args[*]}"
  local cmd_rc=0
  set +e
  if [[ -n "$TIMEOUT_CMD" ]]; then
    "$TIMEOUT_CMD" "${TIMEOUT_SECS}s" adb -s "$serial" shell am instrument "${args[@]}" > "$log_file" 2>&1
    cmd_rc=$?
  else
    adb -s "$serial" shell am instrument "${args[@]}" > "$log_file" 2>&1
    cmd_rc=$?
  fi
  set -e

  if [[ "$cmd_rc" -eq 124 || "$cmd_rc" -eq 137 ]]; then
    echo "[$serial] ERROR: am instrument timed out after ${TIMEOUT_SECS}s (exit=$cmd_rc)." >&2
    capture_on_failure "$serial" "$dev_dir"
    return "$cmd_rc"
  fi

  if [[ "$cmd_rc" -ne 0 ]]; then
    echo "[$serial] ERROR: adb transport failure running am instrument (exit=$cmd_rc)." >&2
    cat "$log_file" || true
    capture_on_failure "$serial" "$dev_dir"
    return "$cmd_rc"
  fi

  # am instrument ALWAYS exits 0 regardless of test failures; parse INSTRUMENTATION_STATUS_CODE!
  local parse_rc=0
  set +e
  tr -d '\r' < "$log_file" | awk '
  BEGIN {
    passed=0; errors=0; failures=0; ignored=0; assumption=0; final_seen=0; final_code=0
  }
  /^INSTRUMENTATION_STATUS_CODE:[ \t]*/ {
    code = $0
    sub(/^INSTRUMENTATION_STATUS_CODE:[ \t]*/, "", code)
    gsub(/[ \t]+/, "", code)
    if (code == "0") passed++
    else if (code == "-1") errors++
    else if (code == "-2") failures++
    else if (code == "-3") ignored++
    else if (code == "-4") assumption++
  }
  /^INSTRUMENTATION_CODE:[ \t]*/ {
    final_seen = 1
    final_code = $0
    sub(/^INSTRUMENTATION_CODE:[ \t]*/, "", final_code)
    gsub(/[ \t]+/, "", final_code)
  }
  END {
    printf "Results: passed=%d, failures(-2)=%d, errors(-1)=%d, ignored(-3)=%d, assumption_skipped(-4)=%d, INSTRUMENTATION_CODE=%s\n",
      passed, failures, errors, ignored, assumption, (final_seen ? final_code : "MISSING")
    if (errors > 0 || failures > 0 || !final_seen || final_code == "0") {
      exit 1
    }
    exit 0
  }
  '
  parse_rc=$?
  set -e

  if [[ "$parse_rc" -ne 0 ]]; then
    echo "[$serial] FAILED: Instrumentation tests reported errors/failures or runner crash." >&2
    capture_on_failure "$serial" "$dev_dir"
    return 1
  fi

  echo "[$serial] PASSED."
  return 0
}

OVERALL_RC=0
N_DEVS="${#TARGET_SERIALS[@]}"

if [[ "$PARALLEL" == "true" && "$N_DEVS" -gt 1 ]]; then
  PIDS=()
  for i in "${!TARGET_SERIALS[@]}"; do
    s="${TARGET_SERIALS[$i]}"
    run_single_device "$s" "$i" "$N_DEVS" &
    PIDS+=($!)
  done
  for p in "${PIDS[@]}"; do
    wait "$p" || OVERALL_RC=1
  done
else
  run_single_device "${TARGET_SERIALS[0]}" "$SHARD_INDEX" "$NUM_SHARDS" || OVERALL_RC=$?
fi

exit "$OVERALL_RC"
