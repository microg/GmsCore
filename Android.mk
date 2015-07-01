# Copyright (c) 2014 μg Project Team
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := GmsCore
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := GmsCore

module_root  := $(LOCAL_PATH)
module_dir   := play-services-core
module_out   := $(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
module_build := $(module_root)/$(module_dir)/build
module_apk   := build/outputs/apk/play-services-core-release-unsigned.apk

$(module_root)/$(module_dir)/$(module_apk):
	rm -Rf $(module_build)
	mkdir -p $(module_out)
	ln -s $(module_out) $(module_build)
	echo "sdk.dir=$(ANDROID_HOME)" > $(module_root)/local.properties
	cd $(module_root) && git submodule update --recursive --init
	cd $(module_root)/$(module_dir) && JAVA_TOOL_OPTIONS="$(JAVA_TOOL_OPTIONS) -Dfile.encoding=UTF8" ../gradlew assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(module_dir)/$(module_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
