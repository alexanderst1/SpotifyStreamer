package com.example.android.spotifystreamer;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
/**
 * Created by Alexander on 7/16/2015.
 */
public class SpotifyContract {

    public static final String CONTENT_AUTHORITY = "com.example.android.spotifystreamer";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_ARTISTS = "artists";
    public static final String PATH_TRACKS = "tracks";
    public static final String PATH_ARTIST_IMAGES = "artist_images";
    public static final String PATH_TRACK_IMAGES = "track_images";

    public static final String URI_QUERY_KEY_FOR_IMAGE_WIDTH = "imgprefwidth";
    public static final String URI_QUERY_KEY_FOR_IMAGE_HEIGHT = "imgprefheight";

    public static final class ArtistEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTISTS).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTISTS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTISTS;
        public static final String TABLE_NAME = "artists";
        public static final String COLUMN_ARTIST_SPOTIFY_ID = "artist_spotify_id";
        public static final String COLUMN_ARTIST_NAME = "artist_name";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static final long getRowIdFromUri (Uri uri) {
            return Long.parseLong(uri.getLastPathSegment());
        }
        public static Uri buildUriWithIntParameter(String queryKey, int queryValue) {
            return CONTENT_URI.buildUpon().appendQueryParameter(queryKey, Integer.toString(queryValue)).build();
        }
    }

    public static final class TrackEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACKS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACKS;
        public static final String TABLE_NAME = "tracks";
        public static final String COLUMN_TRACK_SPOTIFY_ID = "track_spotify_id";
        public static final String COLUMN_ARTIST_ID = "artist_id";
        public static final String COLUMN_TRACK_NAME = "track_name";
        public static final String COLUMN_ALBUM_NAME = "album_name";
        public static final String COLUMN_IS_PLAYABLE = "is_playable";
        public static final String COLUMN_PREVIEW_URL = "preview_url";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static final long getRowIdFromUri (Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
        public static Uri buildUriWithIntParameter(String queryKey, int queryValue) {
            return CONTENT_URI.buildUpon().appendQueryParameter(queryKey, Integer.toString(queryValue)).build();
        }
    }

    public static final class ArtistImageEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTIST_IMAGES).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTIST_IMAGES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTIST_IMAGES;
        public static final String TABLE_NAME = "artist_images";
        public static final String COLUMN_ARTIST_ID = "artist_id";
        public static final String COLUMN_URI = "uri";
        public static final String COLUMN_HEIGHT = "height";
        public static final String COLUMN_WIDTH = "width";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static final long getRowIdFromUri (Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    public static final class TrackImageEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACK_IMAGES).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACK_IMAGES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACK_IMAGES;
        public static final String TABLE_NAME = "track_images";
        public static final String COLUMN_TRACK_ID = "track_id";
        public static final String COLUMN_URI = "uri";
        public static final String COLUMN_HEIGHT = "height";
        public static final String COLUMN_WIDTH = "width";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static final long getRowIdFromUri (Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }
}

