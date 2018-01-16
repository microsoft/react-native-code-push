package com.microsoft.codepush.react.utils;

import android.util.Log;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.microsoft.codepush.react.CodePushConstants;
import com.microsoft.codepush.react.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.react.exceptions.CodePushUnknownException;
import com.microsoft.codepush.react.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class CodePushUtils {
    private static Gson mGson = new GsonBuilder().create();

    public static String getStringFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            StringBuilder buffer = new StringBuilder();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            return buffer.toString().trim();
        } finally {
            if (bufferedReader != null) bufferedReader.close();
            if (inputStream != null) inputStream.close();
        }
    }

    public static JSONObject getJsonObjectFromFile(String filePath) throws IOException {
        String content = FileUtils.readFileToString(filePath);
        try {
            return new JSONObject(content);
        } catch (JSONException jsonException) {
            // Should not happen
            throw new CodePushMalformedDataException(filePath, jsonException);
        }
    }

    public static void setJSONValueForKey(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new CodePushUnknownException("Unable to set value " + value + " for key " + key + " to JSONObject");
        }
    }

    public static void writeJsonToFile(JSONObject json, String filePath) throws IOException {
        String jsonString = json.toString();
        FileUtils.writeStringToFile(jsonString, filePath);
    }

    public static JSONObject convertObjectToJsonObject(Object object) {
        try {
            return new JSONObject(mGson.toJsonTree(object).toString());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CodePushMalformedDataException(e.toString(), e);
        }
    }

    public static String convertObjectToJsonString(Object object) {
        return mGson.toJsonTree(object).toString();
    }

    public static <T> T convertJsonObjectToObject(JSONObject jsonObject,  Class<T> classOfT) {
        return convertStringToObject(jsonObject.toString(), classOfT);
    }

    public static <T> T convertStringToObject(String stringObject,  Class<T> classOfT) {
        return mGson.fromJson(stringObject, classOfT);
    }

    public static String getQueryStringFromObject(Object object) {
        JsonObject updateRequestJson = mGson.toJsonTree(object).getAsJsonObject();
        Map<String, Object> updateRequestMap = new HashMap<String, Object>();
        updateRequestMap = (Map<String, Object>) mGson.fromJson(updateRequestJson, updateRequestMap.getClass());
        StringBuilder sb = new StringBuilder();
        for (HashMap.Entry<String, Object> e : updateRequestMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue().toString(), "UTF-8"));
            } catch (UnsupportedEncodingException exception) {
                exception.printStackTrace();
                throw new CodePushMalformedDataException(exception.toString(), exception);
            }
        }
        return sb.toString();
    }

}
