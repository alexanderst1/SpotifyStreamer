package com.example.android.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.widget.Toast;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    SpotifyArrayAdapter<Artist> mArrayAdapter;
    Random mRand = new Random();
    private Toast mToast = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Retrieve global list of artists so that when users click back on activity 'Top 10 Tracks'
        // they will find main activity intact: with same list of artists and scrolled position
        final SpotifyStreamerApp app = (SpotifyStreamerApp) getActivity().getApplication();
        List<Artist> artists = app.getArtists();
        if (artists == null) {
            artists = new ArrayList<Artist>();
            app.setArtists(artists);
        }
        mArrayAdapter = new SpotifyArrayAdapter<Artist>(getActivity(),
                R.layout.list_item, artists);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.artists);
        listView.setAdapter(mArrayAdapter);

        // Restore scrolling position of list of artists from application global variable
        Pair<Integer,Integer> pairIndexOffset = app.getArtistsListFirstVisibleIndex();
        listView.setSelectionFromTop(pairIndexOffset.left,pairIndexOffset.right);

        EditText editText = (EditText) rootView.findViewById(R.id.artistName);

        // Restore search string when returning from 'Top 10 Tracks' activity and hide keyboard
        String searchString = app.getArtistSearchString();
        if (searchString != null) {
            editText.setText(searchString);
            // Hide keyboard
            getActivity().getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        // Handle user's click on an artist item in the list of found artists
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (adapterView instanceof ListView) {

                    // Save scrolling position in application global variable
                    ListView lv = (ListView)adapterView;
                    int index = lv.getFirstVisiblePosition();
                    View childAt0 = lv.getChildAt(0);
                    int offset = (childAt0 == null) ? 0 : (childAt0.getTop() - lv.getPaddingTop());
                    app.setArtistsListFirstVisibleIndex(new Pair<Integer, Integer>(index, offset));
                }
                // Start activity to show top 10 tracks and pass user ID (to query spotify)
                // and user name (to display in activity title bar)
                Intent intent = new Intent(getActivity(), TopTracksActivity.class)
                        .putExtra(MainActivity.EXTRA_ARTIST_ID, mArrayAdapter.items.get(position).id)
                        .putExtra(MainActivity.EXTRA_ARTIST_NAME, mArrayAdapter.items.get(position).name);
                startActivity(intent);
            }
        });

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
                    app.setArtistSearchString(searchString);
                    SearchForArtistTask task = new SearchForArtistTask();
                    task.execute(searchString);
                    handled = true;
                }
                return handled;
            }
        });

        return rootView;
    }

    public class SearchForArtistTask extends AsyncTask<String, Void, ArtistsPager> {
        private final String LOG_TAG = SearchForArtistTask.class.getSimpleName();

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
            mArrayAdapter.clear();
            if (result != null) {
                items = result.artists.items;
                size = items.size();
                for (Artist item : items) {
                    mArrayAdapter.add(item);
                }
            }
            // Save list of artist in application global variable
            // so that list of artists could be restored after returning from 'Top 10 Tracks' activity
            ((SpotifyStreamerApp) getActivity().getApplication())
                    .setArtists(items);
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
            mArrayAdapter
                    .SetNoImageBgColorStartIndex(size > 0 ? mRand.nextInt(size) : 0);
        }
    }
}


