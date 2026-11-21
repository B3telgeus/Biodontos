#!/usr/bin/env bash
set -euo pipefail

# Validates Jetpack Compose composable stability against a committed baseline.
# Reads app/build/compose_compiler/app_debug-composables.txt and app_debug-module.json
# (plus optional app_debug-composables.csv for FQN resolution).
# Excludes @Preview functions (name ending in Preview or inside presentation.previews).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

UPDATE_BASELINE=false
COMPOSE_REPORTS_DIR="app/build/compose_compiler"
BASELINE_PATH="app/stability/app.stability"

usage() {
  cat <<'EOF'
Usage: check-compose-stability.sh [OPTIONS]

Options:
  --update-baseline        Write deterministic baseline to app/stability/app.stability
  --reports-dir <dir>      Compose compiler reports dir (default: app/build/compose_compiler)
  --baseline <path>        Baseline file path (default: app/stability/app.stability)
  -h, --help               Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --update-baseline)
      UPDATE_BASELINE=true
      shift
      ;;
    --reports-dir)
      COMPOSE_REPORTS_DIR="${2:?missing value for --reports-dir}"
      shift 2
      ;;
    --baseline)
      BASELINE_PATH="${2:?missing value for --baseline}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

resolve_path() {
  local p="$1"
  if [[ "$p" = /* || "$p" =~ ^[A-Za-z]:[\\/] ]]; then
    printf '%s' "$p"
  else
    printf '%s/%s' "$REPO_ROOT" "$p"
  fi
}

REPORTS_FULL_DIR="$(resolve_path "$COMPOSE_REPORTS_DIR")"
BASELINE_FULL_PATH="$(resolve_path "$BASELINE_PATH")"
COMPOSABLES_TXT="$REPORTS_FULL_DIR/app_debug-composables.txt"
COMPOSABLES_CSV="$REPORTS_FULL_DIR/app_debug-composables.csv"
MODULE_JSON="$REPORTS_FULL_DIR/app_debug-module.json"

if [[ ! -f "$COMPOSABLES_TXT" ]]; then
  echo "ERROR: Missing Compose compiler report: $COMPOSABLES_TXT" >&2
  echo "Run './gradlew :app:compileDebugKotlin' first." >&2
  exit 1
fi

if [[ ! -f "$MODULE_JSON" ]]; then
  echo "ERROR: Missing Compose module metrics: $MODULE_JSON" >&2
  echo "Run './gradlew :app:compileDebugKotlin' first." >&2
  exit 1
fi

json_metric() {
  local key="$1"
  local val
  val=$(tr -d '\r' < "$MODULE_JSON" | awk -v k="\"$key\"" '
    index($0, k) > 0 {
      line = $0
      sub(/^.*:[ \t]*/, "", line)
      sub(/,[ \t]*$/, "", line)
      gsub(/[ \t]+$/, "", line)
      print line
      exit
    }
  ')
  printf '%s' "${val:-0}"
}

TMP_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_DIR" || true
}
trap cleanup EXIT

CURRENT_RECORDS="$TMP_DIR/current_records.tsv"
CURRENT_META="$TMP_DIR/current_meta.txt"

