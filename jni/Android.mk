LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

external         := $(NDK_APP_PROJECT_PATH)/external
prefix           := data/data/info.guardianproject.gpg/app_opt
APP_OPTIM        := debug
LOCAL_MODULE     := libgnupg-for-java
LOCAL_C_INCLUDES := $(external)/$(prefix)/include $(external)/gpgme/src
LOCAL_CFLAGS     += -Wformat -Werror=format-security
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
prefix           := data/data/info.guardianproject.gpg/app_opt
APP_OPTIM        := debug
LOCAL_MODULE     := libpinentry
LOCAL_CFLAGS := -DHAVE_CONFIG_H -Wformat -Werror=format-security
LOCAL_LDLIBS     += -llog
LOCAL_C_INCLUDES := \
	$(external)/pinentry/assuan \
	$(external)/pinentry/pinentry \
	$(external)/pinentry/secmem
LOCAL_SRC_FILES  := \
	pinentry_cmd_handler.c \
	../external/pinentry/pinentry/pinentry.c \
	../external/pinentry/assuan/assuan-buffer.c \
	../external/pinentry/assuan/assuan-errors.c \
	../external/pinentry/assuan/assuan-handler.c \
	../external/pinentry/assuan/assuan-listen.c \
	../external/pinentry/assuan/assuan-pipe-server.c \
	../external/pinentry/assuan/assuan-util.c \
	../external/pinentry/secmem/secmem.c \
	../external/pinentry/secmem/util.c

# pinentry's assuan requires assuan/mkerrors to be run in order to generate
# assuan-errors.c, which is the assuan_strerror() implementation
GENERATE_PINENTRY_ASSUAN_ERRORS_C := \
	$(shell $(external)/pinentry/assuan/mkerrors \
		< $(external)/pinentry/assuan/assuan.h \
		> $(external)/pinentry/assuan/assuan-errors.c)

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := libposix
LOCAL_CFLAGS += -Wformat -Werror=format-security
LOCAL_LDLIBS += -llog
LOCAL_SRC_FILES := info_guardianproject_gpg_Posix.c

include $(BUILD_SHARED_LIBRARY)
