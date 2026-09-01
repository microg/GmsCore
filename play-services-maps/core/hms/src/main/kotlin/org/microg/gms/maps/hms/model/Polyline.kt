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
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.internal.IPolylineDelegate
import com.huawei.hms.maps.model.*
import com.huawei.hms.maps.model.ButtCap as HmsButtCap
import com.huawei.hms.maps.model.Cap as HmsCap
import com.huawei.hms.maps.model.CustomCap as HmsCustomCap
import com.huawei.hms.maps.model.RoundCap as HmsRoundCap
import com.huawei.hms.maps.model.SquareCap as HmsSquareCap
import com.huawei.hms.maps.model.LatLng as HmsLatLng
import org.microg.gms.maps.hms.utils.toGms
import org.microg.gms.maps.hms.utils.toGmsPolylineWidth
import org.microg.gms.maps.hms.utils.toHms
import org.microg.gms.maps.hms.utils.toHmsPolylineWidth

class PolylineImpl(private val polyline: Polyline, polylineOptions: PolylineOptions) : IPolylineDelegate.Stub() {
    private var tag: Any? = null
    private val linePoints = arrayListOf<LatLng>()
    private var lastPointsHash: Int = 0

    private val toHmsCache = mutableMapOf<LatLng, HmsLatLng>()
    private fun List<LatLng>.toHmsList(): List<HmsLatLng> {
        return this.map { latLng ->
            toHmsCache.getOrPut(latLng) { latLng.toHms() }
        }
    }

    override fun remove() {
        linePoints.clear()
        toHmsCache.clear()
        polyline.remove()
    }

    override fun getId(): String = polyline.id

    override fun setPoints(points: List<LatLng>) {
        if (linePoints.size == points.size && linePoints == points) {
            Log.d(TAG, "setPoints skipped: identical points")
            return
        }

        val newHash = points.hashCode()
        if (newHash == lastPointsHash) {
            Log.d(TAG, "setPoints skipped: hash unchanged")
            return
        }
        lastPointsHash = newHash

        linePoints.clear()
        linePoints.addAll(points)
        polyline.points = linePoints.toHmsList()
        Log.d(TAG, "setPoints updated, size=${linePoints.size}")
    }

    override fun getPoints(): List<LatLng> {
        return linePoints
    }

    override fun setWidth(width: Float) {
        polyline.width = toHmsPolylineWidth(width)
    }

    override fun getWidth(): Float {
        return toGmsPolylineWidth(polyline.width)
    }

    override fun setColor(color: Int) {
        polyline.color = color
    }

    override fun getColor(): Int {
        return polyline.color
    }

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "setZIndex: $zIndex")
        polyline.zIndex = zIndex
    }

    override fun getZIndex(): Float {
        Log.d(TAG, "getZIndex")
        return polyline.zIndex
    }

    override fun setVisible(visible: Boolean) {
        polyline.isVisible = visible
    }

    override fun isVisible(): Boolean {
        return polyline.isVisible
    }

    override fun setGeodesic(geod: Boolean) {
        Log.d(TAG, "setGeodesic: $geod")
        polyline.isGeodesic = geod
    }

    override fun isGeodesic(): Boolean {
        Log.d(TAG, "isGeodesic")
        return polyline.isGeodesic
    }

    override fun setClickable(clickable: Boolean) {
        Log.d(TAG, "setClickable: $clickable")
        polyline.isClickable = clickable
    }

    override fun isClickable(): Boolean {
        Log.d(TAG, "isClickable")
        return polyline.isClickable
    }

    override fun equalsRemote(other: IPolylineDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is PolylineImpl) {
            return other.id == id
        }
        return false
    }

    override fun getPattern(): List<PatternItem>? {
        Log.d(TAG, "Method: getStrokePattern")
        return polyline.pattern?.map { it.toGms() }
    }

    override fun getTag(): IObjectWrapper {
        return ObjectWrapper.wrap(this.tag)
    }

    override fun setJointType(jointType: Int) {
        polyline.jointType = jointType
    }

    override fun getJointType(): Int {
        return polyline.jointType
    }

    override fun setPattern(pattern: List<PatternItem>?) {
        Log.d(TAG, "Method: setStrokePattern")
        polyline.pattern = pattern?.map { it.toHms() }
    }

    override fun setTag(tag: IObjectWrapper?) {
        this.tag = tag.unwrap()
    }

    override fun setEndCap(endCap: Cap) {
        polyline.endCap = endCap.toHms()
    }

    override fun getEndCap(): Cap {
        return polyline.endCap.toGms()
    }

    override fun setStartCap(startCap: Cap) {
        polyline.startCap = startCap.toHms()
    }

    override fun getStartCap(): Cap {
        return polyline.startCap.toGms()
    }

    private fun Cap.toHms(): HmsCap {
        return when (this) {
            is ButtCap -> HmsButtCap()
            is SquareCap -> HmsSquareCap()
            is RoundCap -> HmsRoundCap()
            is CustomCap -> HmsCustomCap(bitmapDescriptor.remoteObject.unwrap(), refWidth)
            else -> HmsButtCap()
        }
    }

    private fun com.huawei.hms.maps.model.Cap.toGms(): Cap {
        return when (this) {
            is HmsButtCap -> ButtCap()
            is HmsSquareCap -> SquareCap()
            is HmsRoundCap -> RoundCap()
            is HmsCustomCap -> CustomCap(BitmapDescriptor(ObjectWrapper.wrap(bitmapDescriptor)), refWidth)
            else -> ButtCap()
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        private val TAG = "GmsMapPolyline"
    }
}