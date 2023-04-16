/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.vision.barcode;

import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Barcode represents a single recognized barcode and its value.
 * <p>
 * The barcode's raw, unmodified, and uninterpreted content is returned in the {@link #rawValue} field, while the barcode type (i.e. its encoding) can be found in the {@link #format} field.
 * <p>
 * Barcodes that contain structured data (commonly done with QR codes) are parsed and iff valid, the {@link #valueFormat} field is set to one of the value format constants (e.g. {@link #GEO}) and the corresponding field is set (e.g. {@link #geoPoint}).
 */
@PublicApi
public class Barcode extends AutoSafeParcelable {
    /**
     * Barcode value format constant for contact information.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int CONTACT_INFO = 1;
    /**
     * Barcode value format constant for email message details.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int EMAIL = 2;
    /**
     * Barcode value format constant for ISBNs.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int ISBN = 3;
    /**
     * Barcode value format constant for phone numbers.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int PHONE = 4;
    /**
     * Barcode value format constant for product codes.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int PRODUCT = 5;
    /**
     * Barcode value format constant for SMS details.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int SMS = 6;
    /**
     * Barcode value format constant for plain text.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int TEXT = 7;
    /**
     * Barcode value format constant for URLs/bookmarks.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int URL = 8;
    /**
     * Barcode value format constant for WiFi access point details.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int WIFI = 9;
    /**
     * Barcode value format constant for geographic coordinates.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int GEO = 10;
    /**
     * Barcode value format constant for calendar events.
     * Specifies the format of a Barcode value via the {@link #valueFormat} field.
     */
    public static final int CALENDAR_EVENT = 11;
    public static final int DRIVER_LICENSE = 12;

    /**
     * Barcode format constant representing the union of all supported formats.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize.
     * This is also the default setting.
     */
    public static final int ALL_FORMATS = 0;
    /**
     * Barcode format constant for Code 128.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int CODE_128 = 1;
    /**
     * Barcode format constant for Code 39.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int CODE_39 = 2;
    /**
     * Barcode format constant for Code 93.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int CODE_93 = 4;
    /**
     * Barcode format constant for Codebar.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int CODABAR = 8;
    /**
     * Barcode format constant for Data Matrix.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int DATA_MATRIX = 16;
    /**
     * Barcode format constant for EAN-13.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int EAN_13 = 32;
    /**
     * Barcode format constant for EAN-8.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int EAN_8 = 64;
    /**
     * Barcode format constant for ITF (Interleaved Two-of-Five).
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int ITF = 128;
    /**
     * Barcode format constant for QR Code.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int QR_CODE = 256;
    /**
     * Barcode format constant for UPC-A.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int UPC_A = 512;
    /**
     * Barcode format constant for UPC-E.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int UPC_E = 1024;
    /**
     * Barcode format constant for PDF-417.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int PDF417 = 2048;
    /**
     * Barcode format constant for AZTEC.
     * Pass into {@link BarcodeDetector.Builder#setBarcodeFormats(int)} to select formats to recognize,
     * and also specifies a detected Barcode's {@link #format} via the format field.
     */
    public static final int AZTEC = 4096;

    @Field(1)
    private final int versionCode = 1;
    /**
     * Barcode format, for example {@link #EAN_13}.
     * <p>
     * Note that this field may contain values not present in the current set of format constants.
     * When mapping this value to something else, it is advisable to have a default/fallback case.
     */
    @Field(2)
    public int format;
    /**
     * Barcode value as it was encoded in the barcode. Structured values are not parsed, for example: 'MEBKM:TITLE:Google;URL://www.google.com;;' Does not include the supplement value.
     */
    @Field(3)
    public String rawValue;
    /**
     * Barcode value in a user-friendly format.
     * May omit some of the information encoded in the barcode.
     * For example, in the case above the display_value might be '//www.google.com'.
     * If {@link #valueFormat}=={@link #TEXT}, this field will be equal to {@link #rawValue}.
     * This value may be multiline, for example, when line breaks are encoded into the original {@link #TEXT} barcode value.
     * May include the supplement value.
     */
    @Field(4)
    public String displayValue;
    /**
     * Format of the barcode value. For example, {@link #TEXT}, {@link #PRODUCT}, {@link #URL}, etc.
     * <p>
     * Note that this field may contain values not present in the current set of value format constants.
     * When mapping this value to something else, it is advisable to have a default/fallback case.
     */
    @Field(5)
    public int valueFormat;
    /**
     * 4 corner points in clockwise direction starting with top-left.
     * Due to the possible perspective distortions, this is not necessarily a rectangle.
     */
    @Field(6)
    public Point[] cornerPoints;
    /**
     * Parsed email details (set iff {@link #valueFormat} is {@link #EMAIL}).
     */
    @Nullable
    @Field(7)
    public Barcode.Email email;
    /**
     * Parsed phone details (set iff {@link #valueFormat} is {@link #PHONE}).
     */
    @Nullable
    @Field(8)
    public Barcode.Phone phone;
    /**
     * Parsed SMS details (set iff {@link #valueFormat} is {@link #SMS}).
     */
    @Nullable
    @Field(9)
    public Barcode.Sms sms;
    /**
     * Parsed WiFi AP details (set iff {@link #valueFormat} is {@link #WIFI}).
     */
    @Nullable
    @Field(10)
    public Barcode.WiFi wifi;
    /**
     * Parsed URL bookmark details (set iff {@link #valueFormat} is {@link #URL}).
     */
    @Nullable
    @Field(11)
    public Barcode.UrlBookmark url;
    /**
     * Parsed geo coordinates (set iff {@link #valueFormat} is {@link #GEO}).
     */
    @Nullable
    @Field(12)
    public Barcode.GeoPoint geoPoint;
    /**
     * Parsed calendar event details (set iff {@link #valueFormat} is {@link #CALENDAR_EVENT}).
     */
    @Nullable
    @Field(13)
    public Barcode.CalendarEvent calendarEvent;
    /**
     * Parsed contact details (set iff {@link #valueFormat} is {@link #CONTACT_INFO}).
     */
    @Nullable
    @Field(14)
    public Barcode.ContactInfo contactInfo;
    /**
     * Parsed driver's license details (set iff {@link #valueFormat} is {@link #DRIVER_LICENSE}).
     */
    @Nullable
    @Field(15)
    public Barcode.DriverLicense driverLicense;
    /**
     * Barcode value as it was encoded in the barcode as byte array.
     */
    @Field(16)
    public byte[] rawBytes;
    /**
     * If outputUnrecognizedBarcodes is set, isRecognized can be set to false to indicate failure in decoding the detected barcode.
     */
    @Field(17)
    public boolean isRecognized;

    /**
     * Returns the barcode's axis-aligned bounding box.
     */
    public Rect getBoundingBox() {
        int left = Integer.MAX_VALUE, top = Integer.MIN_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
        for (Point point : cornerPoints) {
            left = Math.min(left, point.x);
            top = Math.max(top, point.y);
            right = Math.max(right, point.x);
            bottom = Math.min(bottom, point.y);
        }
        return new Rect(left, top, right, bottom);
    }

    /**
     * An address.
     */
    public static class Address extends AutoSafeParcelable {
        /**
         * Address type.
         */
        public static final int UNKNOWN = 0;
        public static final int WORK = 1;
        public static final int HOME = 2;

        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public int type;
        /**
         * Formatted address, multiple lines when appropriate. This field always contains at least one line.
         */
        @Field(3)
        public String[] addressLines;

        public static Creator<Address> CREATOR = new AutoCreator<>(Address.class);
    }

    /**
     * DateTime data type used in calendar events. If hours/minutes/seconds are not specified in the barcode value, they will be set to -1.
     */
    public static class CalendarDateTime extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public int year;
        @Field(3)
        public int month;
        @Field(4)
        public int day;
        @Field(5)
        public int hours;
        @Field(6)
        public int minutes;
        @Field(7)
        public int seconds;
        @Field(8)
        public boolean isUtc;
        @Field(9)
        public String rawValue;

        public static Creator<CalendarDateTime> CREATOR = new AutoCreator<>(CalendarDateTime.class);
    }

    /**
     * A calendar event extracted from QRCode.
     */
    public static class CalendarEvent extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public String summary;
        @Field(3)
        public String description;
        @Field(4)
        public String location;
        @Field(5)
        public String organizer;
        @Field(6)
        public String status;
        @Field(7)
        public Barcode.CalendarDateTime start;
        @Field(8)
        public Barcode.CalendarDateTime end;

        public static Creator<CalendarEvent> CREATOR = new AutoCreator<>(CalendarEvent.class);
    }

    /**
     * A person's or organization's business card. For example a VCARD.
     */
    public static class ContactInfo extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public Barcode.PersonName name;
        @Field(3)
        public String organization;
        @Field(4)
        public String title;
        @Field(5)
        public Barcode.Phone[] phones;
        @Field(6)
        public Barcode.Email[] emails;
        @Field(7)
        public String[] urls;
        @Field(8)
        public Barcode.Address[] addresses;

        public static Creator<ContactInfo> CREATOR = new AutoCreator<>(ContactInfo.class);
    }

    /**
     * A driver license or ID card.
     */
    public static class DriverLicense extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        /**
         * "DL" for driver licenses, "ID" for ID cards.
         */
        @Field(2)
        public String documentType;
        /**
         * Holder's first name.
         */
        @Field(3)
        public String firstName;
        @Field(4)
        public String middleName;
        @Field(5)
        public String lastName;
        /**
         * Gender. 1 - male, 2 - female.
         */
        @Field(6)
        public String gender;
        /**
         * Holder's street address.
         */
        @Field(7)
        public String addressStreet;
        @Field(8)
        public String addressCity;
        @Field(9)
        public String addressState;
        @Field(10)
        public String addressZip;
        /**
         * Driver license ID number.
         */
        @Field(11)
        public String licenseNumber;
        /**
         * The date format depends on the issuing country. MMDDYYYY for the US, YYYYMMDD for Canada.
         */
        @Field(12)
        public String issueDate;
        @Field(13)
        public String expiryDate;
        @Field(14)
        public String birthDate;
        /**
         * Country in which DL/ID was issued. US = "USA", Canada = "CAN".
         */
        @Field(15)
        public String issuingCountry;

        public static Creator<DriverLicense> CREATOR = new AutoCreator<>(DriverLicense.class);
    }

    /**
     * An email message from a 'MAILTO:' or similar QRCode type.
     */
    public static class Email extends AutoSafeParcelable {
        /**
         * Email type.
         */
        public static final int UNKNOWN = 0;
        public static final int WORK = 1;
        public static final int HOME = 2;

        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public int type;
        @Field(3)
        public String address;
        @Field(4)
        public String subject;
        @Field(5)
        public String body;

        public static Creator<Email> CREATOR = new AutoCreator<>(Email.class);
    }

    /**
     * GPS coordinates from a 'GEO:' or similar QRCode type.
     */
    public static class GeoPoint extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public double lat;
        @Field(3)
        public double lng;

        public static Creator<GeoPoint> CREATOR = new AutoCreator<>(GeoPoint.class);
    }

    /**
     * A person's name, both formatted version and individual name components.
     */
    public static class PersonName extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        /**
         * Properly formatted name.
         */
        @Field(2)
        public String formattedName;
        /**
         * Designates a text string to be set as the kana name in the phonebook. Used for Japanese contacts.
         */
        @Field(3)
        public String pronunciation;
        @Field(4)
        public String prefix;
        @Field(5)
        public String first;
        @Field(6)
        public String middle;
        @Field(7)
        public String last;
        @Field(8)
        public String suffix;

        public static Creator<PersonName> CREATOR = new AutoCreator<>(PersonName.class);
    }

    /**
     * A phone number from a 'TEL:' or similar QRCode type.
     */
    public static class Phone extends AutoSafeParcelable {
        /**
         * Phone type.
         */
        public static final int UNKNOWN = 0;
        public static final int WORK = 1;
        public static final int HOME = 2;
        public static final int FAX = 3;
        public static final int MOBILE = 4;

        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public int type;
        @Field(3)
        public String number;

        public static Creator<Phone> CREATOR = new AutoCreator<>(Phone.class);
    }

    /**
     * An sms message from an 'SMS:' or similar QRCode type.
     */
    public static class Sms extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public String message;
        @Field(3)
        public String phoneNumber;

        public static Creator<Sms> CREATOR = new AutoCreator<>(Sms.class);
    }

    /**
     * A URL and title from a 'MEBKM:' or similar QRCode type.
     */
    public static class UrlBookmark extends AutoSafeParcelable {
        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public String title;
        /**
         * Bookmark URL. Note that some common errors may be corrected here. For example, "http//...", "http:...", etc. will be replaced with "//".
         */
        @Field(3)
        public String url;

        public static Creator<UrlBookmark> CREATOR = new AutoCreator<>(UrlBookmark.class);
    }

    /**
     * A wifi network parameters from a 'WIFI:' or similar QRCode type.
     */
    public static class WiFi extends AutoSafeParcelable {
        /**
         * WiFi encryption type.
         */
        public static final int OPEN = 1;
        public static final int WPA = 2;
        public static final int WEP = 3;

        @Field(1)
        private int versionCode = 1;
        @Field(2)
        public String ssid;
        @Nullable
        @Field(3)
        public String password;
        @Field(4)
        public int encryptionType;

        public static Creator<WiFi> CREATOR = new AutoCreator<>(WiFi.class);
    }

    public static final Creator<Barcode> CREATOR = new AutoCreator<>(Barcode.class);
}
