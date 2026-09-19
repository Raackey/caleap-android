# CaLeap Open AI Sources Registry

This file records open model candidates researched for CaLeap. A model is NOT considered production-integrated merely because it appears here.

| Model | License | Intended use | Production status |
|---|---|---|---|
| dima806/indian_food_image_detection | Apache-2.0 | Indian food image classification | Evaluation candidate |
| therealcyberlord/vit-indian-food | Apache-2.0 | Indian food image classification | Evaluation candidate; checkpoint is large |
| Subhash5/indian-food-classifier | MIT | Indian food image classification | Evaluation candidate |

## Rules

1. Verify the exact model revision and license before shipping.
2. Verify dataset licensing separately from model licensing.
3. Benchmark accuracy on CaLeap's target Indian-food set, not just the model card's reported metric.
4. Benchmark latency and memory on real Android devices.
5. Prefer a mobile-converted/quantized artifact only after validating the conversion.
6. Keep a user correction path and abstain when confidence is insufficient.
7. Never infer nutrition directly from a classifier label; map the confirmed food into CaLeap's food knowledge layer.

## Sources

- https://huggingface.co/dima806/indian_food_image_detection
- https://huggingface.co/therealcyberlord/vit-indian-food
- https://huggingface.co/Subhash5/indian-food-classifier
- https://github.com/e1bhl1n/Indian_Food_Classification
