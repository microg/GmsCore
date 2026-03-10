/*
 * Copyright (C) 2013-2017 microG Project Team
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

package com.google.android.gms.location.places;

import android.net.Uri;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.Locale;

public interface Place extends Freezable<Place> {
    int TYPE_ACCOUNTING = 1;

    int TYPE_ADMINISTRATIVE_AREA_LEVEL_1 = 1001;

    int TYPE_ADMINISTRATIVE_AREA_LEVEL_2 = 1002;

    int TYPE_ADMINISTRATIVE_AREA_LEVEL_3 = 1003;

    int TYPE_AIRPORT = 2;

    int TYPE_AMUSEMENT_PARK = 3;

    int TYPE_AQUARIUM = 4;

    int TYPE_ART_GALLERY = 5;

    int TYPE_ATM = 6;

    int TYPE_BAKERY = 7;

    int TYPE_BANK = 8;

    int TYPE_BAR = 9;

    int TYPE_BEAUTY_SALON = 10;

    int TYPE_BICYCLE_STORE = 11;

    int TYPE_BOOK_STORE = 12;

    int TYPE_BOWLING_ALLEY = 13;

    int TYPE_BUS_STATION = 14;

    int TYPE_CAFE = 15;

    int TYPE_CAMPGROUND = 16;

    int TYPE_CAR_DEALER = 17;

    int TYPE_CAR_RENTAL = 18;

    int TYPE_CAR_REPAIR = 19;

    int TYPE_CAR_WASH = 20;

    int TYPE_CASINO = 21;

    int TYPE_CEMETERY = 22;

    int TYPE_CHURCH = 23;

    int TYPE_CITY_HALL = 24;

    int TYPE_CLOTHING_STORE = 25;

    int TYPE_COLLOQUIAL_AREA = 1004;

    int TYPE_CONVENIENCE_STORE = 26;

    int TYPE_COUNTRY = 1005;

    int TYPE_COURTHOUSE = 27;

    int TYPE_DENTIST = 28;

    int TYPE_DEPARTMENT_STORE = 29;

    int TYPE_DOCTOR = 30;

    int TYPE_ELECTRICIAN = 31;

    int TYPE_ELECTRONICS_STORE = 32;

    int TYPE_EMBASSY = 33;

    int TYPE_ESTABLISHMENT = 34;

    int TYPE_FINANCE = 35;

    int TYPE_FIRE_STATION = 36;

    int TYPE_FLOOR = 1006;

    int TYPE_FLORIST = 37;

    int TYPE_FOOD = 38;

    int TYPE_FUNERAL_HOME = 39;

    int TYPE_FURNITURE_STORE = 40;

    int TYPE_GAS_STATION = 41;

    int TYPE_GENERAL_CONTRACTOR = 42;

    int TYPE_GEOCODE = 1007;

    int TYPE_GROCERY_OR_SUPERMARKET = 43;

    int TYPE_GYM = 44;

    int TYPE_HAIR_CARE = 45;

    int TYPE_HARDWARE_STORE = 46;

    int TYPE_HEALTH = 47;

    int TYPE_HINDU_TEMPLE = 48;

    int TYPE_HOME_GOODS_STORE = 49;

    int TYPE_HOSPITAL = 50;

    int TYPE_INSURANCE_AGENCY = 51;

    int TYPE_INTERSECTION = 1008;

    int TYPE_JEWELRY_STORE = 52;

    int TYPE_LAUNDRY = 53;

    int TYPE_LAWYER = 54;

    int TYPE_LIBRARY = 55;

    int TYPE_LIQUOR_STORE = 56;

    int TYPE_LOCALITY = 1009;

    int TYPE_LOCAL_GOVERNMENT_OFFICE = 57;

    int TYPE_LOCKSMITH = 58;

    int TYPE_LODGING = 59;

    int TYPE_MEAL_DELIVERY = 60;

    int TYPE_MEAL_TAKEAWAY = 61;

    int TYPE_MOSQUE = 62;

    int TYPE_MOVIE_RENTAL = 63;

    int TYPE_MOVIE_THEATER = 64;

    int TYPE_MOVING_COMPANY = 65;

    int TYPE_MUSEUM = 66;

    int TYPE_NATURAL_FEATURE = 1010;

    int TYPE_NEIGHBORHOOD = 1011;

    int TYPE_NIGHT_CLUB = 67;

    int TYPE_OTHER = 0;

    int TYPE_PAINTER = 68;

    int TYPE_PARK = 69;

    int TYPE_PARKING = 70;

    int TYPE_PET_STORE = 71;

    int TYPE_PHARMACY = 72;

    int TYPE_PHYSIOTHERAPIST = 73;

    int TYPE_PLACE_OF_WORSHIP = 74;

    int TYPE_PLUMBER = 75;

    int TYPE_POINT_OF_INTEREST = 1013;

    int TYPE_POLICE = 76;

    int TYPE_POLITICAL = 1012;

    int TYPE_POSTAL_CODE = 1015;

    int TYPE_POSTAL_CODE_PREFIX = 1016;

    int TYPE_POSTAL_TOWN = 1017;

    int TYPE_POST_BOX = 1014;

    int TYPE_POST_OFFICE = 77;

    int TYPE_PREMISE = 1018;

    int TYPE_REAL_ESTATE_AGENCY = 78;

    int TYPE_RESTAURANT = 79;

    int TYPE_ROOFING_CONTRACTOR = 80;

    int TYPE_ROOM = 1019;

    int TYPE_ROUTE = 1020;

    int TYPE_RV_PARK = 81;

    int TYPE_SCHOOL = 82;

    int TYPE_SHOE_STORE = 83;

    int TYPE_SHOPPING_MALL = 84;

    int TYPE_SPA = 85;

    int TYPE_STADIUM = 86;

    int TYPE_STORAGE = 87;

    int TYPE_STORE = 88;

    int TYPE_STREET_ADDRESS = 1021;

    int TYPE_SUBLOCALITY = 1022;

    int TYPE_SUBLOCALITY_LEVEL_1 = 1023;

    int TYPE_SUBLOCALITY_LEVEL_2 = 1024;

    int TYPE_SUBLOCALITY_LEVEL_3 = 1025;

    int TYPE_SUBLOCALITY_LEVEL_4 = 1026;

    int TYPE_SUBLOCALITY_LEVEL_5 = 1027;

    int TYPE_SUBPREMISE = 1028;

    int TYPE_SUBWAY_STATION = 89;

    int TYPE_SYNAGOGUE = 90;

    int TYPE_SYNTHETIC_GEOCODE = 1029;

    int TYPE_TAXI_STAND = 91;

    int TYPE_TRAIN_STATION = 92;

    int TYPE_TRANSIT_STATION = 1030;

    int TYPE_TRAVEL_AGENCY = 93;

    int TYPE_UNIVERSITY = 94;

    int TYPE_VETERINARY_CARE = 95;

    int TYPE_ZOO = 96;

    /**
     * Returns a human readable address for this Place. May return null if the address is unknown.
     * <p/>
     * The address is localized according to the locale returned by {@link com.google.android.gms.location.places.Place#getLocale()}.
     */
    CharSequence getAddress();

    /**
     * Returns the attributions to be shown to the user if data from the {@link com.google.android.gms.location.places.Place} is used.
     * <p/>
     * We recommend placing this information below any place information. See
     * <a href="https://developers.google.com/places/android/attributions#third-party">Displaying Attributions</a> for more details.
     *
     * @return The attributions in HTML format, or null if there are no attributions to display.
     */
    CharSequence getAttributions();

    /**
     * Returns the unique id of this Place.
     * <p/>
     * This ID can be passed to {@link com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient, java.lang.String...)} to lookup the same place at a later
     * time, but it is not guaranteed that such a lookup will succeed (the place may no longer exist
     * in our database). It is possible that the returned Place in such a lookup will have a
     * different ID (so there may be multiple ID's for one given place).
     */
    String getId();

    /**
     * Returns the location of this Place.
     * <p/>
     * The location is not necessarily the center of the Place, or any particular entry or exit
     * point, but some arbitrarily chosen point within the geographic extent of the Place.
     */
    LatLng getLatLng();

    /**
     * Returns the locale in which the names and addresses were localized.
     */
    Locale getLocale();

    /**
     * Returns the name of this Place.
     * <p/>
     * The name is localized according to the locale returned by {@link com.google.android.gms.location.places.Place#getLocale()}.
     */
    CharSequence getName();

    /**
     * Returns the place's phone number in international format. Returns null if no phone number is
     * known, or the place has no phone number.
     * <p/>
     * International format includes the country code, and is prefixed with the plus (+) sign. For
     * example, the international phone number for Google's Mountain View, USA office is +1
     * 650-253-0000.
     */
    CharSequence getPhoneNumber();

    /**
     * Returns a list of place types for this Place.
     * <p/>
     * The elements of this list are drawn from <code>Place.TYPE_*</code> constants, though one should
     * expect there could be new place types returned that were introduced after an app was
     * published.
     */
    List<Integer> getPlaceTypes();

    /**
     * Returns the price level for this place on a scale from 0 (cheapest) to 4.
     * <p/>
     * If no price level is known, a negative value is returned.
     * <p/>
     * The price level of the place, on a scale of 0 to 4. The exact amount indicated by a specific
     * value will vary from region to region. Price levels are interpreted as follows:
     */
    int getPriceLevel();

    /**
     * Returns the place's rating, from 1.0 to 5.0, based on aggregated user reviews.
     * <p/>
     * If no rating is known, a negative value is returned.
     */
    float getRating();

    /**
     * Returns a viewport for displaying this Place. May return null if the size of the place is not
     * known.
     * <p/>
     * This returns a viewport of a size that is suitable for displaying this Place. For example, a
     * Place representing a store may have a relatively small viewport, while a Place representing a
     * country may have a very large viewport.
     */
    LatLngBounds getViewport();

    /**
     * Returns the URI of the website of this Place. Returns null if no website is known.
     * <p/>
     * This is the URI of the website maintained by the Place, if available. Note this is a
     * third-party website not affiliated with the Places API.
     */
    Uri getWebsiteUri();

}
