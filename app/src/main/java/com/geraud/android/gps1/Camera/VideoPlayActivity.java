package com.geraud.android.gps1.Camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.geraud.android.gps1.Chat.TransferActivity;
import com.geraud.android.gps1.Models.Stories;
import com.geraud.android.gps1.R;
import com.geraud.android.gps1.Utils.RandomStringGenerator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class VideoPlayActivity extends AppCompatActivity implements
        SurfaceHolder.Callback,
        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mMediaPlayer;
    private Uri mVideoUri;
    private SurfaceView mSurfaceView;
    private AudioManager mAudioManager;
    private IntentFilter mNoisyIntentFilter;
    private AudioBecomingNoisy mAudioBecomingNoisy;

    private DatabaseReference mDatabaseReference;
    private StorageReference mStorageReference;

    private EditText mDescriptionEditText;
    private Button mPostButton;


    private boolean location = true;
    private double mLatitude = 0;
    private double mLongitude = 0;

    private String mDescription;

    private class AudioBecomingNoisy extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mediaPause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        mDescriptionEditText = findViewById(R.id.descriptionEditText);
        mPostButton = findViewById(R.id.postButton);
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent().getExtras() != null) {
                    Intent transferIntent = new Intent(getApplicationContext(), TransferActivity.class);
                    transferIntent.putExtra("uri", mVideoUri);
                    transferIntent.putExtra("text", mDescriptionEditText.getText().toString() == null ? " " : mDescriptionEditText.getText().toString());
                    startActivity(transferIntent);
                    finish();
                } else
                    postStatus();
            }
        });

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("STORIES").child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        mStorageReference = FirebaseStorage.getInstance().getReference().child("STORIES").child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        mSurfaceView = findViewById(R.id.videoSurfaceView);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //play video
                        mediaPlay();
                        break;

                    case MotionEvent.ACTION_UP:
                        // pause video
                        mediaPause();
                        break;
                }
                return true;
            }
        });

        Intent callingIntent = this.getIntent();
        if (callingIntent != null) {
            mVideoUri = callingIntent.getData();
        }
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mAudioBecomingNoisy = new AudioBecomingNoisy();
        mNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    }

    @Override
    protected void onStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer == null) {
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.addCallback(this);
        }
    }

    private void mediaPlay() {
        registerReceiver(mAudioBecomingNoisy, mNoisyIntentFilter);
        int requestAudioFocusResult = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (requestAudioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mMediaPlayer.start();
        }
    }

    private void mediaPause() {
        mMediaPlayer.pause();
        mAudioManager.abandonAudioFocus(this);
        unregisterReceiver(mAudioBecomingNoisy);
    }

    private MediaPlayer.OnCompletionListener mMediaPlayerOnCompleteListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            // video was played till the end
            mediaPause();
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mMediaPlayer = MediaPlayer.create(this, mVideoUri, holder);
        mMediaPlayer.setOnCompletionListener(mMediaPlayerOnCompleteListener);
        mediaPlay();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onAudioFocusChange(int audioFocusChanged) {
        switch (audioFocusChanged) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaPause();
                break;

            case AudioManager.AUDIOFOCUS_GAIN:
                mediaPlay();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mediaPause();
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void postStatus() {

        mDescription = mDescriptionEditText.getText().toString() == null ? " " : mDescriptionEditText.getText().toString();

        if (mLatitude == 0 && mLongitude == 0)
            location = false;

        StorageReference filepath = mStorageReference.child(RandomStringGenerator.randomString() + ".jpg");
        UploadTask uploadTask = filepath.putFile(mVideoUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Stories stories = new Stories(location, taskSnapshot.getDownloadUrl().toString(),
                        mDescription, System.currentTimeMillis(), mLatitude, mLongitude, "video",
                        FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

                mDatabaseReference.push().setValue(stories).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Successfully created story", Toast.LENGTH_SHORT).show();
                            finish();
                        } else
                            Toast.makeText(getApplicationContext(), "Couldn't create story", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
