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
package com.google.android.gms.location.places

import android.net.Uri
import com.google.android.gms.common.data.Freezable
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.*

interface Place : Freezable<Place?> {
    /**
     * Returns a human readable address for this Place. May return null if the address is unknown.
     *
     *
     * The address is localized according to the locale returned by [com.google.android.gms.location.places.Place.getLocale].
     */
    fun getAddress(): CharSequence?

    /**
     * Returns the attributions to be shown to the user if data from the [com.google.android.gms.location.places.Place] is used.
     *
     *
     * We recommend placing this information below any place information. See
     * [Displaying Attributions](https://developers.google.com/places/android/attributions#third-party) for more details.
     *
     * @return The attributions in HTML format, or null if there are no attributions to display.
     */
    fun getAttributions(): CharSequence?

    /**
     * Returns the unique id of this Place.
     *
     *
     * This ID can be passed to [com.google.android.gms.location.places.GeoDataApi.getPlaceById] to lookup the same place at a later
     * time, but it is not guaranteed that such a lookup will succeed (the place may no longer exist
     * in our database). It is possible that the returned Place in such a lookup will have a
     * different ID (so there may be multiple ID's for one given place).
     */
    fun getId(): String?

    /**
     * Returns the location of this Place.
     *
     *
     * The location is not necessarily the center of the Place, or any particular entry or exit
     * point, but some arbitrarily chosen point within the geographic extent of the Place.
     */
    fun getLatLng(): LatLng?

    /**
     * Returns the locale in which the names and addresses were localized.
     */
    fun getLocale(): Locale?

    /**
     * Returns the name of this Place.
     *
     *
     * The name is localized according to the locale returned by [com.google.android.gms.location.places.Place.getLocale].
     */
    fun getName(): CharSequence?

    /**
     * Returns the place's phone number in international format. Returns null if no phone number is
     * known, or the place has no phone number.
     *
     *
     * International format includes the country code, and is prefixed with the plus (+) sign. For
     * example, the international phone number for Google's Mountain View, USA office is +1
     * 650-253-0000.
     */
    fun getPhoneNumber(): CharSequence?

    /**
     * Returns a list of place types for this Place.
     *
     *
     * The elements of this list are drawn from `Place.TYPE_*` constants, though one should
     * expect there could be new place types returned that were introduced after an app was
     * published.
     */
    fun getPlaceTypes(): List<Int?>?

    /**
     * Returns the price level for this place on a scale from 0 (cheapest) to 4.
     *
     *
     * If no price level is known, a negative value is returned.
     *
     *
     * The price level of the place, on a scale of 0 to 4. The exact amount indicated by a specific
     * value will vary from region to region. Price levels are interpreted as follows:
     */
    fun getPriceLevel(): Int

    /**
     * Returns the place's rating, from 1.0 to 5.0, based on aggregated user reviews.
     *
     *
     * If no rating is known, a negative value is returned.
     */
    fun getRating(): Float

    /**
     * Returns a viewport for displaying this Place. May return null if the size of the place is not
     * known.
     *
     *
     * This returns a viewport of a size that is suitable for displaying this Place. For example, a
     * Place representing a store may have a relatively small viewport, while a Place representing a
     * country may have a very large viewport.
     */
    fun getViewport(): LatLngBounds?

    /**
     * Returns the URI of the website of this Place. Returns null if no website is known.
     *
     *
     * This is the URI of the website maintained by the Place, if available. Note this is a
     * third-party website not affiliated with the Places API.
     */
    fun getWebsiteUri(): Uri?

