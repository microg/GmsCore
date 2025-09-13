package org.microg.gms.maps.mapbox.model

import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.plugins.annotation.Options

data class AnnotationTracker<T : Annotation<*>, S : Options<T>>(
    var options: S,
    var annotation: T? = null
)
