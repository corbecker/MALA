package com.corbinbecker.data;
/*
JSONParser class inspired by code from: http://techlovejump.com/android-json-parser-from-url/
Handles the sending and receiving of JSON objects and arrays via HTTP requests
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonParser {

    //Field Declarations
    private static InputStream inputStream = null;
    private static JSONObject jsonObject = null;
    private static JSONArray jsonArray = null;
    private static String jsonString = "";

    // constructor
    public JsonParser() {

    }

    public JSONObject getJSONFromUrl(String url, JSONObject jsonObject, String HttpType) {

        // Try Make HTTP request
        try {
            //DefaultHttpClient object
            DefaultHttpClient httpClient = new DefaultHttpClient();

            //Create HttpPost object and pass in URL of the resource
            HttpPost httpPost = new HttpPost(url);
            HttpGet httpGet = new HttpGet(url);
            HttpDelete httpDelete = new HttpDelete(url);


            if(HttpType == "post"){
                StringEntity se = new StringEntity(jsonObject.toString());
                httpPost.setEntity(se);
                //HttpResponse is the value returned from the HttpPost request
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                //Pass the retrieved entity from the http request to input stream reader
                inputStream = httpEntity.getContent();
            }else if(HttpType == "get"){
                //HttpResponse is the value returned from the HttpPost request
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();

                //Pass the retrieved entity from the http request to input stream reader
                inputStream = httpEntity.getContent();
            }else if(HttpType == "delete"){
                //HttpResponse is the value returned from the HttpPost request
                HttpResponse httpResponse = httpClient.execute(httpDelete);
                HttpEntity httpEntity = httpResponse.getEntity();
                //Pass the retrieved entity from the http request to input stream reader
                inputStream = httpEntity.getContent();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Try convert from Byte->Character->String
        try {
            //InputStream inputStream passed to BufferedReader for Byte->Character conversion
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, "iso-8859-1"), 8);

            //Buffered reader character stream converted to a string object
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            //Close input stream and create jsonString from StringBuilder output
            inputStream.close();
            jsonString = stringBuilder.toString();

        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }


        // try parse the string to a JSON object
        try {
            JsonParser.jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }


        // return the JSON Object
        return JsonParser.jsonObject;

    }


    public JSONArray getJSONArrayFromUrl(String url, JSONObject jobj, String HttpType) {

        // Try Make HTTP request
        try {
            //DefaultHttpClient object
            DefaultHttpClient httpClient = new DefaultHttpClient();

            //Create HttpPost object and pass in URL of the resource
            HttpPost httpPost = new HttpPost(url);
            HttpGet httpGet = new HttpGet(url);

            if (HttpType.equals("post")) {
                StringEntity se = new StringEntity(jobj.toString());
                httpPost.setEntity(se);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                //Pass the retrieved entity from the http request to input stream reader
                inputStream = httpEntity.getContent();

            } else if (HttpType.equals("get")) {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                inputStream = httpEntity.getContent();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Try convert from Byte->Character->String
        try {
            //InputStream inputStream passed to BufferedReader for Byte->Character conversion
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, "iso-8859-1"), 8);

            //Buffered reader character stream converted to a string object
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            //Close input stream and create jsonString from StringBuilder output
            inputStream.close();
            jsonString = stringBuilder.toString();

        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }

        // try parse the string to a JSON object
        try {
            jsonArray = new JSONArray(jsonString);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }

        // return the JSON Object
        return jsonArray;

    }
}