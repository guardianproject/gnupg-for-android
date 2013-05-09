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
PROJECT_ROOT = $(CWD)/..
EXTERNAL_ROOT = $(PROJECT_ROOT)/external

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
NDK_BASE ?= /opt/android-ndk
NDK_PLATFORM_LEVEL ?= 8
NDK_ABI=arm
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-$(NDK_ABI)
NDK_UNAME:=`uname -s | tr '[A-Z]' '[a-z]'`
#NDK_COMPILER_VERSION=4.4.3
NDK_COMPILER_VERSION = 4.7
ifeq ($(NDK_ABI),x86)
 HOST = i686-linux-android
 NDK_TOOLCHAIN = $(NDK_ABI)-$(NDK_COMPILER_VERSION)
else
 HOST = $(NDK_ABI)-linux-androideabi
 NDK_TOOLCHAIN = $(HOST)-$(NDK_COMPILER_VERSION)
endif
NDK_TOOLCHAIN_BASE=$(NDK_BASE)/toolchains/$(NDK_TOOLCHAIN)/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)


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

CFLAGS = -DANDROID -I$(LOCAL)/include
LDFLAGS = -L$(SYSROOT)/usr/lib -llog -L$(LOCAL)/lib

# turn off gcc optmization on the native builds for now, to aid debugging

# build as small as possible, mostly useful for static binaries
#CFLAGS += -fdata-sections -ffunction-sections -Os
#LDFLAGS += -Wl,--gc-sections
# unoptimized debug builds!
CFLAGS += -g


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
	pth-build pth-clean pth-install \
	libiconv-build libiconv-clean libiconv-install

all: gnupg-install gpgme-install android-assets


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
			--disable-languages \
			--enable-static \
			--host=$(HOST) \
			--prefix=$(LOCAL)


libgpg-error-build-stamp: libgpg-error/Makefile
	$(MAKE) -C libgpg-error
	touch libgpg-error-build-stamp

libgpg-error-build: libgpg-error-build-stamp

$(LOCAL)/lib/libgpg-error.a: libgpg-error-build
	$(MAKE) -C libgpg-error prefix=$(LOCAL) install

libgpg-error-install: $(LOCAL)/lib/libgpg-error.a

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
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)

libgcrypt-build-stamp: $(LOCAL)/lib/libgpg-error.a libgcrypt/Makefile
	$(MAKE) -C libgcrypt
	touch libgcrypt-build-stamp

libgcrypt-build: libgcrypt-build-stamp

$(LOCAL)/lib/libgcrypt.a: libgcrypt-build
	$(MAKE) -C libgcrypt prefix=$(LOCAL) install
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
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)

libassuan-build-stamp: $(LOCAL)/lib/libgpg-error.a libassuan/Makefile
	$(MAKE) -C libassuan
	touch libassuan-build-stamp

$(LOCAL)/lib/libassuan.a: libassuan-build-stamp
	$(MAKE) -C libassuan prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libassuan.a

libassuan-install: $(LOCAL)/lib/libassuan.a

libassuan-clean:
	-rm -f libassuan-build-stamp


#------------------------------------------------------------------------------#
# npth

npth/config.sub: config.sub
	cp config.sub npth/config.sub

npth/config.guess: config.guess
	cp config.guess npth/config.guess

npth/configure: npth/configure.ac
	cd npth && ./autogen.sh

npth/Makefile: npth/config.sub npth/config.guess npth/configure
	cd npth && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gnu-ld \
				--prefix=$(LOCAL)

npth-build-stamp: $(LOCAL)/lib/libgpg-error.a npth/Makefile
	$(MAKE) -C npth
	touch npth-build-stamp

$(LOCAL)/lib/libnpth.a: npth-build-stamp
	$(MAKE) -C npth prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libnpth.a

npth-install: $(LOCAL)/lib/libnpth.a

npth-clean:
	-rm -f npth-build-stamp


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
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(LOCAL) \
				--prefix=$(LOCAL)
	$(MAKE) -C libksba

$(LOCAL)/lib/libksba.a: libksba-build
	$(MAKE) -C libksba prefix=$(LOCAL) install
	ls -l $(LOCAL)/lib/libksba.a

libksba-install: $(LOCAL)/lib/libksba.a

libksba-clean:
	-rm -f libksba-build-stamp


