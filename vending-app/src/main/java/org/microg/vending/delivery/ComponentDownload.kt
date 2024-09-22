package org.microg.vending.delivery

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.vending.installer.packageDownloadLocation
import com.google.android.finsky.splitinstallservice.PackageComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.microg.vending.billing.core.HttpClient
import java.io.File

private const val TAG = "GmsVendingComponentDl"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun HttpClient.downloadPackageComponents(
    context: Context,
    downloadList: List<PackageComponent>,
    tag: Any
): Map<PackageComponent, File?> = coroutineScope {
    downloadList.map { info ->
        Log.d(TAG, "downloadSplitPackage: $info")
        async {
            info to runCatching {
                val file = File(context.packageDownloadLocation().toString(), info.componentName)
                download(
                    url = info.url,
                    downloadFile = file,
                    tag = tag
                )
                file
            }.onFailure {
                Log.w(TAG, "package component failed to downlaod from url ${info.url}, " +
                        "to be saved as `${info.componentName}`", it)
            }.getOrNull()
        }
    }.awaitAll().associate { it }
}
