# GNUPG for Android

**A port of the whole gnupg 2.1 suite to Android.**

If you are using these tools in your own apps, we'd love to hear about it. Email us
at root@guardianproject.info.

## Using GnuPG from the Terminal

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

### Setting up all of the tools

To enable the whole suite of tools, including dirmngr to work with keyservers,
you need to set another environment variable:

    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/data/data/info.guardianproject.gpg/app_opt/lib:/data/data/info.guardianproject.gpg/lib

The technical reason why is that GnuPG uses a lot of shared libraries, and
the only way Android has for finding shared libraries is the `LD_LIBRARY_PATH`
environment variable.  GNU/Linux has `rpath`, Mac OS X has *install names*,
but Android has none of this stuff.

## Target Platform

We would like to target as many Android platforms as possible.  Currently
there are two limiting APIs:

* regex:
    provided in Android 2.2, SDK android-8 and above
* pthread_rwlock\*:
    provided in Android 2.3, SDK android-9 and above

regex could easily be included in the build, pthread_rwlock\* would be more 
difficult.


## Build Setup

On **Debian/Ubuntu/Mint/etc.**:

	sudo apt-get install autoconf automake libtool transfig wget patch \
	texinfo ant gettext build-essential ia32-libs bison

On **Fedora 17 x64**:

	sudo yum install ncurses-libs.i686 libstdc++.i686 libgcc.i686 zlib.i686 gcc.i686

Install the Android NDK for the command line version, and the Android SDK for
the Android app version:

SDK: http://developer.android.com/sdk/
NDK: http://developer.android.com/sdk/ndk/


## Building

First the get all of the source code from git:

	git clone https://github.com/guardianproject/gnupg-for-android
	git submodule init
	git submodule update


### How to build the whole app

    make -C external/ distclean clean-assets
	make -C external/
	ndk-build clean
	ndk-build
	android update project --path . --name GnuPrivacyGuard
	ant clean debug


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


# Testing

## pinentry

Testing pinentry is easiest on a rooted device

    adb shell
    $ su
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/data/data/info.guardianproject.gpg/app_opt/lib
    export PATH=$PATH:/data/data/info.guardianproject.gpg/app_opt/bin
    export HOME=/data/data/info.guardianproject.gpg/app_home
    export GNUPGHOME=/data/data/info.guardianproject.gpg/app_home
    # to test pinentry we want to import a secret key
    # start adb logcat in another terminal so you dont miss the action
    gpg2 --import /data/data/info.guardianproject.gpg/app_opt/tests/pinentry/secret-keys.gpg
