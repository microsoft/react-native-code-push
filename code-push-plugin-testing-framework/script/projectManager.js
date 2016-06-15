"use strict";
var TestConfig = require("./testConfig");
/**
 * In charge of project related operations.
 */
var ProjectManager = (function () {
    function ProjectManager() {
    }
    //// ABSTRACT METHODS
    // (not actually abstract because there are some issues with our dts generator that causes it to incorrectly generate abstract classes)
    /**
     * Returns the name of the plugin being tested, for example Cordova or React-Native.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.getPluginName = function () { throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG; };
    /**
     * Creates a new test application at the specified path, and configures it
     * with the given server URL, android and ios deployment keys.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.setupProject = function (projectDirectory, templatePath, appName, appNamespace, version) {
        if (version === void 0) { version = ProjectManager.DEFAULT_APP_VERSION; }
        throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG;
    };
    /**
     * Sets up the scenario for a test in an already existing project.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.setupScenario = function (projectDirectory, appId, templatePath, jsPath, targetPlatform, version) {
        if (version === void 0) { version = ProjectManager.DEFAULT_APP_VERSION; }
        throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG;
    };
    /**
     * Creates a CodePush update package zip for a project.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.createUpdateArchive = function (projectDirectory, targetPlatform, isDiff) { throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG; };
    /**
     * Prepares a specific platform for tests.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.preparePlatform = function (projectDirectory, targetPlatform) { throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG; };
    /**
     * Cleans up a specific platform after tests.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.cleanupAfterPlatform = function (projectDirectory, targetPlatform) { throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG; };
    /**
     * Runs the test app on the given target / platform.
     *
     * Overwrite this in your implementation!
     */
    ProjectManager.prototype.runApplication = function (projectDirectory, targetPlatform) { throw ProjectManager.NOT_IMPLEMENTED_ERROR_MSG; };
    ProjectManager.DEFAULT_APP_VERSION = "Store version";
    ProjectManager.NOT_IMPLEMENTED_ERROR_MSG = "This method is unimplemented! Please extend ProjectManager and overwrite it!";
    return ProjectManager;
}());
exports.ProjectManager = ProjectManager;
//////////////////////////////////////////////////////////////////////////////////////////
// Wrapper functions for simpler code in test cases.
/**
 * Wrapper for ProjectManager.setupScenario in the TestRun directory.
 */
function setupTestRunScenario(projectManager, targetPlatform, scenarioJsPath, version) {
    return projectManager.setupScenario(TestConfig.testRunDirectory, TestConfig.TestNamespace, TestConfig.templatePath, scenarioJsPath, targetPlatform, version);
}
exports.setupTestRunScenario = setupTestRunScenario;
/**
 * Creates an update and zip for the test app using the specified scenario and version.
 */
function setupUpdateScenario(projectManager, targetPlatform, scenarioJsPath, version) {
    return projectManager.setupScenario(TestConfig.updatesDirectory, TestConfig.TestNamespace, TestConfig.templatePath, scenarioJsPath, targetPlatform, version)
        .then(projectManager.createUpdateArchive.bind(projectManager, TestConfig.updatesDirectory, targetPlatform));
}
exports.setupUpdateScenario = setupUpdateScenario;
