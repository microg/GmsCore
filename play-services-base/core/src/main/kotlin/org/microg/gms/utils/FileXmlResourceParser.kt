/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.content.res.XmlResourceParser
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.Reader

class FileXmlResourceParser(private val reader: Reader, private val parser: XmlPullParser = Xml.newPullParser()) :
    XmlResourceParser,
    XmlPullParser by parser,
    Closeable by reader {
    constructor(file: File) : this(FileReader(file))

    init {
        parser.setInput(reader)
    }

    override fun getAttributeNameResource(index: Int): Int {
        return 0
    }

    override fun getAttributeListValue(
        namespace: String?, attribute: String?,
        options: Array<String?>?, defaultValue: Int
    ): Int {
        val s = getAttributeValue(namespace, attribute)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeBooleanValue(
        namespace: String?, attribute: String?,
        defaultValue: Boolean
    ): Boolean {

        val s = getAttributeValue(namespace, attribute)
        return s?.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun getAttributeResourceValue(
        namespace: String?, attribute: String?,
        defaultValue: Int
    ): Int {
        val s = getAttributeValue(namespace, attribute)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeIntValue(
        namespace: String?, attribute: String?,
        defaultValue: Int
    ): Int {
        val s = getAttributeValue(namespace, attribute)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeUnsignedIntValue(
        namespace: String?, attribute: String?,
        defaultValue: Int
    ): Int {
        val s = getAttributeValue(namespace, attribute)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeFloatValue(
        namespace: String?, attribute: String?,
        defaultValue: Float
    ): Float {
        val s = getAttributeValue(namespace, attribute)
        return s?.toFloat() ?: defaultValue
    }

    override fun getAttributeListValue(
        index: Int,
        options: Array<String?>?, defaultValue: Int
    ): Int {
        val s = getAttributeValue(index)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeBooleanValue(index: Int, defaultValue: Boolean): Boolean {
        val s = getAttributeValue(index)
        return s?.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun getAttributeResourceValue(index: Int, defaultValue: Int): Int {
        val s = getAttributeValue(index)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeIntValue(index: Int, defaultValue: Int): Int {
        val s = getAttributeValue(index)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int): Int {
        val s = getAttributeValue(index)
        return s?.toInt() ?: defaultValue
    }

    override fun getAttributeFloatValue(index: Int, defaultValue: Float): Float {
        val s = getAttributeValue(index)
        return s?.toFloat() ?: defaultValue
    }

    override fun getIdAttribute(): String? {
        return getAttributeValue(null, "id")
    }

    override fun getClassAttribute(): String? {
        return getAttributeValue(null, "class")
    }

    override fun getIdAttributeResourceValue(defaultValue: Int): Int {
        return getAttributeResourceValue(null, "id", defaultValue)
    }

    override fun getStyleAttribute(): Int {
        return getAttributeResourceValue(null, "style", 0)
    }
}