#------------------------------------------------------------------------------#
# openldap

openldap/configure:
	cd openldap && git checkout configure

# TODO try --with-tls=openssl or gnutls
openldap/Makefile: openldap/configure
	cd openldap && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-static \
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
	ls -l $(LOCAL)/lib/libldap.a

openldap-install: $(LOCAL)/lib/libldap.a

openldap-clean:
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
	cd curl && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--enable-static \
				--host=$(HOST) \
				--prefix=$(prefix) \
				--with-ldap-lib=$(LOCAL) \
				--with-lber-lib=$(LOCAL) \
				--with-gnu-ld

curl-build-stamp: curl/Makefile
	$(MAKE) -C curl
	touch curl-build-stamp

curl-build: curl-build-stamp

$(LOCAL)/lib/libcurl.a: curl-build
	$(MAKE) -C curl DESTDIR=$(DESTDIR) prefix=$(prefix) install
	$(MAKE) -C curl DESTDIR=$(DESTDIR) prefix=$(prefix) install
	ls -l $(LOCAL)/lib/libcurl.a

curl-install: $(LOCAL)/lib/libcurl.a

curl-clean:
	-rm -f curl-build-stamp


#------------------------------------------------------------------------------#
# pth

pth-2.0.7.tar.gz.sig: pth-2.0.7.tar.gz
	wget http://ftp.gnu.org/gnu/pth/pth-2.0.7.tar.gz.sig
	gpg --recv-key  4E23E878D41A0A88EDFCFA5A6E744ACBA9C09E30
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

pth-2.0.7/Makefile: pth-2.0.7 pth-2.0.7/aclocal.m4 pth-2.0.7/config.guess pth-2.0.7/config.sub
	patch -N -p0 --reject-file=- < pth-fix-include.patch || true
	cd pth-2.0.7 && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--prefix=$(prefix) \
				--host=$(HOST) \
				--enable-static \
				--disable-shared \
				--with-mctx-mth=sjlj \
				--with-mctx-dsp=ssjlj \
				--with-mctx-stk=sas

pth-build: pth-2.0.7/Makefile
	$(MAKE) -C pth-2.0.7

$(LOCAL)/lib/libpth.a: pth-build
	$(MAKE) -C pth-2.0.7 DESTDIR=$(DESTDIR) prefix=$(prefix) install

pth-install: $(LOCAL)/lib/libpth.a

pth-clean:
	-rm -rf pth-2.0.7
	-rm -f pth-2.0.7.tar.gz
	-rm -f pth-2.0.7.tar.gz.sig*


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

adns-build: adns-1.4/Makefile
	$(MAKE) -C adns-1.4

$(LOCAL)/lib/libadns.a: adns-build
	$(MAKE) -C adns-1.4 DESTDIR=$(DESTDIR) prefix=$(prefix) install

adns-install: $(LOCAL)/lib/libadns.a

adns-clean:
	-rm -rf adns-1.4
	-rm -f adns-1.4.tar.gz
	-rm -f adns-1.4.tar.gz.sig*


#------------------------------------------------------------------------------#
# libiconv

libiconv-1.14.tar.gz.sig: libiconv-1.14.tar.gz
	wget -c http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz.sig
	gpg --verify libiconv-1.14.tar.gz.sig

libiconv-1.14.tar.gz:
	wget -c http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz
	sha1sum -c libiconv-1.14.tar.gz.sha1

libiconv-1.14: libiconv-1.14.tar.gz.sig
	tar xzf libiconv-1.14.tar.gz

libiconv-1.14/config.sub: libiconv-1.14 config.sub
	cp config.sub libiconv-1.14/config.sub

libiconv-1.14/config.guess: libiconv-1.14 config.guess
	cp config.guess libiconv-1.14/config.guess

libiconv-1.14/Makefile: libiconv-1.14 libiconv-1.14/Makefile.in libiconv-1.14/config.guess libiconv-1.14/config.sub
	cd libiconv-1.14 && \
		CFLAGS="$(CFLAGS) -mandroid" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--prefix=$(prefix) \
				--host=arm-linux-eabi \
				--with-sysroot=$(NDK_SYSROOT) \
				--with-gnu-ld

libiconv-build-stamp: libiconv-1.14/Makefile
	$(MAKE) -C libiconv-1.14
	touch libiconv-build-stamp

