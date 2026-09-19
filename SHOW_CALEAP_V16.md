# CaLeap V16 — SHOW CALEAP Experience Contract

V16 defines the user-facing state machine for the complete food capture journey.

## User flow

READY
-> CAPTURING
-> UNDERSTANDING
-> FOOD CANDIDATES
-> VARIANT (only if necessary)
-> PORTION
-> NUTRITION REVIEW
-> SAVED
-> TIMELINE / CONTEXT

## Product behavior

The capture screen should have one dominant action: SHOW CALEAP.

After capture, the user should see an explicit understanding state.

When confidence is insufficient, CaLeap presents candidates or asks a focused question.

The user remains in control before a meal is committed.

## Architecture

The UI state model is deliberately independent of the AI model. This allows us to replace or improve the vision/voice model without redesigning the capture experience.
