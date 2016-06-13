"use strict";
// IMPORTS //
var os = require("os");
var path = require("path");
var TestUtil_1 = require("./TestUtil");
//////////////////////////////////////////////////////////////////////////////////////////
// Configuration variables.
// What plugin to use, what project directories to use, etc.
// COMMAND LINE OPTION NAMES, FLAGS, AND DEFAULTS
var TEST_RUN_DIRECTORY_OPTION_NAME = "--test-directory";
var DEFAULT_TEST_RUN_DIRECTORY = path.join(os.tmpdir(), TestUtil_1.TestUtil.getPluginName(), "test-run");
var TEST_UPDATES_DIRECTORY_OPTION_NAME = "--updates-directory";
var DEFAULT_UPDATES_DIRECTORY = path.join(os.tmpdir(), TestUtil_1.TestUtil.getPluginName(), "updates");
var CORE_TESTS_ONLY_FLAG_NAME = "--core";
var PULL_FROM_NPM_FLAG_NAME = "--npm";
var DEFAULT_PLUGIN_PATH = path.join(__dirname, "../../..");
var NPM_PLUGIN_PATH = TestUtil_1.TestUtil.getPluginName();
var SETUP_FLAG_NAME = "--setup";
var RESTART_EMULATORS_FLAG_NAME = "--clean";
// CONST VARIABLES
exports.TestAppName = "TestCodePush";
exports.TestNamespace = "com.microsoft.codepush.test";
exports.AcquisitionSDKPluginName = "code-push";
exports.templatePath = path.join(__dirname, "../../../test/template");
exports.thisPluginPath = TestUtil_1.TestUtil.readMochaCommandLineFlag(PULL_FROM_NPM_FLAG_NAME) ? NPM_PLUGIN_PATH : DEFAULT_PLUGIN_PATH;
exports.testRunDirectory = TestUtil_1.TestUtil.readMochaCommandLineOption(TEST_RUN_DIRECTORY_OPTION_NAME, DEFAULT_TEST_RUN_DIRECTORY);
exports.updatesDirectory = TestUtil_1.TestUtil.readMochaCommandLineOption(TEST_UPDATES_DIRECTORY_OPTION_NAME, DEFAULT_UPDATES_DIRECTORY);
exports.onlyRunCoreTests = TestUtil_1.TestUtil.readMochaCommandLineFlag(CORE_TESTS_ONLY_FLAG_NAME);
exports.shouldSetup = TestUtil_1.TestUtil.readMochaCommandLineFlag(SETUP_FLAG_NAME);
exports.restartEmulators = TestUtil_1.TestUtil.readMochaCommandLineFlag(RESTART_EMULATORS_FLAG_NAME);
