# CaLeap V24 — Food Vision Benchmark Lab

V24 changes the process from “model available” to “model measured”.

## Candidate currently documented

`zeyuai/efficientnet-food-classifier` publishes an Apache-2.0 TFLite artifact and reports 93.3% self-reported accuracy, but only for 8 classes and with limitations around single, clearly visible food images. It is therefore a benchmark/runtime candidate, not a CaLeap production model.

## Benchmark design

The test suite must include:
- Indian vegetarian foods
- Indian non-vegetarian foods
- regional/global cuisines
- mixed plates
- multiple foods
- occlusion
- low light
- non-food images
- visually similar foods

Minimum production-gate dataset: 200 documented cases.

## Required metrics

1. Top-1 accuracy
2. Top-5 accuracy
3. Abstention rate
4. Per-class recall
5. Confidence calibration
6. Non-food rejection
7. Multiple-food failure rate

No model accuracy is claimed by CaLeap until measured on the documented benchmark.

## V24 outcome

The application now has a reusable benchmark evaluator and confidence-calibration foundation. The next stage is to feed a real verified TFLite artifact and benchmark it. If the model fails on the target distribution, CaLeap should reject it rather than ship it.
