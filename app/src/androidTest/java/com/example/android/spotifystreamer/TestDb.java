package com.example.android.spotifystreamer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import junit.framework.Assert;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(SpotifyDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(SpotifyContract.ArtistEntry.TABLE_NAME);
        tableNameHashSet.add(SpotifyContract.TrackEntry.TABLE_NAME);
        tableNameHashSet.add(SpotifyContract.ArtistImageEntry.TABLE_NAME);
        tableNameHashSet.add(SpotifyContract.TrackImageEntry.TABLE_NAME);

        mContext.deleteDatabase(SpotifyDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new SpotifyDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without required tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?

        final HashSet<String> columnNameHashSet = new HashSet<String>();

        columnNameHashSet.add(SpotifyContract.ArtistEntry._ID);
        columnNameHashSet.add(SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID);
        columnNameHashSet.add(SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME);
        verifyColumnsExist(SpotifyContract.ArtistEntry.TABLE_NAME, columnNameHashSet, db);
        columnNameHashSet.clear();

        columnNameHashSet.add(SpotifyContract.TrackEntry._ID);
        columnNameHashSet.add(SpotifyContract.TrackEntry.COLUMN_TRACK_SPOTIFY_ID);
        columnNameHashSet.add(SpotifyContract.TrackEntry.COLUMN_ARTIST_ID);
        columnNameHashSet.add(SpotifyContract.TrackEntry.COLUMN_TRACK_NAME);
        columnNameHashSet.add(SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME);
        verifyColumnsExist(SpotifyContract.TrackEntry.TABLE_NAME, columnNameHashSet, db);
        columnNameHashSet.clear();

        columnNameHashSet.add(SpotifyContract.ArtistImageEntry._ID);
        columnNameHashSet.add(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID);
        columnNameHashSet.add(SpotifyContract.ArtistImageEntry.COLUMN_URI);
        columnNameHashSet.add(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT);
        columnNameHashSet.add(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH);
        verifyColumnsExist(SpotifyContract.ArtistImageEntry.TABLE_NAME, columnNameHashSet, db);
        columnNameHashSet.clear();

        columnNameHashSet.add(SpotifyContract.TrackImageEntry._ID);
        columnNameHashSet.add(SpotifyContract.TrackImageEntry.COLUMN_TRACK_ID);
        columnNameHashSet.add(SpotifyContract.TrackImageEntry.COLUMN_URI);
        columnNameHashSet.add(SpotifyContract.TrackImageEntry.COLUMN_HEIGHT);
        columnNameHashSet.add(SpotifyContract.TrackImageEntry.COLUMN_WIDTH);
        verifyColumnsExist(SpotifyContract.TrackImageEntry.TABLE_NAME, columnNameHashSet, db);
        columnNameHashSet.clear();

        db.close();
    }

    void verifyColumnsExist(String tableName, HashSet<String> columnNames, SQLiteDatabase db){
        Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());
        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            columnNames.remove(columnName);
        } while(c.moveToNext());
        // if this fails, it means that table doesn't contain all of the required columns
        assertTrue("Error: Table " + tableName + " doesn't contain all of the required columns",
                columnNames.isEmpty());
    }

    public void testNew() {
        SpotifyDbHelper dbHelper = new SpotifyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        long rowID = -1;

        values.clear();
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID, "abcdefgh");
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME, "Tom");
        rowID = db.insert(SpotifyContract.ArtistEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);

        values.clear();
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID, "zxcvbbnm");
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME, "Jerry");
        rowID = db.insert(SpotifyContract.ArtistEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);

        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 1L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img11.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 100L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 110L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 1L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img12.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 200L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 210L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 1L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img13.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 300L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 310L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 1L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img14.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 400L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 410L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);

        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 2L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img21.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 100L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 110L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 2L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img22.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 200L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 210L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 2L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img23.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 300L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 310L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
        assertTrue(rowID != -1);
        values.clear();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, 2L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "http://example.com/img24.jpg");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 400L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 410L);
        rowID = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);

        Cursor c;

        c = db.rawQuery("select * from artist_images", null);
        printCursorToLog(c);
        c.close();

        long dimImg = 240;
        float ratio = 0.9f;
        final String dim = SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT;

        //final String tblImg = SpotifyContract.ArtistImageEntry.TABLE_NAME;

        //If artist_images table contains images with size more than the 90% 0f size of image placeholder
        //then pick up from those image the one with size closest to the size of image placeholder
        //Otherwise pick up any image with size closest to the size of image placeholder
        //e.g. for placeholder with height = 240, from available images with width 100, 200, 300, 400,
        //an image with width 300 will be picked up. Even though 200 is close to 240 than 300, we don't
        //want image upscaling as it looses quality. We can tolerate upscaling by no more than 10%
        //that's why we have added ratio 0.9

        //SELECT *, (SELECT width FROM artist_images WHERE
        //(CASE (SELECT COUNT(*) FROM artist_images WHERE width>0.9*240)>0 WHEN 1 THEN width>0.9*240 ELSE 1 END)
        //ORDER BY ABS(width-240)) FROM ARTIST

        String[] columns = new String[2];
        columns[0] = "*";
        columns[1] = "(select " + SpotifyContract.ArtistImageEntry.COLUMN_URI + " from " + SpotifyContract.ArtistImageEntry.TABLE_NAME + " where " +
        "(case (select count(*) from " + SpotifyContract.ArtistImageEntry.TABLE_NAME + " where " + dim + " > " + dimImg * ratio
                + ") > 0 when 1" + " then " + dim + " > " + dimImg * ratio + " else 1 end) " +
                "order by abs(" + dim + "-" + dimImg + ") LIMIT 1) as uri";

        c = db.query(SpotifyContract.ArtistEntry.TABLE_NAME, columns, null, null, null, null, null);
        printCursorToLog(c);
        c.close();

        rowID = 1;
        values.clear();
        values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_SPOTIFY_ID, "zxcvbnm");
        values.put(SpotifyContract.TrackEntry.COLUMN_ARTIST_ID, rowID);
        values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_NAME, "track1");
        values.put(SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME, "album1");
        rowID = db.insert("tracks", null, values);
        assertTrue(rowID != -1);

        db.close();
    }

    void printCursorToLog(Cursor c)
    {
        if (c != null && c.moveToFirst()) {
            do {
                String s = "";
                for (int i = 0; i < c.getColumnCount(); i++) {
                    s += c.getString(i) + "\t\t";
                }
                Log.d(LOG_TAG, s);
            }while (c.moveToNext());
        }
    }

    public void testArtistTable() {
        insertArtistValues();
    }
    public void testTrackTable() {
        insertTrackValues();
    }
    public void testArtistImageTable() {
        final String tableName = SpotifyContract.ArtistImageEntry.TABLE_NAME;
        long artistRowId = insertArtistValues();
        ContentValues values = TestUtilities.createArtistImageValues(artistRowId);
        insertValues(tableName, values);
    }
    public void testTrackImageTable() {
        final String tableName = SpotifyContract.TrackImageEntry.TABLE_NAME;
        long trackRowId = insertTrackValues();
        ContentValues values = TestUtilities.createTrackImageValues(trackRowId);
        insertValues(tableName, values);
    }

    long insertArtistValues() {
        final String tableName = SpotifyContract.ArtistEntry.TABLE_NAME;
        ContentValues values = TestUtilities.createArtistValues();
        long rowId = insertValues(tableName, values);
        assertFalse("Error: " + tableName + " not inserted correctly", rowId == -1L);
        return rowId;
    }
    long insertTrackValues() {
        final String tableName = SpotifyContract.TrackEntry.TABLE_NAME;
        long artistRowId = insertArtistValues();
        ContentValues values =  TestUtilities.createTrackValues(artistRowId);
        long rowId = insertValues(tableName, values);
        assertFalse("Error: " + tableName + " not inserted correctly", rowId == -1L);
        return rowId;
    }
    long insertValues(String tableName, ContentValues values) {
        SpotifyDbHelper dbHelper = new SpotifyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowID = db.insert(tableName, null, values);
        assertTrue(rowID != -1);
        Cursor cursor = db.query(
                tableName,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        assertTrue( "Error: no records returned from " + tableName + " query", cursor.moveToFirst() );
        TestUtilities.validateCurrentRecord("Error: " + tableName + " query validation failed",
                cursor, values);
        assertFalse( "Error: more than one record returned from " + tableName + " query",
                cursor.moveToNext() );
        cursor.close();
        db.close();
        return rowID;
    }
}