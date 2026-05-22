# microG Services

    Copyright 2013-2024 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.

microG Services is a FLOSS (Free/Libre Open Source Software) framework to allow applications designed for Google Play Services to run on systems, where Play Services is not available.

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

### WearOS Support

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := microg-wear-api
LOCAL_SRC_FILES := $(call all-java-files-under, play-services-wearable)
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current
include $(BUILD_PACKAGE)
- Support for WearOS standalone applications
- Google Play Services API compatibility for WearOS apps

To enable WearOS support:

1. Install the latest microG services
2. Ensure WearOS device pairing is enabled in system settings
3. Configure notification synchronization in the microG settings app

If you'd like to help translate microG, take a look at [TRANSLATION](TRANSLATION.md).


License
-------
    Copyright 2013-2025 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
