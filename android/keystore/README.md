# Signing keys

`dev.jks` is a development key shared by every build of this project (debug and release, local and CI),
so an installed copy can always be updated with a newer APK. It is committed on purpose: keep the
repository private, and never use this key for Google Play.

For Google Play, create a private upload key once (keep it and its passwords safe forever):

    keytool -genkeypair -v -keystore keystore/upload.jks -storetype PKCS12 \
        -alias upload -keyalg RSA -keysize 2048 -validity 10000

Local builds: create `keystore.properties` in the project root (it is git-ignored):

    storeFile=keystore/upload.jks
    storePassword=...
    keyAlias=upload
    keyPassword=...

GitHub Actions: add repository secrets SIGNING_KEYSTORE_BASE64 (output of `base64 -w0 keystore/upload.jks`),
SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS and SIGNING_KEY_PASSWORD. The workflow then also builds the
.aab bundle for the Play Console.

Note: switching an already-installed app from the dev key to the upload key requires uninstalling it
first (Android refuses updates signed with a different key), so decide before distributing widely.
