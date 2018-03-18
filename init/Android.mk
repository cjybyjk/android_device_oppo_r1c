LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := \
    system/core/base/include \
    system/core/init
LOCAL_CFLAGS := -Wall
LOCAL_SRC_FILES := init_r1c.cpp
LOCAL_MODULE := libinit_r1c

include $(BUILD_STATIC_LIBRARY)
