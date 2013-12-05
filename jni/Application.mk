# hack to make this file work in both ndk-build and external/Makefile
APP_PROJECT_PATH := $(shell pwd | sed s,/external$$,,)
NDK_TOOLCHAIN_VERSION=4.7
APP_ABI := armeabi-v7a
# the NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
APP_PLATFORM := android-$(shell sed -n 's,.*android:minSdkVersion="\([0-9][0-9]*\)".*,\1,p' \
	$(APP_PROJECT_PATH)/AndroidManifest.xml)