# Extract normalized composable records into TSV (mawk & gawk compatible):
# id \t restartable \t skippable \t paramCount \t stab\034name\034type [\035 ...]
LC_ALL=C awk -v csv_path="$COMPOSABLES_CSV" -v meta_out="$CURRENT_META" '
BEGIN {
  has_csv = 0
  csv_n = 0
  if (csv_path != "") {
    while ((getline cline < csv_path) > 0) {
      gsub(/\r$/, "", cline)
      if (csv_n == 0 && cline ~ /^package,name,/) {
        csv_n++
        continue
      }
      if (cline != "") {
        split(cline, cols, ",")
        idx = csv_n
        csv_pkg[idx] = cols[1]
        csv_name[idx] = cols[2]
        csv_n++
      }
    }
    close(csv_path)
    if (csv_n > 1) {
      has_csv = 1
      csv_total = csv_n - 1
    }
  }
  parsed_n = 0
  in_fun = 0
  in_def = 0
}
function flush_fun() {
  if (!in_fun) return
  parsed_n++
  fname[parsed_n] = cur_name
  fid[parsed_n] = cur_name
  frest[parsed_n] = cur_rest
  fskip[parsed_n] = cur_skip
  fpcount[parsed_n] = cur_pcount
  fparams[parsed_n] = cur_params
  in_fun = 0
  in_def = 0
}
{
  gsub(/\r$/, "", $0)
  line = $0

  if (line ~ /^((restartable|skippable|readonly|inline)[ \t]+)*(scheme\([^)]*\)[ \t]+)?fun[ \t]+[^ \t(]+\(/) {
    flush_fun()

    tmp = line
    sub(/fun[ \t]+.*$/, "", tmp)
    cur_rest = (tmp ~ /(^|[ \t])restartable([ \t]|$)/) ? "true" : "false"
    cur_skip = (tmp ~ /(^|[ \t])skippable([ \t]|$)/) ? "true" : "false"

    after_fun = line
    sub(/^.*fun[ \t]+/, "", after_fun)
    paren_pos = index(after_fun, "(")
    cur_name = substr(after_fun, 1, paren_pos - 1)
    rest = substr(after_fun, paren_pos + 1)

    cur_pcount = 0
    cur_params = ""
    if (rest == ")") {
      in_fun = 1
      flush_fun()
    } else {
      in_fun = 1
      in_def = 0
    }
    next
  }

  if (in_fun) {
    if (line ~ /^\)/) {
      flush_fun()
      next
    }
    if (in_def) {
      if (line ~ /^tmp[0-9]+_group@/) {
        in_def = 0
      }
      next
    }
    if (line ~ /^  (unused[ \t]+)?((stable|unstable|uncertain)[ \t]+)?(<this>|[A-Za-z_][A-Za-z0-9_]*):[ \t]*.+$/) {
      pline = substr(line, 3)
      sub(/^unused[ \t]+/, "", pline)

      stab = "uncertain"
      if (pline ~ /^(stable|unstable|uncertain)[ \t]+/) {
        sp = index(pline, " ")
        stab = substr(pline, 1, sp - 1)
        pline = substr(pline, sp + 1)
        sub(/^[ \t]+/, "", pline)
      }

      colon = index(pline, ":")
      pname = substr(pline, 1, colon - 1)
      raw_type = substr(pline, colon + 1)
      sub(/^[ \t]+/, "", raw_type)

      if (index(raw_type, "$composer.startReplaceGroup(") > 0) {
        in_def = 1
      }

      clean_type = raw_type
      sub(/[ \t]*=[ \t]*@(static|dynamic)[ \t]+.*$/, "", clean_type)
      sub(/[ \t]+$/, "", clean_type)

      cur_pcount++
      pentry = stab "\034" pname "\034" clean_type
      if (cur_params == "") {
        cur_params = pentry
      } else {
        cur_params = cur_params "\035" pentry
      }
    }
  }
}
END {
  flush_fun()

  if (has_csv && csv_total == parsed_n) {
    ok = 1
    for (i = 1; i <= parsed_n; i++) {
      if (csv_name[i] != fname[i]) {
        ok = 0
        break
      }
    }
    if (ok) {
      for (i = 1; i <= parsed_n; i++) {
        if (csv_pkg[i] != "") {
          fid[i] = csv_pkg[i]
        }
      }
    }
  }

  excluded = 0
  kept = 0
  for (i = 1; i <= parsed_n; i++) {
    nm = fname[i]
    id = fid[i]
    is_prev_name = (nm ~ /Preview$/)
    is_prev_pkg = (id ~ /(^|\.)presentation\.previews(\.|$)/)
    if (is_prev_name || is_prev_pkg) {
      excluded++
      continue
    }
    kept++
    k_id[kept] = id
    k_rest[kept] = frest[i]
    k_skip[kept] = fskip[i]
    k_pcount[kept] = fpcount[i]
    k_params[kept] = fparams[i]
    id_count[id]++
  }

  for (i = 1; i <= kept; i++) {
    id = k_id[i]
    if (id_count[id] > 1) {
      n_p = split(k_params[i], arr, "\035")
      pnames = ""
      for (j = 1; j <= n_p; j++) {
        split(arr[j], parts, "\034")
        pnames = (j == 1) ? parts[2] : (pnames "," parts[2])
      }
      id = id "(" pnames ")"
    }
    printf "%s\t%s\t%s\t%s\t%s\n", id, k_rest[i], k_skip[i], k_pcount[i], k_params[i]
  }

  printf "TRACKED=%d\nEXCLUDED=%d\n", kept, excluded > meta_out
  close(meta_out)
}
' "$COMPOSABLES_TXT" | LC_ALL=C sort -t $'\t' -k1,1 > "$CURRENT_RECORDS"

source "$CURRENT_META"

