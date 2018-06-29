LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    atomic.cpp \
    android/sensor.cpp \
    gui/SensorManager.cpp \
    ui/GraphicBuffer.cpp

LOCAL_C_INCLUDES := gui
LOCAL_SHARED_LIBRARIES := libsensor libutils liblog libbinder libandroid libui
LOCAL_MODULE := libshim_camera
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_VENDOR_MODULE := true

include $(BUILD_SHARED_LIBRARY)

