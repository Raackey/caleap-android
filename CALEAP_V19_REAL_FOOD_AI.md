# CaLeap V19 — Real Food AI Architecture

V19 is the bridge from the V18 unified capture engine to a verified, real food-vision model.

## Core pipeline

PHOTO
→ FOOD VISION MODEL
→ CANDIDATES
→ CONFIDENCE
→ CANDIDATE FUSION
→ RECOGNITION POLICY
→ FOOD GRAPH / VARIANT
→ PORTION
→ SOURCE-BACKED NUTRITION
→ USER CONFIRMATION
→ SAVE

## Important quality rule

V19 does **not** pretend that a model is bundled when it is not. The project contains a strict `FoodVisionModelAdapter` boundary and an explicit unavailable adapter. A verified model can be added without rewriting the capture architecture.

## Model requirements

Before bundling any model:
- confirm exact model artifact/version
- confirm license and commercial redistribution terms
- record source and provenance
- benchmark on representative foods
- test false positives and low-confidence images
- keep nutrition separate from visual classification
- preserve an explicit unknown/review path

## Candidate policy

- >= 0.82: candidate may enter the confident path
- 0.55–0.819: user review required
- < 0.55: no reliable match
- model unavailable: explicit unavailable state

Thresholds are engineering defaults, not claims of model accuracy. They must be calibrated against real validation data before production use.

## V19 deliverable

This package establishes the production boundary for a real model while preserving V18 capture and V15 food orchestration layers.
