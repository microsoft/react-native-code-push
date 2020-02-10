"use strict";
var Q = require("q");
var ServerUtil = require("./serverUtil");
var testBuilder_1 = require("./testBuilder");
var TestConfig = require("./testConfig");
var testUtil_1 = require("./testUtil");
//////////////////////////////////////////////////////////////////////////////////////////
/**
 * Call this function to initialize the automated tests.
 */
function initializeTests(projectManager, supportedTargetPlatforms, describeTests) {
    // DETERMINE PLATFORMS TO TEST //
    /** The platforms to test on. */
    var targetPlatforms = [];
    supportedTargetPlatforms.forEach(function (supportedPlatform) {
        if (testUtil_1.TestUtil.readMochaCommandLineFlag(supportedPlatform.getCommandLineFlagName()))
            targetPlatforms.push(supportedPlatform);
    });
    // Log current configuration
    console.log("Initializing tests for " + testUtil_1.TestUtil.getPluginName());
    console.log(TestConfig.TestAppName + "\n" + TestConfig.TestNamespace);
    console.log("Testing " + TestConfig.thisPluginPath + ".");
    targetPlatforms.forEach(function (platform) {
        console.log("On " + platform.getName());
    });
    console.log("test run directory = " + TestConfig.testRunDirectory);
    console.log("updates directory = " + TestConfig.updatesDirectory);
    if (TestConfig.onlyRunCoreTests)
        console.log("--only running core tests--");
    if (TestConfig.shouldSetup)
        console.log("--setting up--");
    if (TestConfig.restartEmulators)
        console.log("--restarting emulators--");
    // FUNCTIONS //
    function cleanupTest() {
        console.log("Cleaning up!");
        ServerUtil.updateResponse = undefined;
        ServerUtil.testMessageCallback = undefined;
        ServerUtil.updateCheckCallback = undefined;
        ServerUtil.testMessageResponse = undefined;
    }
    /**
     * Sets up tests for each platform.
     * Creates the test project directory and the test update directory.
     * Starts required emulators.
     */
    function setupTests() {
        it("sets up tests correctly", function (done) {
            var promises = [];
            targetPlatforms.forEach(function (platform) {
                promises.push(platform.getEmulatorManager().bootEmulator(TestConfig.restartEmulators));
            });
            console.log("Building test project.");
            // create the test project
            promises.push(createTestProject(TestConfig.testRunDirectory)
                .then(function () {
                    console.log("Building update project.");
                    // create the update project
                    return createTestProject(TestConfig.updatesDirectory);
                }).then(function () { return null; }));
            Q.all(promises).then(function () { done(); }, function (error) { done(error); });
        });
    }
    /**
     * Creates a test project directory at the given path.
     */
    function createTestProject(directory) {
        return projectManager.setupProject(directory, TestConfig.templatePath, TestConfig.TestAppName, TestConfig.TestNamespace);
    }
    /**
     * Creates and runs the tests from the projectManager and TestBuilderDescribe objects passed to initializeTests.
     */
    function createAndRunTests(targetPlatform) {
        describe("CodePush", function () {
            before(function () {
                ServerUtil.setupServer(targetPlatform);
                return targetPlatform.getEmulatorManager().uninstallApplication(TestConfig.TestNamespace)
                    .then(projectManager.preparePlatform.bind(projectManager, TestConfig.testRunDirectory, targetPlatform))
                    .then(projectManager.preparePlatform.bind(projectManager, TestConfig.updatesDirectory, targetPlatform));
            });
            after(function () {
                ServerUtil.cleanupServer();
                return projectManager.cleanupAfterPlatform(TestConfig.testRunDirectory, targetPlatform).then(projectManager.cleanupAfterPlatform.bind(projectManager, TestConfig.updatesDirectory, targetPlatform));
            });
            testBuilder_1.TestContext.projectManager = projectManager;
            testBuilder_1.TestContext.targetPlatform = targetPlatform;
            // Build the tests.
            describeTests(projectManager, targetPlatform);
        });
    }
    // BEGIN TESTING //
    describe("CodePush " + projectManager.getPluginName() + " Plugin", function () {
        this.timeout(100 * 60 * 1000);
        if (TestConfig.shouldSetup)
            describe("Setting Up For Tests", function () { return setupTests(); });
        else {
            targetPlatforms.forEach(function (platform) {
                var prefix = (TestConfig.onlyRunCoreTests ? "Core Tests " : "Tests ") + TestConfig.thisPluginPath + " on ";
                describe(prefix + platform.getName(), function () { return createAndRunTests(platform); });
            });
        }
    });
}
exports.initializeTests = initializeTests;
