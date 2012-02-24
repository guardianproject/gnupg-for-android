
A port of the whole gnupg 2.1 suite to Android.


Build Setup
-----------

On Debian/Ubuntu/Mint/etc.:

  sudo apt-get install autoconf automake libtool transfig wget patch \
       texinfo ant

Install the Android NDK for the command line version, and the Android SDK for
the Android app version:

SDK: http://developer.android.com/sdk/
NDK: http://developer.android.com/sdk/ndk/


How to Build the Command Line Utilities
---------------------------------------

make -C external/ gnupg-install
make -C external/ gnupg-static

The results will be in external/data/data/info.guardianproject.gpg


How to Build the Android Test App
---------------------------------

make -C external/ android-assets
android update project --path . --target android-8 \
  --name GnuPrivacyGuard
ant clean debug

