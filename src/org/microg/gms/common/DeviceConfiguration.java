/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.common;

import java.util.List;

public class DeviceConfiguration {
    public List<String> availableFeatures;
    public int densityDpi;
    public int deviceClass;
    public int glEsVersion;
    public List<String> glExtensions;
    public boolean hasFiveWayNavigation;
    public boolean hasHardKeyboard;
    public int heightPixels;
    public int keyboardType;
    public List<String> locales;
    public List<String> nativePlatforms;
    public int navigation;
    public int screenLayout;
    public List<String> sharedLibraries;
    public int touchScreen;
    public int widthPixels;
}
