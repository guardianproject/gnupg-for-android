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

# GnuPG does not work with automake 1.14, it has proven reliable on 1.11, and
# that's widely available, so we'll add it to the env here, and the
# ./autogen.sh scripts will pick it up.
# http://lists.gnupg.org/pipermail/gnupg-devel/2013-August/027857.html
export AUTOMAKE=automake-1.11


EXTERNAL_ROOT := $(shell pwd)
# this gives use NDK_TOOLCHAIN_VERSION, APP_ABI, etc.
include $(EXTERNAL_ROOT)/../jni/Application.mk

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

# Android NDK setup
ANDROID_NDK_HOME ?= /opt/android-ndk
NDK_ABI=arm
# NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
NDK_SYSROOT=$(ANDROID_NDK_HOME)/platforms/$(APP_PLATFORM)/arch-$(NDK_ABI)
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
ifeq ($(NDK_ABI),x86)
 HOST = i686-linux-android
 NDK_TOOLCHAIN = $(NDK_ABI)-$(NDK_TOOLCHAIN_VERSION)
else
 HOST = $(NDK_ABI)-linux-androideabi
 NDK_TOOLCHAIN = $(HOST)-$(NDK_TOOLCHAIN_VERSION)
endif
NDK_TOOLCHAIN_BASE=$(ANDROID_NDK_HOME)/toolchains/$(NDK_TOOLCHAIN)/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

# include Android's build flags
TARGET_ARCH_ABI = $(APP_ABI)
include $(ANDROID_NDK_HOME)/toolchains/$(NDK_TOOLCHAIN)/setup.mk


# install root for built files
DESTDIR = $(EXTERNAL_ROOT)
# TODO try adding the Android-style /data/app.name here
prefix = /data/data/info.guardianproject.gpg/app_opt
LOCAL := $(DESTDIR)$(prefix)

PATH := ${PATH}:$(NDK_TOOLCHAIN)/bin:$(LOCAL)/bin

CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-g++
CPP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-cpp
LD := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ld
AR := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ar
RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-ranlib
STRIP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)-strip \
	--strip-unneeded -R .note -R .comment

CFLAGS = -DANDROID -I$(LOCAL)/include $(TARGET_CFLAGS)
LDFLAGS = -llog -L$(LOCAL)/lib $(TARGET_LDFLAGS)

# change 'release' to 'debug' for unoptimized debug builds
ifeq ($(APP_ABI),armeabi-v7a)
	CFLAGS += $(TARGET_arm_release_CFLAGS)
endif
ifeq ($(APP_ABI),armeabi)
	CFLAGS += $(TARGET_thumb_release_CFLAGS)
endif


.PHONY = clean distclean install-clean \
	libgpg-error-build libgpg-error-clean libgpg-error-install \
	libgcrypt-build libgcrypt-clean libgcrypt-install \
	libassuan-build libassuan-clean libassuan-install \
	libksba-build libksba-clean libksba-install \
	gnupg-build gnupg-clean \
	pinentry-build pinentry-clean \
	curl-build curl-clean curl-install \
	openldap-build openldap-clean openldap-install \
	npth-build npth-clean npth-install \
	assets clean-assets \
	showsetup

all: gnupg-install gpgme-install assets


#------------------------------------------------------------------------------#
# debugging stuff

showsetup:
	@echo "NDK_TOOLCHAIN_VERSION: $(NDK_TOOLCHAIN_VERSION)"
	@echo "NDK_TOOLCHAIN: $(NDK_TOOLCHAIN)"
	@echo "NDK_SYSROOT: $(NDK_SYSROOT)"
	@echo "APP_PLATFORM: $(APP_PLATFORM)"
	@echo "APP_ABI: $(APP_ABI)"
	@echo "HOST: $(HOST)"
	@echo "CC: $(CC)"
	@echo "LD: $(LD)"
	@echo "CFLAGS: $(CFLAGS)"
	@echo "LDFLAGS: $(LDFLAGS)"


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
			AR=$(AR) \
			RANLIB=$(RANLIB) \
			CFLAGS="$(CFLAGS)" \
			LDFLAGS="$(LDFLAGS)" \
			--disable-doc \
			--disable-languages \
			--host=$(HOST) \
			--prefix=$(LOCAL)
	ls -l libgpg-error/libtool
	# brute force and ignorance to make libtool comply with android style
	sed -i 's,^fast_install=.*,fast_install=needless,' libgpg-error/libtool
	sed -i 's,^version_type=.*,version_type=none,' libgpg-error/libtool
	sed -i 's,^shlibpath_overrides_runpath=.*,shlibpath_overrides_runpath=yes,' libgpg-error/libtool
	sed -i 's,^library_names_spec=.*,library_names_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error/libtool
	sed -i 's,^soname_spec=.*,soname_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error/libtool
	sed -i 's,^finish_cmds=.*,finish_cmds="",' libgpg-error/libtool
	sed -i 's,^sys_lib_dlsearch_path_spec=.*,sys_lib_dlsearch_path_spec="/lib /usr/lib",' libgpg-error/libtool

