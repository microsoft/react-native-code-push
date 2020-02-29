"use strict";
// IMPORTS
var assert = require("assert");
var bodyParser = require("body-parser");
var express = require("express");
var Q = require("q");
//////////////////////////////////////////////////////////////////////////////////////////
// Use these functions to set up and shut down the server.
/**
 * Sets up the server that the test app uses to send test messages and check for and download updates.
 */
function setupServer(targetPlatform) {
    console.log("Setting up server at " + targetPlatform.getServerUrl());
    var app = express();
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({ extended: true }));
    app.use(function (req, res, next) {
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "*");
        res.setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, X-CodePush-SDK-Version");
        next();
    });
    app.get("/v0.1/public/codepush/update_check", function (req, res) {
        exports.updateCheckCallback && exports.updateCheckCallback(req);
        res.send(exports.updateResponse);
        console.log("Update check called from the app.");
        console.log("Request: " + JSON.stringify(req.query));
        console.log("Response: " + JSON.stringify(exports.updateResponse));
    });
    app.get("/v0.1/public/codepush/report_status/download", function (req, res) {
        console.log("Application downloading the package.");
        res.download(exports.updatePackagePath);
    });
    app.post("/reportTestMessage", function (req, res) {
        console.log("Application reported a test message.");
        console.log("Body: " + JSON.stringify(req.body));
        if (!exports.testMessageResponse) {
            console.log("Sending OK");
            res.sendStatus(200);
        }
        else {
            console.log("Sending body: " + exports.testMessageResponse);
            res.status(200).send(exports.testMessageResponse);
        }
        exports.testMessageCallback && exports.testMessageCallback(req.body);
    });
    var serverPortRegEx = /:([0-9]+)/;
    exports.server = app.listen(+targetPlatform.getServerUrl().match(serverPortRegEx)[1]);
}
exports.setupServer = setupServer;
/**
 * Closes the server.
 */
function cleanupServer() {
    if (exports.server) {
        exports.server.close();
        exports.server = undefined;
    }
}
exports.cleanupServer = cleanupServer;
//////////////////////////////////////////////////////////////////////////////////////////
// Classes and methods used for sending mock responses to the app.
/**
 * Class used to mock the codePush.checkForUpdate() response from the server.
 */
var CheckForUpdateResponseMock = (function () {
    function CheckForUpdateResponseMock() {
    }
    return CheckForUpdateResponseMock;
}());
exports.CheckForUpdateResponseMock = CheckForUpdateResponseMock;
/**
 * The model class of the codePush.checkForUpdate() request to the server.
 */
var UpdateCheckRequestMock = (function () {
    function UpdateCheckRequestMock() {
    }
    return UpdateCheckRequestMock;
}());
exports.UpdateCheckRequestMock = UpdateCheckRequestMock;
/**
 * Returns a default empty response to give to the app in a checkForUpdate request
 */
function createDefaultResponse() {
    var defaultResponse = new CheckForUpdateResponseMock();
    defaultResponse.download_url = "";
    defaultResponse.is_disabled = false;
    defaultResponse.description = "";
    defaultResponse.is_available = false;
    defaultResponse.is_mandatory = false;
    defaultResponse.target_binary_range = "";
    defaultResponse.package_hash = "";
    defaultResponse.label = "";
    defaultResponse.package_size = 0;
    defaultResponse.should_run_binary_version = false;
    defaultResponse.update_app_version = false;
    return defaultResponse;
}
exports.createDefaultResponse = createDefaultResponse;
/**
 * Returns a default update response to give to the app in a checkForUpdate request
 */
function createUpdateResponse(mandatory, targetPlatform, randomHash) {
    if (mandatory === void 0) { mandatory = false; }
    if (randomHash === void 0) { randomHash = true; }
    var updateResponse = new CheckForUpdateResponseMock();
    updateResponse.is_available = true;
    updateResponse.is_disabled = false;
    updateResponse.target_binary_range = "1.0.0";
    updateResponse.download_url = "mock.url/v0.1/public/codepush/report_status/download";
    updateResponse.is_mandatory = mandatory;
    updateResponse.label = "mock-update";
    updateResponse.package_hash = "12345-67890";
    updateResponse.package_size = 12345;
    updateResponse.should_run_binary_version = false;
    updateResponse.update_app_version = false;
    if (!!targetPlatform)
        updateResponse.download_url = targetPlatform.getServerUrl() + "/v0.1/public/codepush/report_status/download";
    // We need unique hashes to avoid conflicts.
    if (randomHash) {
        updateResponse.package_hash = "randomHash-" + Math.floor(Math.random() * 10000);
    }
    return updateResponse;
}
exports.createUpdateResponse = createUpdateResponse;
/**
 * Returns a promise that waits for the next set of test messages sent by the app and resolves if that they are equal to the expected messages or rejects if they are not.
 */
