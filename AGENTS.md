# CaLeap Engineering Rules

CaLeap is a Personal Health Context Engine, not a generic calorie tracker.

Core loop:
SEE -> UNDERSTAND -> CONNECT -> EXPLAIN -> ACT -> TRACK -> LEARN

Product promise:
Show CaLeap. Let AI understand it. Tell me what matters.

Engineering rules:
- Preserve the CaLeap product architecture while fixing technical issues.
- Prefer stable, boring dependencies over unnecessary libraries.
- Never change Gradle, AGP, Kotlin, SDK, or dependency versions casually.
- Verify builds after configuration changes.
- Do not invent medical certainty. The app must not diagnose, prescribe, or change medication doses.
- Food estimates should support ranges, confidence, detected items, and user correction rather than fake precision.
- Ask users only for information necessary to improve an answer.
