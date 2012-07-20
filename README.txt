
A port of the whole gnupg 2.1 suite to Android.

Target Platform
---------------

We would like to target as many Android platforms as possible.  Currently
there are two limiting APIs:

regex
    provided in Android 2.2, SDK android-8 and above
pthread_rwlock*
    provided in Android 2.3, SDK android-9 and above

regex could easily be included in the build, pthread_rwlock* would be more
difficult.


Build Setup
-----------

On Debian/Ubuntu/Mint/etc.:

  sudo apt-get install autoconf automake libtool transfig wget patch \
       texinfo ant gettext build-essential ia32-libs bison

Install the Android NDK for the command line version, and the Android SDK for
the Android app version:

SDK: http://developer.android.com/sdk/
NDK: http://developer.android.com/sdk/ndk/


How to Build the Command Line Utilities
---------------------------------------


git submodule init
git submodule update
make -C external/ gnupg-install
make -C external/ gnupg-static

The results will be in external/data/data/info.guardianproject.gpg


How to Build the Android Test App
---------------------------------

make -C external/ android-assets
android update project --path . --target android-8 \
  --name GnuPrivacyGuard
ant clean debug

