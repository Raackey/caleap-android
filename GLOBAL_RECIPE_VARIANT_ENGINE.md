# CaLeap V14 — Global Recipe & Variant Engine

V14 separates a canonical food from its real-world variants.

Example:

`food:dosa`
- Plain Dosa
- Masala Dosa
- Ragi Dosa
- Neer Dosa

A photo model may only be confident that something is "dosa". The variant engine can then ask a targeted question if the distinction changes the food record.

## Design rule

Do not force a variant when evidence is weak.

Candidate:
> Dosa

Then:
> Is it plain, masala, ragi, or neer dosa?

Only ask when the answer materially changes the ingredients, preparation or nutrition record.

## Recipe representation

Each variant can carry:
- canonical food ID
- aliases
- country/region
- cuisine
- ingredients
- preparation methods
- common serving units
- tags

Nutrition remains linked through the source-aware FoodKnowledgeRecord rather than hard-coded into the variant.

## Next

Build source-backed nutrition variants and recipe normalization, then connect variant selection to the photo/voice confirmation UI.
