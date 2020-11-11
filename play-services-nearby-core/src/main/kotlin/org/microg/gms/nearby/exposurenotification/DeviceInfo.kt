/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package org.microg.gms.nearby.exposurenotification

import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.exposurenotification.CalibrationConfidence
import kotlin.math.roundToInt

data class DeviceInfo(val oem: String, val device: String, val model: String, val rssiCorrection: Byte, val txPowerCorrection: Byte, @CalibrationConfidence val confidence: Int = CalibrationConfidence.MEDIUM)

private var knownDeviceInfo: DeviceInfo? = null

fun averageCurrentDeviceInfo(oem: String, device: String, model: String, deviceInfos: List<DeviceInfo>, @CalibrationConfidence confidence: Int = CalibrationConfidence.LOW): DeviceInfo =
        DeviceInfo(oem, device, model, deviceInfos.map { it.rssiCorrection }.average().roundToInt().toByte(), deviceInfos.map { it.txPowerCorrection }.average().roundToInt().toByte(), confidence)

val currentDeviceInfo: DeviceInfo
    get() {
        var deviceInfo = knownDeviceInfo
        if (deviceInfo == null) {
            // Note: Custom ROMs sometimes have slightly different model information, so we have some flexibility for those
            val byOem = allDeviceInfos.filter { it.oem.equalsIgnoreCase(Build.MANUFACTURER) }
            val byDevice = allDeviceInfos.filter { it.device.equalsIgnoreCase(Build.DEVICE) }
            val byModel = allDeviceInfos.filter { it.model.equalsIgnoreCase(Build.MODEL) }
            val exactMatch = byOem.find { it.device.equalsIgnoreCase(Build.DEVICE) && it.model.equalsIgnoreCase(Build.MODEL) }
            deviceInfo = when {
                exactMatch != null -> {
                    // Exact match, use provided confidence
                    exactMatch
                }
                byModel.isNotEmpty() || byDevice.isNotEmpty() -> {
                    // We have data from "sister devices", that's way better than taking the OEM average
                    averageCurrentDeviceInfo(Build.MANUFACTURER, Build.DEVICE, Build.MODEL, (byDevice + byModel).distinct(), CalibrationConfidence.MEDIUM)
                }
                byOem.isNotEmpty() -> {
                    // Fallback to OEM average
                    averageCurrentDeviceInfo(Build.MANUFACTURER, Build.DEVICE, Build.MODEL, byOem, CalibrationConfidence.LOW)
                }
                else -> {
                    // Fallback to all device average
                    averageCurrentDeviceInfo(Build.MANUFACTURER, Build.DEVICE, Build.MODEL, allDeviceInfos, CalibrationConfidence.LOWEST)
                }
            }
            Log.i(TAG, "Selected $deviceInfo")
            knownDeviceInfo = deviceInfo
        }
        return deviceInfo
    }

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun String.equalsIgnoreCase(other: String): Boolean = (this as java.lang.String).equalsIgnoreCase(other)

/*
 * Derived from en-calibration-2020-09-30.csv published via
 * https://developers.google.com/android/exposure-notifications/ble-attenuation-computation#device-list
 */
