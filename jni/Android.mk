LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

external         := $(NDK_APP_PROJECT_PATH)/external
prefix           := data/data/info.guardianproject.gpg/app_opt
APP_OPTIM        := debug
LOCAL_MODULE     := libgnupg-for-java
LOCAL_C_INCLUDES := $(external)/$(prefix)/include $(external)/gpgme/src
LOCAL_LDLIBS     += -L$(external)/$(prefix)/lib -lgpgme -llog
LOCAL_SRC_FILES  := \
	GnuPGContext.c \
	GnuPGData.c \
	GnuPGGenkeyResult.c \
	GnuPGKey.c \
	GnuPGSignature.c \
	gpgmeutils.c 

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

external         := $(NDK_APP_PROJECT_PATH)/external
source_files     := $(NDK_APP_PROJECT_PATH)/../external
prefix           := data/data/info.guardianproject.gpg/app_opt
APP_OPTIM        := debug
LOCAL_MODULE     := libpinentry
LOCAL_CFLAGS := -DHAVE_CONFIG_H
LOCAL_LDLIBS     += -llog
LOCAL_C_INCLUDES := \
	$(external)/pinentry/assuan \
	$(external)/pinentry/pinentry \
	$(external)/pinentry/secmem
LOCAL_SRC_FILES  := \
	pinentry_cmd_handler.c \
	$(source_files)/pinentry/pinentry/pinentry.c \
	$(source_files)/pinentry/assuan/assuan-buffer.c \
	$(source_files)/pinentry/assuan/assuan-errors.c \
	$(source_files)/pinentry/assuan/assuan-handler.c \
	$(source_files)/pinentry/assuan/assuan-listen.c \
	$(source_files)/pinentry/assuan/assuan-pipe-server.c \
	$(source_files)/pinentry/assuan/assuan-util.c \
	$(source_files)/pinentry/secmem/secmem.c \
	$(source_files)/pinentry/secmem/util.c

# pinentry's assuan requires assuan/mkerrors to be run in order to generate
# assuan-errors.c, which is the assuan_strerror() implementation
GENERATE_PINENTRY_ASSUAN_ERRORS_C := \
	$(shell $(external)/pinentry/assuan/mkerrors \
		< $(external)/pinentry/assuan/assuan.h \
		> $(external)/pinentry/assuan/assuan-errors.c)

include $(BUILD_SHARED_LIBRARY)
