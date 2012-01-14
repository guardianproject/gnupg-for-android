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


.PHONY = clean install-clean \
	libgpg-error-build libgpg-error-clean \
	libgcrypt-build libgcrypt-clean \
	libassuan-build libassuan-clean \
	libksba-build libksba-clean \
	gnupg-build gnupg-clean \
	pth-clean

all: $(LOCAL)/lib/libgcrypt.a


#------------------------------------------------------------------------------#
# libgpg-error

libgpg-error/config.sub: config.sub
	cp config.sub libgpg-error/config.sub

libgpg-error/config.guess: config.guess
	cp config.guess libgpg-error/config.guess

libgpg-error/configure: libgpg-error/configure.ac
	cd libgpg-error && ./autogen.sh

libgpg-error/Makefile: libgpg-error/config.sub libgpg-error/config.guess libgpg-error/configure
	cd libgpg-error && \
		./configure \
			CC="$(CC)" \
			CFLAGS="$(CFLAGS)" \
			LDFLAGS="$(LDFLAGS)" \
			--disable-languages \
			--enable-static \
			--host=$(HOST)

libgpg-error-build-stamp: libgpg-error/Makefile
	$(MAKE) -C libgpg-error
	touch libgpg-error-build-stamp

libgpg-error-build: libgpg-error-build-stamp

$(LOCAL)/lib/libgpg-error.a: libgpg-error-build
	$(MAKE) -C libgpg-error DESTDIR=$(DESTDIR) prefix= install

libgpg-error-clean:
	-rm -f libgpg-error-build-stamp


#------------------------------------------------------------------------------#
# libgcrypt

libgcrypt/config.sub: config.sub
	cp config.sub libgcrypt/config.sub

libgcrypt/config.guess: config.guess
	cp config.guess libgcrypt/config.guess

libgcrypt/configure: libgcrypt/configure.ac
	cd libgcrypt && ./autogen.sh

libgcrypt/Makefile: libgcrypt/config.sub libgcrypt/config.guess libgcrypt/configure
	cd libgcrypt && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(prefix)

libgcrypt-build-stamp: $(LOCAL)/lib/libgpg-error.a libgcrypt/Makefile
	$(MAKE) -C libgcrypt
	touch libgcrypt-build-stamp

libgcrypt-build: libgcrypt-build-stamp

$(LOCAL)/lib/libgcrypt.a: libgcrypt-build
	$(MAKE) -C libgcrypt DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libgcrypt.a

libgcrypt-clean:
	-rm -f libgcrypt-build-stamp


#------------------------------------------------------------------------------#
# libassuan

libassuan/config.sub: config.sub
	cp config.sub libassuan/config.sub

libassuan/config.guess: config.guess
	cp config.guess libassuan/config.guess

libassuan/configure: libassuan/configure.ac
	cd libassuan && ./autogen.sh

libassuan/Makefile: libassuan/config.sub libassuan/config.guess libassuan/configure
	cd libassuan && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(prefix)

libassuan-build-stamp: $(LOCAL)/lib/libgpg-error.a libassuan/Makefile
	$(MAKE) -C libassuan
	touch libassuan-build-stamp

$(LOCAL)/lib/libassuan.a: libassuan-build-stamp
	$(MAKE) -C libassuan DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/libassuan.a

libassuan-install: $(LOCAL)/lib/libassuan.a

libassuan-clean:
	-rm -f libassuan-build-stamp


#------------------------------------------------------------------------------#
# libksba

libksba/config.sub: config.sub
	cp config.sub libksba/config.sub

libksba/config.guess: config.guess
	cp config.guess libksba/config.guess

libksba/configure: libksba/configure.ac
	cd libksba && ./autogen.sh

libksba-build: $(LOCAL)/lib/libgpg-error.a libksba/config.sub libksba/config.guess libksba/configure
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

libksba-clean:
	-rm -f libksba-build-stamp


#------------------------------------------------------------------------------#
# pth

