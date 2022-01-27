package com.github.andreyasadchy.xtra.ui.streams.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import javax.inject.Inject

class StreamsViewModel @Inject constructor(
        private val repository: TwitchService) : PagedListViewModel<Stream>() {

    private val filter = MutableLiveData<Filter>()
    override val result: LiveData<Listing<Stream>> = Transformations.map(filter) {
        if (it.useHelix)
            repository.loadTopStreams(it.clientId, it.token, it.gameId, it.languages, it.thumbnailsEnabled, viewModelScope)
        else {
            if (it.gameName == null) {
                repository.loadTopStreamsGQL(it.clientId, it.thumbnailsEnabled, viewModelScope)
            } else {
                repository.loadGameStreamsGQL(it.clientId, it.gameName, viewModelScope)
            }
        }
    }

    fun loadStreams(useHelix: Boolean, clientId: String?, token: String? = null, channelId: String? = null, gameId: String? = null, gameName: String? = null, languages: String? = null, thumbnailsEnabled: Boolean = true) {
        Filter(useHelix, clientId, token, channelId, gameId, gameName, languages, thumbnailsEnabled).let {
            if (filter.value != it) {
                filter.value = it
            }
        }
    }

    private data class Filter(
            val useHelix: Boolean,
            val clientId: String?,
            val token: String?,
            val channelId: String?,
            val gameId: String?,
            val gameName: String?,
            val languages: String?,
            val thumbnailsEnabled: Boolean)
}
