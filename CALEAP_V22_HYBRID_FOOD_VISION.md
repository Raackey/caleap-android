# CaLeap V22 — Hybrid Food Vision Runtime

V22 adds the TensorFlow Lite runtime boundary and a strict verified-model resolver.

## Current architecture

PHOTO
→ FoodVisionRuntime
→ verified TFLite model when artifact + validation are present
→ otherwise explicit unavailable state
→ existing ML Kit/general vision fallback remains available through the V21 path
→ candidate fusion
→ recognition policy
→ Food Graph
→ variant / portion / nutrition

## Why no model binary is silently bundled

A model card can identify an Apache-2.0 model and publish a mobile file, but CaLeap still needs to verify the exact binary, checksum, tensor contract and preprocessing before putting it in a production APK.

The researched `zeyuai/efficientnet-food-classifier` model publishes an Apache-2.0 TFLite artifact and 8 food categories, but its own card says it is limited to clearly visible single-item images. That makes it useful as a runtime integration candidate, not sufficient by itself for CaLeap's global food intelligence.

## V22 quality gate

Do not mark the model as production-ready until:
1. the exact TFLite binary is present,
2. SHA-256 is recorded,
3. tensor input/output contract is tested,
4. preprocessing is reproduced exactly,
5. labels are verified,
6. representative Indian + global photos are benchmarked,
7. low-confidence and unknown cases are tested,
8. licensing/provenance is stored.

Next phase: install the verified artifact and implement the model-specific tensor preprocessing/output decoder, then benchmark it against CaLeap's Food Graph.
