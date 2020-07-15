/*
 * Copyright (C) 2013-2017 microG Project Team
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

package com.google.android.gms.wearable;

import java.util.Set;

/**
 * Information about a Capability on the network and where it is available.
 */
public interface CapabilityInfo {
    /**
     * Returns the name of the capability.
     */
    String getName();

    /**
     * Returns the set of nodes for the capability. Disconnected nodes may or may not be included in the set.
     */
    Set<Node> getNodes();
}
