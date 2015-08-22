package com.example.android.spotifystreamer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageButton;

/**
 * Created by Alexander on 6/23/2015.
 */
public class Utility {

    // Sequence of background colors used for "No Image" icon
    // (for artists/tracks not having images)
    // main ('500') colors used from http://www.google.com/design/spec/style/color.html#color-color-palette
    public final static int[] NOIMAGE_BACKGROUND_COLOR_IDS = new int[] {
            R.color.no_image_text_background_red,
            R.color.no_image_text_background_pink,
            R.color.no_image_text_background_purple,
            R.color.no_image_text_background_deeppurple,
            R.color.no_image_text_background_indigo,
            R.color.no_image_text_background_blue,
            R.color.no_image_text_background_lightblue,
            R.color.no_image_text_background_cyan,
            R.color.no_image_text_background_teal,
            R.color.no_image_text_background_green,
            R.color.no_image_text_background_lightgreen,
            R.color.no_image_text_background_lime,
            R.color.no_image_text_background_amber,
            R.color.no_image_text_background_orange,
            R.color.no_image_text_background_deeporange,
            R.color.no_image_text_background_brown,
            R.color.no_image_text_background_grey,
            R.color.no_image_text_background_bluegrey,
    };
    public static int getListPreferredItemHeight(Activity activity)
    {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        TypedValue.coerceToString(value.type, value.data);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (int)value.getDimension(metrics);
    }
    public static float getFloatFromResources(Resources resources, int resourceID) {
        TypedValue typedValue = new TypedValue();
        resources.getValue(resourceID, typedValue, true);
        return typedValue.getFloat();
    }
    public static String getCountryCodeFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_country_id_key),
                context.getString(R.string.pref_country_id_default));
    }

    public static String getSearchStringFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_search_string_key),
                context.getString(R.string.pref_search_string_default));
    }

    public static void putSearchStringToPrefs(Context context, String searchString) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(context.getString(R.string.pref_search_string_key), searchString)
                .apply();
    }

    public static String getCountryNameFromPrefs(Context context) {
        // See SettingsActivity.sBindPreferenceSummaryToValueListener
        // key 'country_id'       --> value 'US'
        // key 'country_id_title' --> value 'United States of America'
        // would like to show full country name on a toast that no tracks were found
        // for that country (as for the user to decipher country code might be difficult).
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_country_id_key) +
                        context.getString(R.string.pref_country_suffix_for_title),
                context.getString(R.string.pref_country_id_default));
    }

    public static String getArtistName(Context context, Uri artistUri) {
        return getArtistSingleField(context, artistUri,
                SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME);
    }

    public static String getArtistSpotifyId(Context context, Uri artistUri) {
        return getArtistSingleField(context, artistUri,
                SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID);
    }

    public static void setArtistNameInSubtitleOfActionBar(ActionBarActivity activity, Uri artistUri)
    {
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setSubtitle(Utility.getArtistName(activity, artistUri));
        }
    }

    public static String getArtistSingleField(Context context, Uri artistUri, String fieldName) {
        String fieldValue = null;
        Cursor c = context.getContentResolver().query(artistUri,
                new String[]{fieldName}, null, null, null);
        if (c.moveToFirst() && c != null) {
            fieldValue = c.getString(0);
        }
        c.close();
        return fieldValue;
    }

    public static Cursor getTracksForArtistUri(Context context, Uri artistUri, String[] columns) {
        Cursor cursor = null;
            Long artistId = SpotifyContract.ArtistEntry.getRowIdFromUri(artistUri);
            if (artistId != null) {
                cursor = context.getContentResolver().query(SpotifyContract.TrackEntry.CONTENT_URI,
                        columns,
                        SpotifyContract.TrackEntry.COLUMN_ARTIST_ID + "=" + artistId,
                        null, null);
            }
            return cursor;
    }

    public static void deleteAllFromTracksTables(Context context) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(SpotifyContract.TrackImageEntry.CONTENT_URI, null, null);
        cr.delete(SpotifyContract.TrackEntry.CONTENT_URI, null, null);
    }

    public static void deleteAllFromArtistTables(Context context) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(SpotifyContract.TrackImageEntry.CONTENT_URI, null, null);
        cr.delete(SpotifyContract.TrackEntry.CONTENT_URI, null, null);
        cr.delete(SpotifyContract.ArtistImageEntry.CONTENT_URI, null, null);
        cr.delete(SpotifyContract.ArtistEntry.CONTENT_URI, null, null);
    }

    /**
     * http://stackoverflow.com/questions/8196206/disable-an-imagebutton
     *
     * Sets the specified image button to the given state, while modifying or
     * "graying-out" the icon as well
     *
     * @param enabled The state of the menu item
     * @param item The menu item to modify
     * @param iconResId The icon ID
     */
    public static void setImageButtonEnabled(Context ctxt, boolean enabled, ImageButton item,
                                             int iconResId) {
        item.setEnabled(enabled);
        Drawable originalIcon = ctxt.getResources().getDrawable(iconResId);
        Drawable icon = enabled ? originalIcon : convertDrawableToGrayScale(originalIcon);
        item.setImageDrawable(icon);
    }

    /**
     * http://stackoverflow.com/questions/8196206/disable-an-imagebutton
     *
     * Mutates and applies a filter that converts the given drawable to a Gray
     * image. This method may be used to simulate the color of disable icons in
     * Honeycomb's ActionBar.
     *
     * @return a mutated version of the given drawable with a color filter
     *         applied.
     */
    public static Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable res = drawable.mutate();
        res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        return res;
    }
}
