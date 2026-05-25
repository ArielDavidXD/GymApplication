package com.arieldavid.gymapplication.local.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var INSTANCE: ClientDatabase? = null

    fun getDatabase(context: Context): ClientDatabase {

        if (INSTANCE == null) {

            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                ClientDatabase::class.java,
                "gym_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        return INSTANCE!!
    }
}