package com.example.ciaran.songle

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import java.sql.Types.INTEGER

/**
 * Created by ciaran on 11/12/2017.
 */
class DatabaseHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "mydb") {

    companion object {
        private var instance: DatabaseHelper? = null

        @Synchronized
        private fun getInstance(ctx: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable("SongList", true,
                "Num" to org.jetbrains.anko.db.INTEGER + PRIMARY_KEY,
                "Artist" to TEXT,
                "Title" to TEXT,
                "Link" to TEXT)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable(
                "SongList",
                true
        )
    }



    // Access property for Context
    val Context.database: DatabaseHelper
        get() = DatabaseHelper.getInstance(applicationContext)
}