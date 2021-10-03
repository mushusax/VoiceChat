package com.example.voicechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private String TAG = "VOICE";

    private Button btnRecord;
    private int bufferSize = 4096;
    private int sampleRate = 32000;

    private AudioRecord audioRecord;
    private Thread recordingThread;

    private File rawFile;
    private File encodedFile;
    private boolean isRecording = false;
    private FileOutputStream fileOutputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (view.getId()) {
            case R.id.btnRecord:
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "Btn pressed");
                    requestPermission();
                    startRecording();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "Btn released");
                    stopRecording();
                }
                return true;

            default:
                return false;
        }
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void stopRecording() {
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        recordingThread = null;
    }

    private void startRecording() {

        //create recorder
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //create file
        rawFile = new File(getFilesDir(), "raw.wav");
        try {
            fileOutputStream = new FileOutputStream(rawFile);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found. Ending recording");
            e.printStackTrace();
            return;
        }

        audioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(32000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        audioRecord.startRecording();
        isRecording = true;
        recordingThread = new Thread(() -> {
           writeToFile();
        });
        recordingThread.start();
    }

    private void writeToFile() {

        while(isRecording) {
            try {
                Log.d(TAG, "recording");
                byte[] bytes = new byte[4096];
                while (audioRecord != null && audioRecord.read(bytes, 0, 4096) != -1) {
                    fileOutputStream.write(bytes);
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "could not open output stream to rawFile");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "could not write to rawFile output stream");
                e.printStackTrace();
            }
        }

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}