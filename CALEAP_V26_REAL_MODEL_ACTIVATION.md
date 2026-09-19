# CaLeap V26 — Real Model Activation

V26 contains the final Android-side activation path for the researched
`zeyuai/efficientnet-food-classifier` TFLite model.

Verified from the current model card:
- Apache-2.0 license
- EfficientNet-B0
- 224x224 RGB
- 8 food classes
- TFLite artifact published
- model-card test accuracy 0.9328 (self-reported)

The Android adapter now:
1. loads `food_model.tflite` only if it is actually present,
2. validates the image path,
3. resizes to 224x224,
4. feeds RGB float32 pixel values according to the published TFLite example,
5. decodes 8 output scores,
6. returns top-5 candidates,
7. preserves model identity/version/license,
8. falls back explicitly if the binary is absent.

IMPORTANT:
The current execution environment could verify the public model card and its
published artifact metadata, but could not retrieve the binary artifact itself.
Therefore this package does NOT contain a fabricated or zero-filled model.
The activation switch will automatically become SPECIALIZED_MODEL_ACTIVE
when the genuine `food_model.tflite` is placed in app/src/main/assets/.

This is the correct “real model” activation boundary; no fake binary is used.
