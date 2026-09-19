# CaLeap V15 — End-to-End Food Capture Orchestration

V15 connects the food intelligence layers into one explicit state machine.

## User journey

1. Input — photo or voice
2. Understanding — AI/model analysis
3. Candidate foods
4. Variant disambiguation only when necessary
5. Portion selection
6. Nutrition review
7. User confirmation
8. Save to timeline
9. Feed context engine

## Example

User shows a dosa.

AI candidate:
> Dosa

CaLeap checks the graph.

Variants:
- Plain
- Masala
- Ragi
- Neer

If multiple variants remain plausible, CaLeap asks a targeted question.

After selection:
> How much? Small / Regular / Large

Then nutrition is shown from the source-backed FoodKnowledgeRecord.

The user confirms.

Only then is the meal committed to the timeline.

## Safety/product rule

The orchestrator does not generate nutrition values. It coordinates existing food, variant, portion and provenance layers. This separation makes it easier to test, replace models, and audit data.