libgpg-error/src/.libs/libgpg-error.so: libgpg-error/Makefile
	$(MAKE) -C libgpg-error

libgpg-error-build: libgpg-error/src/.libs/libgpg-error.so

$(LOCAL)/lib/libgpg-error.so: libgpg-error/src/.libs/libgpg-error.so
	$(MAKE) -C libgpg-error prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libgpg-error.so*

libgpg-error-install: $(LOCAL)/lib/libgpg-error.so

libgpg-error-clean:
	-$(MAKE) -C libgpg-error clean
	rm -rf libgpg-error/configure libgpg-error/Makefile


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
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)
	-patch -N -p1 --reject-file=- libgcrypt/tests/random.c libgcrypt-disable-hanging-random-test.patch

libgcrypt/src/.libs/libgcrypt.so: $(LOCAL)/lib/libgpg-error.so libgcrypt/Makefile
	$(MAKE) -C libgcrypt

$(LOCAL)/lib/libgcrypt.so: libgcrypt/src/.libs/libgcrypt.so
	$(MAKE) -C libgcrypt prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libgcrypt.so

libgcrypt-build: libgcrypt/src/.libs/libgcrypt.so

libgcrypt-install: $(LOCAL)/lib/libgcrypt.so

libgcrypt-clean:
	-$(MAKE) -C libgcrypt clean
	rm -rf libgcrypt/configure libgcrypt/Makefile


#------------------------------------------------------------------------------#
# libassuan

libassuan/config.sub: config.sub
	cp config.sub libassuan/config.sub

libassuan/config.guess: config.guess
	cp config.guess libassuan/config.guess

libassuan/configure: libassuan/configure.ac
	cd libassuan && ./autogen.sh && autoreconf --install --force --verbose

libassuan/Makefile: libassuan/config.sub libassuan/config.guess libassuan/configure
	-patch -N -p1 --reject-file=- libassuan/m4/libtool.m4 libtool-Add-Android-Linux-support.patch
	cd libassuan && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)

libassuan/src/.libs/libassuan.so: $(LOCAL)/lib/libgpg-error.so libassuan/Makefile
	$(MAKE) -C libassuan

$(LOCAL)/lib/libassuan.so: libassuan/src/.libs/libassuan.so
	$(MAKE) -C libassuan prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libassuan.so

libassuan-build: libassuan/src/.libs/libassuan.so

libassuan-install: $(LOCAL)/lib/libassuan.so

libassuan-clean:
	-$(MAKE) -C libassuan clean
	rm -rf libassuan/configure libassuan/Makefile


#------------------------------------------------------------------------------#
# npth

npth/config.sub: config.sub
	cp config.sub npth/config.sub

npth/config.guess: config.guess
	cp config.guess npth/config.guess

npth/configure: npth/configure.ac
	cd npth && ./autogen.sh && autoreconf --install --force --verbose

npth/Makefile: npth/config.sub npth/config.guess npth/configure
	-patch -N -p1 --reject-file=- npth/m4/libtool.m4 libtool-Add-Android-Linux-support.patch
	cd npth && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gnu-ld \
				--prefix=$(LOCAL)

npth/src/.libs/libnpth.so: $(LOCAL)/lib/libgpg-error.so npth/Makefile
	$(MAKE) -C npth

$(LOCAL)/lib/libnpth.so: npth/src/.libs/libnpth.so
	$(MAKE) -C npth prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libnpth.so*

npth-build: npth/src/.libs/libnpth.so

npth-install: $(LOCAL)/lib/libnpth.so

npth-clean:
	-$(MAKE) -C npth clean
	rm -rf npth/Makefile npth/configure


#------------------------------------------------------------------------------#
# libksba

libksba/config.sub: config.sub
	cp config.sub libksba/config.sub

libksba/config.guess: config.guess
	cp config.guess libksba/config.guess

