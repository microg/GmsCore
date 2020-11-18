/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;
import org.microg.gms.nearby.exposurenotification.Constants;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Interface for contact tracing APIs.
 */
@PublicApi
public interface ExposureNotificationClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Activity action which shows the exposure notification settings screen.
     */
    String ACTION_EXPOSURE_NOTIFICATION_SETTINGS = Constants.ACTION_EXPOSURE_NOTIFICATION_SETTINGS;
    /**
     * Action which will be invoked via a BroadcastReceiver as a callback when matching has finished and no matches were found.
     * Also see {@link #EXTRA_TOKEN}, which will be included in this broadcast.
     */
    String ACTION_EXPOSURE_NOT_FOUND = Constants.ACTION_EXPOSURE_NOT_FOUND;
    /**
     * Action which will be invoked via a BroadcastReceiver as a callback when the user has an updated exposure status.
     * Also see {@link #EXTRA_EXPOSURE_SUMMARY} and {@link #EXTRA_TOKEN}, which will be included in this broadcast.
     */
    String ACTION_EXPOSURE_STATE_UPDATED = Constants.ACTION_EXPOSURE_STATE_UPDATED;
    /**
     * Action which will be invoked via a BroadcastReceiver when the user modifies the state of exposure notifications via the Google Settings page.
     * {@link #EXTRA_SERVICE_STATE} will be included as part of this broadcast.
     */
    String ACTION_SERVICE_STATE_UPDATED = Constants.ACTION_SERVICE_STATE_UPDATED;
    /**
     * Extra attached to the {@link #ACTION_EXPOSURE_STATE_UPDATED} broadcast, giving a summary of the exposure details detected.
     * Also see {@link #getExposureSummary(String)}.
     *
     * @deprecated {@link ExposureSummary} is no longer provided when using the {@link #getExposureWindows()} API. Instead, use {@link #getDailySummaries(DailySummariesConfig)}.
     */
    @Deprecated
    String EXTRA_EXPOSURE_SUMMARY = Constants.EXTRA_EXPOSURE_SUMMARY;
    /**
     * Boolean extra attached to the {@link #ACTION_SERVICE_STATE_UPDATED} broadcast signifying whether the service is enabled or disabled.
     */
    String EXTRA_SERVICE_STATE = Constants.EXTRA_SERVICE_STATE;
    /**
     * Extra attached to the {@link #ACTION_EXPOSURE_STATE_UPDATED} broadcast, providing the token associated with the {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)} request.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless versions of {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}, {@link #getExposureWindows()}, and {@link #getDailySummaries(DailySummariesConfig)}.
     */
    @Deprecated
    String EXTRA_TOKEN = Constants.EXTRA_TOKEN;
    /**
     * Token to be used with ExposureWindows API. Must be used with {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider) }request when later using {@link #getExposureWindows()}.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless versions of {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}, {@link #getExposureWindows()}, and {@link #getDailySummaries(DailySummariesConfig)}.
     */
    @Deprecated
    String TOKEN_A = Constants.TOKEN_A;

    /**
     * Checks whether the device supports Exposure Notification BLE scanning without requiring location to be enabled first.
     */
    boolean deviceSupportsLocationlessScanning();

    /**
     * Gets {@link CalibrationConfidence} of the current device.
     */
    Task<Integer> getCalibrationConfidence();

    /**
     * Retrieves the per-day exposure summaries associated with the provided configuration.
     * <p>
     * A valid configuration must be provided to compute the summaries.
     */
    Task<List<DailySummary>> getDailySummaries(DailySummariesConfig dailySummariesConfig);

    /**
     * Retrieves the current {@link DiagnosisKeysDataMapping}.
     */
    Task<DiagnosisKeysDataMapping> getDiagnosisKeysDataMapping();

    /**
     * Gets detailed information about exposures that have occurred related to the provided token, which should match the token provided in {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}.
     * <p>
     * When multiple ExposureInformation objects are returned, they can be:
     * <ul>
     * <li>Multiple encounters with a single diagnosis key.</li>
     * <li>Multiple encounters with the same device across key rotation boundaries.</li>
     * <li>Encounters with multiple devices.</li>
     * </ul>
     * Records of calls to this method will be retained and viewable by the user.
     *
     * @deprecated When using the ExposureWindow API, use {@link #getExposureWindows()} instead.
     */
    @Deprecated
    Task<List<ExposureInformation>> getExposureInformation(String token);

    /**
     * Gets a summary of the exposure calculation for the token, which should match the token provided in {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}.
     *
     * @deprecated When using the ExposureWindow API, use {@link #getDailySummaries(DailySummariesConfig)} instead.
     */
    @Deprecated
    Task<ExposureSummary> getExposureSummary(String token);

    /**
     * Retrieves the list of exposure windows corresponding to the TEKs given to {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}.
     * <p>
     * Long exposures to one TEK are split into windows of up to 30 minutes of scans, so a given TEK may lead to several exposure windows if beacon sightings for it spanned more than 30 minutes. The link between them (the fact that they all correspond to the same TEK) is lost because those windows are shuffled before being returned and the underlying TEKs are not exposed by the API.
     */
    Task<List<ExposureWindow>> getExposureWindows();

    /**
     * Retrieves the list of exposure windows corresponding to the TEKs given to provideKeys with token=TOKEN_A.
     * <p>
     * Long exposures to one TEK are split into windows of up to 30 minutes of scans, so a given TEK may lead to several exposure windows if beacon sightings for it spanned more than 30 minutes. The link between them (the fact that they all correspond to the same TEK) is lost because those windows are shuffled before being returned and the underlying TEKs are not exposed by the API.
     * <p>
     * The provided token must be TOKEN_A.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless version of {@link #getExposureWindows()}.
     */
    @Deprecated
    Task<List<ExposureWindow>> getExposureWindows(String token);

    /**
     * Retrieves the associated {@link PackageConfiguration} for the calling package. Note that this value can be null if no configuration was when starting.
     */
    Task<PackageConfiguration> getPackageConfiguration();

    /**
     * Gets the current Exposure Notification status.
     */
    Task<Set<ExposureNotificationStatus>> getStatus();

    /**
     * Gets {@link TemporaryExposureKey} history to be stored on the server.
     * <p>
     * This should only be done after proper verification is performed on the client side that the user is diagnosed positive. Each key returned will have an unknown transmission risk level, clients should choose an appropriate risk level for these keys before uploading them to the server.
     * <p>
     * The keys provided here will only be from previous days; keys will not be released until after they are no longer an active exposure key.
     * <p>
     * This shows a user permission dialog for sharing and uploading data to the server.
     */
    Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory();

    /**
     * Gets the current Exposure Notification version.
     */
    Task<Long> getVersion();

    /**
     * Indicates whether contact tracing is currently running for the requesting app.
     */
    Task<Boolean> isEnabled();

    /**
     * Provides a list of diagnosis key files for exposure checking. The files are to be synced from the server. Old diagnosis keys (for example older than 14 days), will be ignored.
     * <p>
     * Diagnosis keys will be stored and matching will be performed in the near future, after which you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     * <p>
     * The diagnosis key files must be signed appropriately. Results from this request can also be queried at any time via {@link #getExposureWindows()} and {@link #getDailySummaries(DailySummariesConfig)}.
     * <p>
     * After the result Task has returned, keyFiles can be deleted.
     * <p>
     * Results remain for 14 days.
     *
     * @deprecated Prefer the {@link DiagnosisKeyFileProvider} version of this method instead, which scales better when a large number of files are passed at the same time.
     */
    @Deprecated
    Task<Void> provideDiagnosisKeys(List<File> keyFiles);

    /**
     * Provides diagnosis key files for exposure checking. The files are to be synced from the server. Old diagnosis keys (for example older than 14 days), will be ignored.
     * <p>
     * Diagnosis keys will be stored and matching will be performed in the near future, after which you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     * <p>
     * The diagnosis key files must be signed appropriately. Results from this request can also be queried at any time via {@link #getExposureWindows()} and {@link #getDailySummaries(DailySummariesConfig)}.
     * <p>
     * After the result Task has returned, files can be deleted.
     * <p>
     * Results remain for 14 days.
     */
    Task<Void> provideDiagnosisKeys(DiagnosisKeyFileProvider provider);

    /**
     * Provides a list of diagnosis key files for exposure checking. The files are to be synced from the server. Old diagnosis keys (for example older than 14 days), will be ignored.
     * <p>
     * Diagnosis keys will be stored and matching will be performed in the near future, after which you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     * <p>
     * The diagnosis key files must be signed appropriately. Exposure configuration options can be provided to tune the matching algorithm. A unique token for this batch can also be provided, which will be used to associate the matches with this request as part of {@link #getExposureSummary(String)} and {@link #getExposureInformation(String)}. Alternatively, the same token can be passed in multiple times to concatenate results.
     * <p>
     * After the result Task has returned, keyFiles can be deleted.
     * <p>
     * Results for a given token remain for 14 days.
     *
     * @deprecated Tokens and configuration are no longer used. Instead, prefer using the tokenless, configuration-less version of {@link #provideDiagnosisKeys(DiagnosisKeyFileProvider)}.
     */
    @Deprecated
    Task<Void> provideDiagnosisKeys(List<File> keys, ExposureConfiguration configuration, String token);

    /**
     * Sets the diagnosis keys data mapping if it wasn't already changed recently.
     * <p>
     * If called twice within 7 days, the second call will have no effect and will raise an exception with status code FAILED_RATE_LIMITED.
     */
    Task<Void> setDiagnosisKeysDataMapping(DiagnosisKeysDataMapping diagnosisKeysMetadataMapping);

    /**
     * Starts BLE broadcasts and scanning based on the defined protocol.
     * <p>
     * If not previously started, this shows a user dialog for consent to start exposure detection and get permission.
     * <p>
     * Callbacks regarding exposure status will be provided via a BroadcastReceiver. Clients should register a receiver in their AndroidManifest which can handle the following action:
     * <ul>
     * <li>{@code com.google.android.gms.exposurenotification.ACTION_EXPOSURE_STATE_UPDATED}</li>
     * </ul>
     * This receiver should also be guarded by the {@code com.google.android.gms.nearby.exposurenotification.EXPOSURE_CALLBACK} permission so that other apps are not able to fake this broadcast.
     */
    Task<Void> start();

    /**
     * Disables advertising and scanning. Contents of the database and keys will remain.
     * <p>
     * If the client app has been uninstalled by the user, this will be automatically invoked and the database and keys will be wiped from the device.
     */
    Task<Void> stop();
}
