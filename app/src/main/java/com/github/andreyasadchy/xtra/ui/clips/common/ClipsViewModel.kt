package com.github.andreyasadchy.xtra.ui.clips.common

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import javax.inject.Inject

class ClipsViewModel @Inject constructor(
        context: Application,
        private val repository: TwitchService) : PagedListViewModel<Clip>() {

    private val _sortText = MutableLiveData<CharSequence>()
    val sortText: LiveData<CharSequence>
        get() = _sortText
    private val filter = MutableLiveData<Filter>()
    override val result: LiveData<Listing<Clip>> = Transformations.map(filter) {
        if (it.useHelix) {
            val started = when (it.period) {
                Period.ALL -> null
                else -> TwitchApiHelper.getClipTime(it.period)
            }
            val ended = when (it.period) {
                Period.ALL -> null
                else -> TwitchApiHelper.getClipTime()
            }
            repository.loadClips(it.clientId, it.token, it.channelId, it.channelLogin, it.gameId, started, ended, viewModelScope)
        } else {
            val period = when (it.period) {
                Period.DAY -> "LAST_DAY"
                Period.WEEK -> "LAST_WEEK"
                Period.MONTH -> "LAST_MONTH"
                else -> "ALL_TIME" }
            if (it.gameName == null) {
                repository.loadChannelClipsGQL(it.clientId, it.channelLogin, period, viewModelScope)
            } else {
                repository.loadGameClipsGQL(it.clientId, it.gameName, period, viewModelScope)
            }
        }
    }
    val period: Period
        get() = filter.value!!.period
    val languageIndex: Int
        get() = filter.value!!.languageIndex

    init {
        _sortText.value = context.getString(R.string.sort_and_period, context.getString(R.string.view_count), context.getString(R.string.this_week))
    }

    fun loadClips(useHelix: Boolean, clientId: String?, channelId: String? = null, channelLogin: String? = null, gameId: String? = null, gameName: String? = null, token: String? = null) {
        if (filter.value == null) {
            filter.value = Filter(useHelix = useHelix, clientId = clientId, token = token, channelId = channelId, channelLogin = channelLogin, gameId = gameId, gameName = gameName)
        } else {
            filter.value?.copy(useHelix = useHelix, clientId = clientId, token = token, channelId = channelId, channelLogin = channelLogin, gameId = gameId).let {
                if (filter.value != it)
                    filter.value = it
            }
        }
    }

    fun filter(useHelix: Boolean, clientId: String?, period: Period, languageIndex: Int, text: CharSequence, token: String? = null) {
        filter.value = filter.value?.copy(useHelix = useHelix, clientId = clientId, token = token, period = period, languageIndex = languageIndex)
        _sortText.value = text
    }

    private data class Filter(
        val useHelix: Boolean,
        val clientId: String?,
        val token: String?,
        val channelId: String?,
        val channelLogin: String?,
        val gameId: String?,
        val gameName: String?,
        val period: Period = Period.WEEK,
        val languageIndex: Int = 0)
}
