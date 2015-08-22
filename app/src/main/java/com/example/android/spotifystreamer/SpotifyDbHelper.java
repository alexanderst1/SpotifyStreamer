package com.example.android.spotifystreamer;

/**
 * Created by Alexander on 7/16/2015.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.android.spotifystreamer.SpotifyContract.ArtistEntry;
import com.example.android.spotifystreamer.SpotifyContract.ArtistImageEntry;
import com.example.android.spotifystreamer.SpotifyContract.TrackEntry;
import com.example.android.spotifystreamer.SpotifyContract.TrackImageEntry;

public class SpotifyDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    static final String DATABASE_NAME = "spotify.db";

    public SpotifyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_ARTIST_TABLE = "CREATE TABLE " + ArtistEntry.TABLE_NAME + " (" +
                ArtistEntry._ID + " INTEGER PRIMARY KEY," +
                ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID + " TEXT NOT NULL, " +
                ArtistEntry.COLUMN_ARTIST_NAME + " TEXT NOT NULL, " +
                " UNIQUE (" + ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_TRACK_TABLE = "CREATE TABLE " + TrackEntry.TABLE_NAME + " (" +
                TrackEntry._ID + " INTEGER PRIMARY KEY," +
                TrackEntry.COLUMN_TRACK_SPOTIFY_ID + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_ARTIST_ID + " INTEGER NOT NULL, " +
                TrackEntry.COLUMN_TRACK_NAME + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_ALBUM_NAME + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_IS_PLAYABLE + " INTEGER, " +
                TrackEntry.COLUMN_PREVIEW_URL + " TEXT, " +
                " FOREIGN KEY (" + TrackEntry.COLUMN_ARTIST_ID + ") REFERENCES " +
                ArtistEntry.TABLE_NAME + " (" + ArtistEntry._ID + "), " +
                " UNIQUE (" + TrackEntry.COLUMN_TRACK_SPOTIFY_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_ARTIST_IMAGES_TABLE = "CREATE TABLE " + ArtistImageEntry.TABLE_NAME + " (" +
                ArtistImageEntry._ID + " INTEGER PRIMARY KEY," +
                ArtistImageEntry.COLUMN_ARTIST_ID + " INTEGER NOT NULL, " +
                ArtistImageEntry.COLUMN_URI + " TEXT NOT NULL, " +
                ArtistImageEntry.COLUMN_WIDTH + " INTEGER NOT NULL, " +
                ArtistImageEntry.COLUMN_HEIGHT + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + ArtistImageEntry.COLUMN_ARTIST_ID + ") REFERENCES " +
                ArtistEntry.TABLE_NAME + " (" + ArtistEntry._ID + "), " +
                " UNIQUE (" + ArtistImageEntry.COLUMN_URI + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_TRACK_IMAGES_TABLE = "CREATE TABLE " + TrackImageEntry.TABLE_NAME + " (" +
                TrackImageEntry._ID + " INTEGER PRIMARY KEY," +
                TrackImageEntry.COLUMN_TRACK_ID + " INTEGER NOT NULL, " +
                TrackImageEntry.COLUMN_URI + " TEXT NOT NULL, " +
                TrackImageEntry.COLUMN_WIDTH + " INTEGER NOT NULL, " +
                TrackImageEntry.COLUMN_HEIGHT + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + TrackImageEntry.COLUMN_TRACK_ID + ") REFERENCES " +
                TrackEntry.TABLE_NAME + " (" + TrackEntry._ID + "), " +
                " UNIQUE (" + TrackImageEntry.COLUMN_URI + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_ARTIST_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRACK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ARTIST_IMAGES_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRACK_IMAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 4 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ArtistEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TrackEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ArtistImageEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TrackImageEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