pth-2.0.7.tar.gz.sig: pth-2.0.7.tar.gz
	wget http://ftp.gnu.org/gnu/pth/pth-2.0.7.tar.gz.sig
	gpg --verify pth-2.0.7.tar.gz.sig

pth-2.0.7.tar.gz:
	wget -c http://ftp.gnu.org/gnu/pth/pth-2.0.7.tar.gz
	sha1sum -c pth-2.0.7.tar.gz.sha1

pth-2.0.7: pth-2.0.7.tar.gz.sig
	tar xzf pth-2.0.7.tar.gz

pth-2.0.7/config.sub: pth-2.0.7 config.sub
	cp config.sub pth-2.0.7/config.sub

pth-2.0.7/config.guess: pth-2.0.7 config.guess
	cp config.guess pth-2.0.7/config.guess

pth-2.0.7/aclocal.m4: /usr/share/libtool/libltdl/aclocal.m4
	cp /usr/share/libtool/libltdl/aclocal.m4 pth-2.0.7/aclocal.m4
#	cd pth-2.0.7 && libtoolize --install --force
#	mkdir pth-2.0.7/m4
#	cp -a /usr/share/aclocal/*.m4 pth-2.0.7/m4

pth-build: pth-2.0.7 pth-2.0.7/aclocal.m4 pth-2.0.7/config.guess pth-2.0.7/config.sub
	patch -N -p0 --reject-file=- < pth-fix-include.patch
	cd pth-2.0.7 && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--prefix=$(prefix) \
				--host=$(HOST) \
				--enable-static \
				--with-mctx-mth=sjlj \
				--with-mctx-dsp=ssjlj \
				--with-mctx-stk=sas
	$(MAKE) -C pth-2.0.7

$(LOCAL)/lib/libpth.a: pth-build
	$(MAKE) -C pth-2.0.7 DESTDIR=$(DESTDIR) prefix= install

pth-install: $(LOCAL)/lib/libpth.a

pth-clean:
	-rm -rf pth-2.0.7
	-rm -f pth-2.0.7.tar.gz.sig*


#------------------------------------------------------------------------------#
# gnupg

GNUPG_SOURCES := $(wildcard gnupg/include/*.h) $(wildcard gnupg/*/*.c)

gnupg/config.sub: config.sub
	cp config.sub gnupg/config.sub

gnupg/config.guess: config.guess
	cp config.guess gnupg/config.guess

gnupg/configure:
	cd gnupg && ./autogen.sh

gnupg/Makefile: gnupg/configure gnupg/config.sub gnupg/config.guess
	cd gnupg && \
		CC="$(CC)" CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--with-libgcrypt-prefix=$(LOCAL) \
				--with-libassuan-prefix=$(LOCAL) \
				--with-ksba-prefix=$(LOCAL) \
				--with-pth-prefix=$(LOCAL) \
				--enable-symcryptrun \
				--disable-ldap \
				--without-ldap \
				--prefix=$(prefix)

gnupg-build: gnupg-build-stamp 
gnupg-build-stamp: gnupg/Makefile $(LOCAL)/lib/libgpg-error.a $(LOCAL)/lib/libgcrypt.a $(LOCAL)/lib/libksba.a $(LOCAL)/lib/libassuan.a $(LOCAL)/lib/libpth.a
#gnupg-build-stamp: gnupg/Makefile
	$(MAKE) -C gnupg
	touch gnupg-build-stamp

$(LOCAL)/bin/gpg2: gnupg-build $(GNUPG_SOURCES) gnupg/configure
	$(MAKE) -C gnupg DESTDIR=$(DESTDIR) prefix= install
	ls -l $(LOCAL)/lib/gnupg.a

gnupg-clean:
	-rm -f gnupg-build-stamp

#------------------------------------------------------------------------------#
# clean

install-clean:
	rm -rf -- bin include lib man sbin share

clean: install-clean gnupg-clean libksba-clean libassuan-clean libgcrypt-clean libgpg-error-clean pth-clean
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
