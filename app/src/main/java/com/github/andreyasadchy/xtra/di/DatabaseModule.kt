package com.github.andreyasadchy.xtra.di

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.andreyasadchy.xtra.db.*
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import dagger.Module
import dagger.Provides
import java.util.*
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun providesRepository(videosDao: VideosDao, requestsDao: RequestsDao): OfflineRepository = OfflineRepository(videosDao, requestsDao)

    @Singleton
    @Provides
    fun providesVideosDao(database: AppDatabase): VideosDao = database.videos()

    @Singleton
    @Provides
    fun providesRequestsDao(database: AppDatabase): RequestsDao = database.requests()

    @Singleton
    @Provides
    fun providesRecentEmotesDao(database: AppDatabase): RecentEmotesDao = database.recentEmotes()

    @Singleton
    @Provides
    fun providesVideoPositions(database: AppDatabase): VideoPositionsDao = database.videoPositions()

    @Singleton
    @Provides
    fun providesLocalFollows(database: AppDatabase): LocalFollowsDao = database.localFollows()

    @Singleton
    @Provides
    fun providesAppDatabase(application: Application): AppDatabase =
        Room.databaseBuilder(application, AppDatabase::class.java, "database")
            .addMigrations(
                object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE videos1 (id INTEGER NOT NULL, url TEXT NOT NULL, name TEXT NOT NULL, channel_id TEXT, channel_login TEXT, channel_name TEXT NOT NULL, channel_logo TEXT NOT NULL, thumbnail TEXT NOT NULL, game TEXT NOT NULL, duration INTEGER NOT NULL, upload_date INTEGER NOT NULL, download_date INTEGER NOT NULL, is_vod INTEGER NOT NULL, downloaded INTEGER NOT NULL, PRIMARY KEY (id))")
                        val cursor = database.query("SELECT * FROM videos")
                        while (cursor.moveToNext()) {
                            val values = ContentValues().apply {
                                put("id", cursor.getInt(0))
                                put("url", cursor.getString(2))
                                put("name", cursor.getString(3))
                                put("channel_id", cursor.getString(4))
                                put("channel_login", cursor.getString(5))
                                put("channel_name", cursor.getString(6))
                                put("channel_logo", cursor.getString(11))
                                put("thumbnail", cursor.getString(10))
                                put("game", cursor.getString(7))
                                put("duration", cursor.getLong(8))
                                put("upload_date", TwitchApiHelper.parseIso8601Date(cursor.getString(9)))
                                put("download_date", Calendar.getInstance().time.time)
                                put("is_vod", cursor.getInt(1))
                                put("downloaded", 1)
                            }
                            database.insert("videos1", SQLiteDatabase.CONFLICT_NONE, values)
                        }
                        cursor.close()
                        database.execSQL("DROP TABLE videos")
                        database.execSQL("ALTER TABLE videos1 RENAME TO videos")
                    }
                },
                object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS emotes (id INTEGER NOT NULL, code TEXT NOT NULL, PRIMARY KEY (id))")
                    }
                },
                object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE videos1 (id INTEGER NOT NULL, url TEXT NOT NULL, source_url TEXT NOT NULL, source_start_position INTEGER, name TEXT NOT NULL, channel_id TEXT, channel_login TEXT, channel_name TEXT NOT NULL, channel_logo TEXT NOT NULL, thumbnail TEXT NOT NULL, game TEXT NOT NULL, duration INTEGER NOT NULL, upload_date INTEGER NOT NULL, download_date INTEGER NOT NULL, is_vod INTEGER NOT NULL, downloaded INTEGER NOT NULL, last_watch_position INTEGER NOT NULL, PRIMARY KEY (id))")
                        val cursor = database.query("SELECT * FROM videos")
                        while (cursor.moveToNext()) {
                            val values = ContentValues().apply {
                                put("id", cursor.getInt(0))
                                put("url", cursor.getString(3))
                                put("source_url", "")
                                putNull("source_start_position")
                                put("name", cursor.getString(4))
                                put("channel_id", cursor.getString(5))
                                put("channel_login", cursor.getString(6))
                                put("channel_name", cursor.getString(7))
                                put("channel_logo", cursor.getString(8))
                                put("thumbnail", cursor.getString(9))
                                put("game", cursor.getString(10))
                                put("duration", cursor.getLong(11) * 1000L)
                                put("upload_date", cursor.getLong(12))
                                put("download_date", cursor.getLong(13))
                                put("is_vod", cursor.getInt(1))
                                put("downloaded", cursor.getInt(2))
                                put("last_watch_position", 0L)
                            }
                            database.insert("videos1", SQLiteDatabase.CONFLICT_NONE, values)
                        }
                        cursor.close()
                        database.execSQL("DROP TABLE videos")
                        database.execSQL("ALTER TABLE videos1 RENAME TO videos")
                    }
                },
                object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS requests (offline_video_id INTEGER NOT NULL, url TEXT NOT NULL, path TEXT NOT NULL, video_id TEXT, segment_from INTEGER, segment_to INTEGER, PRIMARY KEY (offline_video_id), FOREIGN KEY('offline_video_id') REFERENCES videos('id') ON DELETE CASCADE)")

                        database.execSQL("CREATE TABLE videos1 (id INTEGER NOT NULL, url TEXT NOT NULL, source_url TEXT NOT NULL, source_start_position INTEGER, name TEXT NOT NULL, channel_id TEXT, channel_login TEXT, channel_name TEXT NOT NULL, channel_logo TEXT NOT NULL, thumbnail TEXT NOT NULL, game TEXT NOT NULL, duration INTEGER NOT NULL, upload_date INTEGER NOT NULL, download_date INTEGER NOT NULL, is_vod INTEGER NOT NULL, last_watch_position INTEGER NOT NULL, progress INTEGER NOT NULL, max_progress INTEGER NOT NULL, status INTEGER NOT NULL, PRIMARY KEY (id))")
                        val cursor = database.query("SELECT * FROM videos")
                        while (cursor.moveToNext()) {
                            val values = ContentValues().apply {
                                put("id", cursor.getInt(0))
                                put("url", cursor.getString(4))
                                put("source_url", cursor.getString(5))
                                put("source_start_position", cursor.getLong(6))
                                put("name", cursor.getString(7))
                                put("channel_id", cursor.getString(8))
                                put("channel_login", cursor.getString(9))
                                put("channel_name", cursor.getString(10))
                                put("channel_logo", cursor.getString(11))
                                put("thumbnail", cursor.getString(12))
                                put("game", cursor.getString(13))
                                put("duration", cursor.getLong(14))
                                put("upload_date", cursor.getLong(15))
                                put("download_date", cursor.getLong(16))
                                put("last_watch_position", cursor.getLong(3))
                                put("progress", 0)
                                put("max_progress", 0)
                                put("status", 2)
                                put("is_vod", cursor.getInt(1))
                            }
                            database.insert("videos1", SQLiteDatabase.CONFLICT_NONE, values)
                        }
                        cursor.close()
                        database.execSQL("DROP TABLE videos")
                        database.execSQL("ALTER TABLE videos1 RENAME TO videos")
                    }
                },
                object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS recent_emotes (name TEXT NOT NULL, url TEXT NOT NULL, used_at INTEGER NOT NULL, PRIMARY KEY (name))")
                    }
                },
                object : Migration(6, 7) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS video_positions (id INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (id))")
                    }
                },
                object : Migration(7, 8) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE emotes1 (id INTEGER NOT NULL, code TEXT NOT NULL COLLATE NOCASE, PRIMARY KEY (id))")
                        database.execSQL("INSERT INTO emotes1 SELECT * FROM emotes")
                        database.execSQL("DROP TABLE emotes")
                        database.execSQL("ALTER TABLE emotes1 RENAME TO emotes")
                    }
                },
                object : Migration(8, 9) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS local_follows (user_id TEXT NOT NULL, user_login TEXT, user_name TEXT, channelLogo TEXT)")
                    }
                }
            )
            .build()
}
