"use strict";
var ServerUtil = require("./serverUtil");
var TestConfig = require("./testConfig");
//////////////////////////////////////////////////////////////////////////////////////////
// Use this class to create and structure the tests.
// Usage is almost identical to Mocha, but with the addition of the optional "scenarioPath" in describe() and the required "isCoreTest" in it().
var TestBuilder = (function () {
    function TestBuilder() {
    }
    TestBuilder.describe = getDescribe();
    TestBuilder.it = getIt();
    return TestBuilder;
}());
exports.TestBuilder = TestBuilder;
//////////////////////////////////////////////////////////////////////////////////////////
// Mocha mimicry
/** Singleton class for TestBuilder.describe to use internally to define the context. */
var TestContext = (function () {
    function TestContext() {
    }
    return TestContext;
}());
exports.TestContext = TestContext;
function describeInternal(func, description, spec, scenarioPath) {
    if (!TestContext.projectManager || !TestContext.targetPlatform) {
        throw new Error("TestContext.projectManager or TestContext.targetPlatform are not defined! Did you call TestBuilder.describe outside of a function you passed to PluginTestingFramework.initializeTests?");
    }
    return func(description, function () {
        afterEach(function () {
            console.log("Cleaning up!");
            ServerUtil.updateResponse = undefined;
            ServerUtil.testMessageCallback = undefined;
            ServerUtil.updateCheckCallback = undefined;
            ServerUtil.testMessageResponse = undefined;
        });
        beforeEach(function () {
            return TestContext.targetPlatform.getEmulatorManager().prepareEmulatorForTest(TestConfig.TestNamespace)
                .catch(function () { });
        });
        if (scenarioPath) {
            before(function () {
                return TestContext.projectManager.setupScenario(TestConfig.testRunDirectory, TestConfig.TestNamespace, TestConfig.templatePath, scenarioPath, TestContext.targetPlatform);
            });
        }
        spec();
    });
}
/**
 * Returns a hybrid type that mimics mocha's describe object.
 */
function getDescribe() {
    var describer = function (description, spec, scenarioPath) {
        describeInternal(describe, description, spec, scenarioPath);
    };
    describer.only = function (description, spec, scenarioPath) {
        describeInternal(describe.only, description, spec, scenarioPath);
    };
    describer.skip = function (description, spec, scenarioPath) {
        describeInternal(describe.skip, description, spec, scenarioPath);
    };
    return describer;
}
function itInternal(func, expectation, isCoreTest, assertion) {
    if ((!TestConfig.onlyRunCoreTests || isCoreTest)) {
        // Create a wrapper around the assertion to set the timeout on the test to 10 minutes.
        var assertionWithTimeout = function (done) {
            this.timeout(10 * 2 * 60 * 1000);
            assertion(done);
        };
        return it(expectation, assertionWithTimeout);
    }
    return null;
}
/**
 * Returns a hybrid type that mimics mocha's it object.
 */
function getIt() {
    var itr = function (expectation, isCoreTest, assertion) {
        itInternal(it, expectation, isCoreTest, assertion);
    };
    itr.only = function (expectation, isCoreTest, assertion) {
        itInternal(it.only, expectation, isCoreTest, assertion);
    };
    itr.skip = function (expectation, isCoreTest, assertion) {
        itInternal(it.skip, expectation, isCoreTest, assertion);
    };
    return itr;
}
