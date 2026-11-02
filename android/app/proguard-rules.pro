# Project-specific R8 rules.
# AndroidX, Google Mobile Ads and kotlinx.serialization ship their own consumer rules.
# If a release build crashes but the debug build works, a class is being removed or renamed:
# add a -keep rule for it here (see references/troubleshooting.md).

# Methods the platform page calls through window.DorossiApp must keep their names.
-keepclassmembers class org.dorossi.app.ui.platform.PlatformBridge {
    @android.webkit.JavascriptInterface <methods>;
}
