package com.microsoft.codepush.react;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public class CodePushUtils {

    public static String appendPathComponent(String basePath, String appendPathComponent) {
        return new File(basePath, appendPathComponent).getAbsolutePath();
    }

    public static WritableArray convertJsonArrayToWritable(JSONArray jsonArr) {
        WritableArray arr = Arguments.createArray();
        for (int i=0; i<jsonArr.length(); i++) {
            Object obj = null;
            try {
                obj = jsonArr.get(i);
            } catch (JSONException jsonException) {
                // Should not happen.
                throw new CodePushUnknownException(i + " should be within bounds of array " + jsonArr.toString(), jsonException);
            }

            if (obj instanceof JSONObject)
                arr.pushMap(convertJsonObjectToWritable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                arr.pushArray(convertJsonArrayToWritable((JSONArray) obj));
            else if (obj instanceof String)
                arr.pushString((String) obj);
            else if (obj instanceof Double)
                arr.pushDouble((Double) obj);
            else if (obj instanceof Integer)
                arr.pushInt((Integer) obj);
            else if (obj instanceof Boolean)
                arr.pushBoolean((Boolean) obj);
            else if (obj == null)
                arr.pushNull();
            else
                throw new CodePushUnknownException("Unrecognized object: " + obj);
        }

        return arr;
    }

    public static WritableMap convertJsonObjectToWritable(JSONObject jsonObj) {
        WritableMap map = Arguments.createMap();
        Iterator<String> it = jsonObj.keys();
        while(it.hasNext()){
            String key = it.next();
            Object obj = null;
            try {
                obj = jsonObj.get(key);
            } catch (JSONException jsonException) {
                // Should not happen.
                throw new CodePushUnknownException("Key " + key + " should exist in " + jsonObj.toString() + ".", jsonException);
            }

            if (obj instanceof JSONObject)
                map.putMap(key, convertJsonObjectToWritable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                map.putArray(key, convertJsonArrayToWritable((JSONArray) obj));
            else if (obj instanceof String)
                map.putString(key, (String) obj);
            else if (obj instanceof Double)
                map.putDouble(key, (Double) obj);
            else if (obj instanceof Integer)
                map.putInt(key, (Integer) obj);
            else if (obj instanceof Boolean)
                map.putBoolean(key, (Boolean) obj);
            else if (obj == null)
                map.putNull(key);
            else
                throw new CodePushUnknownException("Unrecognized object: " + obj);
        }

        return map;
    }

    public static JSONArray convertReadableToJsonArray(ReadableArray arr) {
        JSONArray jsonArr = new JSONArray();
        for (int i=0; i<arr.size(); i++) {
            ReadableType type = arr.getType(i);
            switch (type) {
                case Map:
                    jsonArr.put(convertReadableToJsonObject(arr.getMap(i)));
                    break;
                case Array:
                    jsonArr.put(convertReadableToJsonArray(arr.getArray(i)));
                    break;
                case String:
                    jsonArr.put(arr.getString(i));
                    break;
                case Number:
                    Double number = arr.getDouble(i);
                    if ((number == Math.floor(number)) && !Double.isInfinite(number)) {
                        // This is a whole number.
                        jsonArr.put(number.longValue());
                    } else {
                        try {
                            jsonArr.put(number.doubleValue());
                        } catch (JSONException jsonException) {
                            throw new CodePushUnknownException("Unable to put value " + arr.getDouble(i) + " in JSONArray");
                        }
                    }
                    break;
                case Boolean:
                    jsonArr.put(arr.getBoolean(i));
                    break;
                case Null:
                    jsonArr.put(null);
                    break;
            }
        }

        return jsonArr;
    }

    public static JSONObject convertReadableToJsonObject(ReadableMap map) {
        JSONObject jsonObj = new JSONObject();
        ReadableMapKeySetIterator it = map.keySetIterator();
        while (it.hasNextKey()) {
            String key = it.nextKey();
            ReadableType type = map.getType(key);
            try {
                switch (type) {
                    case Map:
                        jsonObj.put(key, convertReadableToJsonObject(map.getMap(key)));
                        break;
                    case Array:
                        jsonObj.put(key, convertReadableToJsonArray(map.getArray(key)));
                        break;
                    case String:
                        jsonObj.put(key, map.getString(key));
                        break;
                    case Number:
                        jsonObj.put(key, map.getDouble(key));
                        break;
                    case Boolean:
                        jsonObj.put(key, map.getBoolean(key));
                        break;
                    case Null:
                        jsonObj.put(key, null);
                        break;
                    default:
                        throw new CodePushUnknownException("Unrecognized type: " + type + " of key: " + key);
                }
            } catch (JSONException jsonException) {
                throw new CodePushUnknownException("Error setting key: " + key + " in JSONObject", jsonException);
            }
        }

        return jsonObj;
    }

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

    public static void log(String message) {
        Log.d(CodePushConstants.REACT_NATIVE_LOG_TAG, "[CodePush] " + message);
    }

    public static void log(Throwable tr) {
        Log.e(CodePushConstants.REACT_NATIVE_LOG_TAG, "[CodePush] Exception", tr);
    }

    public static void logBundleUrl(String path) {
        log("Loading JS bundle from \"" + path + "\"");
    }

    public static void setJSONValueForKey(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new CodePushUnknownException("Unable to set value " + value + " for key " + key + " to JSONObject");
        }
    }

    public static String tryGetString(ReadableMap map, String key) {
        try {
            return map.getString(key);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    public static void writeJsonToFile(JSONObject json, String filePath) throws IOException {
        String jsonString = json.toString();
        FileUtils.writeStringToFile(jsonString, filePath);
    }
}
