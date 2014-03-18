# Gnu Privacy Guard for Android

**A port of the whole GnuPG 2.1 suite to Android.**

If you are using these tools in your own apps, we'd love to hear about
it. Email us at support@guardianproject.info.

Gnu Privacy Guard (GPG) gives you access to the entire GnuPG suite of
encryption software. GnuPG is GNU’s tool for end-to-end secure communication
and encrypted data storage. This trusted protocol is the free software
alternative to PGP. GnuPG 2.1 is the new modularized version of GnuPG that now
supports OpenPGP and S/MIME.

## Using Gnu Privacy Guard's Android Integration

One of the core goals of Gnu Privacy Guard is to provide integrated encryption
support in a way that feels natural on Android. That means it tries to be as
transparent as possible, and only pop up with there is no other way:

* if you want to send someone an encrypted file, find them in your
  Contacts/People app, and click on "Encrypt File To"
* if you want encrypt something, then share it to Gnu Privacy Guard
* if you want to view an encrypted file, share it with Gnu Privacy Guard and
  it'll prompt you for your password and the app to view it with
* if you want to import keys into your keyring, there are a few ways to do it:
  * click on a .pkr, .skr, .key, or pubring.gpg file in your email, dropbox,
    SD card, etc.
  * click on a fingerprint URL, for example: openpgp4fpr:9F0FE587374BBE81
  * scan a fingerprint QRCode

Look for more features in the Android integration, and please post your ideas
in our issues tracker:
https://dev.guardianproject.info/projects/gpgandroid/issues


## Using Gnu Privacy Guard from the Terminal

Before using Gnu Privacy Guard, be sure to launch the app and let it finish its
installation process. Once it has completed, then you're ready to use it. The
easiest way to get started with Gnu Privacy Guard is to install Android Terminal
Emulator. gpgcli will automatically configure Android Terminal Emulator as
long as you have the "Allow PATH extensions" settings enabled. Get the Android
Terminal Emulator at
https://play.google.com/store/apps/details?id=jackpal.androidterm

Currently, this app offers the full gnupg suite of commands in the terminal.
When you run the GnuPG tools in an app, a GNUPGHOME folder will be created for
that specific app.  Because of the Android permissions model, its not possible
to create a shared GNUPGHOME without having it world-writable.


### Use in Android Terminal Emulator

The app automatically configures Android Terminal Emulator to use the GnuPG
tools, as long as you have the *Allow PATH extensions* preference set.


### Manual configuration and using it with other apps

In order to use the GnuPG tools in your app, preferred terminal emulator, or
adb shell, you need to set the `PATH` to include the full path to the GnuPG
aliases, for example:

    export PATH=$PATH:/data/data/info.guardianproject.gpg/app_opt/aliases

Or you can call the aliases using the full path:

    /data/data/info.guardianproject.gpg/app_opt/aliases/gpg --encrypt secretfile.txt

**WARNING**: The above method stores key material inside the data dir of Gnu Privacy Guard

Gnu Privacy Guard is not able to read your key material, only root or your app
can, but the material will remain after the app is uninstalled. If this is not
desirable for you then you should set the environment variables managed in

    /data/data/info.guardianproject.gpg/app_opt/aliases/common

and set the PATH to `/data/data/info.guardianproject.gpg/app_opt/bin:$PATH`
instead of using the aliases method described above.

At a minimum you should set the environment variables LD_LIBRARY_PATH, HOME,
GNUPGHOME, and PATH.

GNUPGHOME should be set to a secure path inside your app's data directory, for
example you could call `getDir("gnupghome")` from your Activity.


### Setting up all of the tools

To enable the whole suite of tools, including dirmngr to work with keyservers,
you need to set another environment variable:

    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/data/data/info.guardianproject.gpg/app_opt/lib:/data/data/info.guardianproject.gpg/lib

The technical reason why is that GnuPG uses a lot of shared libraries, and
the only way Android has for finding shared libraries is the `LD_LIBRARY_PATH`
environment variable.  GNU/Linux has `rpath`, Mac OS X has *install names*,
but Android has none of this stuff.


## Please Report Bugs

This is an early release of a big project, so there will inevitable be
bugs. Help us improve this software by filing bug reports about any problem
that you encounter. Feature requests are also welcome!
https://dev.guardianproject.info/projects/gpgandroid/issues


## Target Platform

We would like to target as many Android platforms as possible.  Currently
there are two limiting APIs:

* `regex` - provided in Android 2.2, SDK android-8 and above
* `pthread_rwlock*` - provided in Android 2.3, SDK android-9 and above
* `pthread_atfork` - provided in Android 4.0, SDK android-14 and above

regex could easily be included in the build, `pthread_rwlock\*` would be more 
difficult. `pthread_atfork` is not really fully required, but might be in the
future.


## Build Setup

### Debian/Ubuntu/Mint/etc

	sudo apt-get install build-essential autoconf automake-1.11 libtool \
        transfig wget patch texinfo ant gettext bison

### Fedora 17 x64

	sudo yum install autoconf automake libtool transfig wget patch texinfo \
        ant gettext bison \
        ncurses-libs.i686 libstdc++.i686 libgcc.i686 zlib.i686 gcc.i686

You might need to apply some patches to get GnuPG subprojects building with
newer versions of automake:

* http://lists.gnupg.org/pipermail/gnupg-devel/2013-August/027857.html

### Android SDK, NDK, appcompat

Install the Android NDK v9b or newer for the command line version, and the
Android SDK for the Android app version:

* SDK: http://developer.android.com/sdk/
* NDK: http://developer.android.com/sdk/ndk/

If you are building in Eclipse or IntelliJ, you need to also set up the
`android-support-v7-appcompat` library project:

* https://developer.android.com/tools/support-library/setup.html#libs-with-res


## Building

First the get all of the source code from git:

	git clone https://github.com/guardianproject/gnupg-for-android
	git submodule update --init --recursive


### How to build the whole app

    make -C external/ distclean clean-assets
	make -C external/
	ndk-build clean
	ndk-build
	./setup-ant.sh
	ant clean debug


### Running the GnuPG tests

The GnuPG subprojects all include their own test suites.  They end up being
quite large, so they are not included in the APK by default.  If you want to
include all of the tests, then run `make -C external/ assets-tests` before
`ndk-build`.  That gives you the same sequence as used in `jenkins-build.sh`,
which is how we run our tests:

    make -C external/ distclean clean-assets
	make -C external/
	make -C external/ assets-tests
	ndk-build clean
	ndk-build
	./setup-ant.sh
	ant clean debug

Then to run the tests, first install the APK and run it so that it sets up all
of its included assets.  Once the Android app has completed its initial setup,
run:

    ./assets/tests/launch_run-tests_on-android.sh


### How to Build Individual Components

To compile the components individually you can use commands like (the order
that you run them is important):

	make -C external/ gnupg-install
	make -C external/ gnupg-static
	make -C external/ gpgme-install

The results will be in `external/data/data/info.guardianproject.gpg`


### How to Build the Android Test App

    make -C external/ clean-assets
	make -C external/ android-assets
	ndk-build
	android update project --path . --name GnuPrivacyGuard
	ant clean debug

## Hacking Notes

Please conform to our code format standard.

For C files use format-code.sh.

For Java, please apply the official [Android formatter profile](https://android.googlesource.com/platform/development/+/master/ide/eclipse/)

