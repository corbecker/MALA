package com.corbinbecker.activities;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.DolchWord;
import com.corbinbecker.services.FavouriteThread;
import com.corbinbecker.services.TTSHelper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by corbinbecker
 * Activity to display dolch list words. Next/prev buttons cycle through the array items
 using a simple index field. The currentSpeakingView is for the TTS engine to know which button
 to highlight as it is being spoken. The favouriteCheckbox allows users to save
 words to the vocabulary builder a method in DatabaseManager checks if that word is saved
 already and accordingly marks the checkbox as checked or unchecked
 *
 */
public class DolchListActivity extends ActionBarActivity implements View.OnClickListener, TTSHelper.speakingListener,
TTSHelper.loadingListener{

    public static final String TAG = "com.corbinbecker.adultliteracypp";
    private static String audioFilePath;

    private TTSHelper ttsHelper = null;
    private DatabaseManager databaseManager;
    private ApiRequests apiRequests;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    private DolchWord dolchWord;
    private boolean isSpeaking;
    private int dolchWordIndex;
    private String[] dolchWordsArray;

    private TextView dolchWordTextView;
    private CheckBox favouriteCheckBox;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private Button prevButton, nextButton, playButton, recordButton, hearButton;
    private ViewSwitcher switcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dolch_list_layout);
        getSupportActionBar().setTitle("Back");

        //data access service instances
        databaseManager = new DatabaseManager(this);
        apiRequests = new ApiRequests();

        //gets the dolch words from a string array from resources
        dolchWordsArray = this.getResources().getStringArray(R.array.dolch_words);

        //gets a random word to begin with
        dolchWord = databaseManager.getWord(dolchWordsArray[new Random().nextInt(dolchWordsArray.length)]);
        dolchWordIndex = Arrays.asList(dolchWordsArray).indexOf(dolchWord.getText());

        //ViewSwitcher instance
        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

        //path to the audio recording
        audioFilePath =
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.3gp";

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
        super.onPause();
    }

    //method to setup UI items
    private void componentSetup(){
        //initialise dolch word card TextView
        dolchWordTextView = (TextView) findViewById(R.id.dolch_list_card_text);
        dolchWordTextView.setText(dolchWord.getText());


        //Instantiate buttons & checkbox
        prevButton = (Button) findViewById(R.id.dolch_card_prev_button);
        nextButton = (Button) findViewById(R.id.dolch_card_next_button);
        playButton = (Button) findViewById(R.id.dolch_card_play_button);
        favouriteCheckBox = (CheckBox) findViewById(R.id.favouriteStarCheckBox);
        recordButton = (Button) findViewById(R.id.dolch_record_button);
        hearButton = (Button) findViewById(R.id.play_recording_button_dolch);

        //onTouch listener set for record/hear buttons
        recordButton.setOnTouchListener(recordTouchListener);
        hearButton.setOnTouchListener(playTouchListener);

        //OnClickListener setting
        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        favouriteCheckBox.setOnClickListener(this);

        //onLongclicklistener setting
        prevButton.setOnLongClickListener(listener);
        nextButton.setOnLongClickListener(listener);
        playButton.setOnLongClickListener(listener);
        favouriteCheckBox.setOnLongClickListener(listener);

        //getting word status to set favourite star to checked/unchecked
        try {
            if (databaseManager.wordSaved(dolchWord.getText())) {
                favouriteCheckBox.setChecked(true);
            } else {
                favouriteCheckBox.setChecked(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* OnClick method handles UI click interaction
       Next/prev buttons goes to next or prev dolch word. Play button uses TTS to speak the word
       FavouriteCheckBox saves the word to the vocabulary builder
     */
    @Override
    public void onClick(View view) {

        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }

        switch (view.getId()) {

            case R.id.dolch_card_next_button:
                //if at end of array loop to beginning
                if (dolchWordIndex == dolchWordsArray.length - 1) {
                    dolchWordIndex = -1;
                }
                dolchWordIndex++;
                dolchWord = databaseManager.getWord(dolchWordsArray[dolchWordIndex]);
                break;

            case R.id.dolch_card_prev_button:
                //if at beginning of array loop to end
                if (dolchWordIndex == 0) {
                    dolchWordIndex = dolchWordsArray.length;
                }
                dolchWordIndex--;
                dolchWord = databaseManager.getWord(dolchWordsArray[dolchWordIndex]);
                break;

            case R.id.dolch_card_play_button:
                //highlight and speak displayed word
                currentSpeakingView = dolchWordTextView;
                ttsHelper.say(dolchWordTextView.getText().toString());
                break;

            case R.id.favouriteStarCheckBox:
                //save word if star not checked and vice versa
                if (favouriteCheckBox.isChecked()) {
                    saveWord();
                } else {
                    removeWord();
                }
                break;
        }

        try {
            //if word already saved or not mark the star accordingly
            if (databaseManager.wordSaved(dolchWord.getText())) {
                favouriteCheckBox.setChecked(true);
            } else {
                favouriteCheckBox.setChecked(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //set the text in the textview to the new word
        dolchWordTextView.setText(dolchWord.getText());
    }



    /*Saves the word to local DB. If the user is logged in and network available saves to remote
      database also
    */
    public void saveWord(){
        databaseManager.saveWord(dolchWord.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())){
            if (isNetworkAvailable()) {
                new FavouriteThread(this,dolchWord,"add").execute();
            }
        }
    }

    /*Removes the word from local Database. If the user is logged in and network available
     removes from remote database also
     */
    public void removeWord(){
        databaseManager.removeWord(dolchWord.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())) {
            if (isNetworkAvailable()) {
                new FavouriteThread(this,dolchWord,"remove").execute();
            }
        }
    }

    /*sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    checks for device network connectivity
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    //end citation

    /*
    Listens for longclicks to read content descriptions to the user
     */
    View.OnLongClickListener listener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (isSpeaking) {
                ttsHelper.stopSpeaking();
            }
            currentSpeakingView = v;
            ttsHelper.say(currentSpeakingView.getContentDescription().toString());
            return true;
        }
    };

    /*
    Method from speakinglistener interface. TTS will notify when it starts and stops speaking
    so the UI can highlight the corresponding components by changing their backgrounds
     */
    @Override
    public void onVariableChanged(boolean speaking) {
        if (speaking) {

            isSpeaking = true;

            if (currentSpeakingView == null) {
                //change help icon background to highlighted
                currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);

            } else {

                if (currentSpeakingView == favouriteCheckBox ||
                    currentSpeakingView == dolchWordTextView) {
                    //highlight the views background
                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.generic_clear_background_highlighted));

                } else {
                    //highlight button background
                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.generic_button_highlighted));
                }
            }
        } else {

            isSpeaking = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentSpeakingView == null) {
                        //reset help icon to normal background
                        currentSpeakingMenuItem.setIcon(R.drawable.help_icon);

                    } else {

                        if(currentSpeakingView == favouriteCheckBox ||
                           currentSpeakingView == dolchWordTextView){
                            //reset to normal background
                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                    R.drawable.generic_clear_background));
                        }else{
                            //reset button to normal background
                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                    R.drawable.generic_button_not_pressed));

                        }
                    }
                }
            });

        }
    }

    //menu creation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    //click listener for menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }

        switch (item.getItemId()) {
            //speak help info
            case R.id.action_help:
                //if help clicked null the currentspeaking view so menu item is highlighted
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say(getString(R.string.dolch_list_help));
                return true;
            //start the main activity intent if home button clicked
            case R.id.action_home:
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            case R.id.home:
                //if back button clicked finish the activity
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
   Method from loadingListener interface. this will notify the UI to switch the the activity view
   from the loading screen once the text to speech engine is loaded. Just to avoid the UI hanging
   and confusing the user.
    */
    @Override
    public void onLoaded(boolean loaded) {
        if (loaded) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switcher.showNext();
                    componentSetup();
                }
            });
        }
        else{
            switcher.setDisplayedChild(1);
        }
    }

    //load the tts engine & set listeners
    private void loadTTS(){
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
        ttsHelper.setLoadingListener(this);
    }

    /*
    If record button held down start recording audio using mic
    Stop recording when the button is released
     */
    View.OnTouchListener recordTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch (View v, MotionEvent event){
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    break;
            }
            return false;
        }
    };

    /*
    If play button held down play the audio file that has been recorded
    Stop playing when the button is released
     */
    View.OnTouchListener playTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    try {
                        playAudio();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(DolchListActivity.TAG, "RECORDING");
                    break;
                case MotionEvent.ACTION_UP:
                    try {
                        stopAudio();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            return false;
        }
    };

    /*
    New mediaplayer instance, point to audio file in external storage
    If there is no file dont play, if there is play the file
     */
    public void playAudio() throws IOException {
        File file = new File(audioFilePath);
        mediaPlayer = new MediaPlayer();
        if (file.exists()) {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        }

    }

    //stop playing audio
    public void stopAudio() throws IOException {
        mediaPlayer.stop();
    }

    /*
        Set the recording source to mic and the format & destination of the file
        Once setup complete start recording
         */
    public void startRecording() {

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaRecorder.start();

    }

    //Stop, reset and release the recording & recorder instance
    public void stopRecording() {

        if (null != mediaRecorder) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();

            mediaRecorder = null;
        }
    }

}
