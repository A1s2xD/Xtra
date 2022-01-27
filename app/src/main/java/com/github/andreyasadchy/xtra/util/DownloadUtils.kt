package com.github.andreyasadchy.xtra.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.andreyasadchy.xtra.GlideApp
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.Downloadable
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.model.offline.Request
import com.github.andreyasadchy.xtra.ui.download.BaseDownloadDialog.Storage
import com.github.andreyasadchy.xtra.ui.download.DownloadService
import com.github.andreyasadchy.xtra.ui.download.DownloadService.Companion.KEY_REQUEST
import com.github.andreyasadchy.xtra.ui.download.DownloadService.Companion.KEY_WIFI
import java.io.File

object DownloadUtils {

    val isExternalStorageAvailable: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    fun download(context: Context, request: Request, wifiOnly: Boolean = false) {
        val intent = Intent(context, DownloadService::class.java)
                .putExtra(KEY_REQUEST, request)
                .putExtra(KEY_WIFI, wifiOnly)
        context.startService(intent)
        DownloadService.activeRequests.add(request.offlineVideoId)
    }

    fun prepareDownload(context: Context, downloadable: Downloadable, url: String, path: String, duration: Long?, startPosition: Long?, segmentFrom: Int? = null, segmentTo: Int? = null): OfflineVideo {
        val offlinePath = if (downloadable is Video) {
            "$path${System.currentTimeMillis()}.m3u8"
        } else {
            "$path.mp4"
        }
        val glide = GlideApp.with(context)
        return with(downloadable) {
            var downloadedThumbnail: String?
            var downloadedLogo: String?
            try {
                downloadedThumbnail = glide.downloadOnly().load(thumbnail).submit().get().absolutePath
                downloadedLogo = glide.downloadOnly().load(channelLogo).submit().get().absolutePath
            } catch (e: Exception) {
                downloadedThumbnail = thumbnail
                downloadedLogo = channelLogo
            }
            OfflineVideo(offlinePath, url, startPosition, title, channelId, channelLogin, channelName, downloadedLogo, downloadedThumbnail, gameId, gameName,
                duration, uploadDate?.let { TwitchApiHelper.parseIso8601Date(it) }, System.currentTimeMillis(), 0L, 0, if (segmentTo != null && segmentFrom != null) segmentTo - segmentFrom + 1 else 100, type = type)
        }
    }

    fun hasStoragePermission(activity: Activity): Boolean {
        fun requestPermissions() {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ||
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(activity)
                    .setMessage(R.string.storage_permission_message)
                    .setTitle(R.string.storage_permission_title)
                    .setPositiveButton(android.R.string.ok) { _, _ -> requestPermissions() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> activity.toast(R.string.permission_denied) }
                    .show()
        } else {
            requestPermissions()
        }
        return false
    }

    fun getAvailableStorage(context: Context): List<Storage> {
        val storage = ContextCompat.getExternalFilesDirs(context, ".downloads")
        val list = mutableListOf<Storage>()
        for (i in storage.indices) {
            val storagePath = storage[i]?.absolutePath ?: continue
            val name = if (i == 0) {
                context.getString(R.string.internal_storage)
            } else {
                val endRootIndex = storagePath.indexOf("/Android/data")
                if (endRootIndex < 0) continue
                val startRootIndex = storagePath.lastIndexOf(File.separatorChar, endRootIndex - 1)
                storagePath.substring(startRootIndex + 1, endRootIndex)
            }
            list.add(Storage(i, name, storagePath))
        }
        return list
    }
}
