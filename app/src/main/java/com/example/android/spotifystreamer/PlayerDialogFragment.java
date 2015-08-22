package com.example.android.spotifystreamer;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.concurrent.TimeUnit;

/**
 * Created by Alexander on 8/16/2015.
 */
public class PlayerDialogFragment extends DialogFragment {

    boolean mIsNewInstance = false;
    public PlayerDialogFragment() {
        mIsNewInstance = true;
    }

    private Uri mArtistUri;
    private String mPreviewUrl;
    private int mTrackPosition;
    private int mNoImageBgColorStartIndex;

    private String mArtistName;
    private Cursor mTracksCursor;

    private ImageButton mBtnPrevTrack;
    private ImageButton mBtnNextTrack;
    private ImageButton mBtnPlayOrPause;
    private TextView mElapsedTime;
    private TextView mTotalTime;
    private SeekBar mSeekBar;
    private boolean mIsPaused;

    private TextView mTextTrackName;
    private TextView mTextAlbumName;
    private TextView mTextNoTrackImage;
    private ImageView mTrackImage;

    private final String KEY_POSITION = "KEY_POSITION";
    private final String KEY_PROGRESS = "KEY_PROGRESS";
    private final String KEY_COLOR_INDEX = "KEY_COLOR_INDEX";
    private final String KEY_ARTIST_URI = "KEY_ARTIST_URI";
    private final String KEY_PREVIEW_URL = "KEY_PREVIEW_URL";
    private final String KEY_IS_PAUSED = "KEY_IS_PAUSED";

    public static final int COL_TRACK_SPOTIFY_ID = 0;
    public static final int COL_TRACK_NAME = 1;
    public static final int COL_ALBUM_NAME = 2;
    public static final int COL_TRACK_IMAGE_URI = 3;
    public static final int COL_IS_PLAYABLE = 4;
    public static final int COL_PREVIEW_URL = 5;

    private static final String[] TOPTRACKS_COLUMNS = {
            SpotifyContract.TrackEntry.COLUMN_TRACK_SPOTIFY_ID,
            SpotifyContract.TrackEntry.COLUMN_TRACK_NAME,
            SpotifyContract.TrackEntry.COLUMN_ALBUM_NAME,
            SpotifyContract.TrackImageEntry.COLUMN_URI,
            SpotifyContract.TrackEntry.COLUMN_IS_PLAYABLE,
            SpotifyContract.TrackEntry.COLUMN_PREVIEW_URL,
    };

    private static MediaPlayerService mPlayerService;

    private Handler mPlayerControlsHandler = new Handler();
    int mCurrentProgress = 0;
    int mMaxProgress = 0;

    private boolean mKeepUpdatingPlayerControls = true;
    //handler to change seek bar, elapsed time and state of play/pause button
    private Runnable mUpdatePlayerControls = new Runnable() {
        public void run() {
            //Update seek bar
            mCurrentProgress= 0;
            mMaxProgress = 0;
            if (mPlayerService.canSeekTo()) {
                mCurrentProgress = mPlayerService.getCurrentPosition();
                mMaxProgress = mPlayerService.getDuration();
            }
            if (mMaxProgress < mCurrentProgress) {
                mMaxProgress = mCurrentProgress;
            }
            mSeekBar.setMax(mMaxProgress);
            mSeekBar.setProgress(mCurrentProgress);

            //Update text with elapsed and total time
            mElapsedTime.setText(String.format("%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) mCurrentProgress),
                    TimeUnit.MILLISECONDS.toSeconds((long) mCurrentProgress)));
            mTotalTime.setText(String.format("%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) mMaxProgress),
                    TimeUnit.MILLISECONDS.toSeconds((long) mMaxProgress)));

            //Update state of play/pause button

            mIsPaused = true;
            FragmentActivity activity = getActivity();
            if (activity != null) {
                MediaPlayerService.PlaybackState state = mPlayerService.getPlaybackState();
                if (state == MediaPlayerService.PlaybackState.PREPARING) {
                    Utility.setImageButtonEnabled(getActivity(), false,
                            mBtnPlayOrPause, android.R.drawable.ic_media_play); //button 'Play' disabled
                } else if (mPlayerService.isPlaying()) {
                    mIsPaused = false;
                    Utility.setImageButtonEnabled(getActivity(), true,
                            mBtnPlayOrPause, android.R.drawable.ic_media_pause); //button 'Pause' enabled
                } else {
                    Utility.setImageButtonEnabled(getActivity(), true,
                            mBtnPlayOrPause, android.R.drawable.ic_media_play); //button 'Play' enabled
                }
            }
            if (mKeepUpdatingPlayerControls) {
                //repeat yourself in 100 miliseconds
                mPlayerControlsHandler.postDelayed(this, 100);
            }
        }
    };

