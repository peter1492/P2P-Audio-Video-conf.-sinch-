package com.sample.sinchdemotelecom.audio;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sample.sinchdemotelecom.AudioPlayer;
import com.sample.sinchdemotelecom.BaseActivity;
import com.sample.sinchdemotelecom.R;
import com.sample.sinchdemotelecom.SinchService;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.calling.CallState;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class IncommingCallActivity extends BaseActivity implements View.OnClickListener, SensorEventListener {
    static final String ADDED_LISTENER = "addedListener";
    private static final String TAG = "IncommingCallActivity";

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;

     private TextView mCallDuration;
    private TextView mCallState;
    private TextView mCallerName;


    private TextView mCallingStatus;
    private TextView mCallingName;
    private LinearLayout mCallingNotify;
    private Button mCallingAnswer;
    private Button mCallingReject;
    private LinearLayout mCallingActionButton;
    private Call call;
    private Ringtone r;
    private boolean isIncomming;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private View mCallingBlacksreen;
    private String mCallId;
    private boolean mAddedListener = false;

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            IncommingCallActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCallDuration();
                }
            });
        }
    }

    private void updateCallDuration() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            mCallDuration.setText(formatTimespan(call.getDetails().getDuration()));
        }
    }

    private String formatTimespan(int totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.incommingcall_layout);



        initView();

        mAudioPlayer = new AudioPlayer(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);


    }
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
    }

    @Override
    public void onServiceConnected() {
         call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        updateUI();
    }

    private void updateUI() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }
        isIncomming=getIntent().getBooleanExtra("incomming", true);


        mCallerName.setText(call.getRemoteUserId());
        mCallState.setText(call.getState().toString());

        if(isIncomming) {
            setBlinking(mCallingNotify, true);
            mCallingStatus.setText("TELEPON MASUK");
            mCallingName.setText(call.getRemoteUserId()+"");
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ////UiUtils.setFullscreen(this, true);
            r.play();
        }else{
            ////UiUtils.setFullscreen(this, false);
            mCallingStatus.setText("MEMANGGIL...");
            mCallingAnswer.setVisibility(View.GONE);
            mCallingName.setText(call.getRemoteUserId()+"");
            mCallingReject.setText("END");
        }
    }


    private void initView() {
        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);


        mCallingStatus = findViewById(R.id.calling_status);
        mCallingName = findViewById(R.id.calling_name);
        mCallingNotify = findViewById(R.id.calling_notify);
        mCallingAnswer = findViewById(R.id.calling_answer);
        mCallingAnswer.setOnClickListener(this);
        mCallingReject = findViewById(R.id.calling_reject);
        mCallingReject.setOnClickListener(this);
        mCallingActionButton = findViewById(R.id.calling_action_button);
        mCallingBlacksreen=findViewById(R.id.calling_blackscreen);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.calling_answer:
                call.answer();
                mCallingAnswer.setVisibility(View.GONE);
                mCallingReject.setText("END");
                mCallingStatus.setText("ACTIVE CALL");
                setBlinking(mCallingNotify, false);
                if(r!=null)r.stop();
                //UiUtils.setFullscreen(this, false);
                break;
            case R.id.calling_reject:
                call.hangup();
                if(r!=null)r.stop();
                finish();
                break;
        }
    }




    @Override
    public void onStop() {
        super.onStop();
        mDurationTask.cancel();
        mTimer.cancel();
    }

    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mDurationTask = new UpdateCallDurationTask();
        mTimer.schedule(mDurationTask, 0, 500);
        updateUI();
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();


    }


    private void setBlinking(View object, boolean status) {
        if(!status){
            object.animate().cancel();
            return;
        }
        ObjectAnimator anim= ObjectAnimator.ofFloat(object, View.ALPHA, 0.1f,1.0f);
        anim.setDuration(1000);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(event.values[0]==0){
            if(!isIncomming || call.getState() == CallState.ESTABLISHED) {
                params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                params.screenBrightness = 0;
                getWindow().setAttributes(params);
                //UiUtils.enableDisableViewGroup((ViewGroup) findViewById(R.id.calling_root).getParent(), false);
                //UiUtils.setFullscreen(this, true);
                mCallingBlacksreen.setVisibility(View.VISIBLE);
            }
        }else {
            params.screenBrightness = -1;
            getWindow().setAttributes(params);
            //UiUtils.enableDisableViewGroup((ViewGroup)findViewById(R.id.calling_root).getParent(),true);
            //UiUtils.setFullscreen(this, false);
            mCallingBlacksreen.setVisibility(View.GONE);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }


    public class SinchCallListener implements CallListener {
        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            mCallState.setText(call.getState().toString());
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = getSinchServiceInterface().getAudioController();
            audioController.enableSpeaker();

            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            Toast.makeText(IncommingCallActivity.this, endMsg, Toast.LENGTH_LONG).show();

            endCall();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> list) {

        }
    }

}
