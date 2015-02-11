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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.AlphabetWord;
import com.corbinbecker.services.FavouriteThread;
import com.corbinbecker.services.TTSHelper;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by corbinbecker
 * Class for the alphabet exercise that allows users to cycle through
 * the letters in the alphabet with example words and corresponding images
 * for each letter. Next & prev buttons cycle the letters. A play button speaks
 * the displayed word using text to speech. A favourite star checkbox saves the
 * word to the users vocabulary builder. And hear/record buttons allow for recording & playing sound
 * The class extends ActionBarActivity and implements the custom speakingListener
 * and loadingListener interfaces allowing it to know when TTS has loaded and
 * finished speaking.
 */
public class AlphabetExerciseActivity extends ActionBarActivity implements View.OnClickListener, TTSHelper.speakingListener,
TTSHelper.loadingListener{

    //field declarations
    private static final String WORD_INDEX = "word";
    private static String audioFilePath;

    private DatabaseManager databaseManager;
    private TTSHelper ttsHelper = null;
    private static MediaRecorder mediaRecorder;
    private static MediaPlayer mediaPlayer;
    private ApiRequests apiRequests;

    private List<AlphabetWord> alphabetWords;
    private AlphabetWord alphabetWord;
    private int alphabetWordIndex = 0;
    private boolean isSpeaking;

    private TextView alphabetWordTextView;
    private ImageView alphabetWordImage;
    private CheckBox favouriteCheckBox;
    private ViewSwitcher switcher;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private Button nextButton, prevButton, playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alphabet_exercise_layout);
        getSupportActionBar().setTitle("Back");

        //data access services declarations
        databaseManager = new DatabaseManager(this);
        apiRequests = new ApiRequests();

        //putting all words into an array list and setting the word to the first one (i.e 'A')
        alphabetWords = databaseManager.getAllAlphabetWords();
        alphabetWord = alphabetWords.get(0);

        //assigning UI components to xml resources
        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
        alphabetWordTextView = (TextView) findViewById(R.id.alphabet_text_view);
        alphabetWordImage = (ImageView) findViewById(R.id.alphabet_image);

        //declaring path for audio file
        audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.3gp";


    }

    /*Takes first letter of current word and gets upper and lowercase
    to display to the user i.e (Ambulance -> will display 'Aa'
     */
    private String getLetters(AlphabetWord word){
        return "" + word.getText().charAt(0) + word.getText().toLowerCase().charAt(0);
    }

    /*Used for handling when user rotates device to recreate the instance variables etc
    Image, word and index all need to be saved.
    OnSavedInstance saves the current word index in the array list
    onRestoreInstance takes the saved index and reconstructs everything
    Not required: following feedback it was advised to lock orientation to portrait
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        alphabetWordIndex = savedInstanceState.getInt(WORD_INDEX);
        alphabetWord = alphabetWords.get(alphabetWordIndex);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(WORD_INDEX, alphabetWordIndex);
        super.onSaveInstanceState(savedInstanceState);
    }

    //calls the loadTTS() function when the activity resumes
    //sets displayedChild(0) (resetting the viewswitcher)
    @Override
    protected void onResume() {
        super.onResume();
        switcher.setDisplayedChild(0);
        loadTTS();

    }

    //shuts down the TTS if it is currently active before pausing
    @Override
    public void onPause() {
        if (ttsHelper != null) {
            ttsHelper.destroyTTS();
        }
        super.onPause();
    }

    //method to setup UI items
    public void componentSetup() {

        //Instantiate buttons
        prevButton = (Button) findViewById(R.id.alphabet_button_prev);
        nextButton = (Button) findViewById(R.id.alphabet_button_next);
        playButton = (Button) findViewById(R.id.alphabet_button_play);
        Button recordButton = (Button) findViewById(R.id.alphabet_record_button);
        Button hearButton = (Button) findViewById(R.id.play_recording_button);
        favouriteCheckBox = (CheckBox) findViewById(R.id.alphabetFavouriteStarCheckBox);

        //assigning touchlistener for record/hear buttons
        recordButton.setOnTouchListener(recordTouchListener);
        hearButton.setOnTouchListener(playTouchListener);

        //Button OnClickListeners
        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        favouriteCheckBox.setOnClickListener(this);

        //button onLongClcikListeners
        prevButton.setOnLongClickListener(listener);
        nextButton.setOnLongClickListener(listener);
        playButton.setOnLongClickListener(listener);
        favouriteCheckBox.setOnLongClickListener(listener);
    }

    /*
    OnClick for handling button clicks. Next/prev buttons cycle through the array list items
    using a simple index field. The currentSpeakingView is for the TTS engine to know which button
    to highlight as it is being spoken. The favouriteCheckbox allows users to save
    words to the vocabulary builder, it is hidden until the word is displayed and a method in
    DatabaseManager checks if that word is saved already and accordingly marks the checkbox as
    checked or unchecked
     */

    @Override
    public void onClick(View view) {

        //if already speaking stops speaking
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
            isSpeaking = false;
        }

        switch (view.getId()) {

            case R.id.alphabet_button_next:
                //if at the end of the arraylist loop back to beginning (ie go from Z to A)
                if (alphabetWordIndex == alphabetWords.size() - 1) {
                    alphabetWordIndex = 0;
                    newWord(alphabetWordIndex);
                }else{
                    alphabetWordIndex++;
                    newWord(alphabetWordIndex);
                }
                break;

            case R.id.alphabet_button_prev:
                //if at beginning of arraylist loop back to end (ie A to Z)
                if (alphabetWordIndex == 0) {
                    alphabetWordIndex = alphabetWords.size();
                }
                alphabetWordIndex--;
                newWord(alphabetWordIndex);
                break;

            case R.id.alphabet_button_play:
                //show checkbox, textview, start speaking word and highlight
                favouriteCheckBox.setVisibility(View.VISIBLE);
                currentSpeakingView = alphabetWordTextView;
                alphabetWordTextView.setText(alphabetWord.getText());
                ttsHelper.say(alphabetWordTextView.getText().toString());
                break;

            case R.id.alphabetFavouriteStarCheckBox:
                //if user checks then save word else remove the word.
                if (favouriteCheckBox.isChecked()) {
                    saveWord();
                } else {
                    removeWord();
                }
        }

        try {
            //if the word is saved then set the favourite checkbox to checked else unchecked
            if (databaseManager.wordSaved(alphabetWord.getText())) {
                favouriteCheckBox.setChecked(true);
            } else {
                favouriteCheckBox.setChecked(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        alphabetWordImage.setImageResource(alphabetWord.getImage());

    }

    /*method used in the onClick for the next and previous buttons to
        get a new word from the array list.
     */
    public void newWord(int wordIndex){
        currentSpeakingView = alphabetWordTextView;
        alphabetWordIndex = wordIndex;
        alphabetWord = alphabetWords.get(alphabetWordIndex);
        alphabetWordTextView.setText(getLetters(alphabetWord));
        ttsHelper.say(String.valueOf(getLetters(alphabetWord).charAt(0)));
        favouriteCheckBox.setVisibility(View.GONE);
    }

    /*Saves the word to local DB. If the user is logged in and network available saves to remote
    database also
     */
    public void saveWord() {
        databaseManager.saveWord(alphabetWord.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())) {
            if (isNetworkAvailable()) {
                new FavouriteThread(this, alphabetWord, "add").execute();
            }
        }
    }

    /*Removes the word from local Database. If the user is logged in and network available
     removes from remote database also
     */
    public void removeWord() {
        databaseManager.removeWord(alphabetWord.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())) {
            if (isNetworkAvailable()) {
                new FavouriteThread(this, alphabetWord, "remove").execute();
            }
        }
    }

    /* Sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    Checks if the device has network connectivity
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
            currentSpeakingView = v;
            if (isSpeaking) {
                ttsHelper.stopSpeaking();
            }
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

            if (currentSpeakingView == playButton ||
                currentSpeakingView == nextButton ||
                currentSpeakingView == prevButton) {

                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_button_highlighted));

            } else {

                if (currentSpeakingView == favouriteCheckBox ||
                    currentSpeakingView == alphabetWordTextView) {

                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.generic_clear_background_highlighted));

                } else {

                    currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);

                }
            }
        } else {

            isSpeaking = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (currentSpeakingView == playButton ||
                            currentSpeakingView == nextButton ||
                            currentSpeakingView == prevButton){

                        currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.generic_button_not_pressed));

                    } else {

                        if (currentSpeakingView == favouriteCheckBox ||
                            currentSpeakingView == alphabetWordTextView) {

                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                    R.drawable.generic_clear_background));

                        } else {

                            currentSpeakingMenuItem.setIcon(R.drawable.help_icon);

                        }
                    }
                }

            });

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
                    alphabetWordTextView.setText(getLetters(alphabetWord));
                    alphabetWordImage.setImageResource(alphabetWord.getImage());
                    favouriteCheckBox.setVisibility(View.GONE);
                }
            });
        } else {
            switcher.setDisplayedChild(1);
        }
    }

    //load the tts engine, assign listeners for the interfaces
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
        ttsHelper.setLoadingListener(this);

    }

    //options menu creation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    //listener for menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //if tts speaking, stop
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
            isSpeaking = false;
        }

        switch (item.getItemId()) {
            case R.id.action_help:
                /*null the currentspeakingview and set currentspeakingmenuitem. This just highlights
                the help item when it is speaking
                 */
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say(getString(R.string.alphabet_activity_help));
                return true;
            case R.id.action_home:
                //Launch the main activity if home button clicked
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    If record button held down start recording audio using mic
    Stop recording when the button is released
     */
    View.OnTouchListener recordTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    Log.d(DolchListActivity.TAG, "RECORDING");
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
