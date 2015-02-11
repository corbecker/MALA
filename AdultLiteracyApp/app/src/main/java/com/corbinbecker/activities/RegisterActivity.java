package com.corbinbecker.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.services.TTSHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by corbinbecker
 * Register activity for cerating a new account
 * Code inspired by tutorial:
 * http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
 */
public class RegisterActivity extends ActionBarActivity implements  TTSHelper.speakingListener{

    private Button registerButton, loginActivityButton;
    private EditText nameEditText, passwordEditText, emailEditText;
    private TextView registerErrorTextView;
    private TTSHelper ttsHelper;
    private boolean isSpeaking;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private Drawable oldBackground, oldPassBackground, oldNameBackground;

    private static String STATUS = "status";
    private static String CONTENT = "content";
    private static String MESSAGE = "message";
    private static String NAME = "user_name";
    private static String EMAIL = "user_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        getSupportActionBar().setTitle("Back");

        // Importing all assets like buttons, text fields
        componentSetup();

        oldBackground = emailEditText.getBackground();
        oldPassBackground = passwordEditText.getBackground();
        oldNameBackground = nameEditText.getBackground();

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTTS();
    }


    @Override
    public void onPause() {
        if (ttsHelper != null) {
            ttsHelper.destroyTTS();
        }
        super.onPause();
    }

    private void componentSetup(){
        nameEditText = (EditText) findViewById(R.id.register_name_edittext);
        emailEditText = (EditText) findViewById(R.id.register_email_edittext);
        passwordEditText = (EditText) findViewById(R.id.register_password);
        registerButton = (Button) findViewById(R.id.register_button);
        loginActivityButton = (Button) findViewById(R.id.login_screen_button);
        registerErrorTextView = (TextView) findViewById(R.id.registration_error);

        nameEditText.setOnLongClickListener(listener);
        emailEditText.setOnLongClickListener(listener);
        passwordEditText.setOnLongClickListener(listener);
        registerButton.setOnLongClickListener(listener);
        loginActivityButton.setOnLongClickListener(listener);

    }


    public void onRegisterButtonClick(View view) {

        ttsHelper.destroyTTS();
        if (!isEmailValid(emailEditText.getText().toString())) {
            registerErrorTextView.setText("Enter a valid email address.");
        } else if (passwordEditText.getText().toString().equals("")) {
            registerErrorTextView.setText("Enter a password.");
        } else if(nameEditText.getText().toString().equals("")) {
            registerErrorTextView.setText("Enter a name.");
        }
        else{
            if (isNetworkAvailable()) {
                new RegisterThread().execute();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                        Toast.LENGTH_LONG);
                toast.show();
                ttsHelper.say("No Network Connection, please connect to the internet");
            }
        }
    }

    public void onLinkToLoginScreenClick(View view) {
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
        finish();
    }

    //begin citation http://stackoverflow.com/questions/6119722/how-to-check-edittexts-text-is-email-address-or-not

    public static boolean isEmailValid(String email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }
    //end citation

    /*login and register code inspired by:
        http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
         */
    private class RegisterThread extends AsyncTask<String, String, JSONObject> {
        private ProgressDialog pDialog;
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        ApiRequests apiRequests = new ApiRequests();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(RegisterActivity.this);
            pDialog.setMessage("Registering...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {
            JSONObject json = apiRequests.register(name, email, password);
            return json;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            pDialog.dismiss();
            try {
                if (json.getString(STATUS) != null) {
                    registerErrorTextView.setText("");
                    String response = json.getString(STATUS);
                    Log.d("TAG", response);
                    if (Integer.parseInt(response) == 1) {
                        DatabaseManager db = new DatabaseManager(getApplicationContext());
                        Log.d("TAG", json.toString());

                        apiRequests.logoutUser(getApplicationContext());

                        db.addUser(name,email);

                        Intent dashBoard = new Intent(getApplicationContext(), VocabularyBuilderActivity.class);

                        dashBoard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(dashBoard);
                    } else {
                        registerErrorTextView.setText("Error during registration");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

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

            if (currentSpeakingView == loginActivityButton ||
                    currentSpeakingView == registerButton) {

                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_button_highlighted));

            } else {

                if (currentSpeakingView == emailEditText ||
                    currentSpeakingView == passwordEditText ||
                    currentSpeakingView == nameEditText) {
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

                    if (currentSpeakingView == loginActivityButton ||
                            currentSpeakingView == registerButton) {

                        currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.generic_button_not_pressed));

                    } else {

                        if (currentSpeakingView == emailEditText) {

                            currentSpeakingView.setBackgroundDrawable(oldBackground);

                        } else if (currentSpeakingView == passwordEditText) {

                            currentSpeakingView.setBackgroundDrawable(oldPassBackground);

                        }else if( currentSpeakingView == nameEditText){

                            currentSpeakingView.setBackgroundDrawable(oldNameBackground);

                        } else {

                            currentSpeakingMenuItem.setIcon(R.drawable.help_icon);

                        }
                    }
                }

            });

        }
    }

    //create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    //options item listener
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }
        switch (item.getItemId()) {
            case R.id.action_help:
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say(getString(R.string.login_activity_help));
                return true;
            case R.id.action_home:
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //load the tts engine
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);

    }

    //sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    //end citation


}
