# CaLeap Global Country Intelligence

CaLeap V10 adds the current ISO 3166-1 country/area code catalog used as the global geography backbone.

Fields stored:
- English name
- ISO alpha-2
- ISO alpha-3
- ISO numeric-3

Why this matters:
- Country-aware onboarding
- Regional food intelligence
- Localization
- Food/recipe regionalization
- Health-context regional defaults
- Future currency/language/time-zone mapping

Source basis: ISO 3166-1. ISO describes alpha-2 as the recommended general-purpose code, alpha-3 as a three-letter code, and numeric-3 as a numeric representation. ISO states that the country codes can be used free of charge.

Implementation note: the bundled catalog is generated from the pycountry ISO dataset available in the build environment. Before production release, refresh/verify the catalog against the ISO 3166 Maintenance Agency / Online Browsing Platform because ISO maintains and updates the standard.
