package com.corbinbecker.Listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import com.corbinbecker.activities.WordShowcase;

import java.util.ArrayList;

/**
 * Created by corbinbecker
 * Returns the result of the speech recognitions and calls back when the
 * service is ready and when it is recording
 */
public class SpeechRecognitionListener implements RecognitionListener{
    private Context context;

    private recordingListener listener;

    public SpeechRecognitionListener(Context context) {
        this.context = context;
    }

    public void setListener(recordingListener listener) {
        this.listener = listener;
    }

    //informs classes when service is recording
    public void setRecording(boolean recording) {
        if (this.listener != null) {
            this.listener.onRecordingChanged(recording);
        }
    }

    //informs classes when service is ready
    public void setReady(boolean ready) {
        if (this.listener != null) {
            this.listener.onReadyChanged(ready);
        }
    }


    public interface recordingListener {
        public void onRecordingChanged(boolean recording);
        public void onReadyChanged(boolean ready);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        setReady(true);
    }

    @Override
    public void onBeginningOfSpeech() {
        setRecording(true);
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        setRecording(false);
        setReady(false);
    }

    @Override
    public void onError(int error) {
        setReady(false);

    }

    //send results back to calling class
    @Override
    public void onResults(Bundle results) {
        String recognisedString = "";
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        recognisedString = recognisedString + result.get(0);
        Intent intent = new Intent(this.context, WordShowcase.class);
        intent.putExtra("parent", "speechrec");
        intent.putExtra("word", recognisedString);
        this.context.startActivity(intent);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
