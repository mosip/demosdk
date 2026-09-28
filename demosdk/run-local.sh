#!/usr/bin/env bash
# demosdk local runner — Linux, macOS, Windows Git Bash/MSYS.
# Windows cmd: use run-local.bat (standalone).
#
# demosdk is a library (no main class), so there is no start/stop: the commands
# build, test, measure coverage and install the JAR into the local Maven repo.
#
#   ./run-local.sh init | test | coverage | install | javadoc | deps | sonar | clean | all
# Optional env: MAVEN_REPO (default ~/.m2/repository)  MVN_ARGS (extra Maven args)
#               SONAR_TOKEN SONAR_ORG (sonar)
set -euo pipefail

MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_DIR="${MODULE_DIR}/.local"
LOG_DIR="${LOCAL_DIR}/logs"
MODULE="demosdk"
POM="${MODULE_DIR}/pom.xml"
VERSION="$(sed -n '/<artifactId>demosdk<\/artifactId>/,/<version>/ s:.*<version>\(.*\)</version>.*:\1:p' "$POM" | head -n 1)"
KERNEL_CORE_VERSION="$(sed -n 's:.*<kernel.core.version>\(.*\)</kernel.core.version>.*:\1:p' "$POM" | head -n 1)"
REPO_ARGS=()
if [[ -n "${MAVEN_REPO:-}" ]]; then
  REPO_ARGS=("-Dmaven.repo.local=${MAVEN_REPO}")
fi
MAVEN_REPO="${MAVEN_REPO:-${HOME}/.m2/repository}"
JACOCO_CSV="${MODULE_DIR}/target/site/jacoco/jacoco.csv"
JACOCO_HTML="${MODULE_DIR}/target/site/jacoco/index.html"
COVERAGE_MIN="90"
UNAME_S="$(uname -s 2>/dev/null || echo unknown)"
read -r -a EXTRA_ARGS <<< "${MVN_ARGS:-}"

MVN_SKIP=(
  "-Dgpg.skip=true"
  "-Dmaven.javadoc.skip=true"
)

usage() {
  cat <<EOF
Local ${MODULE} ${VERSION} (library — build / test / coverage / install)

  Linux / macOS / Git Bash:
    ./run-local.sh <command>
  Windows cmd:
    run-local.bat <command>

  init      package the JAR (skip tests)
  test      run unit tests
  coverage  tests + JaCoCo report + ${COVERAGE_MIN}% gate, prints summary
  install   install JAR into ${MAVEN_REPO}
  javadoc   generate API docs (target/reports/apidocs)
  deps      dependency tree -> .local/logs/deps.txt
  sonar     mvn verify -Psonar (needs SONAR_TOKEN, SONAR_ORG)
  clean     remove target/ and .local/
  all       init + coverage + install

Optional env: MAVEN_REPO MVN_ARGS SONAR_TOKEN SONAR_ORG
EOF
  print_artifacts
  exit "${1:-0}"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "error: '$1' is required on PATH" >&2
    exit 1
  }
}

ensure_dirs() {
  mkdir -p "$LOG_DIR"
}

print_artifacts() {
  echo
  echo "${MODULE}  version=${VERSION}  kernel-core=${KERNEL_CORE_VERSION}"
  echo "  jar         target/${MODULE}-${VERSION}.jar"
  echo "  fat jar     target/${MODULE}-${VERSION}-jar-with-dependencies.jar"
  echo "  coverage    target/site/jacoco/index.html"
  echo "  javadoc     target/reports/apidocs/index.html"
  echo "  maven       io.mosip.demosdk:${MODULE}:${VERSION}"
  echo
}

check_prereqs() {
  need_cmd java
  need_cmd mvn
  echo "os: ${UNAME_S}"
  local ver
  ver="$(java -version 2>&1 | head -n 1 || true)"
  echo "java: $ver"
  if ! echo "$ver" | grep -E '"21[\. "]' >/dev/null 2>&1; then
    echo "warn: JDK 21 is required. Continuing anyway." >&2
  fi
  local kc="${MAVEN_REPO}/io/mosip/kernel/kernel-core/${KERNEL_CORE_VERSION}"
  if [[ ! -d "$kc" ]]; then
    echo "warn: kernel-core ${KERNEL_CORE_VERSION} not in ${MAVEN_REPO}." >&2
    echo "      Maven will try Central snapshots; or build it first:" >&2
    echo "      (cd ../../commons/kernel/kernel-core && mvn clean install -Dgpg.skip=true -DskipTests)" >&2
  fi
}

