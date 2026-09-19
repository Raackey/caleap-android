# Word2Prompt Android V8.3.2 — Premium Product Layer

This release builds on the supplied V8.3.1 complete Android workspace without replacing the V8.3 intelligence architecture.

## Implemented
- Premium W2P home/discover surface
- Custom bottom navigation: Home / Create / Library / Account
- Account creation with email + password
- Email sign-in and sign-out
- Persistent authenticated session cookie handling
- Usage balance and subscription status display
- Pro monthly/yearly and ₹1 prompt-credit billing-order UI
- Result Workspace, Result Intelligence and Auto-Fix retained
- No provider API secrets in Android
- V8.2.6 no-GPU policy retained

## Production boundary
The supplied V8.3.1 backend currently exposes billing order creation and mock billing for local QA. It does not expose a configured real-money provider checkout/webhook in the supplied package. Therefore this release does not pretend that a live payment gateway is active.

Google sign-in is supported by the existing web account system, but native Android Google credential integration requires a configured Android OAuth client. This package intentionally does not ship a fake credential flow.

## Build compatibility
Kotlin 1.9.25 is paired with Compose Compiler 1.5.15. Android's compatibility map identifies Compose Compiler 1.5.15 as targeting Kotlin 1.9.25. For Kotlin 2.0+, the Compose Compiler Gradle plugin should be used instead.

## Verification
Run `python3 qa_android_v83_premium.py` from this directory. Actual APK compilation must be verified by GitHub Actions or an Android SDK/Gradle environment.
