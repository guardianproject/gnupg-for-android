# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

### these modify the calling shell
# point pkg-config to the .pc files generated from these builds
export PKG_CONFIG_PATH=$(LOCAL)/lib/pkgconfig
# workaround for cross-compiling bug in autoconf
export ac_cv_func_malloc_0_nonnull=yes

CWD = $(shell pwd)
PROJECT_ROOT = $(CWD)
EXTERNAL_ROOT = $(PROJECT_ROOT)/external

# Android NDK setup
NDK_BASE ?=  /usr/local/android-ndk
NDK_PLATFORM_LEVEL ?= 9
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-arm
NDK_UNAME=`uname -s | tr '[A-Z]' '[a-z]'`
NDK_TOOLCHAIN=$(NDK_BASE)/toolchains/arm-linux-androideabi-4.4.3/prebuilt/$(NDK_UNAME)-x86

# to use the real HOST tag, you need the latest libtool files:
# http://stackoverflow.com/questions/4594736/configure-does-not-recognize-androideabi
#HOST := arm-none-linux-gnueabi
HOST := arm-linux-androideabi

# install root for built files
DESTDIR = $(CWD)
# TODO try adding the Android-style /data/app.name here
prefix = 
LOCAL := $(DESTDIR)$(prefix)

PATH := ${PATH}:$(NDK_TOOLCHAIN)/bin:$(LOCAL)/bin

CC := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-g++
CPP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-cpp
LD := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ld
RANLIB := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ranlib
STRIP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-strip \
	--strip-unneeded -R .note -R .comment

CFLAGS = -I$(LOCAL)/include
LDFLAGS = -L$(LOCAL)/lib -Wl,--rpath,$(LOCAL)/lib
#ALL_LDFLAGS = -Wl,--entry=main,-rpath=$(ANDROID_NDK_ROOT)/build/platforms/android-$(NDK_PLATFORM_VER)/arch-arm/usr/lib,-dynamic-linker=/system/bin/linker -L$(NDK_SYSROOT)/usr/lib  -nostdlib -lc -ldl
#LIBS = -lc -ldl


all: libgcrypt-install libassuan-install


#------------------------------------------------------------------------------#
# libgpg-error

libgpg-error/config.sub: libgpg-error config.sub
	cp config.sub libgpg-error/config.sub

libgpg-error/config.guess: libgpg-error config.guess
	cp config.guess libgpg-error/config.guess

libgpg-error/configure:
	cd libgpg-error && ./autogen.sh

libgpg-error-build: libgpg-error/config.sub libgpg-error/config.guess libgpg-error/configure
	cd libgpg-error && \
		./configure CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			--disable-languages --enable-static --host=$(HOST)
	$(MAKE) -C libgpg-error

$(LOCAL)/lib/libgpg-error.a: libgpg-error-build
	$(MAKE) -C libgpg-error DESTDIR=$(DESTDIR) prefix= install

libgpg-error-install: $(LOCAL)/lib/libgpg-error.a


#------------------------------------------------------------------------------#
# libgcrypt

libgcrypt/config.sub: libgcrypt config.sub
	cp config.sub libgcrypt/config.sub

libgcrypt/config.guess: libgcrypt config.guess
	cp config.guess libgcrypt/config.guess

libgcrypt/configure:
	cd libgcrypt && ./autogen.sh

libgcrypt-build: libgpg-error-install libgcrypt libgcrypt/config.sub libgcrypt/config.guess libgcrypt/configure
	cd libgcrypt && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure --enable-maintainer-mode --enable-static --host=$(HOST) --with-gpg-error-prefix=$(LOCAL) --prefix=$(prefix)
	$(MAKE) -C libgcrypt

$(LOCAL)/lib/libgcrypt.a: libgcrypt-build
	$(MAKE) -C libgcrypt DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libgcrypt.a

libgcrypt-install: $(LOCAL)/lib/libgcrypt.a


#------------------------------------------------------------------------------#
# libassuan

libassuan/config.sub: libassuan config.sub
	cp config.sub libassuan/config.sub

libassuan/config.guess: libassuan config.guess
	cp config.guess libassuan/config.guess

libassuan/configure:
	cd libassuan && ./autogen.sh

libassuan-build: libgpg-error-install libassuan libassuan/config.sub libassuan/config.guess libassuan/configure
	cd libassuan && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure --enable-maintainer-mode --enable-static --host=$(HOST) --with-gpg-error-prefix=$(LOCAL) --prefix=$(prefix)
	$(MAKE) -C libassuan

$(LOCAL)/lib/libassuan.a: libassuan-build
	$(MAKE) -C libassuan DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libassuan.a

libassuan-install: $(LOCAL)/lib/libassuan.a

#------------------------------------------------------------------------------#
# clean

install_clean:
	rm -rf -- bin include lib sbin share

clean: install_clean
	-$(MAKE) -C gnupg clean
	-$(MAKE) -C libassuan clean
	-$(MAKE) -C libgcrypt clean
	-$(MAKE) -C libgpg-error clean

distclean: clean
	-$(MAKE) -C gnupg distclean
	-$(MAKE) -C libassuan distclean
	-$(MAKE) -C libgcrypt distclean
	-$(MAKE) -C libgpg-error distclean

.PHONY = libgpg-error-build libgpg-error-install \
	libgcrypt-build libgcrypt-install \
	libassuan-build libassuan-install \
	clean install_clean
