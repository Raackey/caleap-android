# CaLeap V13 — Global Food Graph

V13 adds a relationship-first food graph.

## Why

A classifier should identify a candidate food without being responsible for:
- nutrition
- recipes
- ingredients
- portions
- country context
- localization

The graph provides those relationships as separate layers.

## Current relationship types

- CONTAINS_INGREDIENT
- HAS_TAG

The graph is intentionally expandable to:
- HAS_VARIANT
- COMMONLY_SERVED_WITH
- PREPARED_AS
- REGION_VARIANT
- ALIAS_OF
- HAS_PORTION
- HAS_NUTRITION_SOURCE

## Multilingual design

Canonical food IDs remain language-neutral.
Localized aliases map to the same canonical ID.

Example:

`food:idli`

English: idli / idly
Kannada: ಇಡ್ಲಿ
Hindi: इडली

This allows recognition/search to converge on one food identity while preserving local language.

## Portion principle

Portion estimates are presented as options and require confirmation when evidence is insufficient. No fabricated gram value is inserted merely because a model guessed a dish.
