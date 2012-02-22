
A port of gnupg to Android.

Build Setup
-----------

On Debian/Ubuntu/Mint/etc.:

  sudo apt-get install autoconf automake libtool transfig wget patch texinfo

The build also needs chrpath in order to set the rpath in the binaries
to find their shared libraries once installed in their place on
Android.  The chrpath also needs to match the bitness of the target
CPU.  So if you are compiling for 32-bit ARM, then the chrpath needs
to be i386 or some other 32-bit architecture.  If you are compiling
for 64-bit ARM, then the chrpath needs to be amd86, x86_64, or some
other 64-bit architecture.

If the bitness of your build machine matches the bitness of your
devices, then you can just:

  sudo apt-get install chrpath

If you are building on a 64-bit machine but targetting 32-bit ARM,
then you need to do:

 1. download the 32-bit version of the package for chrpath for your
    distro and release:
    http://packages.debian.org/chrpath
    http://packages.ubuntu.com/chrpath

 2. sudo dpkg -i --force-architecture chrpath