libiconv-build: libiconv-build-stamp

$(LOCAL)/lib/libiconv.a: libiconv-build-stamp
	$(MAKE) -C libiconv-1.14 DESTDIR=$(DESTDIR) prefix=$(prefix) install

libiconv-install: $(LOCAL)/lib/libiconv.a

libiconv-clean:
	-rm -rf libiconv-1.14
	-rm -f libiconv-1.14.tar.gz
	-rm -f libiconv-1.14.tar.gz.sig*
	-rm -f libiconv-build-stamp


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


gnupg-build: gnupg-build-stamp 
gnupg-build-stamp: $(LOCAL)/lib/libgpg-error.a $(LOCAL)/lib/libgcrypt.a $(LOCAL)/lib/libksba.a $(LOCAL)/lib/libassuan.a $(LOCAL)/lib/libldap.a $(LOCAL)/lib/libnpth.a gnupg/Makefile
	$(MAKE) -C gnupg
	touch gnupg-build-stamp

$(LOCAL)/bin/gpg2: gnupg-build $(GNUPG_SOURCES) gnupg/configure
	$(MAKE) -C gnupg prefix=$(LOCAL) install
	ls -l $(LOCAL)/bin/gpg2

gnupg-install: $(LOCAL)/bin/gpg2 pinentry-install
	install -d $(LOCAL)/etc/gnupg
	install -d $(LOCAL)/var/run/gnupg
	install -d $(LOCAL)/var/cache/gnupg

gnupg-clean:
	-rm -f gnupg-build-stamp

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

pinentry-build: pinentry-build-stamp 
pinentry-build-stamp:  pinentry/Makefile
	$(MAKE) -C pinentry LDFLAGS="$(LDFLAGS)"
	touch pinentry-build-stamp

$(LOCAL)/bin/pinentry-android: pinentry-build $(PINENTRY_SOURCES) pinentry/configure
	$(MAKE) -C pinentry prefix=$(LOCAL) install
	ls -l $(LOCAL)/bin/pinentry

pinentry-install: $(LOCAL)/bin/pinentry-android

pinentry-clean:
	-rm -f pinentry-build-stamp

#------------------------------------------------------------------------------#
# gpgme

gpgme/configure: gpgme/configure.ac
	cd gpgme && ./autogen.sh

gpgme/Makefile: gpgme/configure
	cd gpgme && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) CFLAGS="$(CFLAGS)" LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--enable-maintainer-mode \
				--with-gpg-error-prefix=$(LOCAL) \
				--with-libassuan-prefix=$(LOCAL) \
				--with-gpg=$(prefix)/bin/gpg2 \
				--with-gpgsm=$(prefix)/bin/gpgsm \
				--with-gpgconf=$(prefix)/bin/gpgconf \
				--with-g13=$(prefix)/bin/g13 \
				--disable-glibtest \
				--disable-gpg-test \
				--disable-gpgsm-test \
				--disable-gpgconf-test \
				--disable-g13-test \
				--prefix=$(prefix)

gpgme-build-stamp: gpgme/Makefile
	$(MAKE) -C gpgme
	touch gpgme-build-stamp

gpgme-build: gpgme-build-stamp

$(LOCAL)/lib/libgpgme.so: gpgme-build
	$(MAKE) -C gpgme DESTDIR=$(DESTDIR) prefix=$(prefix) install

gpgme-install: gnupg-install $(LOCAL)/lib/libgpgme.so

gpgme-clean:
	-rm -f gpgme-build-stamp gpgme/Makefile gpg/configure



#------------------------------------------------------------------------------#
# separate target for building and installing the tests into assets/

assets-tests: assets-tests-gpgme assets-tests-pinentry
	install -p run-tests.sh $(ASSETS)/tests

