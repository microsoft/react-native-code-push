package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.testutils.CommonFileTestUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealNamedFileWithContent;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getTestingDirectory;
import static com.microsoft.codepush.common.utils.CodePushUtils.convertJsonObjectToObject;
import static com.microsoft.codepush.common.utils.CodePushUtils.convertObjectToJsonObject;
import static com.microsoft.codepush.common.utils.CodePushUtils.convertObjectToJsonString;
import static com.microsoft.codepush.common.utils.CodePushUtils.getJsonObjectFromFile;
import static com.microsoft.codepush.common.utils.CodePushUtils.getObjectFromJsonFile;
import static com.microsoft.codepush.common.utils.CodePushUtils.getQueryStringFromObject;
import static com.microsoft.codepush.common.utils.CodePushUtils.getStringFromInputStream;
import static com.microsoft.codepush.common.utils.CodePushUtils.writeJsonToFile;
import static org.junit.Assert.assertEquals;

public class UtilsAndroidTest {

    private final class SampleObject {
        public String id;
        public String name;

        public SampleObject(String id) {
            this.id = id;
        }

        public SampleObject(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Test
    public void testGetJsonObjectFromCorrectJsonFile() throws IOException, CodePushMalformedDataException {
        String inputJsonString = "{\"key\":\"value\"}";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        JSONObject result = getJsonObjectFromFile(jsonFile.getAbsolutePath());
        Assert.assertEquals(result.toString(), inputJsonString);
    }

    @Test(expected = CodePushMalformedDataException.class)
    public void testGetJsonObjectFromMalformedJsonFile() throws Exception {
        String inputJsonString = "malformed-json";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        getJsonObjectFromFile(jsonFile.getAbsolutePath());
    }

    @Test(expected = CodePushMalformedDataException.class)
    public void testGetJsonObjectFromUnexistentJsonFile() throws Exception {
        getJsonObjectFromFile(getTestingDirectory().getAbsolutePath() + "/this/path/is/not/exist");
    }

    @Test
    public void testGetObjectFromCorrectJsonFile() throws IOException, CodePushMalformedDataException {
        String inputJsonString = "{\"id\":\"000-000-000\"}";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        SampleObject result = getObjectFromJsonFile(jsonFile.getAbsolutePath(), SampleObject.class);
        Assert.assertEquals(result.id, "000-000-000");
    }

    @Test
    public void testConvertConvertableObjectToJsonObject() throws JSONException {
        SampleObject object = new SampleObject("000-000-000");
        JSONObject result = convertObjectToJsonObject(object);
        Assert.assertEquals(object.id, result.getString("id"));
    }

    @Test(expected = JSONException.class)
    public void testConvertUnconvertableObjectToJsonObject() throws JSONException {
        convertObjectToJsonObject(null);
    }

    @Test
    public void testConvertObjectToJsonString() throws JSONException {
        SampleObject object = new SampleObject("000-000-000");
        Assert.assertEquals("{\"id\":\"000-000-000\"}", convertObjectToJsonString(object));
    }

    @Test
    public void testWriteJsonToFile() throws Exception {
        JSONObject json = new JSONObject("{\"key\":\"value\"}");
        String jsonPath = getTestingDirectory().getAbsolutePath() + UtilsAndroidTest.class + "testWriteJsonToFile/json.json";
        File resultFile = new File(jsonPath);
        writeJsonToFile(json, jsonPath);
        Assert.assertTrue(resultFile.exists());
    }

    @Test
    public void testGetQueryStringFromObject() throws Exception {
        SampleObject object = new SampleObject("id1", "name1");
        String queryString = getQueryStringFromObject(object, "UTF-8");
        Assert.assertEquals("name=name1&id=id1", queryString);
    }

    @Test(expected = CodePushMalformedDataException.class)
    public void testGetQueryStringFromObjectWithUnsupportedCharSet() throws Exception {
        SampleObject object = new SampleObject("id1");
        getQueryStringFromObject(object, "unsupported");
    }

    @Test
    public void testConvertJsonObjectToObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":\"000-000-000\"}");
        SampleObject result = convertJsonObjectToObject(jsonObject, SampleObject.class);
        Assert.assertEquals(jsonObject.getString("id"), result.id);
    }

    @Test
    public void testGetStringFromInputStream() throws Exception {
        String expectedString = "string";
        InputStream stream = new ByteArrayInputStream(expectedString.getBytes("UTF-8"));
        assertEquals(expectedString, getStringFromInputStream(stream));
    }

    /**
     * Cleanup created temporary test directories.
     */
    @After
    public void tearDown() throws Exception {
        File testFolder = CommonFileTestUtils.getTestingDirectory();
        testFolder.delete();
    }
}
