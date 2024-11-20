/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import com.google.android.play.core.assetpacks.protocol.*;
import org.microg.gms.common.Hide;

import java.util.*;

@Hide
public class AssetPackServiceClient {
    private static final String TAG = "AssetPackServiceClient";
    private List<TaskCompletionSource<?>> pendingCalls = new ArrayList<>();
    private Context context;
    private AssetPackManagerImpl assetPackManager;

    private interface PendingCall<TResult> {
        void execute(IAssetModuleService service, TaskCompletionSource<TResult> completionSource) throws Exception;
    }

    private <TResult> Task<TResult> execute(PendingCall<TResult> pendingCall) {
        TaskCompletionSource<TResult> completionSource = new TaskCompletionSource<>();
        pendingCalls.add(completionSource);
        try {
            pendingCall.execute(null, completionSource);
        } catch (Exception e) {
            completionSource.trySetException(e);
        }
        Task<TResult> task = completionSource.getTask();
        task.addOnCompleteListener(ignored -> pendingCalls.remove(completionSource));
        return task;
    }

    private Bundle getOptionsBundle() {
        Bundle options = new Bundle();
        // TODO
        BundleKeys.put(options, BundleKeys.PLAY_CORE_VERSION_CODE, 20202);
        BundleKeys.put(options, BundleKeys.SUPPORTED_COMPRESSION_FORMATS, new ArrayList<>(Arrays.asList(CompressionFormat.UNSPECIFIED, CompressionFormat.BROTLI)));
        BundleKeys.put(options, BundleKeys.SUPPORTED_PATCH_FORMATS, new ArrayList<>(Arrays.asList(PatchFormat.PATCH_GDIFF, PatchFormat.GZIPPED_GDIFF)));
        return options;
    }

    private ArrayList<Bundle> getModuleNameBundles(List<String> packNames) {
        ArrayList<Bundle> moduleNameBundles = new ArrayList<>();
        for (String packName : packNames) {
            Bundle arg = new Bundle();
            BundleKeys.put(arg, BundleKeys.MODULE_NAME, packName);
            moduleNameBundles.add(arg);
        }
        return moduleNameBundles;
    }

    private Bundle getInstalledAssetModulesBundle(Map<String, Long> installedAssetModules) {
        Bundle installedAssetModulesBundle = getOptionsBundle();
        ArrayList<Bundle> installedAssetModuleBundles = new ArrayList<>();
        for (String moduleName : installedAssetModules.keySet()) {
            Bundle installedAssetModuleBundle = new Bundle();
            BundleKeys.put(installedAssetModuleBundle, BundleKeys.INSTALLED_ASSET_MODULE_NAME, moduleName);
            BundleKeys.put(installedAssetModuleBundle, BundleKeys.INSTALLED_ASSET_MODULE_VERSION, installedAssetModules.get(moduleName));
            installedAssetModuleBundles.add(installedAssetModuleBundle);
        }
        BundleKeys.put(installedAssetModulesBundle, BundleKeys.INSTALLED_ASSET_MODULE, installedAssetModuleBundles);
        return installedAssetModulesBundle;
    }

    private Bundle getSessionIdentifierBundle(int sessionId) {
        Bundle sessionIdentifierBundle = new Bundle();
        BundleKeys.put(sessionIdentifierBundle, BundleKeys.SESSION_ID, sessionId);
        return sessionIdentifierBundle;
    }

    private Bundle getModuleIdentifierBundle(int sessionId, String moduleName) {
        Bundle moduleIdentifierBundle = getSessionIdentifierBundle(sessionId);
        BundleKeys.put(moduleIdentifierBundle, BundleKeys.MODULE_NAME, moduleName);
        return moduleIdentifierBundle;
    }

    private Bundle getChunkIdentifierBundle(int sessionId, String moduleName, String sliceId, int chunkNumber) {
        Bundle chunkIdentifierBundle = getModuleIdentifierBundle(sessionId, moduleName);
        BundleKeys.put(chunkIdentifierBundle, BundleKeys.SLICE_ID, sliceId);
        BundleKeys.put(chunkIdentifierBundle, BundleKeys.CHUNK_NUMBER, chunkNumber);
        return chunkIdentifierBundle;
    }