if [[ "$UPDATE_BASELINE" == "true" ]]; then
  mkdir -p "$(dirname "$BASELINE_FULL_PATH")"
  TOTAL_COMPOSABLES="$(json_metric totalComposables)"
  RESTARTABLE_COMPOSABLES="$(json_metric restartableComposables)"
  SKIPPABLE_COMPOSABLES="$(json_metric skippableComposables)"
  KNOWN_STABLE_ARGS="$(json_metric knownStableArguments)"
  KNOWN_UNSTABLE_ARGS="$(json_metric knownUnstableArguments)"
  EFF_STABLE_CLASSES="$(json_metric effectivelyStableClasses)"
  TOTAL_CLASSES="$(json_metric totalClasses)"
  STRONG_SKIPPING="$(json_metric StrongSkipping)"

  {
    printf '# Biodontos Compose Stability Baseline\n'
    printf '# Source: app/build/compose_compiler/app_debug-composables.txt\n'
    printf '#\n'
    printf '# Module Summary:\n'
    printf '#   totalComposables: %s\n' "$TOTAL_COMPOSABLES"
    printf '#   restartableComposables: %s\n' "$RESTARTABLE_COMPOSABLES"
    printf '#   skippableComposables: %s\n' "$SKIPPABLE_COMPOSABLES"
    printf '#   knownStableArguments: %s\n' "$KNOWN_STABLE_ARGS"
    printf '#   knownUnstableArguments: %s\n' "$KNOWN_UNSTABLE_ARGS"
    printf '#   effectivelyStableClasses: %s / %s\n' "$EFF_STABLE_CLASSES" "$TOTAL_CLASSES"
    printf '#   strongSkipping: %s\n' "$STRONG_SKIPPING"
    printf '#   trackedComposables: %s (excluded Previews: %s)\n\n' "$TRACKED" "$EXCLUDED"

    LC_ALL=C awk -F '\t' '
    {
      id = $1
      rest = $2
      skip = $3
      pcount = $4 + 0
      params = $5
      printf "@composable %s [restartable=%s, skippable=%s]\n", id, rest, skip
      if (pcount > 0 && params != "") {
        n = split(params, arr, "\035")
        for (i = 1; i <= n; i++) {
          split(arr[i], p, "\034")
          printf "  - %s %s: %s\n", p[1], p[2], p[3]
        }
      }
      printf "\n"
    }
    ' "$CURRENT_RECORDS"
  } > "$BASELINE_FULL_PATH"

  echo "Updated Compose stability baseline at: $BASELINE_FULL_PATH"
  echo "Tracked composables: $TRACKED (excluded Previews: $EXCLUDED)"
  exit 0
fi

# Check mode
if [[ ! -f "$BASELINE_FULL_PATH" ]]; then
  echo "ERROR: Compose stability baseline not found at: $BASELINE_FULL_PATH" >&2
  echo "Generate and review the baseline before running checks:" >&2
  echo "  1. ./gradlew :app:compileDebugKotlin" >&2
  echo "  2. bash tools/ci/check-compose-stability.sh --update-baseline" >&2
  echo "  3. Review and commit app/stability/app.stability" >&2
  exit 1
fi

BASELINE_RECORDS="$TMP_DIR/baseline_records.tsv"

