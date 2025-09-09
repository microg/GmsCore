/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.utils

import android.view.View
import android.view.ViewGroup

enum class MapUiElement(val classType: String) {
    MyLocationButton("MyLocationButton"),
    ZoomView("ZoomView"),
    CompassView("CompassView")
}

class MapUiController(private val rootView: ViewGroup) {

    private val uiViews = mutableMapOf<MapUiElement, View>()
    private val uiStates = mutableMapOf<MapUiElement, Boolean>()

    init {
        MapUiElement.entries.forEach { element ->
            rootView.waitForChild(element.classType) { view ->
                uiViews[element] = view
                val uiEnabled = isUiEnabled(element)
                view.isEnabled = uiEnabled
                view.alpha = if (uiEnabled) 1f else 0f
            }
        }
    }

    fun setUiEnabled(element: MapUiElement, enabled: Boolean) {
        uiStates[element] = enabled
        uiViews[element]?.alpha = if (enabled) 1f else 0f
        uiViews[element]?.isEnabled = enabled
    }

    fun isUiEnabled(element: MapUiElement): Boolean {
        return uiStates[element] ?: true
    }

    fun initUiStates(states: Map<MapUiElement, Boolean>) {
        states.forEach { (element, enabled) ->
            setUiEnabled(element, enabled)
        }
    }
}

private fun ViewGroup.waitForChild(classType: String, onReady: (View) -> Unit) {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child.javaClass.name.contains(classType)) {
            onReady(child)
            return
        }
        if (child is ViewGroup) {
            child.waitForChild(classType, onReady)
        }
    }

    setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View?, child: View?) {
            if (child?.javaClass?.name?.contains(classType) == true) {
                onReady(child)
            }
        }

        override fun onChildViewRemoved(parent: View?, child: View?) {}
    })
}