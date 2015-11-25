package com.microsoft.codepush.react;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;

public class CodePushUtils {

    public static String appendPathComponent(String basePath, String appendPathComponent) {
        return new File(basePath, appendPathComponent).getAbsolutePath();
    }

    public static boolean fileAtPathExists(String filePath) {
        return new File(filePath).exists();
    }

    public static String readFileToString(String filePath) throws IOException {
        FileInputStream fin = null;
        BufferedReader reader = null;
        try {
            File fl = new File(filePath);
            fin = new FileInputStream(fl);
            reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        } finally {
            if (reader != null) reader.close();
            if (fin != null) fin.close();
        }
    }

    public static void writeStringToFile(String content, String filePath) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(filePath);
            out.print(content);
        } finally {
            if (out != null) out.close();
        }
    }

    public static boolean createFolderAtPath(String filePath) {
        File file = new File(filePath);
        return file.mkdir();
    }

    public static WritableMap getWritableMapFromFile(String filePath) throws IOException {

        String content = CodePushUtils.readFileToString(filePath);
        JSONObject json = null;
        try {
            json = new JSONObject(content);
            return convertJsonObjectToWriteable(json);
        } catch (JSONException jsonException) {
            throw new CodePushMalformedDataException(filePath, jsonException);
        }

    }

    public static void writeReadableMapToFile(ReadableMap map, String filePath) throws IOException {
        JSONObject json = CodePushUtils.convertReadableToJsonObject(map);
        String jsonString = json.toString();
        CodePushUtils.writeStringToFile(jsonString, filePath);
    }

    public static WritableMap convertJsonObjectToWriteable(JSONObject jsonObj) {
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
                map.putMap(key, convertJsonObjectToWriteable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                map.putArray(key, convertJsonArrayToWriteable((JSONArray) obj));
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

    public static WritableArray convertJsonArrayToWriteable(JSONArray jsonArr) {
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
                arr.pushMap(convertJsonObjectToWriteable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                arr.pushArray(convertJsonArrayToWriteable((JSONArray) obj));
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

    public static String tryGetString(ReadableMap map, String key) {
        try {
            return map.getString(key);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }
}
