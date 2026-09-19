# V8.3.8 Build / Check Status

## Verified from the GitHub build log
- GitHub Actions reached `:app:mergeDebugResources`.
- Build failed because `Theme.Word2Prompt` existed in both `app/src/main/res/values/styles.xml` and `app/src/main/res/values/themes.xml`.
- Failure was a duplicate Android resource, not a Kotlin/Compose compilation error.

## V8.3.8 repair applied
- `themes.xml` remains the single authoritative definition of `Theme.Word2Prompt`.
- `styles.xml` is retained but contains no `Theme.Word2Prompt` definition, preventing stale repository copies from recreating the duplicate.
- Added a workflow guard that fails early if the theme is defined more than once.
- Workflow standardized on JDK 17 + Gradle 8.7 for the supplied AGP 8.6.1 project.

## Source configuration
- Version code/name: 838 / 8.3.8
- AGP: 8.6.1
- Gradle workflow: 8.7
- Kotlin: 1.9.25
- Compose Compiler: 1.5.15
- compileSdk: 35
- Java/Kotlin target: 17

## Not yet verified
- New GitHub Actions build after this repair
- APK installation and physical-device testing
- Live account/payment provider configuration

**Status:** REPAIRED SOURCE — GitHub rebuild required for build verification.
