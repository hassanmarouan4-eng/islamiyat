# Project-specific R8 rules.
# AndroidX and kotlinx.serialization ship their own consumer rules.
# If a release build crashes but the debug build works, a class is being removed or renamed:
# add a -keep rule for it here (see references/troubleshooting.md).

# Shrink but keep readable class names and line numbers, so a crash report says where it happened.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# Methods the platform page calls through window.DorossiApp must keep their names.
-keepclassmembers class org.dorossi.app.ui.platform.PlatformBridge {
    @android.webkit.JavascriptInterface <methods>;
}
