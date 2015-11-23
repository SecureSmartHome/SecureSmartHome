LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := odroid_weather
LOCAL_SRC_FILES := odroid_weather.cpp
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
