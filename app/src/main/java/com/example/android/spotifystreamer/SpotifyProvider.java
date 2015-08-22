package com.example.android.spotifystreamer;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Alexander on 7/29/2015.
 */
public class SpotifyProvider extends ContentProvider {

    public static final String LOG_TAG = SpotifyProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private SpotifyDbHelper mOpenHelper;

    static final int ARTIST = 100;
    static final int ARTIST_WITH_ID = 101;

    static final int ARTIST_IMAGE = 200;
    static final int ARTIST_IMAGE_WITH_ID = 201;

    static final int TRACK = 300;
    static final int TRACK_WITH_ID = 301;
    static final int TRACK_WITH_ID_AND_IMAGE_SIZE = 302;

    static final int TRACK_IMAGE = 400;
    static final int TRACK_IMAGE_WITH_ID = 401;

    @Override
    public boolean onCreate() {
        mOpenHelper = new SpotifyDbHelper(getContext());
        return true;
    }

    public Cursor getArtistData(Uri uri, String[] projection, String selection,
                                String[] selectionArgs, String sortOrder) {
        String colImageUri;

        String imgDimensionColumn = SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT;
        String value = uri.getQueryParameter(SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_HEIGHT);

        Long imgDimensionValue = null;
        if (value != null)
            imgDimensionValue = Long.parseLong(value);

        if (imgDimensionValue == null) {
            value = uri.getQueryParameter(SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_WIDTH);
            if (value != null)
                imgDimensionValue = Long.parseLong(value);
            if (imgDimensionValue != null) {
                imgDimensionColumn = SpotifyContract.ArtistImageEntry.COLUMN_WIDTH;
            }
        }

        if (imgDimensionValue == null) {
            //if a caller didn't request image preferred size, return an image largest
            //by its area (= width * height)
            colImageUri = "(SELECT " + SpotifyContract.ArtistImageEntry.COLUMN_URI +
                    " FROM " + SpotifyContract.ArtistImageEntry.TABLE_NAME +
                    " WHERE " +
                    SpotifyContract.ArtistImageEntry.TABLE_NAME + "." +
                    SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID + "=" +
                    SpotifyContract.ArtistEntry.TABLE_NAME + "." + SpotifyContract.ArtistEntry._ID +
                    " ORDER BY (" + SpotifyContract.ArtistImageEntry.COLUMN_WIDTH + "*" +
                    SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT + ") DESC LIMIT 1) AS " +
                    SpotifyContract.ArtistImageEntry.COLUMN_URI;
        } else {
            //If artist_images table contains images with size more than the 90% 0f size of image placeholder
            //then pick up from those image the one with size closest to the size of image placeholder,
            //otherwise pick up any image with size closest to the size of image placeholder
            //e.g. for placeholder with height = 240, from available images with width 100, 200, 300, 400,
            //an image with width 300 will be picked up. Even though 200 is close to 240 than 300, we don't
            //want image upscaling as it looses quality. We can tolerate upscaling by no more than 10%
            //that's why we have added ratio 0.9

            //SQL query for image preferred width 240 will look as follows:
            //(SELECT width FROM artist_images WHERE artist_images.artist_id = artists._id AND
            //(CASE (SELECT COUNT(*) FROM artist_images WHERE width>0.9*240)>0 WHEN 1 THEN width>0.9*240 ELSE 1 END)
            //ORDER BY ABS(width-240) LIMIT 1) AS uri
            float ratio = 0.9f;
            colImageUri = "(SELECT " + SpotifyContract.ArtistImageEntry.COLUMN_URI +
                    " FROM " + SpotifyContract.ArtistImageEntry.TABLE_NAME + " WHERE " +
                    SpotifyContract.ArtistImageEntry.TABLE_NAME + "." +
                    SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID + "=" +
                    SpotifyContract.ArtistEntry.TABLE_NAME + "." + SpotifyContract.ArtistEntry._ID +
                    " AND (CASE (SELECT COUNT(*) FROM " + SpotifyContract.ArtistImageEntry.TABLE_NAME +
                    " WHERE " + imgDimensionColumn + " > " + imgDimensionValue * ratio + ") > 0 WHEN 1 THEN " +
                    imgDimensionColumn + " > " + imgDimensionValue * ratio + " ELSE 1 END) ORDER BY ABS("
                    + imgDimensionColumn + "-" + imgDimensionValue + ") LIMIT 1) AS " +
                    SpotifyContract.ArtistImageEntry.COLUMN_URI;
        }

        Log.d(LOG_TAG, "getArtistData() colImageUri:" + colImageUri);


        if (projection == null) {
            //Add our custom column for image URI if null (all) columns are requested
            projection = new String[] {"*", colImageUri};
        } else {
            //Replace image UIR column name string with our custom column name string
            //composed from 'select' statement
            for (int i = 0; i < projection.length; i++) {
                if (projection[i] == SpotifyContract.ArtistImageEntry.COLUMN_URI) {
                    projection[i] = colImageUri;
                }
            }
        }

        return mOpenHelper.getReadableDatabase().query(
                SpotifyContract.ArtistEntry.TABLE_NAME, projection,
                selection, selectionArgs, null, null, sortOrder);
    }

