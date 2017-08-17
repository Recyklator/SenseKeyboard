package com.example.dell.sensekeyboard;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Currency;

/**
 * Class NOT used at this moment
 */
class SettingsBackup extends AsyncTask<String, Integer, String> {

    private static final String apiAddress = "https://storage.p.mashape.com/object?key=";
    //lMgqAuRUxfmshIlRNWBJJlla7evSp1Cj3SLjsn9mgQ4xeWEk2Q

    @Override
    protected void onPreExecute() {
        //progress = ProgressDialog.show(MainActivity.this,"Upozorneni", "Stahuji data...");
    }

    @Override
    protected String doInBackground(String... params) {

        String lwmFeatureSettings = params[0];
        Log.e("SenseKeyboard - B/R","LWM feature settings:"+lwmFeatureSettings);

        try {
            URL url = new URL(apiAddress + "test");
            JsonReader reader = new JsonReader(new InputStreamReader(url.openStream()));


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return "a";
    }
}