    public void setDialogData(Uri artistUri, int position, int noImageBgColorStartIndex) {
        mArtistUri = artistUri;
        mTrackPosition = position;
        mNoImageBgColorStartIndex = noImageBgColorStartIndex;
    }

    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mTrackPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.

        outState.putParcelable(KEY_ARTIST_URI, mArtistUri);
        outState.putString(KEY_PREVIEW_URL, mPreviewUrl);
        outState.putInt(KEY_POSITION, mTrackPosition);
        outState.putInt(KEY_PROGRESS, mCurrentProgress);
        outState.putInt(KEY_COLOR_INDEX, mNoImageBgColorStartIndex);
        outState.putBoolean(KEY_IS_PAUSED, mIsPaused);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        View view = inflater.inflate(R.layout.player_view, container, false);

        if (savedInstanceState != null) {
            mArtistUri = savedInstanceState.getParcelable(KEY_ARTIST_URI);
            mPreviewUrl = savedInstanceState.getString(KEY_PREVIEW_URL);
            mTrackPosition = savedInstanceState.getInt(KEY_POSITION);
            mNoImageBgColorStartIndex = savedInstanceState.getInt(KEY_COLOR_INDEX);
            mCurrentProgress = savedInstanceState.getInt(KEY_PROGRESS);
            mIsPaused = savedInstanceState.getBoolean(KEY_IS_PAUSED);
        }
        mArtistName = Utility.getArtistName(getActivity(), mArtistUri);

        //TODO: add function parameter screen size to return most suitalbe image
        //currently it will return largest image from available which is OK for
        //display quality but not OK for load time and amount of downloaded data.
        //Content provider supports specifying image size in query parameter of URI
        mTracksCursor = Utility.getTracksForArtistUri(getActivity(),
                mArtistUri, TOPTRACKS_COLUMNS);
        if (mTrackPosition > mTracksCursor.getCount() - 1) {
            mTrackPosition = mTracksCursor.getCount() - 1;
        }
        if (mTrackPosition < 0) {
            mTrackPosition = 0;
        }
        if (!mTracksCursor.moveToPosition(mTrackPosition)) {
            if (!mTracksCursor.moveToFirst()) {
                mTracksCursor.close();
            }
        }

        ((TextView)view.findViewById(R.id.artistName)).setText(mArtistName == null ? "" : mArtistName);
        mTextTrackName = (TextView)view.findViewById(R.id.trackName);
        mTextAlbumName = (TextView)view.findViewById(R.id.albumName);

        mTrackImage = (ImageView)view.findViewById(R.id.imageView);
        mTextNoTrackImage = (TextView)view.findViewById(R.id.textNoImage);

        mElapsedTime = (TextView)view.findViewById(R.id.elapsedTime);
        mTotalTime = (TextView)view.findViewById(R.id.totalTime);

