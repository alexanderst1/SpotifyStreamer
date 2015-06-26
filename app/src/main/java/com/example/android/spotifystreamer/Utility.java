package com.example.android.spotifystreamer;

import android.app.Activity;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

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
}
