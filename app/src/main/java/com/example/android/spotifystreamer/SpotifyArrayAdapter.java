package com.example.android.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Alexander on 6/23/2015.
 */
public class SpotifyArrayAdapter<T> extends ArrayAdapter<T> {
    List<T> items;
    Activity mActivity;

    // Start index of sequence of background colors used for "No Image" icon
    // (for artists/tracks not having images)
    // defined in Utility.NOIMAGE_BACKGROUND_COLOR_IDS
    int mNoImageBgColorStartIndex = 0;
    public void SetNoImageBgColorStartIndex(int index){
        mNoImageBgColorStartIndex = index;
    }

    public SpotifyArrayAdapter(Activity activity, int resource, List<T> items) {
        super(activity, resource, items);
        this.items = items;
        this.mActivity = activity;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {

        float ratio = Utility.getFloatFromResources(mActivity.getResources(),
                R.dimen.image_height_to_view_height_ratio);
        // Image will have a square shape
        // ratio = 0.8 is used for image to have some padding from top and bottom of view
        int imgHeight = (int) (Utility.getListPreferredItemHeight(mActivity) * ratio);
        int imgWidth = imgHeight;

        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mActivity).inflate(R.layout.list_item, null);
            holder = new ViewHolder(convertView, imgWidth, imgHeight);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        Object item = items.get(pos);
        List<Image> images = null;
        String text1 = null;
        String text2 = null;
        if (item instanceof Artist) {
            text1 = ((Artist) item).name;
            images = ((Artist) item).images;
        } else if (item instanceof Track) {
            text1 = ((Track)item).name;
            text2 = ((Track)item).album.name;
            images = ((Track)item).album.images;
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
        if (images != null && !images.isEmpty()) {
            // If images are available, then pick up smallest one which is larger then view so
            // that it will be down-scaled a little bit and displayed with good quality
            ImageHelper imgHelper = new ImageHelper(images);
            Image img = imgHelper.getSuitableImageForImageView(imgWidth, imgHeight);
            Picasso.with(mActivity).load(img.url)
                    //.resize(holder.icon.getLayoutParams().width, holder.icon.getLayoutParams().height)
                    //.centerCrop()
                    .into(holder.icon);
            holder.icon.setVisibility(View.VISIBLE);
            holder.textNoImage.setVisibility(View.GONE);
        } else {
            // If images are NOT available, then show text "No Image" with backgrounds of different
            // colors so that visually they would NOT be identified as items of the same kind
            int colorIndex = (pos + mNoImageBgColorStartIndex) %
                    Utility.NOIMAGE_BACKGROUND_COLOR_IDS.length;
            int bgId = Utility.NOIMAGE_BACKGROUND_COLOR_IDS[colorIndex];
            holder.icon.setVisibility(View.GONE);
            holder.textNoImage.setVisibility(View.VISIBLE);
            holder.textNoImage.setBackgroundColor(mActivity.getResources().getColor(bgId));
        }
        return convertView;
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
