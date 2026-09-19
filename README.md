# Word2Prompt Android V8.3.1

This is the Android client for the Word2Prompt V8.3 Dynamic Intelligence web/backend. It is designed to use the same W2P orchestration APIs rather than implementing a separate intelligence brain on-device.

## Flow
Create → Intent/Plan → Material Questions → Multi-angle Research → Creation Intelligence → Prompt Intelligence → Large Result Workspace → Prompt Quality → Result Inspection → Auto-Fix → Creation Job.

## No-GPU
The Android app has no GPU/model dependency and never stores provider API keys. It calls the Word2Prompt backend over HTTPS.

## Result Workspace
The result is the primary workspace. The prompt is scrollable and large; secondary intelligence is collapsible. Generated visual results can be selected from the device and sent to `/api/result-intelligence`. Verified mismatches can be sent to `/api/auto-fix`.

## Backend
Default base URL: `https://www.word2prompt.in`. Change `W2PApi` if a staging backend is required.

## Android Studio
Open the `w2p_android_v83` directory in Android Studio and allow Gradle dependencies to sync. The project uses Kotlin + Jetpack Compose and requires compile/target SDK 35.

## Important
This package is a complete Android client built against the current W2P V8.3 web API contract. It is not a byte-for-byte continuation of an unavailable older native project. Existing web intelligence remains the source of truth.

## V8.3.2 Premium Product Layer

V8.3.2 adds the production-oriented Android product shell on top of the supplied V8.3.1 intelligence client: premium Home, Create, Library and Account surfaces; email account creation/sign-in; persistent W2P session cookies; usage/subscription status; and billing-order UI wired to the existing backend contract.

Live payment settlement is deliberately not faked. The supplied backend currently creates billing orders and supports mock completion for local QA; a real provider checkout/webhook must be configured before accepting real money.


## V8.3.8 Intent Engine 2.0
The Android client now calls the shared `/api/intent-engine` before `/api/v8-3/plan`, displaying intent confidence, requirement completeness, research decision and adaptive questions.