    public Cursor getTrackData(Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        String colUri;

        String imgDimensionColumn = SpotifyContract.TrackImageEntry.COLUMN_HEIGHT;
        String value = uri.getQueryParameter(SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_HEIGHT);

        Long imgDimensionValue = null;
        if (value != null)
            imgDimensionValue = Long.parseLong(value);

        if (imgDimensionValue == null) {
            value = uri.getQueryParameter(SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_WIDTH);
            if (value != null)
                imgDimensionValue = Long.parseLong(value);
            if (imgDimensionValue != null) {
                imgDimensionColumn = SpotifyContract.TrackImageEntry.COLUMN_WIDTH;
            }
        }

        if (imgDimensionValue == null) {
            //if a caller didn't request image preferred size, return an image largest
            //by its area (= width * height)

            colUri = "(SELECT " + SpotifyContract.TrackImageEntry.COLUMN_URI +
                    " FROM " + SpotifyContract.TrackImageEntry.TABLE_NAME + " WHERE " +
                    SpotifyContract.TrackImageEntry.TABLE_NAME + "." +
                    SpotifyContract.TrackImageEntry.COLUMN_TRACK_ID + "=" +
                    SpotifyContract.TrackEntry.TABLE_NAME + "." + SpotifyContract.TrackEntry._ID +
                    " ORDER BY (" + SpotifyContract.TrackImageEntry.COLUMN_WIDTH + "*" +
                    SpotifyContract.TrackImageEntry.COLUMN_HEIGHT + ") DESC LIMIT 1) AS " +
                    SpotifyContract.TrackImageEntry.COLUMN_URI;

        } else {
            //If artist_images table contains images with size more than the 90% 0f size of image placeholder
            //then pick up from those image the one with size closest to the size of image placeholder,
            //otherwise pick up any image with size closest to the size of image placeholder
            //e.g. for placeholder with height = 240, from available images with width 100, 200, 300, 400,
            //an image with width 300 will be picked up. Even though 200 is close to 240 than 300, we don't
            //want image upscaling as it looses quality. We can tolerate upscaling by no more than 10%
            //that's why we have added ratio 0.9

            //SQL query for image preferred width 240 will look as follows:
            //(SELECT width FROM artist_images WHERE track_images.track_id = tracks._id AND
            //(CASE (SELECT COUNT(*) FROM artist_images WHERE width>0.9*240)>0 WHEN 1 THEN width>0.9*240 ELSE 1 END)
            //ORDER BY ABS(width-240) LIMIT 1) AS uri
            float ratio = 0.9f;
            colUri = "(SELECT " + SpotifyContract.TrackImageEntry.COLUMN_URI +
                    " FROM " + SpotifyContract.TrackImageEntry.TABLE_NAME + " WHERE " +
                    SpotifyContract.TrackImageEntry.TABLE_NAME + "." +
                    SpotifyContract.TrackImageEntry.COLUMN_TRACK_ID + "=" +
                    SpotifyContract.TrackEntry.TABLE_NAME + "." + SpotifyContract.TrackEntry._ID +
                    " AND (CASE (SELECT COUNT(*) FROM " + SpotifyContract.TrackImageEntry.TABLE_NAME +
                    " WHERE " + imgDimensionColumn + " > " + imgDimensionValue * ratio + ") > 0 WHEN 1 THEN " +
                    imgDimensionColumn + " > " + imgDimensionValue * ratio + " ELSE 1 END) ORDER BY ABS("
                    + imgDimensionColumn + "-" + imgDimensionValue + ") LIMIT 1) AS " +
                    SpotifyContract.TrackImageEntry.COLUMN_URI;
        }

        if (projection == null) {
            //Add our custom column for image URI if null (all) columns are requested
            projection = new String[] {"*", colUri};
        } else {
            //Replace image UIR column name string with our custom column name string
            //composed from 'select' statement
            for (int i = 0; i < projection.length; i++) {
                if (projection[i] == SpotifyContract.TrackImageEntry.COLUMN_URI) {
                    projection[i] = colUri;
                }
            }
        }

        return mOpenHelper.getReadableDatabase().query(SpotifyContract.TrackEntry.TABLE_NAME,
                projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case ARTIST_WITH_ID: {
                retCursor = getArtistData(uri, projection, SpotifyContract.ArtistEntry._ID + "=" +
                        SpotifyContract.ArtistEntry.getRowIdFromUri(uri), null, sortOrder);
                break;
            }
            case ARTIST: {
                retCursor = getArtistData(uri, projection, selection, selectionArgs, sortOrder);
                break;
            }
            case TRACK_WITH_ID: {
                retCursor = getTrackData(uri, projection, SpotifyContract.TrackEntry._ID + "=" +
                        SpotifyContract.TrackEntry.getRowIdFromUri(uri), null , sortOrder);
                break;
            }
            case TRACK: {
                retCursor = getTrackData(uri, projection, selection, selectionArgs, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        //printCursorToLog(retCursor);

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case ARTIST: {
                long _id = db.insert(SpotifyContract.ArtistEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = SpotifyContract.ArtistEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ARTIST_IMAGE: {
                long _id = db.insert(SpotifyContract.ArtistImageEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = SpotifyContract.ArtistImageEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TRACK: {
                long _id = db.insert(SpotifyContract.TrackEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = SpotifyContract.TrackEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TRACK_IMAGE: {
                long _id = db.insert(SpotifyContract.TrackImageEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = SpotifyContract.TrackImageEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case ARTIST:
                rowsDeleted = db.delete(SpotifyContract.ArtistEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            case ARTIST_IMAGE:
                rowsDeleted = db.delete(SpotifyContract.ArtistImageEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            case TRACK:
                rowsDeleted = db.delete(SpotifyContract.TrackEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            case TRACK_IMAGE:
                rowsDeleted = db.delete(SpotifyContract.TrackImageEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case ARTIST:
                rowsUpdated = db.update(SpotifyContract.ArtistEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case ARTIST_IMAGE:
                rowsUpdated = db.update(SpotifyContract.ArtistImageEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case TRACK:
                rowsUpdated = db.update(SpotifyContract.TrackEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case TRACK_IMAGE:
                rowsUpdated = db.update(SpotifyContract.TrackImageEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SpotifyContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, SpotifyContract.PATH_ARTISTS + "/#", ARTIST_WITH_ID); //out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_ARTISTS + "/*", ARTIST); //in for 'query'
        matcher.addURI(authority, SpotifyContract.PATH_ARTISTS, ARTIST); //in for 'query' and for 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_ARTIST_IMAGES, ARTIST_IMAGE); //out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_ARTIST_IMAGES + "/#", ARTIST_IMAGE_WITH_ID); //out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_TRACKS, TRACK); //in for 'insert', out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_TRACKS + "/#", TRACK_WITH_ID); //in for 'insert', out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_TRACKS + "/#/*", TRACK_WITH_ID_AND_IMAGE_SIZE); //in for 'insert', out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_TRACK_IMAGES, TRACK_IMAGE); //in for 'insert', out from 'insert'
        matcher.addURI(authority, SpotifyContract.PATH_TRACK_IMAGES + "/#", TRACK_IMAGE_WITH_ID); //in for 'insert', out from 'insert'
        return matcher;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case ARTIST:
                return SpotifyContract.ArtistEntry.CONTENT_TYPE;
            case ARTIST_WITH_ID:
                return SpotifyContract.ArtistEntry.CONTENT_ITEM_TYPE;
            case ARTIST_IMAGE_WITH_ID:
                return SpotifyContract.ArtistImageEntry.CONTENT_TYPE;
            case TRACK_WITH_ID:
                return SpotifyContract.TrackEntry.CONTENT_TYPE;
            case TRACK_IMAGE_WITH_ID:
                return SpotifyContract.TrackImageEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
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
}
