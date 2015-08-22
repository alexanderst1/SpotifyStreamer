package com.example.android.spotifystreamer;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

//import com.example.android.sunshine.app.utils.PollingCheck;

import java.util.Map;
import java.util.Set;

/*
    Students: These are functions and some test data to make it easier to test your database and
    Content Provider.  Note that you'll want your WeatherContract class to exactly match the one
    in our solution to use these as-given.
 */
public class TestUtilities extends AndroidTestCase {

    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }

    static ContentValues createArtistValues() {
        ContentValues values = new ContentValues();
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID, "abcdefgh");
        values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME, "Tom");
        return values;
    }
    static ContentValues createTrackValues(long artistID) {
        ContentValues values = new ContentValues();
        values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_SPOTIFY_ID, "zxcvbnm");
        values.put(SpotifyContract.TrackEntry.COLUMN_ARTIST_ID, artistID);
        values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_NAME, "track1");
        values.put(SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME, "album1");
        return values;
    }
    static ContentValues createArtistImageValues(long artistID) {
        ContentValues values = new ContentValues();
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, artistID);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, "URI_for_image_1");
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, 200L);
        values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, 100L);
        return values;
    }
    static ContentValues createTrackImageValues(long trackID) {
        ContentValues values = new ContentValues();
        values.put(SpotifyContract.TrackImageEntry.COLUMN_TRACK_ID, trackID);
        values.put(SpotifyContract.TrackImageEntry.COLUMN_URI, "URI_for_image_1");
        values.put(SpotifyContract.TrackImageEntry.COLUMN_WIDTH, 200L);
        values.put(SpotifyContract.TrackImageEntry.COLUMN_HEIGHT, 100L);
        return values;
    }

    static long insertArtistValues(Context context) {
        // insert our test records into the database
        SpotifyDbHelper dbHelper = new SpotifyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createArtistValues();

        long rowID;
        rowID = db.insert(SpotifyContract.ArtistEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert Artist test values", rowID != -1);

        return rowID;
    }

    /*
        Students: The functions we provide inside of TestProvider use this utility class to test
        the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
        CTS tests.

        Note that this only tests that the onChange function is called; it does not test that the
        correct Uri is returned.
     */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }
}
