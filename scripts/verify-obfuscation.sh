#!/usr/bin/env bash
#
# Asserts that the release build really is obfuscated, and that the names this app (or a library it
# uses) resolves reflectively survived R8. Run after `./gradlew :androidApp:assembleRelease`;
# invoked by the "Minified release" job in .github/workflows/pr.yml and by release.yml.
#
# Why: isMinifyEnabled / isShrinkResources (androidApp/build.gradle.kts) and the keep rules in
# androidApp/proguard-rules.pro can regress silently — a stray blanket `-keep`, or minification
# switched off — and the build still succeeds. Only R8's mapping.txt shows what actually happened, so
# reading it turns "obfuscation is on and reflective lookups still resolve" into a CI gate.
set -euo pipefail

readonly APP_PACKAGE_PREFIX="com.neteinstein.networth."

# Each must keep its original name (a rename is a runtime crash, not a build error):
# - Credential Manager loads its Play Services provider by class name (Google sign-in on Android);
#   pinned by the androidx.credentials rule in proguard-rules.pro.
# - kotlinx.serialization can look generated `$$serializer` classes up by name (the database file
#   schema DTOs); pinned by the `$$serializer` rule in proguard-rules.pro. (Navigation routes may be
#   renamed: their serial names are compile-time strings and they're registered by class reference.)
readonly KEPT_CLASSES=(
    "androidx.credentials.playservices.CredentialProviderPlayServicesImpl"
    "com.neteinstein.networth.core.database.schema.DatabaseDto\$\$serializer"
)

failures=0

fail() {
    echo "::error::$1"
    failures=$((failures + 1))
}

# Counts app classes R8 renamed. Class lines in mapping.txt are unindented ("<original> -> <obfuscated>:");
# member lines are indented, so anchoring on the package prefix at line start matches classes only.
count_renamed_classes() {
    awk -F' -> ' -v prefix="$1" '
        index($0, prefix) == 1 && NF == 2 {
            obfuscated = $2
            sub(/:$/, "", obfuscated)
            if (obfuscated != $1) renamed++
        }
        END { print renamed + 0 }
    ' "$2"
}

verify_mapping() {
    local mapping="$1"
    local variant
    variant="$(basename "$(dirname "$mapping")")"

    local renamed
    renamed="$(count_renamed_classes "$APP_PACKAGE_PREFIX" "$mapping")"
    if [ "$renamed" -eq 0 ]; then
        fail "$variant: no ${APP_PACKAGE_PREFIX}* class was renamed - the release build is not obfuscated. Check isMinifyEnabled in androidApp/build.gradle.kts and look for an over-broad -keep in androidApp/proguard-rules.pro."
    else
        echo "  $variant: $renamed obfuscated app classes"
    fi

    local kept
    for kept in "${KEPT_CLASSES[@]}"; do
        if grep -qxF "$kept -> $kept:" "$mapping"; then
            echo "  $variant: kept $kept"
        else
            fail "$variant: $kept was renamed or removed - its reflective lookup will fail at runtime. Check androidApp/proguard-rules.pro."
        fi
    done
}

mappings=()
while IFS= read -r line; do
    mappings+=("$line")
done < <(find androidApp/build/outputs/mapping -mindepth 2 -maxdepth 2 -name mapping.txt -path '*elease/*' | sort)

if [ ${#mappings[@]} -eq 0 ]; then
    echo "::error::No release mapping.txt under androidApp/build/outputs/mapping/. Run './gradlew :androidApp:assembleRelease' first; if it did run, R8 produced no mapping, so the build is not being minified."
    exit 1
fi

for mapping in "${mappings[@]}"; do
    verify_mapping "$mapping"
done

if [ "$failures" -ne 0 ]; then
    echo "::error::Obfuscation verification failed with $failures problem(s)."
    exit 1
fi

echo "Obfuscation verified across ${#mappings[@]} release variant(s)."
