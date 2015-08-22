package com.example.android.spotifystreamer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

/**
 * Created by Alexander on 6/23/2015.
 */
public class TopTracksActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();

    private SpotifyAdapter mAdapter;
    private ListView mListView;

    private String mCountry;

    private Uri mArtistUri;
    private String mArtistSpotifyId;

    private int mPosition = ListView.INVALID_POSITION;
    private static final int TOPTRACKS_LOADER = 0;

    Random mRand = new Random();

    private Toast mToast = null;

    boolean mIsLargeLayout;

    private static final String SELECTED_KEY = "selected_position";

    public static final int COL_TRACK_ID = 0;
    public static final int COL_TRACK_NAME = 1;
    public static final int COL_ALBUM_NAME = 2;
    public static final int COL_TRACK_IMAGE_URI = 3;

    private static final String[] TOPTRACKS_COLUMNS = {
            SpotifyContract.TrackEntry._ID,
            SpotifyContract.TrackEntry.COLUMN_TRACK_NAME,
            SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME,
            SpotifyContract.TrackImageEntry.COLUMN_URI,
    };

    static private final String KEY_POSITION = "KEY_POSITION";
    static private final String KEY_COLOR_INDEX = "KEY_COLOR_INDEX";

    static public final String KEY_ARTIST_URI = "KEY_ARTIST_URI";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);

        mAdapter = new SpotifyAdapter(SpotifyAdapter.DataType.Tracks, getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        mListView = (ListView) rootView.findViewById(R.id.top_tracks);
        mListView.setAdapter(mAdapter);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mArtistUri = arguments.getParcelable(TopTracksActivityFragment.KEY_ARTIST_URI);
        }

        if (mArtistUri != null) {
            mArtistSpotifyId = Utility.getArtistSpotifyId(getActivity(), mArtistUri);
            String country = Utility.getCountryCodeFromPrefs(getActivity());
            if (country != null && mCountry != null && !country.equals(mCountry)) {
                onCountryChanged();
                mCountry = country;
            } else if (!topTracksAvailable())
                updateTopTracks();
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
                showPlayerDialog(position);
                mPosition = position;
            }
        });
        return rootView;
    }

    public void showPlayerDialog(int position) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

        PlayerDialogFragment playerFragment = new PlayerDialogFragment();
        playerFragment.setDialogData(mArtistUri, position, mAdapter.getNoImageBgColorStartIndex());

        if (mIsLargeLayout) {
            // The device is using a large layout, so show the fragment as a dialog
            playerFragment.show(fragmentManager, "dialog");
        } else {
            // The device is smaller, so show the fragment fullscreen
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, playerFragment)
                    .addToBackStack(null).commit();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String sortOrder = SpotifyContract.TrackEntry._ID + " ASC";

        float ratio = Utility.getFloatFromResources(getActivity().getResources(), R.dimen.image_height_to_view_height_ratio);
        // ratio = 0.8 is used for image to have some padding from top and bottom of view
        int imgHeight = (int) (Utility.getListPreferredItemHeight(getActivity()) * ratio);

        Uri uriWithImageSize = SpotifyContract.TrackEntry
                .buildUriWithIntParameter(
                        SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_HEIGHT, imgHeight);

        Long artistId = getArtistId();
        if (artistId != null) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    uriWithImageSize,
                    TOPTRACKS_COLUMNS,
                    SpotifyContract.TrackEntry.COLUMN_ARTIST_ID + "=" + artistId,
                    null, sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public Long getArtistId(){
        return mArtistUri == null ? null : SpotifyContract.ArtistEntry.getRowIdFromUri(mArtistUri);
    }

    void onCountryChanged() {
        Utility.deleteAllFromTracksTables(getActivity());
        if (updateTopTracks()) {
            getLoaderManager().restartLoader(TOPTRACKS_LOADER, null, this);
        }
    }

    boolean updateTopTracks() {
        boolean updated = false;
        if (mArtistSpotifyId != null && mArtistSpotifyId.length() > 0) {
            GetTopTracksTask task = new GetTopTracksTask(getActivity());
            task.execute(mArtistSpotifyId);
            updated = true;
        }
        return updated;
    }

    public boolean topTracksAvailable() {
        boolean res = false;
        Cursor cursor = Utility.getTracksForArtistUri(getActivity(),
                mArtistUri, new String[]{SpotifyContract.TrackEntry._ID});
        if (cursor != null) {
            res = cursor.getCount() > 0;
            cursor.close();
        }
        return res;
    }

    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        outState.putParcelable(KEY_ARTIST_URI, mArtistUri);
        outState.putInt(KEY_POSITION, mPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mArtistUri = savedInstanceState.getParcelable(KEY_ARTIST_URI);
            mPosition = savedInstanceState.getInt(KEY_POSITION);
        }
        getLoaderManager().initLoader(TOPTRACKS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        String country = Utility.getCountryCodeFromPrefs(getActivity());
        if (country != null && mCountry != null && !country.equals(mCountry)) {
            onCountryChanged();
            mCountry = country;
        }
    }

    public class GetTopTracksTask extends AsyncTask<String, Void, Tracks> {
        private final String LOG_TAG = GetTopTracksTask.class.getSimpleName();

        private final Context mContext;

        public GetTopTracksTask(Context context) {
            mContext = context;
        }

        @Override
        protected Tracks doInBackground(String... artistIDs) {

            Tracks result = null;
            if (artistIDs.length == 0) {
                return null;
            }
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                final Map<String, Object> options = new HashMap<String, Object>();
                options.put(SpotifyService.OFFSET, 0);
                options.put(SpotifyService.LIMIT, 10); //hardcoded for now, TODO: make configurable
                String countryCode = Utility.getCountryCodeFromPrefs(getActivity());
                options.put(SpotifyService.COUNTRY, countryCode);
                result = spotify.getArtistTopTrack(artistIDs[0], options);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Tracks result) {
            int size = 0;
            List<Track> items = null;

            //mAdapter.clear();

            if (result != null) {
                items = result.tracks;
                size = items.size();

                for (Track item : items) {

                    //Insert track into db
                    ContentValues values = new ContentValues();
                    values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_NAME, item.name);
                    values.put(SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME, item.name);
                    values.put(SpotifyContract.TrackEntry.COLUMN_TRACK_SPOTIFY_ID, item.id);
                    values.put(SpotifyContract.TrackEntry.COLUMN_ARTIST_ID, getArtistId());
                    values.put(SpotifyContract.TrackEntry.COLUMN_IS_PLAYABLE,
                            (item.is_playable == null || !item.is_playable) ? 0 : 1);
                    values.put(SpotifyContract.TrackEntry.COLUMN_PREVIEW_URL, item.preview_url);

                    Uri createdRow = mContext.getContentResolver().insert(
                            SpotifyContract.TrackEntry.CONTENT_URI, values);
                    Long trackId = SpotifyContract.TrackEntry.getRowIdFromUri(createdRow);
                    Log.d(LOG_TAG, "SearchForArtistTask::onPostExecute. Track " + item.name + " inserted with _ID " + trackId);

                    //Insert track images into db
                    if (trackId != null && item.album.images != null && item.album.images.size() > 0) {
                        ContentValues[] arrValues = new ContentValues[item.album.images.size()];
                        int i = 0;
                        for (Image image : item.album.images) {
                            values = new ContentValues();
                            values.put(SpotifyContract.TrackImageEntry.COLUMN_TRACK_ID, trackId);
                            values.put(SpotifyContract.TrackImageEntry.COLUMN_URI, image.url);
                            values.put(SpotifyContract.TrackImageEntry.COLUMN_WIDTH, image.width);
                            values.put(SpotifyContract.TrackImageEntry.COLUMN_HEIGHT, image.height);
                            arrValues[i++] = values;
                        }
                        mContext.getContentResolver()
                                .bulkInsert(SpotifyContract.TrackImageEntry.CONTENT_URI, arrValues);
                    }
                }
            }
            if (size == 0) {
                if(mToast != null) {
                    mToast.cancel();
                }
                // Show toast 'No tracks found to [country_name]'
                // as as a reminder which country was used and was so unlucky to not have any tracks
                // so that user could try different country
                mToast = Toast.makeText(getActivity(),
                        getResources().getString(R.string.no_tracks_found) + " " +
                                getResources().getString(R.string.preposition_for) + "\n" +
                                Utility.getCountryNameFromPrefs(getActivity()), Toast.LENGTH_SHORT);
                mToast.show();
            }
            // Set random number which will be used for initializing sequence
            // of background colors used for "No Image" icons (for tracks not having images)
            mAdapter
                    .setNoImageBgColorStartIndex(size > 0 ? mRand.nextInt(size) : 0);
        }
    }

}
