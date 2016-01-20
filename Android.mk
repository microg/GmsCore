LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := MicroGUiTools
LOCAL_SRC_FILES := $(call all-java-files-under, microg-ui-tools/src/main/java)

include $(BUILD_STATIC_JAVA_LIBRARY)
