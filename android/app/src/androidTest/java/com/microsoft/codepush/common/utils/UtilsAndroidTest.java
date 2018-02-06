package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.testutils.CommonFileTestUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealNamedFileWithContent;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getTestingDirectory;
import static org.junit.Assert.assertEquals;

/**
 * This class tests all the {@link CodePushUtils} scenarios.
 */
public class UtilsAndroidTest {

    /**
     * Sample class for JSON mapping.
     */
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

    /**
     * {@link CodePushUtils} instance.
     */
    private CodePushUtils mUtils;

    @Before
    public void setUp() {
        FileUtils fileUtils = FileUtils.getInstance();
        mUtils = CodePushUtils.getInstance(fileUtils);
    }

    /**
     * Tests getting json object from correct json file.
     */
    @Test
    public void testGetJsonObjectFromCorrectJsonFile() throws Exception {
        String inputJsonString = "{\"key\":\"value\"}";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        JSONObject result = mUtils.getJsonObjectFromFile(jsonFile.getAbsolutePath());
        Assert.assertEquals(result.toString(), inputJsonString);
    }

    /**
     * Tests getting json object from malformed json file.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void testGetJsonObjectFromMalformedJsonFile() throws Exception {
        String inputJsonString = "malformed-json";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        mUtils.getJsonObjectFromFile(jsonFile.getAbsolutePath());
    }

    /**
     * Tests getting json object from nonexistent json file.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void testGetJsonObjectFromNonexistentJsonFile() throws Exception {
        mUtils.getJsonObjectFromFile(getTestingDirectory().getAbsolutePath() + "/this/path/is/not/exist");
    }

    /**
     * Tests getting mapped java object from correct json file.
     */
    @Test
    public void testGetObjectFromCorrectJsonFile() throws Exception {
        String inputJsonString = "{\"id\":\"000-000-000\"}";
        File jsonFile = getRealNamedFileWithContent("json.json", inputJsonString);
        SampleObject result = mUtils.getObjectFromJsonFile(jsonFile.getAbsolutePath(), SampleObject.class);
        Assert.assertEquals(result.id, "000-000-000");
    }

    /**
     * Tests converting convertable java object to json file.
     */
    @Test
    public void testConvertConvertableObjectToJsonObject() throws Exception {
        SampleObject object = new SampleObject("000-000-000");
        JSONObject result = mUtils.convertObjectToJsonObject(object);
        Assert.assertEquals(object.id, result.getString("id"));
    }

    /**
     * Tests converting non convertible java object to json file.
     */
    @Test(expected = JSONException.class)
    public void testConvertNonConvertibleObjectToJsonObject() throws Exception {
        mUtils.convertObjectToJsonObject(null);
    }

    /**
     * Tests converting java object to json string.
     */
    @Test
    public void testConvertObjectToJsonString() throws Exception {
        SampleObject object = new SampleObject("000-000-000");
        Assert.assertEquals("{\"id\":\"000-000-000\"}", mUtils.convertObjectToJsonString(object));
    }

    /**
     * Tests writing JSONObject instance to json file.
     */
    @Test
    public void testWriteJsonToFile() throws Exception {
        JSONObject json = new JSONObject("{\"key\":\"value\"}");
        String jsonPath = getTestingDirectory().getAbsolutePath() + "/testWriteJsonToFile/json.json";
        File jsonFile = new File(jsonPath);
        jsonFile.getParentFile().mkdirs();
        jsonFile.createNewFile();
        mUtils.writeJsonToFile(json, jsonPath);
        Assert.assertTrue(jsonFile.exists());
    }

    /**
     * Tests converting java object to query string using supported charset.
     */
    @Test
    public void testGetQueryStringFromObjectWithSupportedCharSet() throws Exception {
        SampleObject object = new SampleObject("id1", "name1");
        String queryString = mUtils.getQueryStringFromObject(object, "UTF-8");
        Assert.assertEquals("name=name1&id=id1", queryString);
    }

    /**
     * Tests converting java object to query string using unsupported charset.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void testGetQueryStringFromObjectWithUnsupportedCharSet() throws Exception {
        SampleObject object = new SampleObject("id1");
        mUtils.getQueryStringFromObject(object, "unsupported");
    }

    /**
     * Tests converting JSONObject instance to java object.
     */
    @Test
    public void testConvertJsonObjectToObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":\"000-000-000\"}");
        SampleObject result = mUtils.convertJsonObjectToObject(jsonObject, SampleObject.class);
        Assert.assertEquals(jsonObject.getString("id"), result.id);
    }

    /**
     * Tests getting string from InputStream instance.
     */
    @Test
    public void testGetStringFromInputStream() throws Exception {
        String expectedString = "string";
        InputStream stream = new ByteArrayInputStream(expectedString.getBytes("UTF-8"));
        assertEquals(expectedString, mUtils.getStringFromInputStream(stream));
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