LC_ALL=C awk '
function flush_b() {
  if (!has_b) return
  printf "%s\t%s\t%s\t%d\t%s\n", b_id, b_rest, b_skip, b_pcount, b_params
  has_b = 0
}
{
  gsub(/\r$/, "", $0)
  if ($0 == "" || $0 ~ /^#/) next
  if ($0 ~ /^@composable /) {
    flush_b()
    has_b = 1
    b_pcount = 0
    b_params = ""

    rest_line = $0
    sub(/^@composable[ \t]+/, "", rest_line)
    sp = index(rest_line, " ")
    b_id = substr(rest_line, 1, sp - 1)
    attrs = substr(rest_line, sp + 1)
    b_rest = (attrs ~ /restartable=true/) ? "true" : "false"
    b_skip = (attrs ~ /skippable=true/) ? "true" : "false"
    next
  }
  if (has_b && $0 ~ /^  - (stable|unstable|uncertain) /) {
    pline = substr($0, 5)
    sp = index(pline, " ")
    stab = substr(pline, 1, sp - 1)
    rest_p = substr(pline, sp + 1)
    colon = index(rest_p, ":")
    pname = substr(rest_p, 1, colon - 1)
    ptype = substr(rest_p, colon + 1)
    sub(/^[ \t]+/, "", ptype)
    sub(/[ \t]+$/, "", ptype)
    b_pcount++
    pentry = stab "\034" pname "\034" ptype
    b_params = (b_params == "") ? pentry : (b_params "\035" pentry)
  }
}
END { flush_b() }
' "$BASELINE_FULL_PATH" | LC_ALL=C sort -t $'\t' -k1,1 > "$BASELINE_RECORDS"

BASELINE_COUNT=$(wc -l < "$BASELINE_RECORDS" | tr -d ' ')

echo "=== Biodontos Compose Stability Check ==="
echo "Baseline: $BASELINE_FULL_PATH ($BASELINE_COUNT tracked)"
echo "Current : $COMPOSABLES_TXT ($TRACKED tracked, $EXCLUDED Preview(s) excluded)"

set +e
LC_ALL=C awk -F '\t' -v curr_file="$CURRENT_RECORDS" '
BEGIN {
  while ((getline line < curr_file) > 0) {
    split(line, f, "\t")
    id = f[1]
    c_exists[id] = 1
    c_rest[id] = f[2]
    c_skip[id] = f[3]
    c_pcount[id] = f[4] + 0
    c_params[id] = f[5]
    all_ids[id] = 1
  }
  close(curr_file)
}
{
  id = $1
  b_exists[id] = 1
  b_rest[id] = $2
  b_skip[id] = $3
  b_pcount[id] = $4 + 0
  b_params[id] = $5
  all_ids[id] = 1
}
END {
  n_ids = 0
  for (id in all_ids) {
    n_ids++
    ids[n_ids] = id
  }
  for (i = 2; i <= n_ids; i++) {
    key = ids[i]
    j = i - 1
    while (j >= 1 && ids[j] > key) {
      ids[j + 1] = ids[j]
      j--
    }
    ids[j + 1] = key
  }

  add_n = 0; rem_n = 0; reg_n = 0; imp_n = 0
  for (idx = 1; idx <= n_ids; idx++) {
    id = ids[idx]
    in_b = (id in b_exists)
    in_c = (id in c_exists)

    if (!in_b && in_c) {
      add_n++
      adds[add_n] = "+ " id " [restartable=" c_rest[id] ", skippable=" c_skip[id] "]"
      continue
    }
    if (in_b && !in_c) {
      rem_n++
      rems[rem_n] = "- " id
      continue
    }

    reasons = ""
    imp_reasons = ""
    if (b_skip[id] == "true" && c_skip[id] == "false") {
      reasons = "lost skippable (true -> false)"
    } else if (b_skip[id] == "false" && c_skip[id] == "true") {
      imp_reasons = "gained skippable (false -> true)"
    }

    for (k in cp_stab) delete cp_stab[k]
    for (k in cp_type) delete cp_type[k]
    if (c_params[id] != "") {
      nc = split(c_params[id], carr, "\035")
      for (j = 1; j <= nc; j++) {
        split(carr[j], cp, "\034")
        if (!(cp[2] in cp_stab)) {
          cp_stab[cp[2]] = cp[1]
          cp_type[cp[2]] = cp[3]
        }
      }
    }

    if (b_params[id] != "") {
      nb = split(b_params[id], barr, "\035")
      for (j = 1; j <= nb; j++) {
        split(barr[j], bp, "\034")
        pname = bp[2]
        if (pname in cp_stab) {
          if (bp[1] == "stable" && cp_stab[pname] == "unstable") {
            msg = "param \047" pname "\047 degraded (stable -> unstable: " cp_type[pname] ")"
            reasons = (reasons == "") ? msg : (reasons "; " msg)
          } else if (bp[1] == "unstable" && cp_stab[pname] == "stable") {
            imsg = "param \047" pname "\047 improved (unstable -> stable: " cp_type[pname] ")"
            imp_reasons = (imp_reasons == "") ? imsg : (imp_reasons "; " imsg)
          }
        }
      }
    }

    if (reasons != "") {
      reg_n++
      regs[reg_n] = "~ " id ": " reasons
    } else if (imp_reasons != "") {
      imp_n++
      imps[imp_n] = "^ " id ": " imp_reasons
    }
  }

  if (add_n > 0) {
    printf "\nAdded composables (%d):\n", add_n
    for (i = 1; i <= add_n; i++) printf "  %s\n", adds[i]
  }
  if (rem_n > 0) {
    printf "\nRemoved composables (%d):\n", rem_n
    for (i = 1; i <= rem_n; i++) printf "  %s\n", rems[i]
  }
  if (imp_n > 0) {
    printf "\nStability improvements (%d):\n", imp_n
    for (i = 1; i <= imp_n; i++) printf "  %s\n", imps[i]
  }
  if (reg_n > 0) {
    printf "\nStability REGRESSIONS detected (%d):\n", reg_n
    for (i = 1; i <= reg_n; i++) printf "  %s\n", regs[i]
    printf "\nERROR: Compose stability check FAILED with %d regression(s). Fix instability or justify and run --update-baseline.\n", reg_n > "/dev/stderr"
    exit 1
  }
  printf "\nCompose stability check PASSED (0 regressions; +%d added, -%d removed).\n", add_n, rem_n
  exit 0
}
' "$BASELINE_RECORDS"
RC=$?
set -e
exit "$RC"
