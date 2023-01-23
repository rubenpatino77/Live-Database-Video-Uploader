package com.example.videouploader;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoPlayerActivity extends AppCompatActivity {

    Intent intent;

    SimpleExoPlayerView exoPlayerView;
    SimpleExoPlayer exoPlayer;
    String videoURL;

    ImageView[] allImageButtons;
    ImageView backButton;
    ImageView moreButton;
    ImageView rewind;
    ImageView fastForward;
    ImageView previous;
    ImageView next;
    ImageView pause;
    ImageView play;
    TextView videoTitle;
    String titleText;
    ImageView lock;
    DefaultTimeBar timeBar;
    boolean locked;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_player);

        exoPlayerView = findViewById(R.id.exoplayer_view);
        timeBar = findViewById(R.id.exo_progress);


        allImageButtons = createImageButtonArray();
        play.setColorFilter(Color.GREEN);
        locked = false;

        intent = getIntent();
        Bundle extras = intent.getExtras();
        videoURL = extras.getString("VIDEO");
        titleText = extras.getString("videoName");

        setupPlayer();
        videoTitle.setText(titleText);

        applyButtonActions();

    }

    private void lockPressed(boolean locked){
        if(locked){
            for (ImageView button: allImageButtons) {
                button.setVisibility(View.INVISIBLE);
            }
            lock.setVisibility(View.VISIBLE);
            lock.setImageResource(R.drawable.ic_locked);
        } else {
            for (ImageView button: allImageButtons) {
                button.setVisibility(View.VISIBLE);
            }
            lock.setImageResource(R.drawable.ic_unlock);
        }

        timeBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return locked;
            }
        });
    }

    private void setupPlayer(){
        videoTitle = findViewById(R.id.video_title);

        try {
            // BandwidthMeter is used for getting default bandwidth
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

            // track selector is used to navigate between video using a default seekbar.
            TrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));

            // we are adding our track selector to exoplayer.
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

            // we are parsing a video url and parsing its video uri.
            Uri videoUri = Uri.parse(videoURL);

            // we are creating a variable for datasource factory and setting its user agent as 'exoplayer_view'
            // DefaultDataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, videoURL);
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, videoURL);

            // we are creating a variable for extractor factory and setting it to default extractor factory.
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

            // we are creating a media source with above variables and passing our event handler as null,
            MediaSource mediaSource = new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null);

            // inside our exoplayer view we are setting our player
            exoPlayerView.setPlayer(exoPlayer);

            // we are preparing our exoplayer with media source.
            exoPlayer.prepare(mediaSource);

            // we are setting our exoplayer when it is ready.
            exoPlayer.setPlayWhenReady(true);

        } catch (Exception e) {
            // below line is used for handling our errors.
            Log.e("TAG", "Error : " + e.toString());
        }
    }

    private ImageView[] createImageButtonArray(){
        ImageView[] buttonsArray;
        List<ImageView> buttonsList = new ArrayList<>();

        buttonsList.add(backButton = findViewById(R.id.video_back));
        buttonsList.add(moreButton = findViewById(R.id.video_more));
        buttonsList.add(rewind = findViewById(R.id.rewind));
        buttonsList.add(fastForward = findViewById(R.id.fast_fwd));
        buttonsList.add(previous = findViewById(R.id.prev_button));
        buttonsList.add(next = findViewById(R.id.next_button));
        buttonsList.add(pause = findViewById(R.id.pause_button));
        buttonsList.add(play = findViewById(R.id.play_button));
        buttonsList.add(lock = findViewById(R.id.unlock));

        Object[] tempArray = buttonsList.toArray();
        buttonsArray = Arrays.copyOf(tempArray, tempArray.length, ImageView[].class);

        return buttonsArray;
    }

    private void applyButtonActions(){
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.setPlayWhenReady(true);
                play.setColorFilter(Color.GREEN);
                pause.setColorFilter(Color.WHITE);
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.setPlayWhenReady(false);
                play.setColorFilter(Color.WHITE);
                pause.setColorFilter(Color.GREEN);
            }
        });

        fastForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.seekTo(exoPlayer.getCurrentPosition() + 5000);
                if(exoPlayer.getCurrentPosition() > exoPlayer.getDuration()){
                    exoPlayer.seekTo(exoPlayer.getDuration());
                }
            }
        });

        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 5000);
                if(exoPlayer.getCurrentPosition() < 0){
                    exoPlayer.seekTo(0);
                }
            }
        });

        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locked = !locked;
                lockPressed(locked);
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.seekTo(0);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exoPlayer.seekTo(exoPlayer.getDuration());
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //NOTHING FOR RIGHT NOW
            }
        });
    }
}