libksba/configure: libksba/configure.ac
	cd libksba && ./autogen.sh

libksba/Makefile: $(LOCAL)/lib/libgpg-error.so libksba/config.sub libksba/config.guess libksba/configure
	-patch -N -p1 --reject-file=- libksba/m4/libtool.m4 libtool-Add-Android-Linux-support.patch
	cd libksba && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)

libksba/src/.libs/libksba.so: libksba/Makefile
	$(MAKE) -C libksba

$(LOCAL)/lib/libksba.so: libksba/src/.libs/libksba.so
	$(MAKE) -C libksba prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libksba.so

libksba-build: libksba/src/.libs/libksba.so

libksba-install: $(LOCAL)/lib/libksba.so

libksba-clean:
	-$(MAKE) -C libksba clean
	rm -rf libksba/configure libksba/Makefile


#------------------------------------------------------------------------------#
# openldap

openldap/configure:
	cd openldap && git checkout configure

# TODO try --with-tls=openssl or gnutls
openldap/Makefile: openldap/configure
	cd openldap && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--prefix=$(prefix) \
				--without-tls \
				--without-threads \
				--without-yielding-select \
				--disable-slapd \
				--disable-syslog \
				--disable-bdb \
				--disable-hdb \
				--with-gnu-ld
# cross-compiling makes openldap's ./configure think memcmp() is broken
# http://zhuqy.wordpress.com/2010/04/22/cross-compile-openldap-error-undefined-reference-to-lutil_memcmp/
	sed --in-place 's|#define NEED_MEMCMP_REPLACEMENT 1|/* #undef NEED_MEMCMP_REPLACEMENT */|' openldap/include/portable.h
# I haven't found a reliable way to install/uninstall a patch from a Makefile,
# so just always try to apply it, and ignore it if it fails. Works fine unless
# the files being patched have changed, in which cause a partial application
# could happen unnoticed.
	-patch -N -p1 --reject-file=- < openldap-disable-sys_errlist.patch

openldap-build-stamp: openldap/Makefile
	$(MAKE) -C openldap
	touch openldap-build-stamp

openldap-build: openldap-build-stamp

$(LOCAL)/lib/libldap.a: openldap-build
	$(MAKE) -C openldap/include DESTDIR=$(DESTDIR) prefix=$(prefix) install
	$(MAKE) -C openldap/libraries DESTDIR=$(DESTDIR) prefix=$(prefix) install
	ls -l $(LOCAL)/lib/libldap.so

openldap-install: $(LOCAL)/lib/libldap.a

openldap-clean:
	-$(MAKE) -C openldap clean
	-rm -f openldap-build-stamp


#------------------------------------------------------------------------------#
# curl

curl/config.sub: config.sub
	cp config.sub curl/config.sub

curl/config.guess: config.guess
	cp config.guess curl/config.guess

curl/configure: curl/configure.ac
	cd curl && ./buildconf

curl/Makefile: curl/config.sub curl/config.guess curl/configure
	-patch -N -p1 --reject-file=- curl/m4/libtool.m4 libtool-Add-Android-Linux-support.patch
	cd curl && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--prefix=$(prefix) \
				--with-gnu-ld \
				--disable-imap \
				--disable-ldap \
				--disable-pop3 \
				--disable-rtsp \
				--disable-smtp
	# brute force and ignorance to make libtool comply with android style
	sed -i 's,^fast_install=.*,fast_install=needless,' curl/libtool
	sed -i 's,^version_type=.*,version_type=none,' curl/libtool
	sed -i 's,^shlibpath_overrides_runpath=.*,shlibpath_overrides_runpath=yes,' curl/libtool
	sed -i 's,^library_names_spec=.*,library_names_spec="\\$$libname\\$$release\\$$shared_ext",' curl/libtool
	sed -i 's,^soname_spec=.*,soname_spec="\\$$libname\\$$release\\$$shared_ext",' curl/libtool
	sed -i 's,^finish_cmds=.*,finish_cmds="",' curl/libtool
	sed -i 's,^sys_lib_dlsearch_path_spec=.*,sys_lib_dlsearch_path_spec="/lib /usr/lib",' curl/libtool

curl/lib/.libs/libcurl.so: curl/Makefile
	$(MAKE) -C curl

$(LOCAL)/lib/libcurl.so: curl/lib/.libs/libcurl.so
	$(MAKE) -C curl DESTDIR=$(DESTDIR) prefix=$(prefix) install
	ls -l $(LOCAL)/lib/libcurl.so

