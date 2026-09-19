# CaLeap Global Food Source Strategy — V11

## Primary global reference

FAO/INFOODS maintains a global directory of food composition tables/databases and describes food composition data as important for nutrition and dietary assessment. The directory includes country, regional and international resources.

Reference:
https://www.fao.org/food-composition/tables-and-databases/

## Country/regional routing

CaLeap V11 introduces a country -> region -> cuisine -> representative-food ontology. This is a routing layer, not a replacement for authoritative nutrient tables.

## Important implementation rule

The FAO/INFOODS directory explicitly notes that inclusion of a database does not imply endorsement. Each underlying database must be evaluated for quality and licensing before its actual data is imported into a commercial CaLeap dataset.

## V11 objective

1. Detect/identify food candidates.
2. Route candidates to the user's country/region/cuisine context.
3. Resolve the food against a nutrition source.
4. Keep source/provenance with the nutrition record.
5. Use ranges/confidence where uncertainty exists.
6. Ask the user only when the distinction changes the nutrition result materially.

## Next phase

Build a normalized Food Knowledge Record:

food_id
canonical_name
aliases
country_iso2
region
cuisine
dish_or_ingredient
serving_units
nutrient_values
nutrient_basis
source_id
source_version
license
confidence
last_verified
