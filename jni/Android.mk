LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

external         := $(NDK_APP_PROJECT_PATH)/external
prefix           := data/data/info.guardianproject.gpg/app_opt
APP_OPTIM        := debug
LOCAL_MODULE     := libgpgme-for-java
LOCAL_C_INCLUDES := $(external)/$(prefix)/include $(external)/gpgme/src
LOCAL_LDFLAGS    := -L$(external)/$(prefix)/lib -lgpgme
LOCAL_LDLIBS     += -L$(external)/$(prefix)/lib -lgpgme
LOCAL_SRC_FILES  := \
	GnuPGContext.c \
	GnuPGData.c \
	GnuPGGenkeyResult.c \
	GnuPGKey.c \
	GnuPGSignature.c \
	gpgmeutils.c 

include $(BUILD_SHARED_LIBRARY)
