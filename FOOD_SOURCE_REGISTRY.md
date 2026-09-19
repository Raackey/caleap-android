# CaLeap V12 Food Source Registry

V12 establishes a provenance-first nutrition architecture.

## Required fields for every imported food

- Stable food ID
- Canonical name
- Aliases/localized names
- ISO country
- Region/cuisine
- Food type
- Ingredients
- Serving basis and local serving alternatives
- Nutrient values or ranges
- Source name/version
- Source URL
- License/commercial-use status
- Last verification date
- Confidence

## Principle

Do not populate a nutrient value merely because a number is available online. The source must be traceable and its reuse terms must be reviewed.

## Current seed

The V12 seed intentionally contains null nutrient values for records whose source/licensing review has not been completed. This prevents false precision and makes missing evidence visible.

## Global source discovery

FAO/INFOODS provides a directory of national, regional and international food-composition resources:
https://www.fao.org/food-composition/tables-and-databases/

ICMR-NIN IFCT 2017 is a candidate authoritative Indian source:
https://www.nin.res.in/ebooks/IFCT2017.pdf

Before commercial ingestion, verify the applicable source terms, data provenance and any redistribution restrictions.

## Next ingestion stage

Build a source adapter per approved database, normalize its fields into `FoodKnowledgeRecord`, preserve the original source identifier, and run validation for units, serving basis, nutrient completeness and duplicate foods.