    companion object {
        const val TYPE_ACCOUNTING = 1
        const val TYPE_ADMINISTRATIVE_AREA_LEVEL_1 = 1001
        const val TYPE_ADMINISTRATIVE_AREA_LEVEL_2 = 1002
        const val TYPE_ADMINISTRATIVE_AREA_LEVEL_3 = 1003
        const val TYPE_AIRPORT = 2
        const val TYPE_AMUSEMENT_PARK = 3
        const val TYPE_AQUARIUM = 4
        const val TYPE_ART_GALLERY = 5
        const val TYPE_ATM = 6
        const val TYPE_BAKERY = 7
        const val TYPE_BANK = 8
        const val TYPE_BAR = 9
        const val TYPE_BEAUTY_SALON = 10
        const val TYPE_BICYCLE_STORE = 11
        const val TYPE_BOOK_STORE = 12
        const val TYPE_BOWLING_ALLEY = 13
        const val TYPE_BUS_STATION = 14
        const val TYPE_CAFE = 15
        const val TYPE_CAMPGROUND = 16
        const val TYPE_CAR_DEALER = 17
        const val TYPE_CAR_RENTAL = 18
        const val TYPE_CAR_REPAIR = 19
        const val TYPE_CAR_WASH = 20
        const val TYPE_CASINO = 21
        const val TYPE_CEMETERY = 22
        const val TYPE_CHURCH = 23
        const val TYPE_CITY_HALL = 24
        const val TYPE_CLOTHING_STORE = 25
        const val TYPE_COLLOQUIAL_AREA = 1004
        const val TYPE_CONVENIENCE_STORE = 26
        const val TYPE_COUNTRY = 1005
        const val TYPE_COURTHOUSE = 27
        const val TYPE_DENTIST = 28
        const val TYPE_DEPARTMENT_STORE = 29
        const val TYPE_DOCTOR = 30
        const val TYPE_ELECTRICIAN = 31
        const val TYPE_ELECTRONICS_STORE = 32
        const val TYPE_EMBASSY = 33
        const val TYPE_ESTABLISHMENT = 34
        const val TYPE_FINANCE = 35
        const val TYPE_FIRE_STATION = 36
        const val TYPE_FLOOR = 1006
        const val TYPE_FLORIST = 37
        const val TYPE_FOOD = 38
        const val TYPE_FUNERAL_HOME = 39
        const val TYPE_FURNITURE_STORE = 40
        const val TYPE_GAS_STATION = 41
        const val TYPE_GENERAL_CONTRACTOR = 42
        const val TYPE_GEOCODE = 1007
        const val TYPE_GROCERY_OR_SUPERMARKET = 43
        const val TYPE_GYM = 44
        const val TYPE_HAIR_CARE = 45
        const val TYPE_HARDWARE_STORE = 46
        const val TYPE_HEALTH = 47
        const val TYPE_HINDU_TEMPLE = 48
        const val TYPE_HOME_GOODS_STORE = 49
        const val TYPE_HOSPITAL = 50
        const val TYPE_INSURANCE_AGENCY = 51
        const val TYPE_INTERSECTION = 1008
        const val TYPE_JEWELRY_STORE = 52
        const val TYPE_LAUNDRY = 53
        const val TYPE_LAWYER = 54
        const val TYPE_LIBRARY = 55
        const val TYPE_LIQUOR_STORE = 56
        const val TYPE_LOCALITY = 1009
        const val TYPE_LOCAL_GOVERNMENT_OFFICE = 57
        const val TYPE_LOCKSMITH = 58
        const val TYPE_LODGING = 59
        const val TYPE_MEAL_DELIVERY = 60
        const val TYPE_MEAL_TAKEAWAY = 61
        const val TYPE_MOSQUE = 62
        const val TYPE_MOVIE_RENTAL = 63
        const val TYPE_MOVIE_THEATER = 64
        const val TYPE_MOVING_COMPANY = 65
        const val TYPE_MUSEUM = 66
        const val TYPE_NATURAL_FEATURE = 1010
        const val TYPE_NEIGHBORHOOD = 1011
        const val TYPE_NIGHT_CLUB = 67
        const val TYPE_OTHER = 0
        const val TYPE_PAINTER = 68
        const val TYPE_PARK = 69
        const val TYPE_PARKING = 70
        const val TYPE_PET_STORE = 71
        const val TYPE_PHARMACY = 72
        const val TYPE_PHYSIOTHERAPIST = 73
        const val TYPE_PLACE_OF_WORSHIP = 74
        const val TYPE_PLUMBER = 75
        const val TYPE_POINT_OF_INTEREST = 1013
        const val TYPE_POLICE = 76
        const val TYPE_POLITICAL = 1012
        const val TYPE_POSTAL_CODE = 1015
        const val TYPE_POSTAL_CODE_PREFIX = 1016
        const val TYPE_POSTAL_TOWN = 1017
        const val TYPE_POST_BOX = 1014
        const val TYPE_POST_OFFICE = 77
        const val TYPE_PREMISE = 1018
        const val TYPE_REAL_ESTATE_AGENCY = 78
        const val TYPE_RESTAURANT = 79
        const val TYPE_ROOFING_CONTRACTOR = 80
        const val TYPE_ROOM = 1019
        const val TYPE_ROUTE = 1020
        const val TYPE_RV_PARK = 81
        const val TYPE_SCHOOL = 82
        const val TYPE_SHOE_STORE = 83
        const val TYPE_SHOPPING_MALL = 84
        const val TYPE_SPA = 85
        const val TYPE_STADIUM = 86
        const val TYPE_STORAGE = 87
        const val TYPE_STORE = 88
        const val TYPE_STREET_ADDRESS = 1021
        const val TYPE_SUBLOCALITY = 1022
        const val TYPE_SUBLOCALITY_LEVEL_1 = 1023
        const val TYPE_SUBLOCALITY_LEVEL_2 = 1024
        const val TYPE_SUBLOCALITY_LEVEL_3 = 1025
        const val TYPE_SUBLOCALITY_LEVEL_4 = 1026
        const val TYPE_SUBLOCALITY_LEVEL_5 = 1027
        const val TYPE_SUBPREMISE = 1028
        const val TYPE_SUBWAY_STATION = 89
        const val TYPE_SYNAGOGUE = 90
        const val TYPE_SYNTHETIC_GEOCODE = 1029
        const val TYPE_TAXI_STAND = 91
        const val TYPE_TRAIN_STATION = 92
        const val TYPE_TRANSIT_STATION = 1030
        const val TYPE_TRAVEL_AGENCY = 93
        const val TYPE_UNIVERSITY = 94
        const val TYPE_VETERINARY_CARE = 95
        const val TYPE_ZOO = 96
    }
}