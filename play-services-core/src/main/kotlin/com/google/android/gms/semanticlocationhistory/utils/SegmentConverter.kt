/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.utils

import android.util.Log
import com.google.android.gms.semanticlocation.Activity
import com.google.android.gms.semanticlocation.ActivityCandidate
import com.google.android.gms.semanticlocation.Date
import com.google.android.gms.semanticlocation.Note
import com.google.android.gms.semanticlocation.Path
import com.google.android.gms.semanticlocation.PeriodSummary
import com.google.android.gms.semanticlocation.PlaceCandidate
import com.google.android.gms.semanticlocation.PointWithDetails
import com.google.android.gms.semanticlocation.TimelineMemory
import com.google.android.gms.semanticlocation.TimelinePath
import com.google.android.gms.semanticlocation.Trip
import com.google.android.gms.semanticlocation.Visit
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_ACTIVITY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_MEMORY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PATH
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PERIOD_SUMMARY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_UNKNOWN
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_VISIT
import com.google.android.gms.semanticlocationhistory.db.SemanticSegment
import org.microg.gms.semanticlocationhistory.ActivityProto
import org.microg.gms.semanticlocationhistory.FinalizationStatus
import org.microg.gms.semanticlocationhistory.DateProto
import org.microg.gms.semanticlocationhistory.DestinationProto
import org.microg.gms.semanticlocationhistory.TimelineDisplayMode
import org.microg.gms.semanticlocationhistory.FeatureId
import org.microg.gms.semanticlocationhistory.LatLngE7
import org.microg.gms.semanticlocationhistory.LocationHistorySegmentProto
import org.microg.gms.semanticlocationhistory.MemoryProto
import org.microg.gms.semanticlocationhistory.NoteProto
import org.microg.gms.semanticlocationhistory.PathProto
import org.microg.gms.semanticlocationhistory.PeriodSummaryProto
import org.microg.gms.semanticlocationhistory.TopPlaceType
import org.microg.gms.semanticlocationhistory.SemanticType
import org.microg.gms.semanticlocationhistory.SegmentTypeUnion
import org.microg.gms.semanticlocationhistory.Timestamp
import org.microg.gms.semanticlocationhistory.TripNameComponents
import org.microg.gms.semanticlocationhistory.TripOrigin
import org.microg.gms.semanticlocationhistory.TripProto
import org.microg.gms.semanticlocationhistory.VisitProto

private typealias PlaceCandidateProto = org.microg.gms.semanticlocationhistory.PlaceCandidate
private typealias ActivityCandidateProto = org.microg.gms.semanticlocationhistory.ActivityCandidate

object SegmentConverter {

    private const val TAG = "SegmentConverter"

    fun toLocationHistorySegment(segment: SemanticSegment): LocationHistorySegment? {
        return try {
            val proto = LocationHistorySegmentProto.ADAPTER.decode(segment.data)

            val startTimeSec = getTimestampSeconds(proto.start_time)
            val endTimeSec = getTimestampSeconds(proto.end_time)
            val segmentId = proto.segment_id ?: ""
            val hierarchyLevel = proto.hierarchy_level ?: 0
            val finalizationState = proto.finalization_state ?: 0
            val displayMode = proto.display_mode?.value ?: 0
            val finalizationStatus = proto.finalization_status?.value ?: 0

            val segmentData = proto.segment_data
            when {
                segmentData?.visit != null -> {
                    val visit = convertVisit(segmentData.visit!!)
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_VISIT, visit, null, null,
                        displayMode, finalizationStatus, null, null
                    )
                }

                segmentData?.activity != null -> {
                    val activity = convertActivity(segmentData.activity!!)
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_ACTIVITY, null, activity, null,
                        displayMode, finalizationStatus, null, null
                    )
                }

                segmentData?.path != null -> {
                    val path = convertPath(segmentData.path!!, startTimeSec)
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_PATH, null, null, path,
                        displayMode, finalizationStatus, null, null
                    )
                }

