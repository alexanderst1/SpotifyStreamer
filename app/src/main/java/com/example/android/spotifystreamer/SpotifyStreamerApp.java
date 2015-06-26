package com.example.android.spotifystreamer;

import android.app.Application;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by Alexander on 6/24/2015.
 */
public class SpotifyStreamerApp extends Application {
    // Global list of variables to allow users, when users click back on activity 'Top 10 Tracks'
    // to find main activity intact, i.e. with same list of artists and scrolled position

    private List<Artist> artists = null;
    public List<Artist> getArtists(){
        return artists;
    }
    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    private Pair<Integer,Integer> pairIndexOffset = new Pair<Integer, Integer>(0,0);
    public Pair<Integer,Integer> getArtistsListFirstVisibleIndex() {
        return pairIndexOffset;
    }
    public void setArtistsListFirstVisibleIndex(Pair<Integer,Integer> pairIndexOffset){
        this.pairIndexOffset = pairIndexOffset;
    }

    private String artistSearchString = null;
    public String getArtistSearchString(){
        return artistSearchString;
    }
    public void setArtistSearchString(String artistSearchString) {
        this.artistSearchString = artistSearchString;
    }

}
