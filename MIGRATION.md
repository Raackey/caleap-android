# V8.3.1 Migration / Integration Notes

- Android calls the same `/api/v8-3/*` planner/command endpoints and the same web intelligence endpoints used by the current W2P web client.
- Smart questions are computed locally using the V8.2 material-question policy, then sent back as `answers` so the backend remains authoritative.
- Prompt Quality is requested after Prompt Intelligence.
- Result Intelligence accepts a generated image as a data URL and compares it with the original requirement.
- Auto-Fix receives only verified mismatches and keeps the existing prompt as the baseline.
- Creation remains approval-gated by the backend.
