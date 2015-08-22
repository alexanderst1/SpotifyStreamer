package com.example.android.spotifystreamer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements MainActivityFragment.Callback {

    public final static String EXTRA_ARTIST_ID =
            "com.example.android.spotifystreamer.EXTRA_ARTIST_ID";
    public final static String EXTRA_ARTIST_NAME =
            "com.example.android.spotifystreamer.EXTRA_ARTIST_NAME";
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mCountry = "";
    private static final String TOPTRACKSFRAGMENT_TAG = "TTFTAG";
    private boolean mTwoPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.toptracks_container) != null) {
            // The top tracks container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.toptracks_container, new TopTracksActivityFragment(),
                                TOPTRACKSFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        String country = Utility.getCountryCodeFromPrefs(this);
//        // update the location in second pane using the fragment manager
//        if (country != null && !country.equals(mCountry)) {
//            TopTracksActivityFragment fragment =
//                    (TopTracksActivityFragment) getSupportFragmentManager()
//                            .findFragmentById(R.id.toptracks_container);
//            if (null != fragment) {
//                fragment.onCountryChanged();
//            }
//            mCountry = country;
//        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {

            Utility.setArtistNameInSubtitleOfActionBar(this, contentUri);

            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(TopTracksActivityFragment.KEY_ARTIST_URI, contentUri);

            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.toptracks_container, fragment, TOPTRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTracksActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }
}
