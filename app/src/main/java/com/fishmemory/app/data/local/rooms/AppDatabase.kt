package com.fishmemory.app.data.local.rooms

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fishmemory.app.data.local.rooms.dao.ArticleDao
import com.fishmemory.app.data.local.rooms.dao.DraftDao
import com.fishmemory.app.data.local.rooms.dao.LocalArticleDao
import com.fishmemory.app.data.local.rooms.entity.ArticleEntity
import com.fishmemory.app.data.local.rooms.entity.DraftEntity
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity

@Database(
    entities = [ArticleEntity::class, DraftEntity::class, LocalArticleEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun draftDao(): DraftDao
    abstract fun localArticleDao(): LocalArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fishmemory_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS drafts (
                      draft_id TEXT NOT NULL,
                      title TEXT NOT NULL,
                      content_json TEXT NOT NULL,
                      status TEXT NOT NULL,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      last_opened_at INTEGER NOT NULL,
                      word_count INTEGER NOT NULL,
                      preview TEXT NOT NULL,
                      PRIMARY KEY(draft_id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_drafts_status ON drafts(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_drafts_updated_at ON drafts(updated_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_drafts_last_opened_at ON drafts(last_opened_at)")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_articles (
                      localId TEXT NOT NULL,
                      title TEXT NOT NULL,
                      authorName TEXT NOT NULL,
                      publishTimeMs INTEGER NOT NULL,
                      coverUrl TEXT,
                      summary TEXT,
                      blocksJson TEXT NOT NULL,
                      readCount INTEGER NOT NULL,
                      likeCount INTEGER NOT NULL,
                      commentCount INTEGER NOT NULL,
                      status TEXT NOT NULL,
                      PRIMARY KEY(localId)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}