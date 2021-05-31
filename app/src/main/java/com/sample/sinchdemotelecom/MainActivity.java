package com.sample.sinchdemotelecom;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.sample.sinchdemotelecom.audio.IncommingCallActivity;
import com.sample.sinchdemotelecom.video.CallerVideoScreenActivity;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;

import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListener {

    public static int CALL_TYPE_AUDIO = 0;
    public static int CALL_TYPE_VIDEO = 1;

    private ProgressDialog mSpinner;
    private TextView mCallerId;
    private EditText mRecipientId;
    private Button mCallButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCallerId = findViewById(R.id.main_myid);
        mRecipientId = findViewById(R.id.callName);
        mCallButton = findViewById(R.id.callButton);

        String userName = (""+(Build.FINGERPRINT+Build.MODEL).hashCode()).replace("-","");
        mCallerId.setText(userName);


        mCallButton.setOnClickListener(view -> {
            if (!getSinchServiceInterface().isStarted()) {
                initService();
            }else {
                showType();
            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         int id = item.getItemId();



        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);
        mCallButton.setEnabled(true);
    }

    @Override
    protected void onPause() {
        if (mSpinner != null) {
            mSpinner.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
        if (mSpinner != null) {
            mSpinner.dismiss();
        }
    }




    @Override
    public void onStarted() {
        if (mSpinner != null) {
            mSpinner.dismiss();
        }

        showType();


    }



    public void showType(){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Choose Type");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1
        );

        arrayAdapter.add("Audio");
        arrayAdapter.add("Video");

        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
            switch (which) {
                case 0:
                    openPlaceCallActivity(CALL_TYPE_AUDIO);
                    break;
                case 1:
                    openPlaceCallActivity(CALL_TYPE_VIDEO);
                    break;

                default:
                    break;
            }
        });

        builderSingle.show();
    }



    private void initService() {
        String userName = mCallerId.getText().toString();



        if (userName.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!userName.equals(getSinchServiceInterface().getUserName())) {
            getSinchServiceInterface().stopClient();
        }

        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(mCallerId.getText().toString());
            showSpinner();
        }
    }

    private void openPlaceCallActivity(int type) {
        String callTo = mRecipientId.getText().toString();
        if (callTo.isEmpty()) {
            Toast.makeText(this, "Please enter a user to call", Toast.LENGTH_LONG).show();
            return;
        }
        Map<String,String> header = new HashMap<>();
        header.put("type", String.valueOf(type));


        if(type == CALL_TYPE_AUDIO){
            Call call = getSinchServiceInterface().callUserAudio(callTo,header);
            String callId = call.getCallId();


            Intent callScreen = new Intent(this, IncommingCallActivity.class);
            callScreen.putExtra(SinchService.CALL_ID, callId);
            callScreen.putExtra("incomming", false);
            callScreen.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(callScreen);


        }else if(type == CALL_TYPE_VIDEO) {
            Call call = getSinchServiceInterface().callUserVideo(callTo,header);
            String callId = call.getCallId();




            Intent callScreen = new Intent(this, CallerVideoScreenActivity.class);
            callScreen.putExtra(SinchService.CALL_ID, callId);
            startActivity(callScreen);
        }
    }


    private void showSpinner() {
        mSpinner = new ProgressDialog(this);
        mSpinner.setTitle("Logging in");
        mSpinner.setMessage("Please wait...");
        mSpinner.show();
    }

}