                segmentData?.memory != null -> {
                    val memory = convertMemory(segmentData.memory!!)
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_MEMORY, null, null, null,
                        displayMode, finalizationStatus, memory, null
                    )
                }

                segmentData?.summary != null -> {
                    val summary = convertPeriodSummary(segmentData.summary!!)
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_PERIOD_SUMMARY, null, null, null,
                        displayMode, finalizationStatus, null, summary
                    )
                }

                else -> {
                    Log.d(TAG, "Segment has no segment_data (type unknown), segmentId=$segmentId")
                    LocationHistorySegment(
                        startTimeSec, endTimeSec, hierarchyLevel, finalizationState, segmentId,
                        SEGMENT_TYPE_UNKNOWN, null, null, null,
                        displayMode, finalizationStatus, null, null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert segment", e)
            null
        }
    }

    fun toProtoBytes(segment: LocationHistorySegment): ByteArray =
        buildProto(segment).encode()

    fun buildProto(segment: LocationHistorySegment, overrideFinalizationStatus: FinalizationStatus? = null): LocationHistorySegmentProto {
        val segmentData = when (segment.type) {
            SEGMENT_TYPE_VISIT -> segment.visit?.let {
                SegmentTypeUnion(visit = convertVisitToProto(it))
            }

            SEGMENT_TYPE_ACTIVITY -> segment.activity?.let {
                SegmentTypeUnion(activity = convertActivityToProto(it))
            }

            SEGMENT_TYPE_PATH -> segment.timelinePath?.let {
                SegmentTypeUnion(path = convertPathToProto(it, segment.startTimestamp))
            }

            SEGMENT_TYPE_MEMORY -> segment.timelineMemory?.let {
                SegmentTypeUnion(memory = convertMemoryToProto(it))
            }

            SEGMENT_TYPE_PERIOD_SUMMARY -> segment.periodSummary?.let {
                SegmentTypeUnion(summary = convertPeriodSummaryToProto(it))
            }

            else -> null
        }

        return LocationHistorySegmentProto(
            start_time = createTimestamp(segment.startTimestamp),
            end_time = createTimestamp(segment.endTimestamp),
            segment_data = segmentData,
            segment_id = segment.segmentId,
            hierarchy_level = segment.hierarchyLevel,
            finalization_state = segment.finalizationState,
            display_mode = TimelineDisplayMode.fromValue(segment.displayMode),
            finalization_status = overrideFinalizationStatus ?: FinalizationStatus.fromValue(segment.finalizationStatus)
        )
    }

    private fun getTimestampSeconds(timestamp: Timestamp?): Long =
        timestamp?.seconds ?: 0L

    private fun createTimestamp(seconds: Long): Timestamp {
        return Timestamp(seconds = seconds)
    }

    private fun convertVisit(proto: VisitProto): Visit {
        val placeCandidate = convertPlaceCandidate(proto.place)

        return Visit(
            proto.hierarchy_level ?: 0,
            proto.probability ?: 0f,
            placeCandidate,
            null,
            proto.is_inferred ?: false,
            null
        )
    }

    private fun convertPlaceCandidate(proto: PlaceCandidateProto?): PlaceCandidate {
        if (proto == null) return createDefaultPlaceCandidate()

        val featureId = proto.feature_id
        val identifier = PlaceCandidate.Identifier(
            featureId?.high ?: 0L,
            featureId?.low ?: 0L
        )

        val location = proto.location
        val point = PlaceCandidate.Point(
            location?.lat_e7 ?: 0,
            location?.lng_e7 ?: 0
        )

        val topPlaceType = proto.top_place_type
        val isCurrentLocation = topPlaceType == TopPlaceType.NEAREST_PLACE
        val isHome = topPlaceType == TopPlaceType.GJ_UPGRADE_HOME

        return PlaceCandidate(
            identifier,
            proto.semantic_type?.value ?: 0,
            proto.probability ?: 0f,
            point,
            isCurrentLocation,
            isHome,
            proto.radius_meters ?: 0.0
        )
    }

    private fun convertActivity(proto: ActivityProto): Activity {
        val startLocation = proto.start_location
        val startPoint = PlaceCandidate.Point(
            startLocation?.lat_e7 ?: 0,
            startLocation?.lng_e7 ?: 0
        )

        val endLocation = proto.end_location
        val endPoint = PlaceCandidate.Point(
            endLocation?.lat_e7 ?: 0,
            endLocation?.lng_e7 ?: 0
        )

        val candidate = proto.candidate
        val activityCandidate = ActivityCandidate(
            candidate?.activity_type ?: 0,
            candidate?.probability ?: 0f
        )

        return Activity(
            startPoint,
            endPoint,
            proto.distance_meters ?: 0f,
            proto.duration_seconds ?: 0f,
            activityCandidate,
            null,
            null
        )
    }

    private fun convertPath(proto: PathProto, startTimeSec: Long): TimelinePath {
        val latE7s = proto.lat_e7s
        val lngE7s = proto.lng_e7s
        val offsets = proto.offset_minutes

        val size = minOf(latE7s.size, lngE7s.size, offsets.size)
        val points = (0 until size).map { i ->
            PointWithDetails(
                PlaceCandidate.Point(latE7s[i], lngE7s[i]),
                startTimeSec + offsets[i] * 60L
            )
        }

        return TimelinePath(Path(points))
    }

    private fun convertMemory(proto: MemoryProto): TimelineMemory {
        return when {
            proto.trip != null -> {
                val trip = convertTrip(proto.trip!!)
                TimelineMemory(trip, null)
            }

            proto.note != null -> {
                TimelineMemory(null, Note(proto.note!!.content ?: ""))
            }

            else -> TimelineMemory(null, null)
        }
    }

    private fun convertTrip(proto: TripProto): Trip {
        val destinations = proto.destinations.map { dest ->
            Trip.Destination(
                PlaceCandidate.Identifier(
                    dest.feature_id?.high ?: 0L,
                    dest.feature_id?.low ?: 0L
                )
            )
        }

        val nameDestinations = proto.name_components?.destinations?.map { dest ->
            Trip.Destination(
                PlaceCandidate.Identifier(
                    dest.feature_id?.high ?: 0L,
                    dest.feature_id?.low ?: 0L
                )
            )
        } ?: emptyList()

        val origin = proto.origin?.let { originProto ->
            Trip.Origin(
                PlaceCandidate.Identifier(
                    originProto.feature_id?.high ?: 0L,
                    originProto.feature_id?.low ?: 0L
                ),
                PlaceCandidate.Point(
                    originProto.location?.lat_e7 ?: 0,
                    originProto.location?.lng_e7 ?: 0
                )
            )
        }

        return Trip(
            proto.duration_seconds ?: 0L,
            destinations,
            Trip.NameComponents(nameDestinations),
            origin
        )
    }

    private fun convertPeriodSummary(proto: PeriodSummaryProto): PeriodSummary {
        val topVisits = proto.top_visits.map { visitProto ->
            convertVisit(visitProto)
        }

        val date = proto.date
        return PeriodSummary(
            topVisits,
            emptyList(),
            Date(date?.year ?: 0, date?.month ?: 0, date?.day ?: 0)
        )
    }

    private fun convertVisitToProto(visit: Visit): VisitProto {
        return VisitProto(
            hierarchy_level = visit.hierarchyLevel,
            probability = visit.probability,
            place = convertPlaceCandidateToProto(visit.place),
            is_inferred = visit.isTimelessVisit
        )
    }

    private fun convertPlaceCandidateToProto(place: PlaceCandidate?): PlaceCandidateProto {
        if (place == null) {
            return PlaceCandidateProto()
        }

        val metadataType = when {
            place.isSensitiveForGorUsage -> TopPlaceType.NEAREST_PLACE
            place.isEligibleForGorUsage -> TopPlaceType.GJ_UPGRADE_HOME
            else -> null
        }

        return PlaceCandidateProto(
            feature_id = FeatureId(
                high = place.identifier.fprint,
                low = place.identifier.cellId
            ),
            semantic_type = SemanticType.fromValue(place.semanticType),
            probability = place.probability,
            location = LatLngE7(
                lat_e7 = place.placeLocation.latE7,
                lng_e7 = place.placeLocation.lngE7
            ),
            radius_meters = place.semanticTypeConfidenceScore,
            top_place_type = metadataType
        )
    }

    private fun convertActivityToProto(activity: Activity): ActivityProto {
        return ActivityProto(
            start_location = LatLngE7(
                lat_e7 = activity.start.latE7,
                lng_e7 = activity.start.lngE7
            ),
            end_location = LatLngE7(
                lat_e7 = activity.end.latE7,
                lng_e7 = activity.end.lngE7
            ),
            distance_meters = activity.distanceMeters,
            duration_seconds = activity.probability,
            candidate = ActivityCandidateProto(
                activity_type = activity.activityCandidate.type,
                probability = activity.activityCandidate.probability
            )
        )
    }

    private fun convertPathToProto(path: TimelinePath, startTimeSec: Long): PathProto {
        val latE7s = mutableListOf<Int>()
        val lngE7s = mutableListOf<Int>()
        val offsetMinutes = mutableListOf<Int>()

        path.path?.points?.forEach { point ->
            latE7s.add(point.point.latE7)
            lngE7s.add(point.point.lngE7)
            offsetMinutes.add(((point.timeOffset - startTimeSec) / 60).toInt())
        }

        return PathProto(
            lat_e7s = latE7s,
            lng_e7s = lngE7s,
            offset_minutes = offsetMinutes
        )
    }

    private fun convertMemoryToProto(memory: TimelineMemory): MemoryProto {
        return when {
            memory.trip != null -> {
                val trip = memory.trip
                MemoryProto(
                    trip = TripProto(
                        duration_seconds = trip.distance,
                        destinations = trip.destinations.map { dest ->
                            DestinationProto(
                                feature_id = FeatureId(
                                    high = dest.identifier.fprint,
                                    low = dest.identifier.cellId
                                )
                            )
                        },
                        name_components = TripNameComponents(
                            destinations = trip.nameComponents.components.map { dest ->
                                DestinationProto(
                                    feature_id = FeatureId(
                                        high = dest.identifier.fprint,
                                        low = dest.identifier.cellId
                                    )
                                )
                            }
                        ),
                        origin = trip.origin?.let { origin ->
                            TripOrigin(
                                feature_id = FeatureId(
                                    high = origin.identifier.fprint,
                                    low = origin.identifier.cellId
                                ),
                                location = LatLngE7(
                                    lat_e7 = origin.point.latE7,
                                    lng_e7 = origin.point.lngE7
                                )
                            )
                        }
                    )
                )
            }

            memory.note != null -> {
                MemoryProto(
                    note = NoteProto(content = memory.note.text)
                )
            }

            else -> MemoryProto()
        }
    }

    private fun convertPeriodSummaryToProto(summary: PeriodSummary): PeriodSummaryProto {
        return PeriodSummaryProto(
            top_visits = summary.visits.map { visit ->
                convertVisitToProto(visit)
            },
            date = DateProto(
                year = summary.date.year,
                month = summary.date.month,
                day = summary.date.day
            )
        )
    }

    fun updateSegmentTimestamps(segment: SemanticSegment, newStartSec: Long, newEndSec: Long): SemanticSegment {
        val updatedData = try {
            val proto = LocationHistorySegmentProto.ADAPTER.decode(segment.data)
            val updatedProto = proto.copy(
                start_time = createTimestamp(newStartSec),
                end_time = createTimestamp(newEndSec)
            )
            updatedProto.encode()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update segment timestamps in proto, using original data", e)
            segment.data
        }

        return segment.copy(
            startTimestamp = newStartSec,
            endTimestamp = newEndSec,
            data = updatedData
        )
    }

    private fun createDefaultPlaceCandidate(): PlaceCandidate {
        return PlaceCandidate(
            PlaceCandidate.Identifier(0, 0),
            0, 0f,
            PlaceCandidate.Point(0, 0),
            false, false, 0.0
        )
    }
}