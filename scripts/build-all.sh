#!/usr/bin/env bash
# Build every version target sequentially; jars land in build/libs/<target>/.
# Usage: scripts/build-all.sh [target ...]  (default: all targets under versions/)
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$script_dir/.."

# A stale JAVA_HOME pointing to a removed JDK would abort every invocation;
# fall back to the java on PATH instead.
if [ -n "${JAVA_HOME:-}" ] && [ ! -x "${JAVA_HOME}/bin/java" ]; then
    echo "warning: JAVA_HOME '${JAVA_HOME}' is not a usable JDK, ignoring it" >&2
    unset JAVA_HOME
fi

if [ $# -gt 0 ]; then
    targets=("$@")
else
    targets=()
    for dir in "$root"/versions/*/; do
        targets+=("$(basename "$dir")")
    done
fi

failed=()
for t in "${targets[@]}"; do
    echo "=== Building target $t ==="
    "$root/gradlew" "-Ptarget=$t" build || failed+=("$t")
done

if [ ${#failed[@]} -gt 0 ]; then
    echo "Failed targets: ${failed[*]}" >&2
    exit 1
fi
echo "=== All targets built ==="
