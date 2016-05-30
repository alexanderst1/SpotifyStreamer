package com.example.android.spotifystreamer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    SpotifyAdapter mAdapter;
    private ListView mListView;

    private int mPosition = ListView.INVALID_POSITION;

    Random mRand = new Random();

    private Toast mToast = null;

    private static final String SELECTED_KEY = "selected_position";

    private SpotifyStreamerApp app = null;

    private static final int TOPARTISTS_LOADER = 0;

    static final int COL_ARTIST_ID = 0;
    static final int COL_ARTIST_SPOTIFY_ID = 1;
    static final int COL_ARTIST_NAME = 2;
    static final int COL_ARTIST_IMAGE_URI = 3;
    private static final String[] TOPARTISTS_COLUMNS = {
            SpotifyContract.ArtistEntry._ID,
            SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID,
            SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME,
            SpotifyContract.ArtistImageEntry.COLUMN_URI
    };

    public interface Callback {
        /**
         * MainActivityFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri artistUri);
    }

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_top_artists_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        app = (SpotifyStreamerApp) getActivity().getApplication();
        mAdapter = new SpotifyAdapter(SpotifyAdapter.DataType.Artists, getActivity(), null, 0);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mListView = (ListView) rootView.findViewById(R.id.artists);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    ((Callback) getActivity())
                            .onItemSelected(SpotifyContract.ArtistEntry.buildUriWithRowId(
                                    cursor.getLong(COL_ARTIST_ID)));
                }
                // Save scrolling position in application global variable to restore when coming back
                // from Top Tracks activity
                if (adapterView instanceof ListView) {
                    ListView lv = (ListView)adapterView;
                    int index = lv.getFirstVisiblePosition();
                    View childAt0 = lv.getChildAt(0);
                    int offset = (childAt0 == null) ? 0 : (childAt0.getTop() - lv.getPaddingTop());
                    app.setArtistsListFirstVisibleIndex(new Pair<Integer, Integer>(index, offset));
                }
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        EditText editText = (EditText) rootView.findViewById(R.id.artistName);

        // Restore search string when returning from 'Top 10 Tracks' activity and hide keyboard
        String searchString = Utility.getSearchStringFromPrefs(getActivity());
        if (searchString != null) {
            editText.setText(searchString);
            // Hide keyboard
            getActivity().getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        // Handle user's click on 'Search' button on keyboard
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Hide virtual keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);

                    // Start artist search task
                    String searchString = v.getText().toString();
                    Utility.putSearchStringToPrefs(getActivity(),searchString);

                    updateTopArtists();
                    handled = true;
                }
                return handled;
            }
        });
        return rootView;
    }

    private void updateTopArtists() {
        SearchForArtistTask task = new SearchForArtistTask(getActivity());
        task.execute(Utility.getSearchStringFromPrefs(getActivity()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Sort order:  Ascending, by artist id.
        String sortOrder = SpotifyContract.ArtistEntry._ID + " ASC";

        float ratio = Utility.getFloatFromResources(getActivity().getResources(), R.dimen.image_height_to_view_height_ratio);
        // ratio = 0.8 is used for image to have some padding from top and bottom of view
        int imgHeight = (int) (Utility.getListPreferredItemHeight(getActivity()) * ratio);

        Uri uriWithImageSize = SpotifyContract.ArtistEntry
                .buildUriWithIntParameter(
                        SpotifyContract.URI_QUERY_KEY_FOR_IMAGE_HEIGHT, imgHeight);

        return new CursorLoader(getActivity(),
                uriWithImageSize,
                TOPARTISTS_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        } else {
            // Restore scrolling position of list of artists from application global variable
            Pair<Integer,Integer> pairIndexOffset = app.getArtistsListFirstVisibleIndex();
            mListView.setSelectionFromTop(pairIndexOffset.left, pairIndexOffset.right);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(TOPARTISTS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    public class SearchForArtistTask extends AsyncTask<String, Void, ArtistsPager> {
        private final String LOG_TAG = SearchForArtistTask.class.getSimpleName();
        private final Context mContext;
        public SearchForArtistTask(Context context) {
            mContext = context;
        }

        @Override
        protected ArtistsPager doInBackground(String... artistNames) {
            ArtistsPager result = null;
            if (artistNames.length == 0) {
                return null;
            }
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                result = spotify.searchArtists(artistNames[0]);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArtistsPager result) {
            int size = 0;
            List<Artist> items = null;

            if (result != null) {
                items = result.artists.items;
                size = items.size();

                Utility.deleteAllFromArtistTables(getActivity());

                for (Artist item : items) {
                    //Insert artist into db
                    ContentValues values = new ContentValues();
                    values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_NAME, item.name);
                    values.put(SpotifyContract.ArtistEntry.COLUMN_ARTIST_SPOTIFY_ID, item.id);
                    Uri createdRow = mContext.getContentResolver().insert(
                            SpotifyContract.ArtistEntry.CONTENT_URI, values);
                    Long artistId = SpotifyContract.ArtistEntry.getRowIdFromUri(createdRow);

//                    Log.d(LOG_TAG, "SearchForArtistTask::onPostExecute. Artist " + item.name +
//                            " inserted with _ID " + artistId);

                    //Insert artist images into db
                    if (artistId != null && item.images != null && item.images.size() > 0) {
                        ContentValues[] arrValues = new ContentValues[item.images.size()];
                        int i = 0;
                        for (Image image : item.images) {
                            values = new ContentValues();
                            values.put(SpotifyContract.ArtistImageEntry.COLUMN_ARTIST_ID, artistId);
                            values.put(SpotifyContract.ArtistImageEntry.COLUMN_URI, image.url);
                            values.put(SpotifyContract.ArtistImageEntry.COLUMN_WIDTH, image.width);
                            values.put(SpotifyContract.ArtistImageEntry.COLUMN_HEIGHT, image.height);
                            arrValues[i++] = values;

//                            String funcName = "SearchForArtistTask::onPostExecute - ";
//                            Log.d(LOG_TAG, funcName + "artistId    :" + artistId);
//                            Log.d(LOG_TAG, funcName + "imageURI    :" + image.url);
//                            Log.d(LOG_TAG, funcName + "imageHeight :" + image.width);
//                            Log.d(LOG_TAG, funcName + "imageWidth  :" + image.height);
                        }
                        getActivity().getContentResolver()
                                .bulkInsert(SpotifyContract.ArtistImageEntry.CONTENT_URI, arrValues);
                    }
                }
            }
            if (size == 0) {
                if(mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(getActivity(),
                        getResources().getString(R.string.no_artists_found), Toast.LENGTH_SHORT);
                mToast.show();
            }
            // Set random number which will be used for initializing sequence
            // of background colors used for "No Image" icons (for artists not having images)
            mAdapter.setNoImageBgColorStartIndex(size > 0 ? mRand.nextInt(size) : 0);
        }
    }
}


