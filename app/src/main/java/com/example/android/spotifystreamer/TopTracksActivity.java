package com.example.android.spotifystreamer;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class TopTracksActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivity.EXTRA_ARTIST_NAME)) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setSubtitle(intent.getStringExtra(MainActivity.EXTRA_ARTIST_NAME));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create menu even if currently it has only one item 'settings' which will be hidden
        // as in the future we might add more menu items

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_settings);
        // If menu item 'settings' is visible, then user would be able to change country
        // and would expect this activity to be refreshed automatically and return top tracks
        // for newly selected country. As this behavior is not implemented, 'settings' item is hidden.
        // If this menu were to have other menu items, they would still be visible...
        item.setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Settings will be available from main activity only (otherwise need to implement
            // top track auto refresh after changing country)
            //startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
