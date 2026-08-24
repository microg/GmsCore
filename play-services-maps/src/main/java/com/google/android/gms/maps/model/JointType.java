/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

/**
 * Joint types for Polyline and outline of Polygon.
 */
public final class JointType {
    /**
     * Default: Mitered joint, with fixed pointed extrusion equal to half the stroke width on the outside of the joint.
     */
    public static final int DEFAULT = 0;
    /**
     * Flat bevel on the outside of the joint.
     */
    public static final int BEVEL = 1;
    /**
     * Rounded on the outside of the joint by an arc of radius equal to half the stroke width, centered at the vertex.
     */
    public static final int ROUND = 2;
}
