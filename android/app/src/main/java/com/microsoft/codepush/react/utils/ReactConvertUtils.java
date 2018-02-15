package com.microsoft.codepush.react.utils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.utils.CodePushUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Utils for managing convert operation within react module.
 */
public class ReactConvertUtils {

    /**
     * Instance of the {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Sigleton instance of the {@link ReactConvertUtils}.
     */
    private static ReactConvertUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private ReactConvertUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static ReactConvertUtils getInstance(CodePushUtils codePushUtils) {
        if (INSTANCE == null) {
            INSTANCE = new ReactConvertUtils();
        }
        INSTANCE.mCodePushUtils = codePushUtils;
        return INSTANCE;
    }

    /**
     * Converts a {@link JSONArray} to a {@link WritableArray}.
     *
     * @param jsonArray instance of {@link JSONArray}.
     * @return instance of {@link WritableArray}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    private WritableArray convertJsonArrayToWritable(JSONArray jsonArray) throws CodePushMalformedDataException {
        WritableArray writableArray = Arguments.createArray();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object object = jsonArray.get(i);
                if (object instanceof JSONObject)
                    writableArray.pushMap(convertJsonObjectToWritable((JSONObject) object));
                else if (object instanceof JSONArray)
                    writableArray.pushArray(convertJsonArrayToWritable((JSONArray) object));
                else if (object instanceof String)
                    writableArray.pushString((String) object);
                else if (object instanceof Double)
                    writableArray.pushDouble((Double) object);
                else if (object instanceof Integer)
                    writableArray.pushInt((Integer) object);
                else if (object instanceof Boolean)
                    writableArray.pushBoolean((Boolean) object);
                else if (object == null)
                    writableArray.pushNull();
            }
        } catch (JSONException e) {
            throw new CodePushMalformedDataException("Error when converting json array to writable.", e);
        }
        return writableArray;
    }

    /**
     * Converts a {@link JSONObject} to a {@link WritableArray}.
     *
     * @param jsonObject instance of {@link JSONObject}.
     * @return instance of {@link WritableMap}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public WritableMap convertJsonObjectToWritable(JSONObject jsonObject) throws CodePushMalformedDataException {
        WritableMap writableMap = Arguments.createMap();
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                Object object = jsonObject.get(key);
                if (object instanceof JSONObject)
                    writableMap.putMap(key, convertJsonObjectToWritable((JSONObject) object));
                else if (object instanceof JSONArray)
                    writableMap.putArray(key, convertJsonArrayToWritable((JSONArray) object));
                else if (object instanceof String)
                    writableMap.putString(key, (String) object);
                else if (object instanceof Double)
                    writableMap.putDouble(key, (Double) object);
                else if (object instanceof Integer)
                    writableMap.putInt(key, (Integer) object);
                else if (object instanceof Boolean)
                    writableMap.putBoolean(key, (Boolean) object);
                else if (object == null)
                    writableMap.putNull(key);
            } catch (JSONException e) {
                throw new CodePushMalformedDataException("Error when converting json object to writable.", e);
            }
        }
        return writableMap;
    }

    /**
     * Converts a {@link ReadableArray} to a {@link JSONArray}.
     *
     * @param readableArray instance of {@link ReadableArray}.
     * @return instance of {@link JSONArray}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    private JSONArray convertReadableToJsonArray(ReadableArray readableArray) throws CodePushMalformedDataException {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            ReadableType readableType = readableArray.getType(i);
            switch (readableType) {
                case Map:
                    jsonArray.put(convertReadableToJsonObject(readableArray.getMap(i)));
                    break;
                case Array:
                    jsonArray.put(convertReadableToJsonArray(readableArray.getArray(i)));
                    break;
                case String:
                    jsonArray.put(readableArray.getString(i));
                    break;
                case Number:
                    Double number = readableArray.getDouble(i);
                    if ((number == Math.floor(number)) && !Double.isInfinite(number)) {

                        /* This is a whole number. */
                        jsonArray.put(number.longValue());
                    } else {
                        try {
                            jsonArray.put(number.doubleValue());
                        } catch (JSONException e) {
                            throw new CodePushMalformedDataException("Error when converting readable to json array.", e);
                        }
                    }
                    break;
                case Boolean:
                    jsonArray.put(readableArray.getBoolean(i));
                    break;
                case Null:
                    jsonArray.put(null);
                    break;
            }
        }
        return jsonArray;
    }

    /**
     * Converts a {@link ReadableMap} to an object of specified type.
     *
     * @param readableMap instance of {@link ReadableMap}.
     * @param classOfT    class of the desired type.
     * @param <T>         type of the object to be converted to.
     * @return instance of {@link T}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public <T> T convertReadableToObject(ReadableMap readableMap, Class<T> classOfT) throws CodePushMalformedDataException {
        return mCodePushUtils.convertJsonObjectToObject(convertReadableToJsonObject(readableMap), classOfT);
    }

    /**
     * Converts a {@link ReadableMap} to a {@link JSONObject}.
     *
     * @param readableMap instance of {@link ReadableMap}.
     * @return instance of {@link JSONObject}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    private JSONObject convertReadableToJsonObject(ReadableMap readableMap) throws CodePushMalformedDataException {
        JSONObject jsonObject = new JSONObject();
        ReadableMapKeySetIterator readableMapKeySetIterator = readableMap.keySetIterator();
        while (readableMapKeySetIterator.hasNextKey()) {
            String key = readableMapKeySetIterator.nextKey();
            ReadableType type = readableMap.getType(key);
            try {
                switch (type) {
                    case Map:
                        jsonObject.put(key, convertReadableToJsonObject(readableMap.getMap(key)));
                        break;
                    case Array:
                        jsonObject.put(key, convertReadableToJsonArray(readableMap.getArray(key)));
                        break;
                    case String:
                        jsonObject.put(key, readableMap.getString(key));
                        break;
                    case Number:
                        jsonObject.put(key, readableMap.getDouble(key));
                        break;
                    case Boolean:
                        jsonObject.put(key, readableMap.getBoolean(key));
                        break;
                    case Null:
                        jsonObject.put(key, null);
                        break;
                    default:
                        throw new CodePushMalformedDataException("Unrecognized type: " + type + " of key: " + key);
                }
            } catch (JSONException jsonException) {
                throw new CodePushMalformedDataException("Error setting key: " + key + " in JSONObject", jsonException);
            }
        }
        return jsonObject;
    }

    /**
     * Converts an object to {@link WritableMap}.
     *
     * @param object object of any type.
     * @return instance of {@link WritableMap}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public WritableMap convertObjectToWritableMap(Object object) throws CodePushMalformedDataException {
        try {
            return convertJsonObjectToWritable(mCodePushUtils.convertObjectToJsonObject(object));
        } catch (JSONException e) {
            throw new CodePushMalformedDataException("Error when converting object to writable map.", e);
        }
    }

    /**
     * Converts an object of {@link DownloadProgress} type to {@link WritableMap}.
     *
     * @param downloadProgress instance of {@link DownloadProgress}.
     * @return instance of {@link WritableMap}.
     */
    public WritableMap convertDownloadProgressToWritableMap(DownloadProgress downloadProgress) {
        WritableMap map = new WritableNativeMap();
        if (downloadProgress.getTotalBytes() < Integer.MAX_VALUE) {
            map.putInt("totalBytes", (int) downloadProgress.getTotalBytes());
            map.putInt("receivedBytes", (int) downloadProgress.getReceivedBytes());
        } else {
            map.putDouble("totalBytes", downloadProgress.getTotalBytes());
            map.putDouble("receivedBytes", downloadProgress.getReceivedBytes());
        }
        return map;
    }
}
