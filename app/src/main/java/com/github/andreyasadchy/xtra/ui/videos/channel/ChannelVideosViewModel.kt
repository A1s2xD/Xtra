package com.github.andreyasadchy.xtra.ui.videos.channel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.repository.PlayerRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.type.VideoSort
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosViewModel
import com.github.andreyasadchy.xtra.util.DownloadUtils
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


class ChannelVideosViewModel @Inject constructor(
        context: Application,
        private val repository: TwitchService,
        playerRepository: PlayerRepository,
        private val offlineRepository: OfflineRepository) : BaseVideosViewModel(playerRepository) {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    private val filter = MutableLiveData<Filter>()
    override val result: LiveData<Listing<Video>> = Transformations.map(filter) {
        repository.loadChannelVideos(it.channelId, it.channelLogin, it.helixClientId, it.helixToken, it.period, it.broadcastType, it.sort, it.gqlClientId,
            when (it.broadcastType) {
                BroadcastType.ARCHIVE -> com.github.andreyasadchy.xtra.type.BroadcastType.ARCHIVE
                BroadcastType.HIGHLIGHT -> com.github.andreyasadchy.xtra.type.BroadcastType.HIGHLIGHT
                BroadcastType.UPLOAD -> com.github.andreyasadchy.xtra.type.BroadcastType.UPLOAD
                else -> null },
            when (it.sort) { Sort.TIME -> VideoSort.TIME else -> VideoSort.VIEWS },
            if (it.broadcastType == BroadcastType.ALL) { null }
            else { it.broadcastType.value.uppercase() }, it.sort.value.uppercase(),
            it.apiPref, viewModelScope)
    }
    val sort: Sort
        get() = filter.value!!.sort
    val period: Period
        get() = filter.value!!.period
    val type: BroadcastType
        get() = filter.value!!.broadcastType

    init {
        _sortText.value = context.getString(R.string.sort_and_period, context.getString(R.string.upload_date), context.getString(R.string.all_time))
    }

    fun setChannelId(channelId: String? = null, channelLogin: String? = null, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>) {
        if (filter.value?.channelId != channelId) {
            filter.value = Filter(channelId, channelLogin, helixClientId, helixToken, gqlClientId, apiPref)
        }
    }

    fun filter(sort: Sort, period: Period, type: BroadcastType, text: CharSequence) {
        filter.value = filter.value?.copy(sort = sort, period = period, broadcastType = type)
        _sortText.value = text
    }

    private data class Filter(
        val channelId: String?,
        val channelLogin: String?,
        val helixClientId: String?,
        val helixToken: String?,
        val gqlClientId: String?,
        val apiPref: ArrayList<Pair<Long?, String?>?>,
        val sort: Sort = Sort.TIME,
        val period: Period = Period.ALL,
        val broadcastType: BroadcastType = BroadcastType.ALL)

    fun saveBookmark(context: Context, video: Video) {
        GlobalScope.launch {
            val items = offlineRepository.getVideosByVideoId(video.id).filter { it.bookmark == true }
            if (!items.isNullOrEmpty()) {
                for (i in items) {
                    offlineRepository.deleteVideo(context, i)
                }
            } else {
                try {
                    Glide.with(context)
                        .asBitmap()
                        .load(video.thumbnail)
                        .into(object: CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {

                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                DownloadUtils.savePng(context, "thumbnails", video.id, resource)
                            }
                        })
                } catch (e: Exception) {

                }
                try {
                    if (video.channelId != null) {
                        Glide.with(context)
                            .asBitmap()
                            .load(video.channelLogo)
                            .into(object: CustomTarget<Bitmap>() {
                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    DownloadUtils.savePng(context, "profile_pics", video.channelId!!, resource)
                                }
                            })
                    }
                } catch (e: Exception) {

                }
                val downloadedThumbnail = File(context.filesDir.toString() + File.separator + "thumbnails" + File.separator + "${video.id}.png").absolutePath
                val downloadedLogo = File(context.filesDir.toString() + File.separator + "profile_pics" + File.separator + "${video.channelId}.png").absolutePath
                val duration = video.duration?.let { TwitchApiHelper.getDuration(it) }
                val createdAt = video.createdAt?.let { TwitchApiHelper.parseIso8601Date(it) }
                offlineRepository.saveVideo(OfflineVideo(
                    url = "",
                    name = video.title,
                    channelId = video.channelId,
                    channelLogin = video.channelLogin,
                    channelName = video.channelName,
                    channelLogo = downloadedLogo,
                    thumbnail = downloadedThumbnail,
                    gameId = video.gameId,
                    gameName = video.gameName,
                    duration = duration,
                    uploadDate = createdAt,
                    progress = 0,
                    maxProgress = 0,
                    type = video.type,
                    videoId = video.id,
                    bookmark = true
                ))
            }
        }
    }
}
