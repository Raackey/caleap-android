from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
APP = ROOT / 'app' / 'src' / 'main' / 'java' / 'com' / 'word2prompt' / 'android'
main = (APP / 'MainActivity.kt').read_text()
api = (APP / 'data' / 'W2PApi.kt').read_text()
vm = (APP / 'ui' / 'Word2PromptViewModel.kt').read_text()
build = (ROOT / 'build.gradle.kts').read_text()
app_build = (ROOT / 'app' / 'build.gradle.kts').read_text()

def check(label, condition):
    print(('PASS' if condition else 'FAIL') + ' ' + label)
    if not condition:
        raise SystemExit(1)

check('V8.3.8 Android version', 'versionCode = 838' in app_build and 'versionName = "8.3.8"' in app_build)
check('Kotlin/Compose compiler compatibility', 'version "1.9.25"' in build and 'kotlinCompilerExtensionVersion = "1.5.15"' in app_build)
check('Java/Kotlin 17 target', 'JavaVersion.VERSION_17' in app_build and 'jvmTarget = "17"' in app_build)
check('Premium navigation is custom and not NavigationBarItem', 'PremiumBottomBar' in main and 'NavigationBarItem' not in main)
check('Account creation and sign-in UI', 'Create account' in main and 'Sign in' in main and 'AccountScreen' in main)
check('Persistent session cookies', 'PersistentCookieJar' in api and 'cookieJar(cookieJar)' in api)
check('Auth API contract wired', all(x in api for x in ['/api/session','/api/auth/register','/api/auth/login','/api/auth/logout']))
check('Usage and billing contract wired', '/api/usage' in api and '/api/billing/order' in api)
check('V8.3 intelligence flow retained', all(x in main for x in ['PlanScreen','ResultScreen','Result Intelligence','Auto-Fix Verified Mismatches']))
check('Backend base URL retained', 'https://www.word2prompt.in' in api)
check('Onboarding experience present', 'OnboardingScreen' in main and 'onboarding_complete' in main)
check('Trending API and UI present', 'api.trends()' in vm and 'TrendingCard' in main and 'trends' in api)
check('Trend-to-create handoff', 'startFromTrend' in vm and 'onTrend' in main)
check('Persistent local library', 'saveCurrentToLibrary' in vm and 'loadLibrary' in vm and 'library' in main)
check('Android Sharesheet sharing', 'Intent.createChooser' in main and 'Intent.ACTION_SEND' in main)
check('Bitmap import regression', 'import android.graphics.Bitmap' in main and 'Bitmap.createScaledBitmap' in main)
check('Password visibility control present', 'PasswordVisualTransformation' in main and 'passwordVisible' in main)
check('No provider secrets in Android source', not re.search(r'AIza[0-9A-Za-z_-]{20,}|OPENAI_API_KEY|GEMINI_API_KEY|sk-[A-Za-z0-9_-]{20,}', main + api + vm))
check('Core pipeline stages', 'PipelineStage' in main and 'PipelineStatusCard' in main)
check('Intent Engine 2.0 client contract', 'intentEngine' in api and '/api/intent-engine' in api and 'intentAnalysis' in main + vm)
check('Intent Engine UI visibility', 'INTENT ENGINE 2.0' in main and 'Requirement completeness' in main)
check('Server-backed usage gate client', 'consumePrompt' in vm and 'UsageConsumeResult' in api)
check('Source-of-truth image input', 'sourceImageName' in main and 'setSourceImage' in vm and 'sourceImageDataUrl' in api)
print('ANDROID V8.3.8 GHOST KILL SOURCE CHECK: PASS')
