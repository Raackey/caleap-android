# CaLeap V25 — Production Model Gate

V25 is the production safety gate between a researched model and a shipped model.

## Candidate

`zeyuai/efficientnet-food-classifier` is documented as an Apache-2.0 EfficientNet-B0 food classifier with a published TFLite artifact and 8 labels. Its model card reports self-reported accuracy of about 93.3%, but also states important limitations. Those reported numbers are NOT copied into CaLeap's benchmark result.

## Gate

The model can enter the production route only when:
- actual binary is bundled,
- SHA-256 is recorded and verified,
- license is verified,
- tensor contract is verified,
- preprocessing is verified,
- label map is verified,
- CaLeap benchmark is executed,
- benchmark metrics are recorded.

Until then, the router uses the safe fallback path.

## Important build limitation

The current build environment has no outbound network access for downloading the Hugging Face binary artifact. Therefore V25 intentionally does NOT invent or embed a fake `.tflite` file. The project is ready for the exact artifact to be supplied/verified.

## Next

Once the real TFLite artifact is available, add it to `app/src/main/assets/`, calculate SHA-256, run inference against the benchmark suite, populate the report, and unlock the production gate only if the results justify it.
