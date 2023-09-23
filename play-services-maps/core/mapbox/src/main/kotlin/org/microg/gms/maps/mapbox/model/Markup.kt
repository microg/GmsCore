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

import android.util.Log
import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.Options

interface Markup<T : Annotation<*>, S : Options<T>> {
    var annotation: T?
    val annotationOptions: S
    var removed: Boolean

    fun update(manager: AnnotationManager<*, T, S, *, *, *>) {
        synchronized(this) {
            if (removed && annotation != null) {
                manager.delete(annotation)
                annotation = null
            } else if (annotation != null) {
                manager.update(annotation)
            } else if (!removed) {
                annotation = manager.create(annotationOptions)
            }
        }
    }

    companion object {
        private val TAG = "GmsMapMarkup"
    }
}