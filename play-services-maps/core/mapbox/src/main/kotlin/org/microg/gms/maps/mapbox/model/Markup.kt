/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.mapbox.model

import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.Options

interface Markup<T : Annotation<*>, S : Options<T>> {
    var annotations: List<AnnotationTracker<T, S>>
    var removed: Boolean

    fun update(manager: AnnotationManager<*, T, S, *, *, *>) {
        synchronized(this) {
            for (tracker in annotations) {
                if (removed && tracker.annotation != null) {
                    manager.delete(tracker.annotation)
                    tracker.annotation = null
                } else if (tracker.annotation != null) {
                    manager.update(tracker.annotation)
                } else if (!removed) {
                    tracker.annotation = manager.create(tracker.options)
                }
            }
        }
    }

    companion object {
        private val TAG = "GmsMapMarkup"
    }
}