function expectTestMessages(expectedMessages) {
    var deferred = Q.defer();
    var messageIndex = 0;
    var lastRequestBody = null;
    exports.testMessageCallback = function (requestBody) {
        try {
            console.log("Message index: " + messageIndex);
            // We should ignore duplicated requests. It is only CI issue. 
            if (lastRequestBody === null || !areEqual(requestBody, lastRequestBody)) {
                if (typeof expectedMessages[messageIndex] === "string") {
                    assert.equal(requestBody.message, expectedMessages[messageIndex]);
                }
                else {
                    assert(areEqual(requestBody, expectedMessages[messageIndex]));
                }

                lastRequestBody = requestBody;
                
                /* end of message array */
                if (++messageIndex === expectedMessages.length) {
                    deferred.resolve(undefined);
                }
            }
        }
        catch (e) {
            deferred.reject(e);
        }
    };
    return deferred.promise;
}
exports.expectTestMessages = expectTestMessages;
;
//////////////////////////////////////////////////////////////////////////////////////////
// Test messages used by the test app to send state information to the server.
/**
 * Contains all the messages sent from the application to the mock server during tests.
 */
var TestMessage = (function () {
    function TestMessage() {
    }
    TestMessage.CHECK_UP_TO_DATE = "CHECK_UP_TO_DATE";
    TestMessage.CHECK_UPDATE_AVAILABLE = "CHECK_UPDATE_AVAILABLE";
    TestMessage.CHECK_ERROR = "CHECK_ERROR";
    TestMessage.DOWNLOAD_SUCCEEDED = "DOWNLOAD_SUCCEEDED";
    TestMessage.DOWNLOAD_ERROR = "DOWNLOAD_ERROR";
    TestMessage.UPDATE_INSTALLED = "UPDATE_INSTALLED";
    TestMessage.INSTALL_ERROR = "INSTALL_ERROR";
    TestMessage.DEVICE_READY_AFTER_UPDATE = "DEVICE_READY_AFTER_UPDATE";
    TestMessage.UPDATE_FAILED_PREVIOUSLY = "UPDATE_FAILED_PREVIOUSLY";
    TestMessage.NOTIFY_APP_READY_SUCCESS = "NOTIFY_APP_READY_SUCCESS";
    TestMessage.NOTIFY_APP_READY_FAILURE = "NOTIFY_APP_READY_FAILURE";
    TestMessage.SKIPPED_NOTIFY_APPLICATION_READY = "SKIPPED_NOTIFY_APPLICATION_READY";
    TestMessage.SYNC_STATUS = "SYNC_STATUS";
    TestMessage.RESTART_SUCCEEDED = "RESTART_SUCCEEDED";
    TestMessage.RESTART_FAILED = "RESTART_FAILED";
    TestMessage.PENDING_PACKAGE = "PENDING_PACKAGE";
    TestMessage.CURRENT_PACKAGE = "CURRENT_PACKAGE";
    TestMessage.SYNC_UP_TO_DATE = 0;
    TestMessage.SYNC_UPDATE_INSTALLED = 1;
    TestMessage.SYNC_UPDATE_IGNORED = 2;
    TestMessage.SYNC_ERROR = 3;
    TestMessage.SYNC_IN_PROGRESS = 4;
    TestMessage.SYNC_CHECKING_FOR_UPDATE = 5;
    TestMessage.SYNC_AWAITING_USER_ACTION = 6;
    TestMessage.SYNC_DOWNLOADING_PACKAGE = 7;
    TestMessage.SYNC_INSTALLING_UPDATE = 8;
    return TestMessage;
}());
exports.TestMessage = TestMessage;
/**
 * Contains all the messages sent from the mock server back to the application during tests.
 */
var TestMessageResponse = (function () {
    function TestMessageResponse() {
    }
    TestMessageResponse.SKIP_NOTIFY_APPLICATION_READY = "SKIP_NOTIFY_APPLICATION_READY";
    return TestMessageResponse;
}());
exports.TestMessageResponse = TestMessageResponse;
/**
 * Defines the messages sent from the application to the mock server during tests.
 */
var AppMessage = (function () {
    function AppMessage(message, args) {
        this.message = message;
        this.args = args;
    }
    AppMessage.fromString = function (message) {
        return new AppMessage(message, undefined);
    };
    return AppMessage;
}());
exports.AppMessage = AppMessage;
/**
 * Checks if two messages are equal.
 */
function areEqual(m1, m2) {
    /* compare objects */
    if (m1 === m2) {
        return true;
    }
    /* compare messages */
    if (!m1 || !m2 || m1.message !== m2.message) {
        return false;
    }
    /* compare arguments */
    if (m1.args === m2.args) {
        return true;
    }
    if (!m1.args || !m2.args || m1.args.length !== m2.args.length) {
        return false;
    }
    for (var i = 0; i < m1.args.length; i++) {
        if (m1.args[i] !== m2.args[i]) {
            return false;
        }
    }
    return true;
}
exports.areEqual = areEqual;