        mBtnPrevTrack = (ImageButton)view.findViewById(R.id.prevTrack);
        mBtnPrevTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackPosition > 0) {
                    mTrackPosition--;
                }
                updateTrackDataOnView();
            }
        });
        mBtnNextTrack = (ImageButton)view.findViewById(R.id.nextTrack);
        mBtnNextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackPosition < mTracksCursor.getCount() - 1) {
                    mTrackPosition++;
                }
                updateTrackDataOnView();
            }
        });
        mBtnPlayOrPause = (ImageButton)view.findViewById(R.id.playOrPause);
        mBtnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService.isPlaying()) {
                    pausePlaying(0);
                } else {
                    startPlaying(0);
                }
            }
        });
        mSeekBar = (SeekBar)view.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mPlayerService.canSeekTo()) {
                        mPlayerService.seekTo(progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        doBindService();

        return view;
    }

    private void initializePlayer() {
        if (mPlayerService.getPlaybackState() == MediaPlayerService
                .PlaybackState.ILLEGAL_STATE_EXCEPTION) {
            mPlayerService.reset(); //reset to idle state if it was in an unexpected state
        }
        if (mPlayerService.getPlaybackState() == MediaPlayerService
                .PlaybackState.IDLE) {
            if (mPreviewUrl != null) {
                mPlayerService.setDataSource(mPreviewUrl); //set data source if it is in idle state
            }
        }
    }

    private void pausePlaying(int currentProgress) {
        initializePlayer();
        mPlayerService.pause(currentProgress);
        //if currentProgress > 0 player will initialize, seek to current progress and then pause
        //need to keep controls updated to reflect player's final state when it's paused
        mKeepUpdatingPlayerControls = currentProgress > 0;
        mPlayerControlsHandler.postDelayed(mUpdatePlayerControls, 100);
    }

    private void startPlaying(int currentProgress) {
        initializePlayer();
        //start (after prepare if required)
        mPlayerService.start(currentProgress);
        //keep updating controls
        mKeepUpdatingPlayerControls = true;
        mPlayerControlsHandler.postDelayed(mUpdatePlayerControls, 100);
    }

    private void updateTrackDataOnView() {
        if (mTracksCursor != null && mTracksCursor.getCount() > 0) {
            mTracksCursor.moveToPosition(mTrackPosition);

            mPreviewUrl = mTracksCursor.getString(COL_PREVIEW_URL);

            Utility.setImageButtonEnabled(getActivity(), mTrackPosition > 0,
                    mBtnPrevTrack, android.R.drawable.ic_media_previous);
            Utility.setImageButtonEnabled(getActivity(), mTrackPosition < mTracksCursor.getCount() - 1,
                    mBtnNextTrack, android.R.drawable.ic_media_next);

            String text;
            text = mTracksCursor.getString(COL_TRACK_NAME);
            mTextTrackName.setText(text == null ? "" : text);
            text = mTracksCursor.getString(COL_ALBUM_NAME);
            mTextAlbumName.setText(text == null ? "" : text);

            //If player is playing music, then stop it and change to next track
            boolean wasPlaying = false;
            MediaPlayerService.PlaybackState state = mPlayerService.getPlaybackState();
            if (mTrackPosition != mPlayerService.getTrackPosition()){
                wasPlaying = mPlayerService.isPlaying();
                if (mPlayerService.getPlaybackState() ==  MediaPlayerService.PlaybackState.END) {
                    mPlayerService.fullReset(false);
                }
                if (mPlayerService.getPlaybackState() !=  MediaPlayerService.PlaybackState.IDLE) {
                    mPlayerService.reset();
                }
            }

            String imageUri = mTracksCursor.getString(COL_TRACK_IMAGE_URI);

            // *** set image ***
            if (imageUri != null && !imageUri.isEmpty()) {
                Picasso.with(getActivity()).load(imageUri)
                        //.resize(holder.icon.getLayoutParams().width, holder.icon.getLayoutParams().height)
                        //.centerCrop()
                        .into(mTrackImage);
                mTrackImage.setVisibility(View.VISIBLE);
                mTextNoTrackImage.setVisibility(View.GONE);
            } else {
                // If images are NOT available, then show text "No Image" with backgrounds of different
                // colors so that visually they would NOT be identified as items of the same kind
                int colorIndex = (mTrackPosition + mNoImageBgColorStartIndex) %
                        Utility.NOIMAGE_BACKGROUND_COLOR_IDS.length;
                int bgId = Utility.NOIMAGE_BACKGROUND_COLOR_IDS[colorIndex];
                mTrackImage.setVisibility(View.GONE);
                mTextNoTrackImage.setVisibility(View.VISIBLE);
                mTextNoTrackImage.setBackgroundColor(getActivity().getResources().getColor(bgId));
            }

            if (wasPlaying) {
                startPlaying(0);
            } else {
                //reset seek bar and duration
                mKeepUpdatingPlayerControls = false;
                mPlayerControlsHandler.postDelayed(mUpdatePlayerControls, 1);
            }
        }
    }

    @Override
    public void onDestroy() {
        mKeepUpdatingPlayerControls = false;
        doUnbindService();
        if (mTracksCursor != null)
            mTracksCursor.close();
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private boolean mIsBound = true;

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        if (getActivity().bindService(new Intent(getActivity(),
                MediaPlayerService.class), mConnection, Context.BIND_AUTO_CREATE)) {
            mIsBound = true;
        }
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mPlayerService = ((MediaPlayerService.LocalBinder)service).getService();
            updateTrackDataOnView();
            if (mIsNewInstance) {
                mIsNewInstance = false;
                if (mCurrentProgress > 0 && mIsPaused) {
                    pausePlaying(mCurrentProgress);
                } else {
                    startPlaying(mCurrentProgress);
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mPlayerService = null;
        }
    };
}