    public Task<ParcelFileDescriptor> getChunkFileDescriptor(int sessionId, String moduleName, String sliceId, int chunkNumber) {
        return execute((service, completionSource) -> {
            service.getChunkFileDescriptor(context.getPackageName(), getChunkIdentifierBundle(sessionId, moduleName, sliceId, chunkNumber), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onGetChunkFileDescriptor(ParcelFileDescriptor chunkFileDescriptor) {
                    completionSource.trySetResult(chunkFileDescriptor);
                }
            });
        });
    }

    public Task<AssetPackStates> getPackStates(List<String> packNames, Map<String, Long> installedAssetModules) {
        return execute((service, completionSource) -> {
            service.requestDownloadInfo(context.getPackageName(), getModuleNameBundles(packNames), getInstalledAssetModulesBundle(installedAssetModules), new BaseCallback(completionSource) {
                @Override
                public void onRequestDownloadInfo(Bundle bundle, Bundle bundle2) {
                    completionSource.trySetResult(AssetPackStatesImpl.fromBundle(bundle, assetPackManager));
                }
            });
        });
    }

    public Task<AssetPackStates> startDownload(List<String> packNames, Map<String, Long> installedAssetModules) {
        Task<AssetPackStates> task = execute((service, completionSource) -> {
            service.startDownload(context.getPackageName(), getModuleNameBundles(packNames), getInstalledAssetModulesBundle(installedAssetModules), new BaseCallback(completionSource) {
                @Override
                public void onStartDownload(int status, Bundle bundle) {
                    completionSource.trySetResult(AssetPackStatesImpl.fromBundle(bundle, assetPackManager, true));
                }
            });
        });
        task.addOnSuccessListener(ignored -> keepAlive());
        return task;
    }

    public Task<List<String>> syncPacks(Map<String, Long> installedAssetModules) {
        return execute((service, completionSource) -> {
            service.getSessionStates(context.getPackageName(), getInstalledAssetModulesBundle(installedAssetModules), new BaseCallback(completionSource) {
                @Override
                public void onGetSessionStates(List<Bundle> list) {
                    ArrayList<String> packNames = new ArrayList<>();
                    for (Bundle bundle : list) {
                        Collection<AssetPackState> packStates = AssetPackStatesImpl.fromBundle(bundle, assetPackManager, true).packStates().values();
                        if (!packStates.isEmpty()) {
                            AssetPackState state = packStates.iterator().next();
                            switch (state.status()) {
                                case AssetPackStatus.PENDING:
                                case AssetPackStatus.DOWNLOADING:
                                case AssetPackStatus.TRANSFERRING:
                                case AssetPackStatus.WAITING_FOR_WIFI:
                                case AssetPackStatus.REQUIRES_USER_CONFIRMATION:
                                    packNames.add(state.name());
                            }
                        }
                    }
                    completionSource.trySetResult(packNames);
                }
            });
        });
    }

    public void cancelDownloads(List<String> packNames) {
        execute((service, completionSource) -> {
            service.cancelDownloads(context.getPackageName(), getModuleNameBundles(packNames), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onCancelDownloads() {
                    completionSource.trySetResult(null);
                }
            });
        });
    }

    public void keepAlive() {
        // TODO
    }

    public void notifyChunkTransferred(int sessionId, String moduleName, String sliceId, int chunkNumber) {
        execute((service, completionSource) -> {
            service.notifyChunkTransferred(context.getPackageName(), getChunkIdentifierBundle(sessionId, moduleName, sliceId, chunkNumber), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onNotifyChunkTransferred(int sessionId, String moduleName, String sliceId, int chunkNumber) {
                    completionSource.trySetResult(null);
                }
            });
        });
    }

    public void notifyModuleCompleted(int sessionId, String moduleName) {
        notifyModuleCompleted(sessionId, moduleName, 10);
    }

    public void notifyModuleCompleted(int sessionId, String moduleName, int maxRetries) {
        execute((service, completionSource) -> {
            service.notifyModuleCompleted(context.getPackageName(), getModuleIdentifierBundle(sessionId, moduleName), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onError(int errorCode) {
                    if (maxRetries > 0) {
                        notifyModuleCompleted(sessionId, moduleName, maxRetries - 1);
                    }
                }
            });
        });
    }

    public void notifySessionFailed(int sessionId) {
        execute((service, completionSource) -> {
            service.notifySessionFailed(context.getPackageName(), getSessionIdentifierBundle(sessionId), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onNotifySessionFailed(int sessionId) {
                    completionSource.trySetResult(null);
                }
            });
        });
    }

    public void removePack(String packName) {
        execute((service, completionSource) -> {
            service.removeModule(context.getPackageName(), getModuleIdentifierBundle(0, packName), getOptionsBundle(), new BaseCallback(completionSource) {
                @Override
                public void onRemoveModule() {
                    completionSource.trySetResult(null);
                }
            });
        });
    }

    private static class BaseCallback extends IAssetModuleServiceCallback.Stub {
        @NonNull
        private final TaskCompletionSource<?> completionSource;

        public BaseCallback(@NonNull TaskCompletionSource<?> completionSource) {
            this.completionSource = completionSource;
        }

        @Override
        public void onStartDownload(int sessionId, Bundle bundle) {
            Log.i(TAG, "onStartDownload(" + sessionId + ")");
            onStartDownload(sessionId);
        }

        public void onStartDownload(int sessionId) {
            completionSource.trySetException(new Exception("Unexpected callback: onStartDownload"));
        }

        @Override
        public void onCancelDownload(int status, Bundle bundle) {
            Log.i(TAG, "onCancelDownload(" + status + ")");
            onCancelDownload(status);
        }

        public void onCancelDownload(int status) {
            completionSource.trySetException(new Exception("Unexpected callback: onCancelDownload"));
        }

        @Override
        public void onGetSession(int status, Bundle bundle) {
            Log.i(TAG, "onGetSession(" + status + ")");
            onGetSession(status);
        }

        public void onGetSession(int status) {
            completionSource.trySetException(new Exception("Unexpected callback: onGetSession"));
        }

        @Override
        public void onGetSessionStates(List<Bundle> list) {
            completionSource.trySetException(new Exception("Unexpected callback: onGetSessionStates"));
        }

        @Override
        public void onNotifyChunkTransferred(Bundle bundle, Bundle bundle2) {
            int sessionId = BundleKeys.get(bundle, BundleKeys.SESSION_ID, 0);
            String moduleName = BundleKeys.get(bundle, BundleKeys.MODULE_NAME);
            String sliceId = BundleKeys.get(bundle, BundleKeys.SLICE_ID);
            int chunkNumber = BundleKeys.get(bundle, BundleKeys.CHUNK_NUMBER, 0);
            Log.i(TAG, "onNotifyChunkTransferred(" + sessionId + ", " + moduleName + ", " + sliceId + ", " + chunkNumber + ")");
            onNotifyChunkTransferred(sessionId, moduleName, sliceId, chunkNumber);
        }

        public void onNotifyChunkTransferred(int sessionId, String moduleName, String sliceId, int chunkNumber) {
            completionSource.trySetException(new Exception("Unexpected callback: onNotifyChunkTransferred"));
        }

        @Override
        public void onError(Bundle bundle) {
            int errorCode = BundleKeys.get(bundle, BundleKeys.ERROR_CODE, AssetPackErrorCode.INTERNAL_ERROR);
            onError(errorCode);
        }

        public void onError(int errorCode) {
            completionSource.trySetException(new AssetPackException(errorCode));
        }

        @Override
        public void onNotifyModuleCompleted(Bundle bundle, Bundle bundle2) {
            int sessionId = BundleKeys.get(bundle, BundleKeys.SESSION_ID, 0);
            String moduleName = BundleKeys.get(bundle, BundleKeys.MODULE_NAME);
            Log.i(TAG, "onNotifyModuleCompleted(" + sessionId + ", " + moduleName + ")");
            onNotifyModuleCompleted(sessionId, moduleName);
        }

        public void onNotifyModuleCompleted(int sessionId, String moduleName) {
            completionSource.trySetException(new Exception("Unexpected callback: onNotifyModuleCompleted"));
        }

        @Override
        public void onNotifySessionFailed(Bundle bundle) {
            int sessionId = BundleKeys.get(bundle, BundleKeys.SESSION_ID, 0);
            Log.i(TAG, "onNotifySessionFailed(" + sessionId + ")");
            onNotifySessionFailed(sessionId);
        }

        public void onNotifySessionFailed(int sessionId) {
            completionSource.trySetException(new Exception("Unexpected callback: onNotifySessionFailed"));
        }

        @Override
        public void onKeepAlive(Bundle bundle, Bundle bundle2) {
            boolean keepAlive = BundleKeys.get(bundle, BundleKeys.KEEP_ALIVE, false);
            Log.i(TAG, "onKeepAlive(" + keepAlive + ")");
            onKeepAlive(keepAlive);
        }

        public void onKeepAlive(boolean keepAlive) {
            completionSource.trySetException(new Exception("Unexpected callback: onKeepAlive"));
        }

        @Override
        public void onGetChunkFileDescriptor(Bundle bundle, Bundle bundle2) {
            ParcelFileDescriptor chunkFileDescriptor = BundleKeys.get(bundle, BundleKeys.CHUNK_FILE_DESCRIPTOR);
            Log.i(TAG, "onGetChunkFileDescriptor(...)");
            onGetChunkFileDescriptor(chunkFileDescriptor);
        }

        public void onGetChunkFileDescriptor(ParcelFileDescriptor chunkFileDescriptor) {
            completionSource.trySetException(new Exception("Unexpected callback: onGetChunkFileDescriptor"));
        }

        @Override
        public void onRequestDownloadInfo(Bundle bundle, Bundle bundle2) {
            Log.i(TAG, "onRequestDownloadInfo()");
            onRequestDownloadInfo();
        }

        public void onRequestDownloadInfo() {
            completionSource.trySetException(new Exception("Unexpected callback: onRequestDownloadInfo"));
        }

        @Override
        public void onRemoveModule(Bundle bundle, Bundle bundle2) {
            Log.i(TAG, "onRemoveModule()");
            onRemoveModule();
        }

        public void onRemoveModule() {
            completionSource.trySetException(new Exception("Unexpected callback: onRemoveModule"));
        }

        @Override
        public void onCancelDownloads(Bundle bundle) {
            Log.i(TAG, "onCancelDownload()");
            onCancelDownloads();
        }

        public void onCancelDownloads() {
            completionSource.trySetException(new Exception("Unexpected callback: onCancelDownloads"));
        }
    }
}
