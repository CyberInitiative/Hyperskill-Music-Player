package org.hyperskill.musicplayer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

const val TAG: String = "DatabaseHelper"

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "musicPlayerDatabase.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_PLAYLIST = "playlist"
        private const val COLUMN_PLAYLIST_NAME = "playlistName"
        private const val COLUMN_SONG_ID = "songId"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_PLAYLIST (
                $COLUMN_PLAYLIST_NAME TEXT NOT NULL,
                $COLUMN_SONG_ID INTEGER NOT NULL,
                PRIMARY KEY ($COLUMN_PLAYLIST_NAME, $COLUMN_SONG_ID)
            );
        """.trimIndent()

        if (db != null) {
            db.execSQL(createTableQuery)
        } else {
            Log.e(TAG, "onCreate; db is null")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db != null) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST")
        } else {
            Log.e(TAG, "onUpgrade; db is null")
        }
        onCreate(db)
    }

    fun insertOrReplacePlaylist(playlistName: String, songIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            deletePlaylist(playlistName) // Remove existing playlist if it exists
            val insertQuery = "INSERT INTO $TABLE_PLAYLIST ($COLUMN_PLAYLIST_NAME, $COLUMN_SONG_ID) VALUES (?, ?)"
            val statement = db.compileStatement(insertQuery)

            for (songId in songIds) {
                statement.clearBindings()
                statement.bindString(1, playlistName)
                statement.bindLong(2, songId.toLong())
                statement.executeInsert()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllPlaylistNames(): List<String> {
        val db = readableDatabase
        val query = "SELECT DISTINCT $COLUMN_PLAYLIST_NAME FROM $TABLE_PLAYLIST"
        val cursor = db.rawQuery(query, null)

        val playlistNames = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                val playlistName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_NAME))
                playlistNames.add(playlistName)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return playlistNames
    }

    fun addSongToPlaylist(playlistName: String, songId: Int) {
        val db = writableDatabase
        val query = """
            SELECT COUNT(*) FROM $TABLE_PLAYLIST
            WHERE $COLUMN_PLAYLIST_NAME = ? AND $COLUMN_SONG_ID = ?
        """
        val cursor = db.rawQuery(query, arrayOf(playlistName, songId.toString()))

        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        if (count == 0) { // Song is not in the playlist
            val insertQuery = "INSERT INTO $TABLE_PLAYLIST ($COLUMN_PLAYLIST_NAME, $COLUMN_SONG_ID) VALUES (?, ?)"
            val statement = db.compileStatement(insertQuery)
            statement.bindString(1, playlistName)
            statement.bindLong(2, songId.toLong())
            statement.executeInsert()
        }
    }

    fun deletePlaylist(playlistName: String) {
        val db = writableDatabase
        db.delete(TABLE_PLAYLIST, "$COLUMN_PLAYLIST_NAME = ?", arrayOf(playlistName))
    }

    fun getPlaylist(playlistName: String): List<Long> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PLAYLIST,
            arrayOf(COLUMN_SONG_ID),
            "$COLUMN_PLAYLIST_NAME = ?",
            arrayOf(playlistName),
            null,
            null,
            null
        )

        val songIds = mutableListOf<Long>()
        if (cursor.moveToFirst()) {
            do {
                val songId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SONG_ID))
                songIds.add(songId)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return songIds
    }

    fun clearAll() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_PLAYLIST")
    }

}