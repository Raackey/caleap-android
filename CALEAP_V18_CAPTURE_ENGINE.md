# CaLeap V18 — Real SHOW CALEAP Capture Engine

V18 moves capture from separate camera/gallery/voice callbacks into one session lifecycle.

## Core lifecycle
CAPTURING → PREPARING → UNDERSTANDING → REVIEW → SAVED

Error is explicit and does not silently become a successful result.

## Inputs
- Camera bitmap
- Gallery image
- Voice transcript

All routes enter the same `CaptureSessionCoordinator` and then `CaLeapCoreCaptureEngine`.

## Intelligence contract
SEE → UNDERSTAND → CONFIRM → SAVE

The current phone-side visual perception remains the existing ML Kit labeler. V18 does **not** claim a specialized food model is installed. The open-model adapter remains pluggable for V19.

## Trust rules
- Preserve confidence when available.
- Preserve raw voice text and visual labels for explainability.
- Surface errors explicitly.
- Do not invent food identity or nutrition when evidence is weak.
- Keep nutrition as ranges where uncertainty exists.

## Version
1.8.0 / versionCode 20
