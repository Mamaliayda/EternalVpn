package com.eternalvpn.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eternalvpn.app.data.model.Node
import com.eternalvpn.app.data.model.ScanResult
import com.eternalvpn.app.data.model.Subscription

@Database(
    entities = [Node::class, Subscription::class, ScanResult::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "eternalvpn.db",
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
