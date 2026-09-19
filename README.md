# CaLeap Premium Master V16 — SHOW CALEAP Experience

V16 connects the food intelligence orchestration to a production-oriented UI state contract.

Added:
- SHOW CALEAP UI state machine
- capture/understanding/candidate/variant/portion/review/saved/error states
- controller that maps the existing FoodCaptureOrchestrator into UI states
- machine-readable UI contract
- explicit user confirmation before saving

The model layer remains replaceable. This build does not claim production-grade visual AI inference; it creates the correct UI and orchestration boundary for it.

Next:
wire camera/gallery/voice events into this controller, then attach the selected open food model and real source-backed nutrition records.


## V17 — Core Intelligence Upgrade
- Unified `CaLeapCoreCaptureEngine` for voice and vision observations.
- Single perception → identity → variant → portion flow.
- Explicit provenance for every capture path.
- Abstention when evidence is weak instead of inventing food identity.
- Global graph fallback after local food vocabulary matching.
- V17 does not claim a bundled open vision model; the model adapter remains pluggable.
- Version: 1.7.0 / versionCode 19.


## V19 — Real Food AI Architecture
See `CALEAP_V19_REAL_FOOD_AI.md`. The model adapter is intentionally explicit and does not claim a bundled model until a verified artifact is installed.


## V20 — Model-Ready Clean Architecture
The food AI boundary is consolidated into one canonical adapter/service path. See `CALEAP_V20_MODEL_READY.md`.
