/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils;

/**
 * Conversion between the internationally accepted geographic coordinate system (WGS-84)
 * and the geographic coordinate system used in China (GCJ-02)
 */
public class CoordinateConverter {
    private static final double A = 6378245.0;
    private static final double EE = 0.006693421622965943;
    private static final double PI = Math.PI;
    private static final double EPSILON = 1e-6;
    private static final int MAX_ITERATIONS = 10;
    private static final double PI_OVER_180 = PI / 180.0;
    private static final double A_1_EE = A * (1 - EE);

    // ---------- WGS-84 to GCJ-02 ----------

    /**
     * Convert WGS-84 coordinates to GCJ-02 coordinates
     *
     * @param wgsLat latitude
     * @param wgsLon longitude
     * @return GCJ-02 coordinate array, index 0 is latitude, 1 is longitude
     */
    public static double[] wgs84ToGcj02(double wgsLat, double wgsLon) {
        if (isOutOfChina(wgsLat, wgsLon)) {
            return new double[]{wgsLat, wgsLon};
        }

        double dLat = transformLat(wgsLon - 105.0, wgsLat - 35.0);
        double dLon = transformLon(wgsLon - 105.0, wgsLat - 35.0);
        double radLat = wgsLat * PI_OVER_180;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);

        dLat = (dLat * 180.0) / (A_1_EE / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);

        return new double[]{wgsLat + dLat, wgsLon + dLon};
    }

    // ---------- GCJ-02 to WGS-84 ----------

    /**
     * Convert GCJ-02 coordinates to WGS-84 coordinates (iterative approximation method)
     *
     * @param gcjLat latitude
     * @param gcjLon longitude
     * @return WGS-84 coordinate array, index 0 is latitude, 1 is longitude
     */
    public static double[] gcj02ToWgs84(double gcjLat, double gcjLon) {
        if (isOutOfChina(gcjLat, gcjLon)) {
            return new double[]{gcjLat, gcjLon};
        }

        double[] result = {gcjLat, gcjLon};
        double[] delta = new double[2];
        int iteration = 0;

        while (iteration++ < MAX_ITERATIONS) {
            double[] gcjGuess = wgs84ToGcj02(result[0], result[1]);
            delta[0] = gcjLat - gcjGuess[0];
            delta[1] = gcjLon - gcjGuess[1];

            double h = 1e-4;
            double[] gradLat = gradient(result[0], result[1], h, 0);
            double[] gradLon = gradient(result[0], result[1], h, 1);

            double det = gradLat[0] * gradLon[1] - gradLat[1] * gradLon[0];
            if (Math.abs(det) < 1e-12) break;

            double stepLat = (delta[0] * gradLon[1] - delta[1] * gradLat[1]) / det;
            double stepLon = (delta[1] * gradLat[0] - delta[0] * gradLon[0]) / det;

            result[0] += stepLat;
            result[1] += stepLon;

            if (Math.abs(stepLat) < EPSILON && Math.abs(stepLon) < EPSILON) {
                break;
            }
        }
        return result;
    }

    // numerical differentiation to compute gradients
    private static double[] gradient(double lat, double lon, double h, int axis) {
        double[] base = wgs84ToGcj02(lat, lon);
        double[] delta;
        if (axis == 0) {
            delta = wgs84ToGcj02(lat + h, lon);
        } else {
            delta = wgs84ToGcj02(lat, lon + h);
        }
        return new double[]{(delta[0] - base[0]) / h, (delta[1] - base[1]) / h};
    }

    // determine whether the coordinates are outside China
    private static boolean isOutOfChina(double lat, double lon) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    // latitude conversion formula
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    // longitude conversion formula
    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}
