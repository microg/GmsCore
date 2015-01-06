# Not working yet
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := GmsApi
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES := $(call all-Iaidl-files-under, src)

include $(BUILD_STATIC_JAVA_LIBRARY)
