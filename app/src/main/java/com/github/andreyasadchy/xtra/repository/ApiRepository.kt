package com.github.andreyasadchy.xtra.repository

import android.util.Log
import androidx.paging.PagedList
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.api.MiscApi
import com.github.andreyasadchy.xtra.model.chat.CheerEmote
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.model.chat.VideoMessagesResponse
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelSearch
import com.github.andreyasadchy.xtra.model.helix.channel.ChannelViewerList
import com.github.andreyasadchy.xtra.model.helix.clip.Clip
import com.github.andreyasadchy.xtra.model.helix.follows.Follow
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.model.helix.tag.Tag
import com.github.andreyasadchy.xtra.model.helix.user.User
import com.github.andreyasadchy.xtra.model.helix.video.BroadcastType
import com.github.andreyasadchy.xtra.model.helix.video.Period
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.repository.datasource.*
import com.github.andreyasadchy.xtra.repository.datasourceGQL.*
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ApiRepository"

@Singleton
class ApiRepository @Inject constructor(
    private val api: HelixApi,
    private val gql: GraphQLRepository,
    private val misc: MiscApi,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val localFollowsGame: LocalFollowGameRepository,
    private val offlineRepository: OfflineRepository) : TwitchService {

    override fun loadTopGames(clientId: String?, userToken: String?, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = GamesDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadGame(clientId: String?, userToken: String?, gameId: String): Game? = withContext(Dispatchers.IO) {
        val gameIds = mutableListOf<String>()
        gameIds.add(gameId)
        api.getGames(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, gameIds).data?.firstOrNull()
    }

    override suspend fun loadStream(clientId: String?, userToken: String?, channelId: String): Stream? = withContext(Dispatchers.IO) {
        val userIds = mutableListOf<String>()
        userIds.add(channelId)
        api.getStreams(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userIds).data?.firstOrNull()
    }

    override fun loadTopStreams(clientId: String?, userToken: String?, gameId: String?, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = StreamsDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, gameId, api, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedStreams(useHelix: Boolean, gqlClientId: String?, helixClientId: String?, userToken: String?, userId: String, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = FollowedStreamsDataSource.Factory(useHelix, localFollowsChannel, this, gqlClientId, helixClientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userId, api, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(10)
                    .setInitialLoadSizeHint(15)
                    .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                    .setInitialLoadSizeHint(30)
                    .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadClips(clientId: String?, userToken: String?, channelId: String?, channelLogin: String?, gameId: String?, started_at: String?, ended_at: String?, coroutineScope: CoroutineScope): Listing<Clip> {
        val factory = ClipsDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, channelId, channelLogin, gameId, started_at, ended_at, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadVideo(clientId: String?, userToken: String?, videoId: String): Video? = withContext(Dispatchers.IO) {
        api.getVideo(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, videoId).data?.firstOrNull()
    }

    override fun loadVideos(clientId: String?, userToken: String?, gameId: String?, period: Period, broadcastType: BroadcastType, language: String?, sort: Sort, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = VideosDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, gameId, period, broadcastType, language, sort, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override fun loadChannelVideos(clientId: String?, userToken: String?, channelId: String, period: Period, broadcastType: BroadcastType, sort: Sort, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = ChannelVideosDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, channelId, period, broadcastType, sort, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadUserById(clientId: String?, userToken: String?, id: String): User? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading user by id $id")
        val userIds = mutableListOf<String>()
        userIds.add(id)
        api.getUserById(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userIds).data?.firstOrNull()
    }

    override fun loadSearchGames(clientId: String?, userToken: String?, query: String, coroutineScope: CoroutineScope): Listing<Game> {
        Log.d(TAG, "Loading games containing: $query")
        val factory = GamesSearchDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, query, api, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(15)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchChannels(clientId: String?, userToken: String?, query: String, coroutineScope: CoroutineScope): Listing<ChannelSearch> {
        Log.d(TAG, "Loading channels containing: $query")
        val factory = ChannelsSearchDataSource.Factory(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, query, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(15)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(5)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadUserFollows(clientId: String?, userToken: String?, userId: String, channelId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading if user is following channel $channelId")
        api.getUserFollows(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userId, channelId).total == 1
    }

    override fun loadFollowedChannels(clientId: String?, userToken: String?, userId: String, coroutineScope: CoroutineScope): Listing<Follow> {
        val factory = FollowedChannelsDataSource.Factory(localFollowsChannel, offlineRepository, clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userId, api, coroutineScope)
        val config = PagedList.Config.Builder()
                .setPageSize(40)
                .setInitialLoadSizeHint(40)
                .setPrefetchDistance(10)
                .setEnablePlaceholders(false)
                .build()
        return Listing.create(factory, config)
    }

    override fun loadFollowedGames(gqlClientId: String?, helixClientId: String?, userToken: String?, userId: String, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = FollowedGamesDataSource.Factory(localFollowsGame, gqlClientId, helixClientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userId, api, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(40)
            .setInitialLoadSizeHint(40)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override suspend fun loadEmotesFromSet(clientId: String?, userToken: String?, setIds: List<String>): List<TwitchEmote>? = withContext(Dispatchers.IO) {
        api.getEmotesFromSet(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, setIds).emotes
    }

    override suspend fun loadCheerEmotes(clientId: String?, userToken: String?, userId: String): List<CheerEmote> = withContext(Dispatchers.IO) {
        api.getCheerEmotes(clientId, userToken?.let { TwitchApiHelper.addTokenPrefix(it) }, userId).emotes
    }

    override suspend fun loadVideoChatLog(clientId: String?, videoId: String, offsetSeconds: Double): VideoMessagesResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading chat log for video $videoId. Offset in seconds: $offsetSeconds")
        misc.getVideoChatLog(clientId, videoId, offsetSeconds, 100)
    }

    override suspend fun loadVideoChatAfter(clientId: String?, videoId: String, cursor: String): VideoMessagesResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading chat log for video $videoId. Cursor: $cursor")
        misc.getVideoChatLogAfter(clientId, videoId, cursor, 100)
    }


    override suspend fun loadVodGamesGQL(clientId: String?, videoId: String?): List<Game> = withContext(Dispatchers.IO) {
        gql.loadVodGames(clientId, videoId).data
    }

    override suspend fun loadChannelViewerListGQL(clientId: String?, channelLogin: String?): ChannelViewerList = withContext(Dispatchers.IO) {
        gql.loadChannelViewerList(clientId, channelLogin).data
    }

    override fun loadTagsGQL(clientId: String?, getGameTags: Boolean, gameId: String?, gameName: String?, query: String?, coroutineScope: CoroutineScope): Listing<Tag> {
        val factory = TagsDataSourceGQL.Factory(clientId, getGameTags, gameId, gameName, query, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10000)
            .setInitialLoadSizeHint(10000)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }
    override fun loadTopGamesGQL(clientId: String?, tags: List<String>?, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = GamesDataSourceGQL.Factory(clientId, tags, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(30)
            .setInitialLoadSizeHint(30)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadTopStreamsGQL(clientId: String?, tags: List<String>?, thumbnailsEnabled: Boolean, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = StreamsDataSourceGQL.Factory(clientId, tags, gql, coroutineScope)
        val builder = PagedList.Config.Builder().setEnablePlaceholders(false)
        if (thumbnailsEnabled) {
            builder.setPageSize(10)
                .setInitialLoadSizeHint(15)
                .setPrefetchDistance(3)
        } else {
            builder.setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setPrefetchDistance(10)
        }
        val config = builder.build()
        return Listing.create(factory, config)
    }

    override fun loadGameStreamsGQL(clientId: String?, gameName: String?, tags: List<String>?, coroutineScope: CoroutineScope): Listing<Stream> {
        val factory = GameStreamsDataSourceGQL.Factory(clientId, gameName, tags, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadGameVideosGQL(clientId: String?, gameName: String?, type: String?, sort: String?, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = GameVideosDataSourceGQL.Factory(clientId, gameName, type, sort, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadGameClipsGQL(clientId: String?, gameName: String?, sort: String?, coroutineScope: CoroutineScope): Listing<Clip> {
        val factory = GameClipsDataSourceGQL.Factory(clientId, gameName, sort, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadChannelVideosGQL(clientId: String?, channelLogin: String?, type: String?, sort: String?, coroutineScope: CoroutineScope): Listing<Video> {
        val factory = ChannelVideosDataSourceGQL.Factory(clientId, channelLogin, type, sort, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadChannelClipsGQL(clientId: String?, channelLogin: String?, sort: String?, coroutineScope: CoroutineScope): Listing<Clip> {
        val factory = ChannelClipsDataSourceGQL.Factory(clientId, channelLogin, sort, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(3)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchChannelsGQL(clientId: String?, query: String, coroutineScope: CoroutineScope): Listing<ChannelSearch> {
        val factory = SearchChannelsDataSourceGQL.Factory(clientId, query, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(15)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }

    override fun loadSearchGamesGQL(clientId: String?, query: String, coroutineScope: CoroutineScope): Listing<Game> {
        val factory = SearchGamesDataSourceGQL.Factory(clientId, query, gql, coroutineScope)
        val config = PagedList.Config.Builder()
            .setPageSize(15)
            .setInitialLoadSizeHint(15)
            .setPrefetchDistance(5)
            .setEnablePlaceholders(false)
            .build()
        return Listing.create(factory, config)
    }
}