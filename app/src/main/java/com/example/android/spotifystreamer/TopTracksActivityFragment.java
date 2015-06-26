package com.example.android.spotifystreamer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

/**
 * Created by Alexander on 6/23/2015.
 */
public class TopTracksActivityFragment extends Fragment {
    SpotifyArrayAdapter<Track> mArrayAdapter;
    Random mRand = new Random();
    private Toast mToast = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mArrayAdapter = new SpotifyArrayAdapter<Track>(getActivity(),
                R.layout.list_item, new ArrayList<Track>());

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.top_tracks);
        listView.setAdapter(mArrayAdapter);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(MainActivity.EXTRA_ARTIST_ID)) {
            GetTopTracksTask task = new GetTopTracksTask();
            task.execute(intent.getStringExtra(MainActivity.EXTRA_ARTIST_ID));
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //TODO: implement at stage 2
            }
        });

        return rootView;
    }

    public class GetTopTracksTask extends AsyncTask<String, Void, Tracks> {
        private final String LOG_TAG = GetTopTracksTask.class.getSimpleName();

        private String getCountryCodeFromPrefs() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return prefs.getString(getString(R.string.pref_country_id_key),
                    getString(R.string.pref_country_id_default));
        }
        private String getCountryNameFromPrefs() {
            // See SettingsActivity.sBindPreferenceSummaryToValueListener
            // key 'country_id'       --> value 'US'
            // key 'country_id_title' --> value 'United States of America'
            // would like to show full country name on a toast that no tracks were found
            // for that country (as for the user decode country code might be difficult).
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return prefs.getString(getString(R.string.pref_country_id_key) +
                            getString(R.string.pref_country_suffix_for_title),
                    getString(R.string.pref_country_id_default));
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
                String countryCode = getCountryCodeFromPrefs();
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
            mArrayAdapter.clear();
            if (result != null) {
                items = result.tracks;
                size = items.size();
                for (Track item : items) {
                    mArrayAdapter.add(item);
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
                                getCountryNameFromPrefs(), Toast.LENGTH_SHORT);
                mToast.show();
            }
            // Set random number which will be used for initializing sequence
            // of background colors used for "No Image" icons (for tracks not having images)
            mArrayAdapter
                    .SetNoImageBgColorStartIndex(size > 0 ? mRand.nextInt(size) : 0);
        }
    }

}
