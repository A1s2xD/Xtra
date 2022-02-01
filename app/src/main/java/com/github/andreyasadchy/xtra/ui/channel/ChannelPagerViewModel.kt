package com.github.andreyasadchy.xtra.ui.channel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.GlideApp
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.repository.TwitchService
import com.github.andreyasadchy.xtra.ui.common.follow.FollowLiveData
import com.github.andreyasadchy.xtra.ui.common.follow.FollowViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChannelPagerViewModel @Inject constructor(
    private val repository: TwitchService,
    private val gql: GraphQLRepository,
    private val localFollows: LocalFollowRepository,
    private val offlineRepository: OfflineRepository) : ViewModel(), FollowViewModel {

    private val _stream = MutableLiveData<Stream?>()
    val stream: MutableLiveData<Stream?>
        get() = _stream

    private val _userId = MutableLiveData<String?>()
    private val _userLogin = MutableLiveData<String?>()
    private val _userName = MutableLiveData<String?>()
    private val _profileImageURL = MutableLiveData<String?>()
    override val userId: String?
        get() { return _userId.value }
    override val userLogin: String?
        get() { return _userLogin.value }
    override val userName: String?
        get() { return _userName.value }
    override val channelLogo: String?
        get() { return _profileImageURL.value }
    override lateinit var follow: FollowLiveData

    override fun setUser(user: User, clientId: String?) {
        if (!this::follow.isInitialized) {
            follow = FollowLiveData(localFollows, userId, userLogin, userName, channelLogo, repository, clientId, user, viewModelScope)
        }
    }

    fun loadStream(useHelix: Boolean, clientId: String?, token: String? = null, channelId: String?, channelLogin: String?, channelName: String?, profileImageURL: String?) {
        if (useHelix && _userId.value != channelId && channelId != null || !useHelix && _userLogin.value != channelLogin && channelLogin != null) {
            _userId.value = channelId
            _userLogin.value = channelLogin
            _userName.value = channelName
            _profileImageURL.value = profileImageURL
            viewModelScope.launch {
                try {
                    val stream = if (useHelix) {
                        val get = repository.loadStream(clientId, token, channelId!!)
                        if (profileImageURL == null) {
                            get?.profileImageURL = repository.loadUserById(clientId, token, channelId)?.profile_image_url
                        }
                        get
                    } else {
                        Stream(user_id = userId, user_login = userLogin, user_name = userName, profileImageURL = profileImageURL, viewer_count = gql.loadViewerCount(clientId, channelLogin).viewers)
                    }
                    _stream.postValue(stream)
                } catch (e: Exception) {

                }
            }
        }
    }

    fun retry(useHelix: Boolean, clientId: String?, token: String? = null) {
        if (_stream.value == null) {
            loadStream(useHelix, clientId, token, _userId.value, _userLogin.value, _userName.value, _profileImageURL.value)
        }
    }

    fun updateLocalUser(context: Context, stream: Stream) {
        GlobalScope.launch {
            try {
                if (stream.user_id != null) {
                    val glide = GlideApp.with(context)
                    val downloadedLogo: String? = try {
                        glide.downloadOnly().load(stream.channelLogo).submit().get().absolutePath
                    } catch (e: Exception) {
                        stream.channelLogo
                    }
                    localFollows.getFollowById(stream.user_id)?.let { localFollows.updateFollow(it.apply {
                        user_login = stream.user_login
                        user_name = stream.user_name
                        channelLogo = downloadedLogo }) }
                    for (i in offlineRepository.getVideosByUserId(stream.user_id.toInt())) {
                        offlineRepository.updateVideo(i.apply {
                            channelLogin = stream.user_login
                            channelName = stream.user_name
                            channelLogo = downloadedLogo })
                    }
                }
            } catch (e: Exception) {

            }
        }
    }
}
