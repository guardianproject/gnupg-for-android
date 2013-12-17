LOCAL_PATH:= $(call my-dir)

external := $(NDK_APP_PROJECT_PATH)/external
prefix   := data/data/info.guardianproject.gpg/app_opt
LOCAL    := $(external)/$(prefix)


include $(CLEAR_VARS)
LOCAL_MODULE := libassuan
LOCAL_SRC_FILES := $(LOCAL)/lib/libassuan.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libcurl
LOCAL_SRC_FILES := $(LOCAL)/lib/libcurl.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libgcrypt
LOCAL_SRC_FILES := $(LOCAL)/lib/libgcrypt.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libgpg-error
LOCAL_SRC_FILES := $(LOCAL)/lib/libgpg-error.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libgpgme
LOCAL_SRC_FILES := $(LOCAL)/lib/libgpgme.so
LOCAL_EXPORT_C_INCLUDES := $(external)/$(prefix)/include $(external)/gpgme/src
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libksba
LOCAL_SRC_FILES := $(LOCAL)/lib/libksba.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnpth
LOCAL_SRC_FILES := $(LOCAL)/lib/libnpth.so
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE     := libgnupg-for-java
LOCAL_CFLAGS     += -Wformat -Werror=format-security
LOCAL_LDLIBS     += -llog
LOCAL_SHARED_LIBRARIES := libgpgme
LOCAL_SRC_FILES  := \
	GnuPGContext.c \
	GnuPGData.c \
	GnuPGGenkeyResult.c \
	GnuPGKey.c \
	GnuPGSignature.c \
	gpgmeutils.c 
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
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
