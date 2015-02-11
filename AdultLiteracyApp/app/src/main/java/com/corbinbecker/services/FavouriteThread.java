package com.corbinbecker.services;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.corbinbecker.activities.DolchListActivity;
import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.Word;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by corbinbecker
 * A thread that adds/removes words from remote database
 */
public class FavouriteThread extends AsyncTask<String, String, JSONObject> {
    private Context context;
    private Word word;
    private String function;

    public FavouriteThread(Context context, Word word, String function){
        this.context = context;
        this.word = word;
        this.function = function;
    }

    private static final String STATUS = "status";

    ApiRequests apiRequests = new ApiRequests();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(String... args) {
        DatabaseManager databaseManager = new DatabaseManager(context);
        String email = databaseManager.getUserEmail();
        //if the function string is add then it calls the add method
        //else it calls the remove word method
        if(function.equals("add")){
            return apiRequests.addToVocabBuilder(email, word.getText());
        }else{
            return apiRequests.removeFromVocabBuilder(email, word.getText());
        }

    }

    @Override
    protected void onPostExecute(JSONObject json) {
        if (function.equals("add")) {
            try {
                if (json.getString(STATUS) != null) {
                    String response = json.getString(STATUS);
                    if (Integer.parseInt(response) == 1) {
                        Log.d(DolchListActivity.TAG, "word added");
                    }
                    else {
                        Log.d(DolchListActivity.TAG, "word not added");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (json.getString(STATUS) != null) {
                    String response = json.getString(STATUS);
                    if (Integer.parseInt(response) == 1) {
                        Log.d(DolchListActivity.TAG, "word deleted");
                    }
                    else {
                        Log.d(DolchListActivity.TAG, "word not deleted");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}
