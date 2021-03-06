package com.royole.yogu.videoplayerlibrary;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.royole.yogu.videoplayerlibrary.utils.DensityUtil;
import com.royole.yogu.videoplayerlibrary.view.VideoController;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,MediaPlayer.OnBufferingUpdateListener,MediaPlayer.OnCompletionListener, VideoController.MediaControlImpl {
    private String Tag = "VideoPlayerActivity";

    private SurfaceView mVideoSurface;
    private MediaPlayer mPlayer;
    private VideoController mController;
    private FrameLayout mAnchorView;//surfaceView container
    private String mPath;
    private int mCurrentPosition;
    private int mCurrentBufferPercentage;
    private View mProgressBarView;//loading progressBar container
    private View mReplayView;// display replay button
    private View mReplayBtn;


    //lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_video_player);
        mPath = getIntent().getStringExtra("path");
        Log.d(Tag, "mpath:" + mPath);

        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getInt("videoPosition", 0);
        }
        initView();
    }

    private void initView() {
        mVideoSurface = (SurfaceView) findViewById(R.id.videoSurfaceView);
        SurfaceHolder videoHolder = mVideoSurface.getHolder();
        videoHolder.addCallback(this);
        mProgressBarView = findViewById(R.id.progressbar);
        mController = new VideoController(this);
        mController.setMediaPlayer(this);
        mAnchorView = (FrameLayout) findViewById(R.id.videoSurfaceContainer);
        mController.setAnchorView(mAnchorView);
        //set shrink mode
        LinearLayout.LayoutParams linearLayoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(DensityUtil.getHeightInPx(this)*0.5));
        mAnchorView.setLayoutParams(linearLayoutParams);
        //replay
        mReplayView = findViewById(R.id.replayContainer);
        mReplayBtn = findViewById(R.id.replayBtn);
        mReplayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReplayView.setVisibility(View.GONE);
                mController.setVisibility(View.VISIBLE);
                mController.doPauseResume();
                mController.setProgress();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("videoPosition", mCurrentPosition);
    }

    /**
     * config the height of the player when orientation changes
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (null == mPlayer) return;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(Tag,"onConfigurationChanged...ORIENTATION_LANDSCAPE");
            LinearLayout.LayoutParams linearLayoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            mAnchorView.setLayoutParams(linearLayoutParams);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(Tag,"onConfigurationChanged...ORIENTATION_PORTRAIT");
            LinearLayout.LayoutParams linearLayoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(DensityUtil.getHeightInPx(this)*0.5));
            mAnchorView.setLayoutParams(linearLayoutParams);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCurrentPosition = this.getCurrentPosition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
    // End lifecycle

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mController.show();
        return false;
    }

    // Implement SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // 创建一个MediaPlayer对象
            mPlayer = new MediaPlayer();
            // 设置播放的视频数据源
            if(mPath.startsWith("http://")){
                mPlayer.setDataSource(this, Uri.parse(mPath));
            }else {
                AssetFileDescriptor descriptor = getAssets().openFd(mPath);
                mPlayer.setDataSource(descriptor.getFileDescriptor(),
                        descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();
            }
            // 设置AudioStreamType
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 将视频输出到SurfaceView
            mPlayer.setDisplay(holder);
            // 播放准备，使用异步方式，配合OnPreparedListener
            mPlayer.prepareAsync();
            // 设置相关的监听器
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnCompletionListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
    // End SurfaceHolder.Callback

    // Implement MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        mController.show();
        mPlayer.start();
        this.seekTo(mCurrentPosition);
        mController.updatePausePlay();
        mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                /*
                     * add what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING
                     * fix : return what == 700 in Lenovo low configuration Android System
                     */
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START
                        || what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
                    mProgressBarView.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }

        });
    }
    // End MediaPlayer.OnPreparedListener

    // Implement OnBufferingUpdateListener
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(Tag,"onBufferingUpdate..."+percent);
        mCurrentBufferPercentage = percent;
    }
    // End OnBufferingUpdateListener

    // Implement OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
        mReplayView.setVisibility(View.VISIBLE);
        mController.setVisibility(View.GONE);
    }
    // End OnCompletionListener

    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        if (mPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mPlayer != null) {
            Log.d(Tag,"mCurrentBufferPercentage:"+mCurrentBufferPercentage);
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public int getDuration() {
        if (mPlayer != null){
            return mPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    @Override
    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Override
    public void seekTo(int i) {
        if (mPlayer != null) {
            mPlayer.seekTo(i);
        }
    }

    @Override
    public void start() {
        if (mPlayer != null) {
            mPlayer.start();
        }
    }

    @Override
    public boolean isFullScreen() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public void toggleFullScreen() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    // End VideoMediaController.MediaPlayerControl
}
