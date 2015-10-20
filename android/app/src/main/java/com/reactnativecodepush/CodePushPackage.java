/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySeyIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

public class CodePushPackage {

    private static File homeDirectory;

    public static String getCodePushDirectory() {
        return homeDirectory.getAbsolutePath();
    }

    public static void setHomeDrectory(File appHomeDirectory) {
        File codePushHomeDirectory = new File(appHomeDirectory, "/CodePush");
        if(!codePushHomeDirectory.exists())
            codePushHomeDirectory.mkdir();

        File currentDirectory = new File(codePushHomeDirectory, "/current");
        if(!currentDirectory.exists())
            currentDirectory.mkdir();

        File previousDirectory = new File(codePushHomeDirectory, "/previous");
        if(!previousDirectory.exists())
            previousDirectory.mkdir();

        CodePushPackage.homeDirectory = codePushHomeDirectory;
    }

    public static String getBundlePath() {
        String bundleFolderPath = getPackageFolderPath();
        String appBundleName = "main.jsbundle";
        return bundleFolderPath + appBundleName;
    }

    public static String getPackageFolderPath() {
        String pathExtension = "/current/";
        return homeDirectory + pathExtension;
    }

    public static String getPreviousPackageFolderPath() {
        String pathExtension = "/previous/";
        return homeDirectory + pathExtension;
    }

    public static String getPackagePath() {
        String packageFolderPath = getPackageFolderPath();
        String appPackageName = "localpackage.json";
        return packageFolderPath + appPackageName;
    }

    public static String getPreviousPackagePath() {
        String packageFolderPath = getPreviousPackageFolderPath();
        String appPackageName = "localpackage.json";
        return packageFolderPath + appPackageName;
    }

    public static void writeStringToFile(String text, String filePath) throws IOException {
        File f = new File(filePath);
        PrintWriter out = new PrintWriter(f);
        out.print(text);
        out.close();
    }

    public static String readFileAsString(String filePath) throws IOException {
        File f = new File(filePath);
        FileInputStream fin = new FileInputStream(f);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        fin.close();
        return sb.toString();
    }

    public static WritableMap getCurrentPackageInfo() throws IOException, JSONException {
        String localPackageStr = readFileAsString(getPackagePath());
        JSONObject packageJson = new JSONObject(localPackageStr);
        return convertJsonObjectToWriteable(packageJson);
    }

    public static WritableMap getPreviousPackageInfo() throws IOException, JSONException {
        String localPackageStr = readFileAsString(getPreviousPackagePath());
        JSONObject packageJson = new JSONObject(localPackageStr);
        return convertJsonObjectToWriteable(packageJson);
    }

    public static void updateCurrentPackageInfo(ReadableMap packageMap) throws IOException, JSONException {
        JSONObject packageJson = convertReadableToJsonObject(packageMap);
        writeStringToFile(getPreviousPackagePath(), packageJson.toString());
    }

    public static String getCurrentPackageHash() throws IOException, JSONException {
        return getCurrentPackageInfo().getString("packageHash");
    }

    public static String getPreviousPackageHash() throws IOException, JSONException {
        return getPreviousPackageInfo().getString("packageHash");
    }

    public static void downloadPackage(ReadableMap updatePackage) throws IOException, JSONException {
        String downloadUrl = updatePackage.getString("downloadUrl");
        URL url = new URL(downloadUrl);
        URLConnection connection = url.openConnection();
        ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
        File bundleFile = new File(getBundlePath());
        if(!bundleFile.exists()) {
            bundleFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(bundleFile, false);
        long expectedSize = connection.getContentLength();
        long transferedSize = 0L;
        while( transferedSize < expectedSize ) {
            transferedSize +=
                    fos.getChannel().transferFrom( rbc, transferedSize, 1 << 24 );
        }
        fos.close();

        JSONObject packageJSON = convertReadableToJsonObject(updatePackage);
        writeStringToFile(packageJSON.toString(), getPackagePath());
    }

    public static WritableMap convertJsonObjectToWriteable(JSONObject jsonObj) throws JSONException{
        WritableMap map = Arguments.createMap();
        Iterator<String> it = jsonObj.keys();
        while(it.hasNext()){
            String key = it.next();
            Object obj = jsonObj.get(key);
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
                throw new JSONException("Unrecognized object: " + obj);
        }
        return map;
    }

    public static WritableArray convertJsonArrayToWriteable(JSONArray jsonArr) throws JSONException{
        WritableArray arr = Arguments.createArray();
        for (int i=0; i<jsonArr.length(); i++) {
            Object obj = jsonArr.get(i);
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
                throw new JSONException("Unrecognized object: " + obj);
        }
        return arr;
    }

    public static JSONObject convertReadableToJsonObject(ReadableMap map) throws JSONException{
        JSONObject jsonObj = new JSONObject();
        ReadableMapKeySeyIterator it = map.keySetIterator();
        while (it.hasNextKey()) {
            String key = it.nextKey();
            ReadableType type = map.getType(key);
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
            }
        }
        return jsonObj;
    }

    public static JSONArray convertReadableToJsonArray(ReadableArray arr) throws JSONException{
        JSONArray jsonArr = new JSONArray();
        for(int i=0; i<arr.size(); i++){
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
                    jsonArr.put(arr.getDouble(i));
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
}
