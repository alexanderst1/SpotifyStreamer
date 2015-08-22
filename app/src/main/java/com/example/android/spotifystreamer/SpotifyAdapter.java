package com.example.android.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by Alexander on 6/23/2015.
 */
public class SpotifyAdapter extends CursorAdapter {
    Activity mActivity;

    public enum DataType {
        Artists,
        Tracks
    }

    DataType mDataType;

    // Start index of sequence of background colors used for "No Image" icon
    // (for artists/tracks not having images)
    // defined in Utility.NOIMAGE_BACKGROUND_COLOR_IDS
    private int mNoImageBgColorStartIndex = 0;
    public void setNoImageBgColorStartIndex(int index){
        mNoImageBgColorStartIndex = index;
    }
    public int getNoImageBgColorStartIndex(){
        return mNoImageBgColorStartIndex;
    }

    public SpotifyAdapter(DataType dataType, Activity activity, Cursor c, int flags) {
        super(activity, c, flags);
        this.mActivity = activity;
        this.mDataType = dataType;
    }

    private int mPos = 0;
    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        mPos = pos;
        return super.getView(pos, convertView, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder holder = (ViewHolder) view.getTag();
        String text1 = null;
        String text2 = null;
        String imageUri = null;
        if (mDataType == DataType.Artists) {
            text1 = cursor.getString(MainActivityFragment.COL_ARTIST_NAME);
            imageUri = cursor.getString(MainActivityFragment.COL_ARTIST_IMAGE_URI);
        } else if (mDataType == DataType.Tracks) {
            text1 = cursor.getString(TopTracksActivityFragment.COL_TRACK_NAME);
            text2 = cursor.getString(TopTracksActivityFragment.COL_ALBUM_NAME);
            imageUri = cursor.getString(TopTracksActivityFragment.COL_TRACK_IMAGE_URI);
        }
        // *** set text ***
        holder.text1.setText(text1);
        if (text2 == null) {
            holder.text2.setVisibility(View.GONE);
        } else {
            holder.text2.setText(text2);
            holder.text2.setVisibility(View.VISIBLE);
        }
        // *** set image ***
        if (imageUri != null && !imageUri.isEmpty()) {
            Picasso.with(mActivity).load(imageUri)
                    //.resize(holder.icon.getLayoutParams().width, holder.icon.getLayoutParams().height)
                    //.centerCrop()
                    .into(holder.icon);
            holder.icon.setVisibility(View.VISIBLE);
            holder.textNoImage.setVisibility(View.GONE);
        } else {
            // If images are NOT available, then show text "No Image" with backgrounds of different
            // colors so that visually they would NOT be identified as items of the same kind
            int colorIndex = (mPos + mNoImageBgColorStartIndex) %
                    Utility.NOIMAGE_BACKGROUND_COLOR_IDS.length;
            int bgId = Utility.NOIMAGE_BACKGROUND_COLOR_IDS[colorIndex];
            holder.icon.setVisibility(View.GONE);
            holder.textNoImage.setVisibility(View.VISIBLE);
            holder.textNoImage.setBackgroundColor(mActivity.getResources().getColor(bgId));
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        float ratio = Utility.getFloatFromResources(mActivity.getResources(), R.dimen.image_height_to_view_height_ratio);
        // Image will have a square shape
        // ratio = 0.8 is used for image to have some padding from top and bottom of view
        int imgHeight = (int) (Utility.getListPreferredItemHeight(mActivity) * ratio);
        int imgWidth = imgHeight;

        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

        ViewHolder holder = new ViewHolder(view, imgWidth, imgHeight);
        view.setTag(holder);

        return view;
    }

    static class ViewHolder {
        ImageView icon;
        TextView textNoImage;
        TextView text1;
        TextView text2;
        public ViewHolder(View view, int imgWidth, int imgHeight) {
            icon = (ImageView) view.findViewById(R.id.listItemImage);
            textNoImage = (TextView) view.findViewById(R.id.listItemTextNoImage);
            text1 = (TextView) view.findViewById(R.id.listItemText1);
            text2 = (TextView) view.findViewById(R.id.listItemText2);
            // Set explicitly size for imageview and textview "No Image" to be always the same
            // size for all items in list view and not depend on image sizes from Spotify
            textNoImage.getLayoutParams().height = imgHeight;
            textNoImage.getLayoutParams().width = imgWidth;
            icon.getLayoutParams().height = imgHeight;
            icon.getLayoutParams().width = imgWidth;
        }
    }
}
