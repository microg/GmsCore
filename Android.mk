LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Generate Gradle BuildConfig.mk file since AOSP does not handle that
# Remove the generated file if you want it to be regenerated with new values

UITOOLS_BUILDCONFIG_CLASS := microg-ui-tools/src/main/java/org/microg/tools/ui/BuildConfig.java
UITOOLS_BC_PATH := $(LOCAL_PATH)/$(UITOOLS_BUILDCONFIG_CLASS)
UITOOLS_BC_APPLICATION_ID := "org.microg.tools.ui"
UITOOLS_BC_VERSION_CODE := -1

$(UITOOLS_BC_PATH):
	echo "/**" > $(UITOOLS_BC_PATH)
	echo "* Automatically generated file. DO NOT MODIFY" >> $(UITOOLS_BC_PATH)
	echo "*/" >> $(UITOOLS_BC_PATH)
	echo "package "$(UITOOLS_BC_APPLICATION_ID)";" >> $(UITOOLS_BC_PATH)
	echo "public final class BuildConfig {" >> $(UITOOLS_BC_PATH)
	echo "    public static final String APPLICATION_ID = \""$(UITOOLS_BC_APPLICATION_ID)"\";" >> $(UITOOLS_BC_PATH)
	echo "    public static final int VERSION_CODE = "$(UITOOLS_BC_VERSION_CODE)";" >> $(UITOOLS_BC_PATH)
	echo "    private BuildConfig() {}" >> $(UITOOLS_BC_PATH)
	echo "}" >> $(UITOOLS_BC_PATH)

LOCAL_MODULE := MicroGUiTools
LOCAL_SRC_FILES := $(call all-java-files-under, microg-ui-tools/src/main/java)
LOCAL_SRC_FILES += $(UITOOLS_BUILDCONFIG_CLASS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/microg-ui-tools/src/main/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_MANIFEST_FILE := microg-ui-tools/src/main/AndroidManifest.xml
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v7-appcompat

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat

include $(BUILD_STATIC_JAVA_LIBRARY)
