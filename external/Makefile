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
		./configure \
			CC="$(CC)" \
			CFLAGS="$(CFLAGS)" \
			LDFLAGS="$(LDFLAGS)" \
			--disable-languages \
			--enable-static \
			--host=$(HOST)
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
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(prefix)
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
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(prefix)
	$(MAKE) -C libassuan

$(LOCAL)/lib/libassuan.a: libassuan-build
	$(MAKE) -C libassuan DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libassuan.a

libassuan-install: $(LOCAL)/lib/libassuan.a


#------------------------------------------------------------------------------#
# libksba

libksba/config.sub: libksba config.sub
	cp config.sub libksba/config.sub

libksba/config.guess: libksba config.guess
	cp config.guess libksba/config.guess

libksba/configure:
	cd libksba && ./autogen.sh

libksba-build: libgpg-error-install libksba libksba/config.sub libksba/config.guess libksba/configure
	cd libksba && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(prefix)
	$(MAKE) -C libksba

$(LOCAL)/lib/libksba.a: libksba-build
	$(MAKE) -C libksba DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libksba.a

libksba-install: $(LOCAL)/lib/libksba.a

#------------------------------------------------------------------------------#
# gnupg

gnupg/config.sub: gnupg config.sub
	cp config.sub gnupg/config.sub

gnupg/config.guess: gnupg config.guess
	cp config.guess gnupg/config.guess

gnupg/configure:
	cd gnupg && ./autogen.sh

gnupg-build: libgpg-error-install libgcrypt-install libksba-install gnupg gnupg/config.sub gnupg/config.guess gnupg/configure
	cd gnupg && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--with-libgcrypt-prefix=$(LOCAL) \
				--with-libassuan-prefix=$(LOCAL) \
				--with-libksba-prefix=$(LOCAL) \
				--disable-ldap \
				--prefix=$(prefix)
	$(MAKE) -C gnupg

$(LOCAL)/lib/gnupg.a: gnupg-build
	$(MAKE) -C gnupg DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/gnupg.a

gnupg-install: $(LOCAL)/lib/gnupg.a

#------------------------------------------------------------------------------#
# clean

install_clean:
	rm -rf -- bin include lib sbin share

clean: install_clean
	-$(MAKE) -C gnupg clean
	-$(MAKE) -C libksba clean
	-$(MAKE) -C libassuan clean
	-$(MAKE) -C libgcrypt clean
	-$(MAKE) -C libgpg-error clean

distclean: clean
	-$(MAKE) -C gnupg distclean
	-$(MAKE) -C libksba distclean
	-$(MAKE) -C libassuan distclean
	-$(MAKE) -C libgcrypt distclean
	-$(MAKE) -C libgpg-error distclean

.PHONY = clean install_clean \
	libgpg-error-build libgpg-error-install \
	libgcrypt-build libgcrypt-install \
	libassuan-build libassuan-install \
	libksba-build libksba-install \
	gnupg-build gnupg-install