val allDeviceInfos = listOf(
        DeviceInfo("asus","ASUS_Z00D","ASUS Asus ZenFone 2 Laser ZE500KG" ,3,-22,1),
        DeviceInfo("asus","ASUS_Z00D","ASUS ZenFone 2E",3,-22,1),
        DeviceInfo("asus","ASUS_Z00D","ASUS ZenFone 2",3,-22,1),
        DeviceInfo("asus", "ASUS_A001", "ASUS_A001", 9, -25, 3),
        DeviceInfo("asus", "ASUS_X008_1", "ASUS_X008DC", 0, -21, 3),
        DeviceInfo("asus", "ASUS_X00T_3", "ASUS_X00TD", 2, -26, 3),
        DeviceInfo("asus", "ASUS_X018_4", "ASUS_X018D", 2, -22, 3),
        DeviceInfo("asus", "ASUS_X01BD_1", "ASUS_X01BDA", 2, -25, 3),
        DeviceInfo("asus", "ASUS_Z010_CD", "ASUS_Z010D", 2, -18, 3),
        DeviceInfo("asus", "ASUS_Z01F_1", "ASUS_Z01FD", 5, -20, 3),
        DeviceInfo("asus", "P027", "P027", 4, -20, 3),
        DeviceInfo("blackberry", "bbe100", "BBE100-4", 6, -27, 3),
        DeviceInfo("blu", "Grand_M", "Grand M", -1, -21, 3),
        DeviceInfo("blu", "Studio_Mega", "Studio Mega", 6, -28, 3),
        DeviceInfo("blu", "Tank_Xtreme_4_0", "Tank Xtreme 4.0", 1, -24, 3),
        DeviceInfo("blu", "BLU_VIVO_5", "VIVO 5", 5, -24, 3),
        DeviceInfo("blu", "V0270WW", "Vivo ONE", 4, -25, 3),
        DeviceInfo("coolpad", "CP8722", "Coolpad 8722V", 0, -20, 3),
        DeviceInfo("essential products", "mata", "PH-1", 7, -24, 3),
        DeviceInfo("google", "sailfish", "Pixel", -3, -26, 3),
        DeviceInfo("google", "blueline", "Pixel 3", 5, -33, 3),
        DeviceInfo("google", "sargo", "Pixel 3a", 2, -29, 3),
        DeviceInfo("google", "bonito", "Pixel 3a XL", 2, -28, 3),
        DeviceInfo("google", "flame", "Pixel 4", 8, -30, 3),
        DeviceInfo("google", "coral", "Pixel 4 XL", 7, -26, 3),
        DeviceInfo("google", "sunfish", "Pixel 4a", 1, -30, 3),
        DeviceInfo("google", "marlin", "Pixel XL", -2, -26, 3),
        DeviceInfo("google", "gobo", "gobo_512", 2, -22, 3),
        DeviceInfo("htc", "htc_pmewhl", "2PS64", -1, -31, 3),
        DeviceInfo("htc", "htc_pmewl", "HTC 10", 1, -31, 3),
        DeviceInfo("htc", "htc_pmeuhl", "HTC 10", 2, -33, 3),
        DeviceInfo("htc", "htc_a16ul", "HTC Desire 530", 5, -28, 3),
        DeviceInfo("htc", "htc_ocnwhl", "HTC U11", 2, -21, 3),
        DeviceInfo("htc", "htc_ocndugl", "HTC U11", 2, -27, 3),
        DeviceInfo("htc", "htc_ocmdugl", "HTC U11 plus", 3, -28, 3),
        DeviceInfo("htc", "htc_imldugl", "HTC U12 life", 2, -23, 3),
        DeviceInfo("htc", "htc_imedugl", "HTC U12+", 7, -33, 3),
        DeviceInfo("htc", "htc_ocedugl", "HTC_U-1u", -5, -29, 3),
        DeviceInfo("htc", "htc_ocndugl", "HTC_U-3u", 5, -23, 3),
        DeviceInfo("huawei", "HWANE", "ANE-LX3", 3, -3, 3),
        DeviceInfo("huawei", "HWCLT", "CLT-L09", -1, -30, 3),
        DeviceInfo("huawei", "HWCOR", "COR-L29", 2, -5, 3),
        DeviceInfo("huawei", "HWDRA-MG", "DRA-LX3", 4, -24, 3),
        DeviceInfo("huawei", "HWTIT-L6735", "HUAWEI TIT-AL00", 4, -10, 3),
        DeviceInfo("huawei", "HW-01K", "HW-01K", -1, -32, 3),
        DeviceInfo("huawei", "hwfdra04l", "HWT31", 3, -24, 3),
        DeviceInfo("huawei", "HNKIW-Q", "KIW-L24", 7, -25, 3),
        DeviceInfo("huawei", "HWMHA", "MHA-L29", 2, -27, 3),
        DeviceInfo("huawei", "HWNEO", "NEO-L29", -3, -28, 3),
        DeviceInfo("huawei", "angler", "Nexus 6P", 6, -27, 3),
        DeviceInfo("huawei", "HWPIC", "PIC-AL00", -1, -3, 3),
        DeviceInfo("huawei", "HWPRA-H", "PRA-LX1", 1, -3, 3),
        DeviceInfo("huawei", "HWWAS-H", "WAS-LX3", 2, -4, 3),
        DeviceInfo("infinix", "Infinix-X627STU", "Infinix X627", 3, -27, 3),
        DeviceInfo("infinix", "Infinix-X650", "Infinix X650", 1, -26, 3),
        DeviceInfo("infinix", "Infinix-X650C", "Infinix X650C", 3, -30, 3),
        DeviceInfo("infinix", "Infinix-X653", "Infinix X653", 4, -24, 3),
        DeviceInfo("infinix", "Infinix-X653C", "Infinix X653C", -1, -23, 3),
        DeviceInfo("itel", "itel-A32F", "itel A32F", 3, -26, 3),
        DeviceInfo("itel", "itel-L5503", "itel L5503", -3, -6, 3),
        DeviceInfo("itel", "itel-L6005", "itel L6005", -4, -1, 3),
        DeviceInfo("itel", "itel-W5504", "itel W5504", -1, -11, 3),
        DeviceInfo("lava", "Z50", "Z50", 6, -26, 3),
        DeviceInfo("leagoo", "T5c", "T5c", -29, -23, 3),
        DeviceInfo("lenovo", "A1010a20", "Lenovo A1010a20", 2, -25, 3),
        DeviceInfo("lenovo", "A6600d40", "Lenovo A6600d40", 3, -26, 3),
        DeviceInfo("lenovo", "K33a42", "Lenovo K33a42", 3, -29, 3),
        DeviceInfo("lenovo", "K33a48", "Lenovo K33a48", 3, -27, 3),
        DeviceInfo("lenovo", "seoul", "Lenovo K520", 4, -20, 3),
        DeviceInfo("lenovo", "k52_e78", "Lenovo K52e78", 0, -18, 3),
        DeviceInfo("lenovo", "K53a48", "Lenovo K53a48", 5, -29, 3),
        DeviceInfo("lenovo", "brady_f", "Lenovo K8", 4, -22, 3),
        DeviceInfo("lenovo", "manning", "Lenovo K8 Note", 4, -26, 3),
        DeviceInfo("lenovo", "jd2018", "Lenovo L78011", 1, -21, 3),
        DeviceInfo("lenovo", "P2a42", "Lenovo P2a42", 3, -19, 3),
        DeviceInfo("lenovo", "TB3-710I", "Lenovo TB3-710I", 3, -26, 3),
        DeviceInfo("lenovo", "zoom_tdd", "Lenovo Z90-3", 5, -22, 3),
        DeviceInfo("letv", "le_s2_ww", "Le X527", 4, -21, 3),
        DeviceInfo("lge", "lv0", "LG-AS110", 6, -24, 3),
        DeviceInfo("lge", "p1", "LG-F500L", -3, -24, 3),
        DeviceInfo("lge", "h1", "LG-F700L", 9, -28, 3),
        DeviceInfo("lge", "c50", "LG-H345", 3, -21, 3),
        DeviceInfo("lge", "g4stylus", "LG-H634", 3, -21, 3),
        DeviceInfo("lge", "p1", "LG-H815", -3, -25, 3),
        DeviceInfo("lge", "mme0n", "LG-K100", 2, -23, 3),
        DeviceInfo("lge", "mm1v", "LG-K350", 1, -21, 3),
        DeviceInfo("lge", "m253", "LG-K430", 2, -22, 3),
        DeviceInfo("lge", "k5", "LG-K500", 6, -22, 3),
        DeviceInfo("lge", "ph1", "LG-K540", 6, -22, 3),
        DeviceInfo("lge", "mlv5", "LG-M250", 3, -22, 3),
        DeviceInfo("lge", "mlv1", "LG-X230", 2, -23, 3),
        DeviceInfo("lge", "mlv3", "LG-X240", 1, -22, 3),
        DeviceInfo("lge", "e7iilte", "LGLK430", 2, -18, 3),
        DeviceInfo("lge", "me0", "LGLS450", 5, -24, 3),
        DeviceInfo("lge", "joan", "LGM-V300L", 4, -21, 3),
        DeviceInfo("lge", "anna", "LGM-X800L", 6, -13, 3),
        DeviceInfo("lge", "p1", "LGUS991", -4, -25, 3),
        DeviceInfo("lge", "mdh30xlm", "LM-K500", 6, -30, 3),
        DeviceInfo("lge", "mh3", "LM-Q620", 7, -33, 3),
        DeviceInfo("lge", "mdh50lm", "LM-Q730", 4, -30, 3),
        DeviceInfo("lge", "phoenix_sprout", "LM-Q910", 4, -24, 3),
        DeviceInfo("lge", "timelm", "LM-V600", -1, -33, 3),
        DeviceInfo("lge", "cv1", "LM-X210", 4, -18, 3),
        DeviceInfo("lge", "bullhead", "Nexus 5X", -6, -25, 3),
        DeviceInfo("lge", "h1", "RS988", 8, -3, 3),
        DeviceInfo("lge", "lucye", "VS988", -3, -8, 3),
        DeviceInfo("lge", "elsa", "VS995", 6, -3, 3),
        DeviceInfo("meizu", "mx4", "MX4", 5, -5, 3),
        DeviceInfo("motorola", "athene_f", "Moto G (4)", 0, -17, 3),
        DeviceInfo("motorola", "cedric", "Moto G (5)", 3, -19, 3),
        DeviceInfo("motorola", "potter_n", "Moto G (5) Plus", 6, -20, 3),
        DeviceInfo("motorola", "potter", "Moto G (5) Plus", 3, -19, 3),
        DeviceInfo("motorola", "sanders", "Moto G (5S) Plus", 4, -19, 3),
        DeviceInfo("motorola", "harpia", "Moto G Play", 5, -23, 3),
        DeviceInfo("motorola", "albus", "Moto Z2 Play", 3, -18, 3),
        DeviceInfo("motorola", "osprey_uds", "MotoG3", 5, -21, 3),
        DeviceInfo("motorola", "shamu", "Nexus 6", 10, -29, 3),
        DeviceInfo("motorola", "clark", "XT1575", 0, -30, 3),
        DeviceInfo("motorola", "harpia", "XT1609", 5, -22, 3),
        DeviceInfo("motorola", "addison", "XT1635-01", 5, -19, 3),
        DeviceInfo("motorola", "griffin", "XT1650", -3, -26, 3),
        DeviceInfo("motorola", "taido_row", "XT1706", 8, -27, 3),
        DeviceInfo("motorola", "pettyl", "moto e5 play", 1, -27, 3),
        DeviceInfo("motorola", "rjames_f", "moto e5 play", 3, -18, 3),
        DeviceInfo("motorola", "jeter", "moto g(6) play", 3, -20, 3),
        DeviceInfo("motorola", "river", "moto g(7)", 3, -18, 3),
        DeviceInfo("motorola", "payton", "moto x4", 2, -23, 3),
        DeviceInfo("motorola", "messi", "moto z3", 4, -26, 3),
        DeviceInfo("motorola", "foles", "moto z4", 10, -34, 3),
        DeviceInfo("motorola", "deen_sprout", "motorola one", 4, -21, 3),
        DeviceInfo("motorola", "chef_sprout", "motorola one power", 2, -22, 3),
        DeviceInfo("nokia", "FRT", "Nokia 1", 2, -24, 3),
        DeviceInfo("nokia", "ANT", "Nokia 1 Plus", 1, -20, 3),
        DeviceInfo("nokia", "E2M", "Nokia 2.1", 0, -28, 3),
        DeviceInfo("nokia", "IRM_sprout", "Nokia 2.3", -3, -21, 3),
        DeviceInfo("nokia", "ES2_sprout", "Nokia 3.1", 6, -23, 3),
        DeviceInfo("nokia", "ROON_sprout", "Nokia 3.1 Plus", 0, -24, 3),
        DeviceInfo("nokia", "DPL_sprout", "Nokia 3.2", 4, -32, 3),
        DeviceInfo("nokia", "PAN_sprout", "Nokia 4.2", 7, -36, 3),
        DeviceInfo("nokia", "CO2_sprout", "Nokia 5.1", 6, -25, 3),
        DeviceInfo("nokia", "PL2_sprout", "Nokia 6.1", 8, -28, 3),
        DeviceInfo("nokia", "DRG_sprout", "Nokia 6.1 Plus", 6, -27, 3),
        DeviceInfo("nokia", "B2N_sprout", "Nokia 7 plus", 2, -20, 3),
        DeviceInfo("nokia", "CTL_sprout", "Nokia 7.1", 6, -24, 3),
        DeviceInfo("nokia", "DDV_sprout", "Nokia 7.2", 3, -7, 3),
        DeviceInfo("nokia", "A1N_sprout", "Nokia 8 Sirocco", 12, -30, 3),
        DeviceInfo("nokia", "PNX_sprout", "Nokia 8.1", 5, -24, 3),
        DeviceInfo("nokia", "BGT_sprout", "Nokia 8.3 5G", 5, -23, 3),
        DeviceInfo("nokia", "AOP_sprout", "Nokia 9", 7, -25, 3),
        DeviceInfo("nokia", "RKU", "Nokia C1", -7, -3, 3),
        DeviceInfo("nokia", "NB1", "TA-1012", 5, -22, 3),
        DeviceInfo("nokia", "PLE", "TA-1025", 2, -17, 3),
        DeviceInfo("nokia", "PL2", "TA-1054", 7, -26, 3),
        DeviceInfo("nokia 5.1 plus", "Panda_00WW", "PDA_sprout", 5, -28, 3),
        DeviceInfo("oneplus", "OnePlus5", "ONEPLUS A5000", 6, -25, 3),
        DeviceInfo("oneplus", "OnePlus5T", "ONEPLUS A5010", 3, -22, 3),
        DeviceInfo("oneplus", "OnePlus6", "ONEPLUS A6000", 4, -26, 3),
        DeviceInfo("oneplus", "OnePlus6", "ONEPLUS A6003", 4, -29, 3),
        DeviceInfo("oneplus", "OnePlus6T", "ONEPLUS A6013", 1, -21, 3),
        DeviceInfo("oppo", "CPH1715", "CPH1715", 2, -25, 3),
        DeviceInfo("oppo", "CPH1717", "CPH1717", -4, -18, 3),
        DeviceInfo("oppo", "CPH1721", "CPH1721", -1, -20, 3),
        DeviceInfo("oppo", "CPH1723", "CPH1723", 3, -24, 3),
        DeviceInfo("oppo", "CPH1803", "CPH1803", -1, -17, 3),
        DeviceInfo("oppo", "CPH1823", "CPH1823", -2, -21, 3),
        DeviceInfo("oppo", "OP4845", "CPH1919", 3, -24, 3),
        DeviceInfo("oppo", "CPH1920", "CPH1920", -2, -24, 3),
        DeviceInfo("oppo", "OP4B79L1", "CPH1931", 4, -32, 3),
        DeviceInfo("oppo", "OP4B65L1", "CPH1945", -1, -26, 3),
        DeviceInfo("oppo", "OP4863", "CPH1969", 2, -27, 3),
        DeviceInfo("oppo", "OP48A1L1", "CPH1979", 0, -36, 3),
        DeviceInfo("oppo", "OP4C4BL1", "CPH1989", -4, -21, 3),
        DeviceInfo("oppo", "OP4C2DL1", "CPH2009", 4, -26, 3),
        DeviceInfo("oppo", "OP4BA1L1", "CPH2023", 9, -37, 3),
        DeviceInfo("orange", "Neva_play", "Orange Neva play", -1, -12, 3),
        DeviceInfo("orange", "Neva_zen", "Orange Neva zen", -5, -3, 3),
        DeviceInfo("orbic", "RC555L", "RC555L", 5, -26, 3),
        DeviceInfo("razer", "cheryl", "Phone", 7, -26, 3),
        DeviceInfo("razer", "aura", "Phone 2", 3, -24, 3),
        DeviceInfo("redmi", "olivelite", "Redmi 8A", 5, -30, 3),
        DeviceInfo("redmi", "lavender", "Redmi Note 7", 5, -29, 3),
        DeviceInfo("redmi", "ginkgo", "Redmi Note 8", 4, -19, 3),
        DeviceInfo("samsung", "poseidonlteatt", "SAMSUNG-SM-G891A", 4, -27, 3),
        DeviceInfo("samsung", "klteatt", "SAMSUNG-SM-G900A", 9, -23, 3),
        DeviceInfo("samsung", "heroqlteaio", "SAMSUNG-SM-G930AZ", 11, -33, 3),
        DeviceInfo("samsung", "hero2qlteatt", "SAMSUNG-SM-G935A", 10, -33, 3),
        DeviceInfo("samsung", "SC-02J", "SC-02J", 11, -31, 3),
        DeviceInfo("samsung", "SC-02K", "SC-02K", 11, -29, 3),
        DeviceInfo("samsung", "SC-02L", "SC-02L", 5, -23, 3),
        DeviceInfo("samsung", "SC-03K", "SC-03K", 15, -34, 3),
        DeviceInfo("samsung", "SCV33", "SCV33", 7, -29, 3),
        DeviceInfo("samsung", "SCV36", "SCV36", 9, -24, 3),
        DeviceInfo("samsung", "a10", "SM-A105F", 3, -23, 3),
        DeviceInfo("samsung", "a10", "SM-A105FN", 4, -25, 3),
        DeviceInfo("samsung", "a10s", "SM-A107F", 5, -30, 3),
        DeviceInfo("samsung", "a20e", "SM-A202F", 2, -26, 3),
        DeviceInfo("samsung", "a20s", "SM-A207F", 8, -32, 3),
        DeviceInfo("samsung", "a3ulte", "SM-A300FU", 6, -23, 3),
        DeviceInfo("samsung", "a30s", "SM-A307FN", 2, -26, 3),
        DeviceInfo("samsung", "a3y17lte", "SM-A320FL", -10, -23, 3),
        DeviceInfo("samsung", "a40", "SM-A405FN", 2, -23, 3),
        DeviceInfo("samsung", "a50", "SM-A505F", 0, -20, 3),
        DeviceInfo("samsung", "a50", "SM-A505FM", 3, -20, 3),
        DeviceInfo("samsung", "a5xelte", "SM-A510F", 6, -2, 3),
        DeviceInfo("samsung", "a5xelte", "SM-A510M", 3, -1, 3),
        DeviceInfo("samsung", "jackpotlte", "SM-A530F", 2, -23, 3),
        DeviceInfo("samsung", "a70q", "SM-A705FN", 5, -33, 3),
        DeviceInfo("samsung", "a7xelte", "SM-A710F", 5, -5, 3),
        DeviceInfo("samsung", "a7y18lte", "SM-A750GN", 6, -23, 3),
        DeviceInfo("samsung", "a9y18qltechn", "SM-A9200", 5, -24, 3),
        DeviceInfo("samsung", "a9y18qlte", "SM-A920F", 4, -21, 3),
        DeviceInfo("samsung", "c5proltechn", "SM-C5010", 9, -26, 3),
        DeviceInfo("samsung", "jadeltechn", "SM-C7100", 8, -26, 3),
        DeviceInfo("samsung", "elitexlte", "SM-G1650", 6, -30, 3),
        DeviceInfo("samsung", "grandpplte", "SM-G532M", 4, -24, 3),
        DeviceInfo("samsung", "on5xelte", "SM-G570F", 7, -24, 3),
        DeviceInfo("samsung", "o7ltechn", "SM-G6000", 3, -22, 3),
        DeviceInfo("samsung", "on7xelte", "SM-G610F", 7, 2, 3),
        DeviceInfo("samsung", "on7xreflte", "SM-G611F", 6, -2, 3),
        DeviceInfo("samsung", "slte", "SM-G850F", 17, -33, 3),
        DeviceInfo("samsung", "astarqltechn", "SM-G8850", 3, -24, 3),
        DeviceInfo("samsung", "astarqlteskt", "SM-G885S", 1, -24, 3),
        DeviceInfo("samsung", "cruiserlteatt", "SM-G892A", 8, -24, 3),
        DeviceInfo("samsung", "klte", "SM-G900F", 12, -24, 3),
        DeviceInfo("samsung", "zeroflte", "SM-G920F", 9, -25, 3),
        DeviceInfo("samsung", "zeroflte", "SM-G920I", 7, -24, 3),
        DeviceInfo("samsung", "zerofltetmo", "SM-G920T", 8, -24, 3),
        DeviceInfo("samsung", "zerolte", "SM-G925F", 8, -25, 3),
        DeviceInfo("samsung", "zerolte", "SM-G925I", 4, -23, 3),
        DeviceInfo("samsung", "heroqltespr", "SM-G930P", 7, -32, 3),
        DeviceInfo("samsung", "heroqlteusc", "SM-G930R4", 12, -35, 3),
        DeviceInfo("samsung", "heroqltevzw", "SM-G930V", 9, -32, 3),
        DeviceInfo("samsung", "hero2qltechn", "SM-G9350", 8, -31, 3),
        DeviceInfo("samsung", "hero2lte", "SM-G935F", 11, -33, 3),
        DeviceInfo("samsung", "hero2qltespr", "SM-G935P", 8, -31, 3),
        DeviceInfo("samsung", "hero2qlteusc", "SM-G935R4", 9, -29, 3),
        DeviceInfo("samsung", "hero2qltetmo", "SM-G935T", 4, -28, 3),
        DeviceInfo("samsung", "hero2qltevzw", "SM-G935V", 14, -34, 3),
        DeviceInfo("samsung", "dreamlte", "SM-G950F", 12, -29, 3),
        DeviceInfo("samsung", "dreamlteks", "SM-G950N", 11, -27, 3),
        DeviceInfo("samsung", "dreamqlteue", "SM-G950U1", 10, -27, 3),
        DeviceInfo("samsung", "dreamqltecan", "SM-G950W", 9, -24, 3),
        DeviceInfo("samsung", "dream2lte", "SM-G955F", 9, -26, 3),
        DeviceInfo("samsung", "dream2lteks", "SM-G955N", 10, -26, 3),
        DeviceInfo("samsung", "dream2qlteue", "SM-G955U1", 10, -26, 3),
        DeviceInfo("samsung", "dream2qltecan", "SM-G955W", 8, -23, 3),
        DeviceInfo("samsung", "starqltechn", "SM-G9600", 4, -22, 3),
        DeviceInfo("samsung", "starlte", "SM-G960F", 8, -28, 3),
        DeviceInfo("samsung", "starlteks", "SM-G960N", 7, -28, 3),
        DeviceInfo("samsung", "starqlteue", "SM-G960U1", 4, -19, 3),
        DeviceInfo("samsung", "star2qltechn", "SM-G9650", 7, -26, 3),
        DeviceInfo("samsung", "star2lte", "SM-G965F", 8, -27, 3),
        DeviceInfo("samsung", "star2lteks", "SM-G965N", 9, -25, 3),
        DeviceInfo("samsung", "star2qlteue", "SM-G965U1", 6, -24, 3),
        DeviceInfo("samsung", "beyond1", "SM-G973F", 2, -29, 3),
        DeviceInfo("samsung", "beyond2", "SM-G975F", 1, -26, 3),
        DeviceInfo("samsung", "beyondx", "SM-G977N", 0, -26, 3),
        DeviceInfo("samsung", "beyondxq", "SM-G977P", -3, -34, 3),
        DeviceInfo("samsung", "j1qltevzw", "SM-J100VPP", 6, -22, 3),
        DeviceInfo("samsung", "j2y18lte", "SM-J250F", 3, -27, 3),
        DeviceInfo("samsung", "j2corelte", "SM-J260F", 5, -25, 3),
        DeviceInfo("samsung", "j2corelte", "SM-J260G", 7, -27, 3),
        DeviceInfo("samsung", "j3ltevzw", "SM-J320V", 1, -17, 3),
        DeviceInfo("samsung", "j3popelteue", "SM-J327U", 6, -24, 3),
        DeviceInfo("samsung", "j3y17ltelgt", "SM-J330F", 3, -24, 3),
        DeviceInfo("samsung", "j3y17ltelgt", "SM-J330L", 2, -22, 3),
        DeviceInfo("samsung", "j3topeltevzw", "SM-J337V", 0, -19, 3),
        DeviceInfo("samsung", "j4lte", "SM-J400F", 6, -27, 3),
        DeviceInfo("samsung", "j5lte", "SM-J500F", 4, -19, 3),
        DeviceInfo("samsung", "j53g", "SM-J500H", -1, -18, 3),
        DeviceInfo("samsung", "j5lte", "SM-J500M", 2, -18, 3),
        DeviceInfo("samsung", "j5xnlte", "SM-J510FN", 1, -18, 3),
        DeviceInfo("samsung", "j5xnlte", "SM-J510GN", 3, -19, 3),
        DeviceInfo("samsung", "j5xnlte", "SM-J510MN", 3, -20, 3),
        DeviceInfo("samsung", "j6primelte", "SM-J610F", 3, -27, 3),
        DeviceInfo("samsung", "j7popltevzw", "SM-J727V", 4, -20, 3),
        DeviceInfo("samsung", "j7y17lte", "SM-J730FM", 7, -34, 3),
        DeviceInfo("samsung", "j7y17ltektt", "SM-J730K", 4, -30, 3),
        DeviceInfo("samsung", "j8y18lte", "SM-J810F", 5, -29, 3),
        DeviceInfo("samsung", "m20lte", "SM-M205F", 3, -22, 3),
        DeviceInfo("samsung", "trelte", "SM-N910C", 11, -31, 3),
        DeviceInfo("samsung", "tblte", "SM-N915G", 7, -24, 3),
        DeviceInfo("samsung", "noblelte", "SM-N9208", 4, -29, 3),
        DeviceInfo("samsung", "noblelte", "SM-N920C", 5, -30, 3),
        DeviceInfo("samsung", "greatlte", "SM-N950F", 8, -24, 3),
        DeviceInfo("samsung", "greatlteks", "SM-N950N", 6, -24, 3),
        DeviceInfo("samsung", "greatqlte", "SM-N950U", 9, -25, 3),
        DeviceInfo("samsung", "greatqlteue", "SM-N950U1", 9, -26, 3),
        DeviceInfo("samsung", "crownlte", "SM-N960F", 7, -28, 3),
        DeviceInfo("samsung", "crownlteks", "SM-N960N", 9, -27, 3),
        DeviceInfo("samsung", "crownqltesq", "SM-N960U", 5, -24, 3),
        DeviceInfo("samsung", "crownqlteue", "SM-N960U1", 5, -24, 3),
        DeviceInfo("samsung", "j3topeltetfnvzw", "SM-S367VL", 3, -21, 3),
        DeviceInfo("samsung", "j7popqltetfnvzw", "SM-S727VL", 4, -18, 3),
        DeviceInfo("samsung", "gt58wifi", "SM-T350", 2, -18, 3),
        DeviceInfo("samsung", "gtesveltevzw", "SM-T378V", 1, -20, 3),
        DeviceInfo("samsung", "gta2swifi", "SM-T380", 4, -19, 3),
        DeviceInfo("samsung", "gta2swifichn", "SM-T380C", 4, -20, 3),
        DeviceInfo("samsung", "gta2sltechn", "SM-T385C", 4, -20, 3),
        DeviceInfo("samsung", "gta2sltektt", "SM-T385K", 3, -18, 3),
        DeviceInfo("samsung", "gta2sltelgt", "SM-T385L", 2, -19, 3),
        DeviceInfo("samsung", "gtactive2wifi", "SM-T390", -11, -23, 3),
        DeviceInfo("samsung", "gtactive2lte", "SM-T395", -8, -26, 3),
        DeviceInfo("samsung", "gtaxlwifi", "SM-T580", -12, -22, 3),
        DeviceInfo("samsung", "gts3lwifi", "SM-T820", -10, -23, 3),
        DeviceInfo("samsung", "gts3lltevzw", "SM-T827V", 0, -22, 3),
        DeviceInfo("samsung", "gts4lltevzw", "SM-T837V", 1, -20, 3),
        DeviceInfo("sg", "OI6", "A001SH", 9, -32, 3),
        DeviceInfo("sharp", "eve_sprout", "507SH", 6, -29, 3),
        DeviceInfo("sharp", "SG509SH", "509SH", 3, -34, 3),
        DeviceInfo("sharp", "SG704SH", "704SH", 5, -32, 3),
        DeviceInfo("sharp", "kaleido_sprout", "S1", 3, -22, 3),
        DeviceInfo("sharp", "rome_sprout", "S3-SH", 5, -39, 3),
        DeviceInfo("sharp", "zeon_sprout", "S5-SH", 2, -25, 3),
        DeviceInfo("sharp", "SH-01K", "SH-01K", 4, -34, 3),
        DeviceInfo("sharp", "SH-01L", "SH-01L", 3, -25, 3),
        DeviceInfo("sharp", "SH-01M", "SH-01M", 7, -27, 3),
        DeviceInfo("sharp", "SH-02J", "SH-02J", 5, -34, 3),
        DeviceInfo("sharp", "SH-02M", "SH-02M", 5, -29, 3),
        DeviceInfo("sharp", "SH-03J", "SH-03J", 6, -24, 3),
        DeviceInfo("sharp", "SH-03K", "SH-03K", 4, -28, 3),
        DeviceInfo("sharp", "SH-04H", "SH-04H", 1, -36, 3),
        DeviceInfo("sharp", "SH-04L", "SH-04L", 5, -27, 3),
        DeviceInfo("sharp", "SH-51A", "SH-51A", 4, -41, 3),
        DeviceInfo("sharp", "SHV36", "SHV36", 6, -36, 3),
        DeviceInfo("sharp", "HUR", "SHV39", 5, -22, 3),
        DeviceInfo("sm-j415f", "j4primeltedx", "j4primelte", 4, -26, 3),
        DeviceInfo("softbank", "Z8851S", "902ZT", 6, -34, 3),
        DeviceInfo("sony", "602SO", "602SO", -3, -30, 3),
        DeviceInfo("sony", "701SO", "701SO", 6, -26, 3),
        DeviceInfo("sony", "E6603", "E6603", 10, -26, 3),
        DeviceInfo("sony", "E6633", "E6633", 13, -26, 3),
        DeviceInfo("sony", "F5121", "F5121", 8, -34, 3),
        DeviceInfo("sony", "F8131", "F8131", 1, -32, 3),
        DeviceInfo("sony", "F8331", "F8331", -2, -29, 3),
        DeviceInfo("sony", "F8332", "F8332", -2, -31, 3),
        DeviceInfo("sony", "G3123", "G3123", 4, -24, 3),
        DeviceInfo("sony", "G3223", "G3223", 8, -27, 3),
        DeviceInfo("sony", "G8142", "G8142", 10, -27, 3),
        DeviceInfo("sony", "G8232", "G8232", 0, -34, 3),
        DeviceInfo("sony", "G8341", "G8341", 14, -34, 3),
        DeviceInfo("sony", "G8342", "G8342", 9, -28, 3),
        DeviceInfo("sony", "G8441", "G8441", 5, -24, 3),
        DeviceInfo("sony", "H8216", "H8216", 5, -31, 3),
        DeviceInfo("sony", "H8266", "H8266", 6, -30, 3),
        DeviceInfo("sony", "H8296", "H8296", 8, -32, 3),
        DeviceInfo("sony", "H8314", "H8314", 9, -33, 3),
        DeviceInfo("sony", "H8324", "H8324", 9, -33, 3),
        DeviceInfo("sony", "H8416", "H8416", 6, -30, 3),
        DeviceInfo("sony", "I4213", "I4213", 8, -27, 3),
        DeviceInfo("sony", "SO-01J", "SO-01J", 0, -32, 3),
        DeviceInfo("sony", "SO-03J", "SO-03J", -1, -33, 3),
        DeviceInfo("sony", "SO-04J", "SO-04J", 10, -30, 3),
        DeviceInfo("sony", "SOV33", "SOV33", -3, -33, 3),
        DeviceInfo("sony", "SOV34", "SOV34", -2, -32, 3),
        DeviceInfo("sony", "SOV36", "SOV36", 8, -27, 3),
        DeviceInfo("sprint", "htc_acawhl", "2PYB2", 10, -27, 3),
        DeviceInfo("tcl", "A5A_INFINI", "5086D", 3, -26, 3),
        DeviceInfo("tcl", "T1_LITE", "T770B", 4, -34, 3),
        DeviceInfo("tcl", "T1", "T780H", 5, -33, 3),
        DeviceInfo("tcl", "Seattle", "T790Y", 2, -32, 3),
        DeviceInfo("tcl", "T1_PRO", "T799H", 1, -31, 3),
        DeviceInfo("tct (alcatel)", "Pixi4-4", "4034D", 13, -40, 3),
        DeviceInfo("tct (alcatel)", "Seoul", "5002D_EEA", 2, -33, 3),
        DeviceInfo("tct (alcatel)", "PIXI3_45_4G", "5017O", 5, -29, 3),
        DeviceInfo("tct (alcatel)", "Faraday", "5024A", -6, -6, 3),
        DeviceInfo("tct (alcatel)", "A3A_XL_3G", "5026A", 0, -26, 3),
        DeviceInfo("tct (alcatel)", "TokyoPro", "5029E", -2, -24, 3),
        DeviceInfo("tct (alcatel)", "Jakarta", "5030D_EEA", -8, -6, 3),
        DeviceInfo("tct (alcatel)", "Morgan_4G", "5032W", 2, -29, 3),
        DeviceInfo("tct (alcatel)", "U3A_PLUS_4G", "5033M", 3, -26, 3),
        DeviceInfo("tct (alcatel)", "U50A_ATT", "5041C", 2, -24, 3),
        DeviceInfo("tct (alcatel)", "BUZZ6T4G", "5044P", 9, -33, 3),
        DeviceInfo("tct (alcatel)", "PIXI4_5_4G", "5045F", 19, -43, 3),
        DeviceInfo("tct (alcatel)", "mickey6t", "5049G", 2, -26, 3),
        DeviceInfo("tct (alcatel)", "Mickey6TVZW", "5049S", 3, -26, 3),
        DeviceInfo("tct (alcatel)", "Mickey6TTMO", "5049Z", 5, -29, 3),
        DeviceInfo("tct (alcatel)", "A3A_PLUS", "5058A", 4, -27, 3),
        DeviceInfo("tct (alcatel)", "U5A_PLUS_4G", "5059A", 1, -20, 3),
        DeviceInfo("tct (alcatel)", "Milan", "5061K", 2, -30, 3),
        DeviceInfo("tct (alcatel)", "shine_lite", "5080X", 4, -27, 3),
        DeviceInfo("tct (alcatel)", "A3A_XL_4G", "5099U", 2, -27, 3),
        DeviceInfo("tct (alcatel)", "FERMI_TF", "A501DL", 1, -24, 3),
        DeviceInfo("tct (alcatel)", "U50A_PLUS_TF", "A502DL", 5, -27, 3),
        DeviceInfo("tct (alcatel)", "BUZZ6T4GTFUMTS", "A574BL", 3, -28, 3),
        DeviceInfo("tct (alcatel)", "FERMI_ATT", "Alcatel_5005R", 2, -25, 3),
        DeviceInfo("tct (alcatel)", "BUZZ6T4GGOPHONE", "Alcatel_5044R", 4, -28, 3),
        DeviceInfo("tct (alcatel)", "U50A_PLUS_ATT", "Alcatel_5059R", 3, -27, 3),
        DeviceInfo("tecno", "TECNO-BA2", "TECNO BA2", 3, -27, 3),
        DeviceInfo("tecno", "TECNO-BB4k", "TECNO BB4k", 6, -28, 3),
        DeviceInfo("tecno", "TECNO-CC6", "TECNO CC6", 0, -28, 3),
        DeviceInfo("tecno", "TECNO-CC7", "TECNO CC7", 1, -25, 3),
        DeviceInfo("tecno", "TECNO-CE9", "TECNO CE9", 3, -27, 3),
        DeviceInfo("tecno", "TECNO-CF7", "TECNO CF7", 3, -11, 3),
        DeviceInfo("tecno", "TECNO-KB7j", "TECNO KB7j", 5, -29, 3),
        DeviceInfo("tecno", "TECNO-KC1", "TECNO KC1", 7, -28, 3),
        DeviceInfo("tecno", "TECNO-KC2", "TECNO KC2", 9, -33, 3),
        DeviceInfo("tecno", "TECNO-KC6", "TECNO KC6", 4, -28, 3),
        DeviceInfo("tecno", "TECNO-KC8", "TECNO KC8", 4, -28, 3),
        DeviceInfo("tecno", "TECNO-LC6", "TECNO LC6", 2, -31, 3),
        DeviceInfo("vivo", "1723", "vivo 1723", 4, -20, 3),
        DeviceInfo("vivo", "1804", "vivo 1804", 5, -26, 3),
        DeviceInfo("vodafone", "VFD610", "VFD 610", 7, -29, 3),
        DeviceInfo("xiaomi", "cancro", "MI 4W", 4, -21, 3),
        DeviceInfo("xiaomi", "gemini", "MI 5", -2, -27, 3),
        DeviceInfo("xiaomi", "capricorn", "MI 5s", -4, -26, 3),
        DeviceInfo("xiaomi", "natrium", "MI 5s Plus", -3, -23, 3),
        DeviceInfo("xiaomi", "sagit", "MI 6", 2, -22, 3),
        DeviceInfo("xiaomi", "dipper", "MI 8", 1, -22, 3),
        DeviceInfo("xiaomi", "platina", "MI 8 Lite", 1, -21, 3),
        DeviceInfo("xiaomi", "equuleus", "MI 8 Pro", 1, -30, 3),
        DeviceInfo("xiaomi", "nitrogen", "MI MAX 3", 3, -24, 3),
        DeviceInfo("xiaomi", "virgo", "MI NOTE LTE", 9, -23, 3),
        DeviceInfo("xiaomi", "lithium", "MIX", -2, -27, 3),
        DeviceInfo("xiaomi", "davinci", "Mi 9T", 3, -31, 3),
        DeviceInfo("xiaomi", "tissot_sprout", "Mi A1", 1, -18, 3),
        DeviceInfo("xiaomi", "jasmine_sprout", "Mi A2", 2, -23, 3),
        DeviceInfo("xiaomi", "daisy_sprout", "Mi A2 Lite", 4, -21, 3),
        DeviceInfo("xiaomi", "laurel_sprout", "Mi A3", 1, -19, 3),
        DeviceInfo("xiaomi", "chiron", "Mi MIX 2", 6, -24, 3),
        DeviceInfo("xiaomi", "polaris", "Mi MIX 2S", 1, -19, 3),
        DeviceInfo("xiaomi", "scorpio", "Mi Note 2", -2, -25, 3),
        DeviceInfo("xiaomi", "ido", "Redmi 3", 3, -21, 3),
        DeviceInfo("xiaomi", "land", "Redmi 3S", -1, -25, 3),
        DeviceInfo("xiaomi", "santoni", "Redmi 4X", -1, -23, 3),
        DeviceInfo("xiaomi", "rosy", "Redmi 5", 4, -31, 3),
        DeviceInfo("xiaomi", "riva", "Redmi 5A", 3, -29, 3),
        DeviceInfo("xiaomi", "cereus", "Redmi 6", 1, -25, 3),
        DeviceInfo("xiaomi", "sakura_india", "Redmi 6 Pro", 5, -21, 3),
        DeviceInfo("xiaomi", "cactus", "Redmi 6A", 2, -27, 3),
        DeviceInfo("xiaomi", "onc", "Redmi 7", 4, -30, 3),
        DeviceInfo("xiaomi", "pine", "Redmi 7A", 4, -34, 3),
        DeviceInfo("xiaomi", "olive", "Redmi 8", 10, -31, 3),
        DeviceInfo("xiaomi", "tiare", "Redmi Go", 5, -31, 3),
        DeviceInfo("xiaomi", "kenzo", "Redmi Note 3", 5, -22, 3),
        DeviceInfo("xiaomi", "whyred", "Redmi Note 5", 1, -20, 3),
        DeviceInfo("xiaomi", "tulip", "Redmi Note 6 Pro", 4, -29, 3),
        DeviceInfo("xiaomi", "violet", "Redmi Note 7 Pro", 4, -30, 3),
        DeviceInfo("xiaomi", "ysl", "Redmi S2", 4, -29, 3),
        DeviceInfo("zte", "P809F15", "BLADE A6 MAX", 4, -28, 3),
        DeviceInfo("zte", "P450L10", "BLADE V9", 7, -31, 3),
        DeviceInfo("zte", "P963F50", "Blade A5 2020-T", -3, -11, 3),
        DeviceInfo("zte", "Z6350T", "Blade A7S 2020-T", -4, 1, 3),
        DeviceInfo("zte", "Z6351O", "P650 Pro", -4, 0, 3),
        DeviceInfo("zte", "T86", "T86", 3, -29, 3),
        DeviceInfo("zte", "Z3351", "Z3351S", 3, -25, 3),
        DeviceInfo("zte", "Z5151", "Z5151V", 6, -33, 3),
        DeviceInfo("zte", "Z5156", "Z5156CC", 0, -25, 3),
        DeviceInfo("zte", "Z5157", "Z5157V", -1, -24, 3),
        DeviceInfo("zte", "Z6201", "Z6201V", 12, -36, 3),
        DeviceInfo("zte", "Z6250", "Z6250CC", 0, -25, 3),
        DeviceInfo("zte", "Z6530", "Z6530V", 15, -39, 3),
        DeviceInfo("zte", "camellia", "Z833", 3, -25, 3),
        DeviceInfo("zte", "Z7750R", "ZR01", 16, -36, 3),
        DeviceInfo("zte", "P963F05", "ZTE 8010", -3, -2, 3),
        DeviceInfo("zte", "P963F05", "ZTE 8010RU", -3, -2, 3),
        DeviceInfo("zte", "P671F60", "ZTE 9000", 4, -28, 3),
        DeviceInfo("zte", "P683S10", "ZTE 9000N", 9, -31, 3),
        DeviceInfo("zte", "P845A01", "ZTE A2019G Pro", 8, -26, 3),
        DeviceInfo("zte", "P855A02", "ZTE A2020 Pro", 6, -25, 3),
        DeviceInfo("zte", "P855A01", "ZTE A2020G Pro", 8, -28, 3),
        DeviceInfo("zte", "P855A21", "ZTE A2020N3 Pro", 6, -26, 3),
        DeviceInfo("zte", "P855A03_NA", "ZTE A2020U Pro", 6, -26, 3),
        DeviceInfo("zte", "P725A12", "ZTE A2021", 9, -29, 3),
        DeviceInfo("zte", "P725A11", "ZTE A2021G", -1, -29, 3),
        DeviceInfo("zte", "P725A12", "ZTE A2021H", 0, -28, 3),
        DeviceInfo("zte", "P725A02", "ZTE A2121", 9, -31, 3),
        DeviceInfo("zte", "P618A01", "ZTE A2121E", 1, -26, 3),
        DeviceInfo("zte", "P963F03", "ZTE A7020", -3, -4, 3),
        DeviceInfo("zte", "P963F03", "ZTE A7020RU", -3, -4, 3),
        DeviceInfo("zte", "P932F20", "ZTE Blade A3 2019", -4, -4, 3),
        DeviceInfo("zte", "P932F50", "ZTE Blade A3 2020", -4, -7, 3),
        DeviceInfo("zte", "P932K30", "ZTE Blade A3 2020", -5, -4, 3),
        DeviceInfo("zte", "P963F50", "ZTE Blade A5 2020", -3, -10, 3),
        DeviceInfo("zte", "P662F02", "ZTE Blade A7s", 2, -27, 3),
        DeviceInfo("zte", "P731K30", "ZTE Blade L130", -5, -5, 3),
        DeviceInfo("zte", "P731F50", "ZTE Blade L210", -4, -2, 3),
        DeviceInfo("zte", "P731F50", "ZTE Blade L210RU", -4, -5, 3),
        DeviceInfo("zte", "P671F20", "ZTE Blade V10", 7, -31, 3),
        DeviceInfo("zte", "P963F01", "ZTE Blade V10 Vita", -3, -10, 3)
)
