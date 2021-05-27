# microG Services Core (HUAWEI patched)

[![Build Status](https://travis-ci.com/jcchikikomori/GmsCoreHuawei.svg?branch=feature%2FHUAWEI-patch)](https://travis-ci.org/jcchikikomori/GmsCoreHuawei)

## What is microG

microG GmsCore is a FLOSS (Free/Libre Open Source Software) framework to allow applications designed for Google Play Services to run on systems, where Play Services is not available.

## Then why GmsCoreHuawei

GmsCoreHuawei is a forked version of microG GmsCore which is a free software reimplementation of Google's Play Services. The difference is to attempt to allow applications calling proprietary Google APIs to run specifically on Google-less HUAWEI devices (EMUI 10 and up).

Also, this fork applies the following compared to original microG

- Fake device info to bypass Google's restrictions
- Based on YouTube Vanced's codebase

### Please refer to the [wiki](https://github.com/jcchikikomori/GmsCoreHuawei/wiki) for downloads and instructions. Also check my [article](https://johncyrillcorsanes.medium.com/google-alternatives-for-huawei-devices-c91f6fae6300)

## DISCLAIMER

    This project is not recommended for production & enterprise purposes, since device faking is somewhat illegal in Google's legal terms.

    This is only a experiment on how Google Services works on the entire Android space.

## License

License
-------
    Copyright 2013-2021 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
