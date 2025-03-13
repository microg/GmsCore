/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import com.google.android.gms.common.api.Status;

public enum GamesStatusCodes {
    ACHIEVEMENT_NOT_INCREMENTAL(3002, "Achievement not incremental"),
    ACHIEVEMENT_UNKNOWN(3001, "Achievement unknown"),
    ACHIEVEMENT_UNLOCKED(3003, "Achievement unlocked"),
    ACHIEVEMENT_UNLOCK_FAILURE(3000, "Achievement unlock failure"),
    APP_MISCONFIGURED(8, "App misconfigured"),
    CLIENT_RECONNECT_REQUIRED(2, "Client reconnect required"),
    GAME_NOT_FOUND(9, "Game not found"),
    INTERNAL_ERROR(1, "Internal error"),
    INTERRUPTED(14, "Interrupted"),
    INVALID_REAL_TIME_ROOM_ID(7002, "Invalid real time room ID"),
    LICENSE_CHECK_FAILED(7, "License check failed"),
    MATCH_ERROR_ALREADY_REMATCHED(6505, "Match error already rematched"),
    MATCH_ERROR_INACTIVE_MATCH(6501, "Match error inactive match"),
    MATCH_ERROR_INVALID_MATCH_RESULTS(6504, "Match error invalid match results"),
    MATCH_ERROR_INVALID_MATCH_STATE(6502, "Match error invalid match state"),
    MATCH_ERROR_INVALID_PARTICIPANT_STATE(6500, "Match error invalid participant state"),
    MATCH_ERROR_LOCALLY_MODIFIED(6507, "Match error locally modified"),
    MATCH_ERROR_OUT_OF_DATE_VERSION(6503, "Match error out of date version"),
    MATCH_NOT_FOUND(6506, "Match not found"),
    MILESTONE_CLAIMED_PREVIOUSLY(8000, "Milestone claimed previously"),
    MILESTONE_CLAIM_FAILED(8001, "Milestone claim failed"),
    MULTIPLAYER_DISABLED(6003, "Multiplayer disabled"),
    MULTIPLAYER_ERROR_CREATION_NOT_ALLOWED(6000, "Multiplayer error creation not allowed"),
    MULTIPLAYER_ERROR_INVALID_MULTIPLAYER_TYPE(6002, "Multiplayer error invalid multiplayer type"),
    MULTIPLAYER_ERROR_INVALID_OPERATION(6004, "Multiplayer error invalid operation"),
    MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER(6001, "Multiplayer error not trusted tester"),
    NETWORK_ERROR_NO_DATA(4, "Network error no data"),
    NETWORK_ERROR_OPERATION_DEFERRED(5, "Network error operation deferred"),
    NETWORK_ERROR_OPERATION_FAILED(6, "Network error operation failed"),
    NETWORK_ERROR_STALE_DATA(3, "Network error stale data"),
    OK(0, "OK"),
    OPERATION_IN_FLIGHT(7007, "Operation in flight"),
    PARTICIPANT_NOT_CONNECTED(7003, "Participant not connected"),
    QUEST_NOT_STARTED(8003, "Quest not started"),
    QUEST_NO_LONGER_AVAILABLE(8002, "Quest no longer available"),
    REAL_TIME_CONNECTION_FAILED(7000, "Real time connection failed"),
    REAL_TIME_INACTIVE_ROOM(7005, "Real time inactive room"),
    REAL_TIME_MESSAGE_SEND_FAILED(7001, "Real time message send failed"),
    REAL_TIME_ROOM_NOT_JOINED(7004, "Real time room not joined"),
    REQUEST_TOO_MANY_RECIPIENTS(2002, "Request too many recipients"),
    REQUEST_UPDATE_PARTIAL_SUCCESS(2000, "Request update partial success"),
    REQUEST_UPDATE_TOTAL_FAILURE(2001, "Request update total failure"),
    SNAPSHOT_COMMIT_FAILED(4003, "Snapshot commit failed"),
    SNAPSHOT_CONFLICT(4004, "Snapshot conflict"),
    SNAPSHOT_CONFLICT_MISSING(4006, "Snapshot conflict missing"),
    SNAPSHOT_CONTENTS_UNAVAILABLE(4002, "Snapshot contents unavailable"),
    SNAPSHOT_CREATION_FAILED(4001, "Snapshot creation failed"),
    SNAPSHOT_FOLDER_UNAVAILABLE(4005, "Snapshot folder unavailable"),
    SNAPSHOT_NOT_FOUND(4000, "Snapshot not found"),
    TIMEOUT(15, "Timeout"),
    VIDEO_ALREADY_CAPTURING(9006, "Video already capturing"),
    VIDEO_NOT_ACTIVE(9000, "Video not active"),
    VIDEO_OUT_OF_DISK_SPACE(9009, "Video out of disk space"),
    VIDEO_PERMISSION_ERROR(9002, "Video permission error"),
    VIDEO_STORAGE_ERROR(9003, "Video storage error"),
    VIDEO_UNEXPECTED_CAPTURE_ERROR(9004, "Video unexpected capture error"),
    VIDEO_UNSUPPORTED(9001, "Video unsupported");

    private final int code;
    private final String description;

    GamesStatusCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Status createStatus(GamesStatusCodes gamesStatusCodes) {
        return new Status(gamesStatusCodes.code, gamesStatusCodes.description);
    }

}
