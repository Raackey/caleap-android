# CaLeap V20 — Model-Ready Clean Architecture

## What was fixed

V20 consolidates the overlapping food-model abstractions from V19 into one canonical path.

```text
App Capture
    ↓
FoodVisionService
    ↓
FoodVisionModelAdapter
    ↓
FoodVisionResult
    ↓
FoodCandidateFusionEngine
    ↓
FoodRecognitionPolicy
    ↓
Global Food Graph
    ↓
Variant / Portion / Nutrition
```

There is now one canonical `FoodVisionModelAdapter`.

## Why the real model is not falsely marked as bundled

A production food model needs more than a model name:
- actual mobile-compatible artifact
- exact version
- checksum
- license suitable for the app's distribution
- runtime compatibility
- input/output contract
- representative validation results

V20 therefore keeps the adapter boundary production-ready and uses an explicit unavailable adapter until those requirements are satisfied.

## Next implementation target

The next model integration should add a real artifact under the app's model assets and implement `FoodVisionModelAdapter` against the selected mobile runtime. After that, run validation on representative Indian and global food images before enabling confident recognition.

## Quality gate

No model output should be converted directly into nutrition. Visual recognition only produces food candidates. Food Graph + source-backed nutrition remains the authority for food knowledge.