assets-tests-gpgme:
	$(MAKE) -C gpgme/tests
	install -d $(ASSETS)/tests/gpgme
	install -p \
		gpgme/tests/.libs/* \
		gpgme/tests/*.txt \
		gpgme/tests/gpg/*.asc \
		$(ASSETS)/tests/gpgme

assets-tests-pinentry:
	install -d $(ASSETS)/tests/pinentry
	install -p \
		pinentry-tests/*gpg \
		$(ASSETS)/tests/pinentry


#------------------------------------------------------------------------------#
# ugly static linking hacks

# crazy ugly hack
gnupg-static: gpg2-static gpg-agent-static gpgv2-static dirmngr-static

gpg-agent-static:
	-rm -f gnupg/agent/gpg-agent
	cd gnupg/agent && $(CC) -DANDROID -I$(LOCAL)/include -Os -Wall -Wcast-align -Wshadow -Wstrict-prototypes -Wformat -Wno-format-y2k -Wformat-security -W -Wno-sign-compare -Wno-missing-field-initializers -Wdeclaration-after-statement -Wno-pointer-sign -Wpointer-arith  -L$(LOCAL)/lib -Wl,--rpath,$(prefix)/lib -o gpg-agent gpg_agent-gpg-agent.o gpg_agent-command.o gpg_agent-command-ssh.o gpg_agent-call-pinentry.o gpg_agent-cache.o gpg_agent-trans.o gpg_agent-findkey.o gpg_agent-pksign.o gpg_agent-pkdecrypt.o gpg_agent-genkey.o gpg_agent-protect.o gpg_agent-trustlist.o gpg_agent-divert-scd.o gpg_agent-cvt-openpgp.o gpg_agent-call-scd.o gpg_agent-learncard.o ../common/libcommonpth.a ../gl/libgnu.a  ../..$(prefix)/lib/libgcrypt.a ../..$(prefix)/lib/libgpg-error.a ../..$(prefix)/lib/libassuan.a ../..$(prefix)/lib/libnpth.a -ldl
	install gnupg/agent/gpg-agent $(LOCAL)/bin/gpg-agent-static
	$(STRIP)  $(LOCAL)/bin/gpg-agent-static

dirmngr-static:
	-rm -f gnupg/dirmngr/dirmngr gnupg/dirmngr/dirmngr-client
	cd gnupg/dirmngr && $(CC) -I$(LOCAL)/include -DANDROID -Os -Wall -Wcast-align -Wshadow -Wstrict-prototypes -Wformat -Wno-format-y2k -Wformat-security -W -Wno-sign-compare -Wno-missing-field-initializers -Wdeclaration-after-statement -Wno-pointer-sign -Wpointer-arith  -Wl,--rpath,$(prefix)/lib -o dirmngr dirmngr.o server.o crlcache.o crlfetch.o ldapserver.o certcache.o cdblib.o ldap.o misc.o ocsp.o validate.o  ks-action.o ks-engine-hkp.o ks-engine-http.o ks-engine-finger.o ks-engine-kdns.o ldap-wrapper.o  ../common/libcommonpth.a ../gl/libgnu.a  ../..$(prefix)/lib/libassuan.a ../..$(prefix)/lib/libgpg-error.a ../..$(prefix)/lib/libgcrypt.a ../..$(prefix)/lib/libksba.a ../..$(prefix)/lib/libnpth.a -ldl
	cd gnupg/dirmngr && $(CC) -I$(LOCAL)/include -DANDROID -O3 -Wall -Wcast-align -Wshadow -Wstrict-prototypes -Wformat -Wno-format-y2k -Wformat-security -W -Wno-sign-compare -Wno-missing-field-initializers -Wdeclaration-after-statement -Wno-pointer-sign -Wpointer-arith  -Wl,--rpath,$(prefix)/lib -o dirmngr-client dirmngr-client.o ../common/libcommon.a no-libgcrypt.o ../gl/libgnu.a ../..$(prefix)/lib/libassuan.a ../..$(prefix)/lib/libgpg-error.a
	install gnupg/dirmngr/dirmngr $(LOCAL)/bin/dirmngr-static
	$(STRIP)  $(LOCAL)/bin/dirmngr-static
	install gnupg/dirmngr/dirmngr-client $(LOCAL)/bin/dirmngr-client-static
	$(STRIP)  $(LOCAL)/bin/dirmngr-client-static

gpg2-static:
	-rm -f gnupg/g10/gpg2
	cd gnupg/g10 && $(CC) -DANDROID -I$(LOCAL)/include -Os -Wall -Wcast-align -Wshadow -Wstrict-prototypes -Wformat -Wno-format-y2k -Wformat-security -W -Wno-sign-compare -Wno-missing-field-initializers -Wdeclaration-after-statement -Wno-pointer-sign -Wpointer-arith  -Wl,--rpath,$(prefix)/lib -o gpg2 gpg.o server.o build-packet.o compress.o  free-packet.o getkey.o keydb.o keyring.o seskey.o kbnode.o mainproc.o armor.o mdfilter.o textfilter.o progress.o misc.o rmd160.o openfile.o keyid.o parse-packet.o cpr.o plaintext.o sig-check.o keylist.o pkglue.o ecdh.o pkclist.o skclist.o pubkey-enc.o passphrase.o decrypt.o decrypt-data.o cipher.o encrypt.o sign.o verify.o revoke.o keyedit.o dearmor.o import.o export.o trustdb.o tdbdump.o tdbio.o delkey.o keygen.o helptext.o keyserver.o call-dirmngr.o photoid.o call-agent.o card-util.o exec.o ../common/libcommon.a ../gl/libgnu.a ../common/libgpgrl.a  ../..$(prefix)/lib/libassuan.a  ../..$(prefix)/lib/libgpg-error.a  ../..$(prefix)/lib/libgcrypt.a  ../..$(prefix)/lib/libksba.a  ../..$(prefix)/lib/libldap.a  ../..$(prefix)/lib/liblber.a  -lz
	install gnupg/g10/gpg2 $(LOCAL)/bin/gpg2-static
	$(STRIP)  $(LOCAL)/bin/gpg2-static

gpgv2-static:
	-rm -f gnupg/g10/gpgv2
	cd gnupg/g10 && $(CC) -DANDROID -I$(LOCAL)/include -Os -Wall -Wcast-align -Wshadow -Wstrict-prototypes -Wformat -Wno-format-y2k -Wformat-security -W -Wno-sign-compare -Wno-missing-field-initializers -Wdeclaration-after-statement -Wno-pointer-sign -Wpointer-arith  -L$(LOCAL)/lib -Wl,--rpath,$(prefix)/lib -o gpgv2 gpgv.o build-packet.o compress.o  free-packet.o getkey.o keydb.o keyring.o seskey.o kbnode.o mainproc.o armor.o mdfilter.o textfilter.o progress.o misc.o rmd160.o openfile.o keyid.o parse-packet.o cpr.o plaintext.o sig-check.o keylist.o pkglue.o ecdh.o verify.o ../common/libcommon.a ../gl/libgnu.a ../common/libgpgrl.a ../..$(prefix)/lib/libgcrypt.a ../..$(prefix)/lib/libgpg-error.a ../..$(prefix)/lib/libassuan.a -lz
	install gnupg/g10/gpgv2 $(LOCAL)/bin/gpgv2-static
	$(STRIP)  $(LOCAL)/bin/gpgv2-static

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
	rm -rf $(ASSETS)/tests
	rm -rf $(ASSETS)/var

ASSETS := $(CWD)/../assets
assets: clean-assets assets-tests
# add the new stuff
	install -d $(ASSETS)
	cp -a $(LOCAL)/* $(ASSETS)
# remove all the stuff we don't need
	rm -f $(ASSETS)/bin/*-static
	rm -f $(ASSETS)/lib/*.a $(ASSETS)/lib/*.la
# remove lib symlinks since Android AssetManager copies them as files
	rm -rf $(ASSETS)/lib/*.so
	for f in $(ASSETS)/lib/*.so.*; do \
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

clean: install-clean pinentry-clean gnupg-clean curl-clean openldap-clean libksba-clean libassuan-clean npth-clean libgcrypt-clean libgpg-error-clean pth-clean adns-clean libiconv-clean
	-$(MAKE) -C gnupg clean
	-$(MAKE) -C pinentry clean
	-$(MAKE) -C curl clean
	-$(MAKE) -C openldap clean
	-$(MAKE) -C libksba clean
	-$(MAKE) -C libassuan clean
	-$(MAKE) -C libgcrypt clean
	-$(MAKE) -C libgpg-error clean

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
gitclean: distclean
	rm -rf $(ASSETS)/bin
	rm -rf $(ASSETS)/etc
	rm -rf $(ASSETS)/lib
	rm -rf $(ASSETS)/libexec
	rm -rf $(ASSETS)/sbin
	rm -rf $(ASSETS)/share
	rm -rf $(ASSETS)/tests
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
