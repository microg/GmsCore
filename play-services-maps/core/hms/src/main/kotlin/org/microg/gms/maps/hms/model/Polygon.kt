/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.internal.IPolygonDelegate
import com.huawei.hms.maps.model.Polygon
import org.microg.gms.maps.hms.utils.toGms
import org.microg.gms.maps.hms.utils.toHms
import org.microg.gms.utils.warnOnTransactionIssues

class PolygonImpl(private val polygon: Polygon) : IPolygonDelegate.Stub() {
    private var tag: Any? = null

    override fun remove() {
        polygon.remove()
    }

    override fun getId(): String = polygon.id

    override fun setPoints(points: List<LatLng>) {
        polygon.points = points.map { it.toHms() }
    }

    override fun getPoints(): List<LatLng> = polygon.points.map { it.toGms() }

    override fun setHoles(holes: List<Any?>?) {
        if (holes == null) {
            polygon.holes = emptyList()
        } else {
            val outHmsHoles: MutableList<MutableList<com.huawei.hms.maps.model.LatLng>> =
                ArrayList()
            for (out in holes) {
                if (out is List<*>) {
                    val inHmsHoles: MutableList<com.huawei.hms.maps.model.LatLng> = ArrayList()
                    for (inn in out) {
                        if (inn is LatLng) {
                            inHmsHoles.add(inn.toHms())
                        }
                    }
                    outHmsHoles.add(inHmsHoles)
                }
            }
            polygon.holes = outHmsHoles
        }
    }

    override fun getHoles(): List<Any?> {
        val outHoles = polygon.holes ?: return emptyList()
        val outGmsHoles: MutableList<MutableList<LatLng>> = ArrayList()
        for (inHoles in outHoles) {
            if (inHoles != null) {
                val inGmsHoles: MutableList<LatLng> = ArrayList()
                for (inHole in inHoles) {
                    inGmsHoles.add(inHole.toGms())
                }
                outGmsHoles.add(inGmsHoles)
            }
        }
        return outGmsHoles
    }

    override fun setStrokeWidth(width: Float) {
        polygon.strokeWidth = width
    }

    override fun getStrokeWidth(): Float = polygon.strokeWidth

    override fun setStrokeColor(color: Int) {
        polygon.strokeColor = color
    }

    override fun getStrokeColor(): Int = polygon.strokeColor

    override fun setFillColor(color: Int) {
        polygon.fillColor = color
    }

    override fun getFillColor(): Int {
        return polygon.fillColor
    }

    override fun setZIndex(zIndex: Float) {
        polygon.zIndex = zIndex
    }

    override fun getZIndex(): Float {
        return polygon.zIndex
    }

    override fun setVisible(visible: Boolean) {
        polygon.isVisible = visible
    }

    override fun isVisible(): Boolean {
        return polygon.isVisible
    }

    override fun setGeodesic(geod: Boolean) {
        polygon.isGeodesic = geod
    }

    override fun isGeodesic(): Boolean {
        return polygon.isGeodesic
    }

    override fun getStrokeJointType(): Int {
        return polygon.strokeJointType
    }

    override fun getStrokePattern(): List<PatternItem>? {
        return polygon.strokePattern?.map { it.toGms() }
    }

    override fun getTag(): IObjectWrapper {
        return ObjectWrapper.wrap(this.tag)
    }

    override fun isClickable(): Boolean {
        return polygon.isClickable
    }

    override fun setClickable(clickable: Boolean) {
        polygon.isClickable = clickable
    }

    override fun setStrokeJointType(jointType: Int) {
        polygon.strokeJointType = jointType
    }

    override fun setStrokePattern(pattern: List<PatternItem>?) {
        polygon.strokePattern = pattern?.map { it.toHms() }
    }

    override fun setTag(tag: IObjectWrapper?) {
        this.tag = tag.unwrap()
    }

    override fun equalsRemote(other: IPolygonDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is PolygonImpl) {
            return other.id == id
        }
        return false
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }

    companion object {
        private val TAG = "GmsMapPolygon"
    }
}