curl-build: curl/lib/.libs/libcurl.so

curl-install: $(LOCAL)/lib/libcurl.so

curl-clean:
	-$(MAKE) -C curl clean
	rm -f curl/Makefile


#------------------------------------------------------------------------------#
# adns

adns-1.4.tar.gz.sig: adns-1.4.tar.gz
	wget http://www.chiark.greenend.org.uk/~ian/adns/ftp/adns-1.4.tar.gz.sig
	gpg --verify adns-1.4.tar.gz.sig

adns-1.4.tar.gz:
	wget -c http://www.chiark.greenend.org.uk/~ian/adns/ftp/adns-1.4.tar.gz
	sha1sum -c adns-1.4.tar.gz.sha1

adns-1.4: adns-1.4.tar.gz.sig
	tar xzf adns-1.4.tar.gz

adns-1.4/config.sub: adns-1.4 config.sub
	cp config.sub adns-1.4/config.sub

adns-1.4/config.guess: adns-1.4 config.guess
	cp config.guess adns-1.4/config.guess

adns-1.4/Makefile: adns-1.4 adns-1.4/Makefile.in adns-1.4/config.guess adns-1.4/config.sub
	patch -N -p1 --reject-file=- < adns-destdir-install-fix.patch
	cd adns-1.4 && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--prefix=$(prefix) \
				--host=$(HOST) \
				--disable-dynamic

adns-build-stamp: adns-1.4/Makefile
	$(MAKE) -C adns-1.4
	touch adns-build-stamp

adns-build: adns-build-stamp

$(LOCAL)/lib/libadns.a: adns-build
	$(MAKE) -C adns-1.4 DESTDIR=$(DESTDIR) prefix=$(prefix) install

adns-install: $(LOCAL)/lib/libadns.a

adns-clean:
	-rm -rf adns-1.4
	-rm -f adns-1.4.tar.gz
	-rm -f adns-1.4.tar.gz.sig*
	-rm -f adns-build-stamp



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
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--with-libgcrypt-prefix=$(LOCAL) \
				--with-libassuan-prefix=$(LOCAL) \
				--with-ksba-prefix=$(LOCAL) \
				--with-npth-prefix=$(LOCAL) \
				--with-libcurl=$(LOCAL) \
				--with-ldap=$(LOCAL) \
				--without-libiconv-prefix \
				--disable-doc \
				--disable-g13 \
				--disable-ntbtls \
				--disable-gnutls \
				--enable-dirmngr-auto-start \
				--with-agent-pgm=$(prefix)/bin/gpg-agent \
				--with-dirmngr-pgm=$(prefix)/bin/dirmngr \
				--with-dirmngr-ldap-pgm=$(prefix)/libexec/dirmngr_ldap \
				--with-pinentry-pgm=$(prefix)/bin/pinentry-android \
				--with-protect-tool-pgm=$(prefix)/libexec/gpg-protect-tool \
				--with-scdaemon-pgm=$(prefix)/bin/scdaemon \
				--prefix=$(prefix)

# TODO adns builds for Android, but gnupg doesn't build for Android with adns enabled
#				--with-adns=$(LOCAL) \ $(LOCAL)/lib/libadns.a


gnupg/g10/gpg2: $(LOCAL)/lib/libgpg-error.so $(LOCAL)/lib/libgcrypt.so $(LOCAL)/lib/libksba.so $(LOCAL)/lib/libassuan.so $(LOCAL)/lib/libldap.a $(LOCAL)/lib/libnpth.so $(LOCAL)/lib/libcurl.so gnupg/Makefile
	$(MAKE) -C gnupg

$(LOCAL)/bin/gpg2: gnupg/g10/gpg2 $(GNUPG_SOURCES) gnupg/configure
	$(MAKE) -C gnupg prefix=$(LOCAL) install
	ls -l $(LOCAL)/bin/gpg2

gnupg-build: gnupg/g10/gpg2

gnupg-install: $(LOCAL)/bin/gpg2 $(LOCAL)/bin/pinentry-android
	install -d $(LOCAL)/etc/gnupg
	install -d $(LOCAL)/var/run/gnupg
	install -d $(LOCAL)/var/cache/gnupg

gnupg-clean:
	-$(MAKE) -C gnupg


#------------------------------------------------------------------------------#
# pinentry