mvn_mod() {
  (
    cd "$MODULE_DIR"
    mvn -B "$@" ${REPO_ARGS[@]+"${REPO_ARGS[@]}"} ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}
  )
}

print_coverage() {
  if [[ ! -f "$JACOCO_CSV" ]]; then
    echo "warn: no JaCoCo report at ${JACOCO_CSV}" >&2
    return 0
  fi
  awk -F, 'NR>1 {
      im+=$4; ic+=$5; bm+=$6; bc+=$7; lm+=$8; lc+=$9
      printf "  %-20s line %3d%%  branch %3s\n", $3, ($8+$9)?100*$9/($8+$9):100, ($6+$7)?sprintf("%d%%",100*$7/($6+$7)):"n/a"
    }
    END {
      printf "  %-20s line %3d%%  branch %3d%%  instruction %3d%%\n", "TOTAL",
        (lm+lc)?100*lc/(lm+lc):100, (bm+bc)?100*bc/(bm+bc):100, (im+ic)?100*ic/(im+ic):100
    }' "$JACOCO_CSV"
  echo "  report  ${JACOCO_HTML}"
}

cmd_init() {
  check_prereqs
  echo "==> packaging ${MODULE} (skip tests)"
  mvn_mod clean package -DskipTests "${MVN_SKIP[@]}"
  echo "init complete"
  print_artifacts
}

cmd_test() {
  check_prereqs
  echo "==> maven tests"
  mvn_mod test "${MVN_SKIP[@]}"
}

cmd_coverage() {
  check_prereqs
  echo "==> tests + JaCoCo (gate ${COVERAGE_MIN}%)"
  mvn_mod clean verify "${MVN_SKIP[@]}"
  echo
  echo "coverage (excludes **/config/**)"
  print_coverage
}

cmd_install() {
  check_prereqs
  echo "==> installing ${MODULE} ${VERSION} into ${MAVEN_REPO}"
  mvn_mod clean install "${MVN_SKIP[@]}"
  echo "installed io.mosip.demosdk:${MODULE}:${VERSION}"
}

cmd_javadoc() {
  check_prereqs
  echo "==> javadoc"
  mvn_mod javadoc:javadoc
  echo "javadoc  ${MODULE_DIR}/target/reports/apidocs/index.html"
}

cmd_deps() {
  check_prereqs
  ensure_dirs
  echo "==> dependency tree"
  mvn_mod dependency:tree "-DoutputFile=${LOG_DIR}/deps.txt"
  echo "deps  ${LOG_DIR}/deps.txt"
}

cmd_sonar() {
  check_prereqs
  if [[ -z "${SONAR_TOKEN:-}" ]]; then
    echo "error: SONAR_TOKEN is required for sonar" >&2
    exit 1
  fi
  echo "==> sonar analysis"
  local args=("-Psonar" "-Dsonar.token=${SONAR_TOKEN}")
  if [[ -n "${SONAR_ORG:-}" ]]; then
    args+=("-Dsonar.organization=${SONAR_ORG}")
  fi
  mvn_mod clean verify "${MVN_SKIP[@]}" "${args[@]}"
}

cmd_clean() {
  echo "==> cleaning target/ and .local/"
  rm -rf "${MODULE_DIR}/target" "$LOCAL_DIR"
  echo "clean."
}

cmd_all() {
  echo "==> all: init + coverage + install"
  cmd_init
  cmd_coverage
  cmd_install
}

main() {
  local cmd="${1:-}"
  shift || true
  case "$cmd" in
    -h|--help|help) usage 0 ;;
    init) cmd_init ;;
    test) cmd_test ;;
    coverage) cmd_coverage ;;
    install) cmd_install ;;
    javadoc) cmd_javadoc ;;
    deps) cmd_deps ;;
    sonar) cmd_sonar ;;
    clean) cmd_clean ;;
    all) cmd_all ;;
    "") usage 1 ;;
    *) echo "error: unknown command '$cmd'" >&2; usage 1 ;;
  esac
}

main "$@"
