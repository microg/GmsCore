<img src="http://i.imgur.com/hXY4lcC.png" height="75px"/> Services Core (GmsCore)
=======
[![Build Status](https://travis-ci.org/microg/android_packages_apps_GmsCore.svg?branch=master)](https://travis-ci.org/microg/android_packages_apps_GmsCore)

microG GmsCore is a FLOSS (Free/Libre Open Source Software) framework to allow applications designed for Google Play Services to run on systems, where Play Services is not available. If you use your phone without GAPPS this might become a useful tool for you.
**This is currently alpha-grade Software. Don't use it if you're not aware of possible consequences. Possible consequences include that your very private data leaks to Fort Meade.**

Project Status
--------------
See "Current Implementation progress" on the [XDA Thread](http://forum.xda-developers.com/android/apps-games/app-microg-gmscore-floss-play-services-t3217616)

Instructions
------------
Requirements and Installation instructions can be found on the [XDA Thread](http://forum.xda-developers.com/android/apps-games/app-microg-gmscore-floss-play-services-t3217616)

Signature Faking
----------------
You need a ROM that supports signature faking. Some custom ROMs are patched to support signature faking out of the box, however most ROMs will require a patch or an Xposed module. Please ask your ROM developer if unsure.

The following ROMs have out-of-box support for signature faking. **Signature spoofing has to be enabled at the bottom of the developer settings first.**
* [OmniROM](http://omnirom.org/)

If you have the **Xposed Framework** installed, the following module will enable signature faking: [FakeGApps by thermatk](http://repo.xposed.info/module/com.thermatk.android.xf.fakegapps)

If you have **Root**, but are not using Xposed, you can try patching your already-installed ROM using [Needle by moosd](https://github.com/moosd/Needle)

If you are a **ROM developer** or just do **custom builds** for whatever reason, you can download and include the patch from [here](https://gerrit.omnirom.org/#/c/14898/) and [here](https://gerrit.omnirom.org/#/c/14899).

Downloads
---------
Standard Releases: [XDA Thread](http://forum.xda-developers.com/android/apps-games/app-microg-gmscore-floss-play-services-t3217616)

Nightly Releases: [Latest](http://files.brnmod.rocks/apps/GmsCore/Latest/play-services-core-debug.apk) or [Other Builds](http://files.brnmod.rocks/apps/GmsCore/)

Building
--------
This can be build using Gradle. 
Prebuilt libraries of [vtm](https://github.com/opensciencemap/vtm) for are included within ./libs.

License
-------
    Copyright 2014-2016 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
