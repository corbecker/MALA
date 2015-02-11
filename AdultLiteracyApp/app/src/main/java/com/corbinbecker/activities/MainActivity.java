package com.corbinbecker.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.corbinbecker.Listeners.SpeechRecognitionListener;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.services.TTSHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
Created by Corbin Becker
Main entry point to the application. Displays buttons that link to
all the other application activities
 */
public class MainActivity extends ActionBarActivity implements View.OnClickListener, TTSHelper.speakingListener,
SpeechRecognitionListener.recordingListener, TTSHelper.loadingListener{

    //external storage directory
    private String DIR = Environment.getExternalStorageDirectory().toString() + "/MALA/";

    private SharedPreferences sharedPreferences;
    private DatabaseManager databaseManager;
    private TTSHelper ttsHelper = null;
    private SpeechRecognizer recognizer;

    //holds the images for the alphabet activity
    private int[] images = {R.drawable.a, R.drawable.b, R.drawable.c, R.drawable.d,
            R.drawable.e, R.drawable.f, R.drawable.g, R.drawable.h, R.drawable.i,
            R.drawable.j, R.drawable.k, R.drawable.l, R.drawable.m, R.drawable.n,
            R.drawable.o, R.drawable.p, R.drawable.q, R.drawable.r, R.drawable.s,
            R.drawable.t, R.drawable.u, R.drawable.v, R.drawable.w, R.drawable.x,
            R.drawable.y, R.drawable.z};

    private Button ocrButton, speechRecognitionButton;
    private Button currentSpeakingButton = null;
    private MenuItem currentSpeakingMenuItem = null;
    private boolean speechRecording;
    private ViewSwitcher switcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //New database manager instance
        databaseManager = new DatabaseManager(this);
        //set the speech recognition listener
        SpeechRecognitionListener speechRecListener = new SpeechRecognitionListener(this);
        speechRecListener.setListener(this);

        //new recognizer & set to listener created above
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(speechRecListener);

        //Initialise shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //stop title displaying in actionbar on homescreen
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        //Add dolch words to database
        addExerciseWords();

        //reads in training data from assets folder
        readTessData();

        //new viewswitcher
        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

    }

    @Override
    protected void onResume() {
        switcher.setDisplayedChild(0);
        loadTTS();
        super.onResume();
    }


    @Override
    public void onPause() {
        if (ttsHelper != null) {
                ttsHelper.destroyTTS();
        }
        if(recognizer != null){
            recognizer.stopListening();
            speechRecording = false;
        }
        super.onPause();
    }



    private void componentSetup() {

        //Instantiate Buttons & link longClickListeners
        Button dolchListButton = (Button) findViewById(R.id.dolch_list_button);
        Button textToSpeechButton = (Button) findViewById(R.id.text_to_speech_button);
        Button vocabularyBuilderButton = (Button) findViewById(R.id.vocabulary_builder_button);
        Button exercisesButton = (Button) findViewById(R.id.exercises_button);
        ocrButton = (Button) findViewById(R.id.ocr_button);
        speechRecognitionButton = (Button) findViewById(R.id.speech_recognition_button);

        dolchListButton.setOnLongClickListener(listener);
        textToSpeechButton.setOnLongClickListener(listener);
        vocabularyBuilderButton.setOnLongClickListener(listener);
        speechRecognitionButton.setOnLongClickListener(listener);
        ocrButton.setOnLongClickListener(listener);
        exercisesButton.setOnLongClickListener(listener);

        speechRecognitionButton.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.home_screen_button));
        speechRecognitionButton.setText("Start Recording");

        //If the device has no camera hide the OCR button
        if(!cameraInstalled()){
            ocrButton.setVisibility(View.GONE);
        }

        //Connect to normal click listeners (These start the activities)
        dolchListButton.setOnClickListener(this);
        textToSpeechButton.setOnClickListener(this);
        vocabularyBuilderButton.setOnClickListener(this);
        speechRecognitionButton.setOnClickListener(this);
        exercisesButton.setOnClickListener(this);
        ocrButton.setOnClickListener(this);

    }

    //Checks if words already inserted. If not, inserts them.
    private void addExerciseWords(){
        if ((!sharedPreferences.getBoolean("dolchWordsInserted", false) || (!sharedPreferences.getBoolean("alphabetWordsInserted", false)))){
            //New thread for copying data
            new Thread(new Runnable() {
                public void run() {
                    //get array from assets folder and pass through to DB Manager to run inserts
                    databaseManager.addWords(getApplicationContext().getResources().getStringArray(
                            R.array.dolch_words));
                    databaseManager.addAlphabetWords(getApplicationContext().getResources().getStringArray(
                            R.array.alphabet_words), images);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    /*once completed save in shared preferences that they are saved so they arent added
                    every time the app opens (Only on install)
                     */
                    editor.putBoolean("dolchWordsInserted", true);
                    editor.putBoolean("alphabetWordsInserted", true);
                    editor.apply();
                }
            }).start();
        }
    }

    /*
    Method inspired by tutorial: http://gaut.am/making-an-ocr-android-app-using-tesseract/
    Reads the tesseract traineddata for the english language from assets to external directory
     */
    public void readTessData() {
        if (isExternalStorageWritable()) {
            //create app directory (MALA)
            File dir = new File(DIR);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    Log.v(DolchListActivity.TAG, "Directory could not be created on sd card");
                } else {
                    Log.v(DolchListActivity.TAG, "Directory created on sd card");
                }
            }
            File tessdir = new File(DIR +"tessdata/");
            if (!tessdir.exists()) {
                if (!tessdir.mkdir()) {
                    Log.v(DolchListActivity.TAG, "Directory could not be created on sd card");
                } else {
                    Log.v(DolchListActivity.TAG, "Directory created on sd card");
                }
            }
            //create file to put traineddata into from assets
            String ENG_TRAINED_TESS_DATA = "tessdata/eng.traineddata";
            if (!(new File(DIR + ENG_TRAINED_TESS_DATA)).exists()) {
                try {
                    AssetManager assetManager = getAssets();
                    InputStream inputStream = assetManager.open(ENG_TRAINED_TESS_DATA);
                    OutputStream outputStream = new FileOutputStream(DIR + ENG_TRAINED_TESS_DATA);
                    copyFile(inputStream, outputStream);

                    //close streams
                    inputStream.close();
                    outputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            //no external storage so hide ocr button
            ocrButton.setVisibility(View.GONE);
        }

    }

    //check if camera installed
    public boolean cameraInstalled(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    //Checks if external storage is writable
    public boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    //Used for copying files (Tess data to external storage)
    public void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buff = new byte[1024];
        int length;
        while ((length = inputStream.read(buff)) > 0) {
            outputStream.write(buff, 0, length);
        }
    }

    //Button click listeners
    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.dolch_list_button:
                Intent dolchListIntent = new Intent(this, DolchListActivity.class);
                startActivity(dolchListIntent);
                break;

            case R.id.text_to_speech_button:
                if(isNetworkAvailable()){
                    Intent textToSpeechWordCheckIntent = new Intent(this, TextToSpeechWordCheck.class);
                    startActivity(textToSpeechWordCheckIntent);
                } else {
                    currentSpeakingButton = null;
                    Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                            Toast.LENGTH_LONG);
                    toast.show();
                    ttsHelper.say("No Network Connection, please connect to the internet");
                }
                break;

            case R.id.vocabulary_builder_button:
                Intent vocabularyBuilderIntent = new Intent(this, VocabularyBuilderActivity.class);
                startActivity(vocabularyBuilderIntent);
                break;


            case R.id.exercises_button:
                Intent exercisesIntent = new Intent(this, AlphabetExerciseActivity.class);
                startActivity(exercisesIntent);
                break;

            case R.id.speech_recognition_button:
                if(isNetworkAvailable()){
                    if (!speechRecording) {
                        startSpeechRec();
                    } else {
                        recognizer.stopListening();
                        speechRecording = false;
                    }
                } else {
                    currentSpeakingButton = null;
                    Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                            Toast.LENGTH_LONG);
                    toast.show();
                    ttsHelper.say("No Network Connection, please connect to the internet");
                }
                break;

            case R.id.ocr_button:
                if(isNetworkAvailable()){
                    Intent ocrIntent = new Intent(this, OCRActivity.class);
                    startActivity(ocrIntent);
                } else {
                    currentSpeakingButton = null;
                    Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                            Toast.LENGTH_LONG);
                    toast.show();
                    ttsHelper.say("No Network Connection, please connect to the internet");
                }
                break;
        }
    }

    //listens for long clicks to highlight and read content descriptions to user
    View.OnLongClickListener listener = new View.OnLongClickListener(){
        @Override
        public boolean onLongClick(View v) {

            if (ttsHelper.isSpeaking()) {
                ttsHelper.stopSpeaking();
            }

            currentSpeakingButton = (Button) v;
            ttsHelper.say(currentSpeakingButton.getContentDescription().toString());

            return true;
        }
    };

    //options menu creation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    //menu item listeners
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_help:
                //null currentspeakingButton and highlight menu item
                currentSpeakingButton = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say("Hold your finger on a button to hear what it does");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    Method from speakinglistener interface. TTS will notify when it starts and stops speaking
    so the UI can highlight the corresponding components by changing their backgrounds
     */
    @Override
    public void onVariableChanged(boolean speaking) {
        if (speaking) {
            if (currentSpeakingMenuItem != null) {
                currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);

            } else if(currentSpeakingButton != null){

                currentSpeakingButton.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.home_screen_button_highlighted));
                currentSpeakingButton.setPadding(getResources().getDimensionPixelSize(R.dimen.card_padding),0,
                        getResources().getDimensionPixelSize(R.dimen.card_padding),0);
            }

        }
        if(!speaking){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentSpeakingMenuItem != null) {

                        currentSpeakingMenuItem.setIcon(R.drawable.help_icon);

                    } else if(currentSpeakingButton != null){

                        currentSpeakingButton.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.home_screen_button));
                        currentSpeakingButton.setPadding(getResources().getDimensionPixelSize(R.dimen.card_padding), 0,
                                getResources().getDimensionPixelSize(R.dimen.card_padding), 0);
                    }

                }
            });

        }
    }

    public void startSpeechRec() {
        //if network is available start recognition service
        if (isNetworkAvailable()){
            Intent speechRecognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-UK");
            speechRecognitionIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            recognizer.startListening(speechRecognitionIntent);
            speechRecording = true;
        } else {
            //no network connection display and speak error
            Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                    Toast.LENGTH_LONG);
            toast.show();
            ttsHelper.say("No Network Connection, please connect to the internet");
        }
    }

    //sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    //end citation

    /*listens for when the recognition service is recording.
    When sound is detected the button background is set to red to indicate to the user
    that it is recording. COnversely set to clear when finished
     */
    @Override
    public void onRecordingChanged(boolean recording) {
        if(recording){
            speechRecording = true;
            speechRecognitionButton.setText("Stop Recording");
            speechRecognitionButton.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.speech_button_recording));
        }else{
            speechRecording = false;
            speechRecognitionButton.setText("Start Recording");
            speechRecognitionButton.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.home_screen_button));
        }
    }

    /*listens for when the recognition service is ready to record.
   When ready the button is set to orange to indicate it is ready to the user
    */
    @Override
    public void onReadyChanged(boolean ready) {
        if (ready) {
            speechRecognitionButton.setText("Speak...");
            speechRecognitionButton.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.speech_button_ready));
        } else {
            speechRecording = false;
            speechRecognitionButton.setText("Start Recording");
            speechRecognitionButton.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.home_screen_button));

        }

    }

    //once TTS engine is loaded
    @Override
    public void onLoaded(boolean loaded) {
        if (loaded) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //loading finished setup buttons & show screen
                    switcher.showNext();
                    componentSetup();
                }
            });
        } else {
            switcher.setDisplayedChild(1);
        }
    }

    //load the tts engine & set listeners
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
        ttsHelper.setLoadingListener(this);
    }


}
