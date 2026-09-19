# CaLeap V17 — Core Intelligence Contract

## Core loop
SEE → UNDERSTAND → CONNECT → EXPLAIN → ACT → TRACK → LEARN

## Capture contract
Every capture becomes a `CaptureObservation`:
- source: VOICE / CAMERA / GALLERY
- query: normalized user language or best vision label
- labels: raw perception labels
- confidence: perception confidence when available

## Decision policy
1. Normalize input.
2. Match known food vocabulary.
3. Resolve global food variants.
4. Resolve canonical food graph identity.
5. Ask a variant question only when multiple meaningful variants exist.
6. Ask portion only when portion changes the interpretation.
7. Prefer source-backed nutrition records.
8. If evidence is insufficient, abstain and ask for better evidence.
9. Preserve provenance so the UI can explain why a result was produced.

## Model boundary
`OpenFoodModelAdapter` is intentionally pluggable. V17 does not pretend that a remote or unbundled checkpoint is running on-device. A mobile model can be added later without changing the capture UI contract.

## Safety/quality
- No fake exact calories.
- No diagnosis.
- No medication changes.
- Confidence and uncertainty remain visible.
- User correction is a first-class path.