PINENTRY_SOURCES := $(wildcard pinentry/pinentry/*.c) $(wildcard pinentry/pinentry/*.h)

pinentry/config.sub: config.sub
	cp config.sub pinentry/config.sub

pinentry/config.guess: config.guess
	cp config.guess pinentry/config.guess

pinentry/configure:
	cd pinentry && ./autogen.sh

pinentry/Makefile: pinentry/configure pinentry/config.sub pinentry/config.guess
	cd pinentry && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--host=$(HOST) \
				--with-libiconv-prefix=$(LOCAL) \
				--without-x \
				--disable-pinentry-gtk \
				--disable-pinentry-gtk2 \
				--disable-pinentry-qt \
				--disable-pinentry-qt4 \
				--disable-pinentry-curses \
				--enable-pinentry-android \
				--prefix=$(prefix) \
				LDFLAGS="$(LDFLAGS)"

pinentry/android/pinentry-android:  pinentry/Makefile
	$(MAKE) -C pinentry LDFLAGS="$(LDFLAGS)"

$(LOCAL)/bin/pinentry-android: pinentry/android/pinentry-android $(PINENTRY_SOURCES) pinentry/configure
	$(MAKE) -C pinentry prefix=$(LOCAL) install
	# we only want pinentry-android, not the 'pinentry' symlink, since it'll
	# be copied as a full file in Android's assets procedure, and we hardcode
	# the full path to pinentry in gnupg. pinentry on all other platforms
	# invokes it via the name 'pinentry' in the PATH, here Android is the
	# outlier, so we have to leave the link in the pinentry build system, and
	# just delete the symlink here.
	rm -f $(LOCAL)/bin/pinentry

pinentry-build: pinentry/android/pinentry-android

pinentry-install: $(LOCAL)/bin/pinentry-android

pinentry-clean:
	-$(MAKE) -C pinentry clean

#------------------------------------------------------------------------------#
# gpgme

gpgme/configure: gpgme/configure.ac
	cd gpgme && ./autogen.sh

gpgme/Makefile: gpgme/configure
	-patch -N -p1 --reject-file=- gpgme/m4/libtool.m4 libtool-Add-Android-Linux-support.patch
	cd gpgme && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--enable-maintainer-mode \
				--with-gpg-error-prefix=$(LOCAL) \
				--with-libassuan-prefix=$(LOCAL) \
				--enable-fixed-path=$(prefix)/bin \
				--without-g13 \
				--disable-glibtest \
				--disable-gpg-test \
				--disable-gpgsm-test \
				--disable-gpgconf-test \
				--disable-g13-test \
				--prefix=$(prefix)

gpgme/src/.libs/libgpgme.so: gpgme/Makefile
	$(MAKE) -C gpgme

$(LOCAL)/lib/libgpgme.so: gpgme/src/.libs/libgpgme.so
	$(MAKE) -C gpgme DESTDIR=$(DESTDIR) prefix=$(prefix) install

gpgme-build: gpgme/src/.libs/libgpgme.so

gpgme-install: $(LOCAL)/bin/gpg2 $(LOCAL)/lib/libgpgme.so

gpgme-clean:
	-$(MAKE) -C gpgme clean
	-rm -f gpgme/Makefile gpgme/configure



#------------------------------------------------------------------------------#
# separate target for building and installing the tests into assets/
# The $(shell find) scripts find all the ELF binaries, which are the only tests
# that will run on Android.  The shell script tests will not since Android's
# /system/bin/sh is ridiculously limited.

assets-tests: assets-tests-libgpg-error assets-tests-libassuan assets-tests-libgcrypt assets-tests-npth assets-tests-libksba assets-tests-gpgme

assets-tests-libgpg-error:
	$(MAKE) -C libgpg-error/tests
	install -d $(ASSETS)/tests/libgpg-error
	install -p \
		`find libgpg-error/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		$(ASSETS)/tests/libgpg-error

assets-tests-libassuan:
	$(MAKE) -C libassuan/tests
	install -d $(ASSETS)/tests/libassuan
	install -p \
		`find libassuan/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		$(ASSETS)/tests/libassuan

assets-tests-libgcrypt:
	$(MAKE) -C libgcrypt/tests
	install -d $(ASSETS)/tests/libgcrypt
	install -p \
		`find libgcrypt/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		$(ASSETS)/tests/libgcrypt

assets-tests-npth:
	$(MAKE) -C npth/tests
	install -d $(ASSETS)/tests/npth
	install -p \
		`find npth/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		$(ASSETS)/tests/npth

assets-tests-libksba:
	$(MAKE) -C libksba/tests
	install -d $(ASSETS)/tests/libksba
	install -p \
		`find libksba/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		libksba/tests/*.der \
		$(ASSETS)/tests/libksba

# TODO the gnupg/tests/ are all based on shell scripts that won't run on Android

assets-tests-gpgme:
	$(MAKE) -C gpgme/tests
	install -d $(ASSETS)/tests/gpgme
	install -p \
		`find gpgme/tests/ -executable -type f -exec file {} \; | grep ELF | cut -d: -f 1` \
		gpgme/tests/*.txt \
		gpgme/tests/gpg/*.asc \
		$(ASSETS)/tests/gpgme


#------------------------------------------------------------------------------#
# setup as assets for Android app

android-assets: assets

clean-assets:
	rm -rf $(ASSETS)/bin
	rm -rf $(ASSETS)/etc
	rm -rf $(ASSETS)/lib
	rm -rf $(ASSETS)/libexec
	rm -rf $(ASSETS)/sbin
	rm -rf $(ASSETS)/share
	rm -rf $(ASSETS)/tests/gpgme
	rm -rf $(ASSETS)/tests/libassuan
	rm -rf $(ASSETS)/tests/libgcrypt
	rm -rf $(ASSETS)/tests/libgpg-error
	rm -rf $(ASSETS)/tests/libksba
	rm -rf $(ASSETS)/tests/npth
	rm -rf $(ASSETS)/var

ASSETS := $(EXTERNAL_ROOT)/../assets
assets: clean-assets
# add the new stuff
	install -d $(ASSETS)
	cp -a $(LOCAL)/* $(ASSETS)
# remove all the stuff we don't need
	rm -f $(ASSETS)/bin/*-static
	rm -f $(ASSETS)/bin/curl*
	rm -f $(ASSETS)/lib/*.a $(ASSETS)/lib/*.la
# remove lib symlinks since Android AssetManager copies them as files
	rm -f $(ASSETS)/lib/*.so
# remove .so.0 symlink and rename the .so.0.12.0 file to it
	for f in $(ASSETS)/lib/*.so.[0-9]*; do \
		echo $$f; \
		test ! -L $$f || \
			(rm $$f && mv $$f.[0-9]* $$f); \
	done
	rm -rf $(ASSETS)/include
	rm -rf $(ASSETS)/share/man $(ASSETS)/share/info $(ASSETS)/share/doc


#------------------------------------------------------------------------------#
# clean

install-clean:
	rm -rf -- $(LOCAL)

clean: install-clean pinentry-clean gnupg-clean curl-clean openldap-clean libksba-clean libassuan-clean npth-clean libgcrypt-clean libgpg-error-clean adns-clean

distclean: clean install-clean
	-$(MAKE) -C gnupg distclean
	-$(MAKE) -C pinentry distclean
	-$(MAKE) -C curl distclean
	-rm -f curl/configure # their distclean fails to rm this
	-$(MAKE) -C openldap distclean
	-$(MAKE) -C libksba distclean
	-$(MAKE) -C npth distclean
	-$(MAKE) -C libassuan distclean
	-$(MAKE) -C libgcrypt distclean
	-$(MAKE) -C libgpg-error distclean

# watch out, this is dangerous!
gitclean:
	rm -rf $(ASSETS)/bin
	rm -rf $(ASSETS)/etc
	rm -rf $(ASSETS)/lib
	rm -rf $(ASSETS)/libexec
	rm -rf $(ASSETS)/sbin
	rm -rf $(ASSETS)/share
	rm -rf $(ASSETS)/tests/gpgme
	rm -rf $(ASSETS)/tests/libassuan
	rm -rf $(ASSETS)/tests/libgcrypt
	rm -rf $(ASSETS)/tests/libgpg-error
	rm -rf $(ASSETS)/tests/libksba
	rm -rf $(ASSETS)/tests/npth
	rm -rf $(ASSETS)/var
	rm -rf $(EXTERNAL_ROOT)/data
	for file in $(EXTERNAL_ROOT)/*; do \
		test -d $$file || continue; \
		echo $$file; \
		cd $$file; \
		git reset --hard; \
		git clean -fdx; \
		cd ..; \
	done


ALL_SOURCES := $(shell find $(EXTERNAL_ROOT) -name '*.[ch]')
TAGS:	$(ALL_SOURCES)
	@echo Generating TAGS...
	@etags $(ALL_SOURCES)
