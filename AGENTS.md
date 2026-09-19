# CaLeap Engineering Rules

CaLeap is a premium Personal Health Context Engine, not a generic calorie tracker.

Core loop:
SEE -> UNDERSTAND -> CONNECT -> EXPLAIN -> ACT -> TRACK -> LEARN

Product principles:
- Preserve the CaLeap vision while implementing features.
- Prefer simple, premium mobile UX with one primary action: SHOW CALEAP.
- Food AI must expose uncertainty and allow correction; never present false precision.
- Ask only necessary questions.
- Never diagnose, prescribe, change medicine doses, or create fake medical certainty.
- Health data is private by default; family sharing requires explicit user control.
- Do not add features merely to imitate competitors; adapt useful patterns to CaLeap's context engine.
- Do not regress working functionality while adding new features.

Engineering:
- Use Kotlin + Jetpack Compose.
- Keep modules small and maintainable.
- Do not change Gradle/AGP/SDK versions casually; verify compatibility before changing them.
- After a build failure, inspect the exact error before editing dependencies.
- Never replace the product architecture with a generic template just to make a build pass.
