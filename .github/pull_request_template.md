<!-- PR title must follow Conventional Commits, e.g. `feat(overview): show liabilities breakdown`. -->

## Summary

<!-- What does this PR do and why? One or two sentences. -->

## Changes

<!-- Bullet list of the notable changes, grouped by module (e.g. feature/overview, composeApp, androidApp, iosApp, CI). -->

-

## Test plan

<!--
What you ran and what it showed. REQUIRED: also list what you could NOT run and why
(e.g. "iOS build not run — no macOS/Xcode in the agent sandbox; relying on the build-ios CI job").
-->

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew ktlintCheck verifyTranslations :androidApp:lintGithubDebug`
- [ ] New or changed strings are translated in `values-pt/` (English and Portuguese)
- [ ] `./gradlew :androidApp:assembleGithubDebug`
- [ ] Manually verified on: <!-- device / simulator / browser, or "not run" -->

**Not run (and why):**

-

## Risk

<!-- What could break? Blast radius, migrations, rollout/rollback notes. Low / Medium / High + reasoning. -->
