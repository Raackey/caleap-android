# CaLeap V23 — Verified Model Gate

V23 adds the actual inference decoder and a strict production gate.

## Model gate

A food model is NOT considered bundled or production-ready unless all are verified:

1. Actual `.tflite` binary is present.
2. SHA-256 matches the recorded artifact.
3. License is verified for commercial distribution.
4. Input tensor shape/type is verified.
5. Preprocessing is reproduced exactly.
6. Output tensor shape/type is verified.
7. Label map is verified.
8. Benchmark set is available.
9. Top-1 and useful top-K performance are measured.
10. Unknown/low-confidence cases are tested.

## Runtime

The adapter currently supports a common float32 RGB image-classification contract. It is intentionally manifest-driven. A model with a different tensor contract must get a model-specific adapter rather than silently using the wrong decoder.

## Important

The package contains a manifest template, not a fabricated `.tflite` model. This keeps the APK honest while making the actual integration path executable as soon as the verified artifact is supplied.

## Next

After the artifact is independently verified, place it in `app/src/main/assets/`, populate the manifest, run the benchmark set, and only then enable it through the runtime resolver.
