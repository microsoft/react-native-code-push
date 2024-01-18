"use strict";
// IMPORTS //
var os = require("os");
var path = require("path");
var TestUtil_1 = require("./testUtil");
//////////////////////////////////////////////////////////////////////////////////////////
// Configuration variables.
// What plugin to use, what project directories to use, etc.
// COMMAND LINE OPTION NAMES, FLAGS, AND DEFAULTS
var DEFAULT_TEST_RUN_DIRECTORY = path.join(os.tmpdir(), TestUtil_1.TestUtil.getPluginName(), "test-run");
var DEFAULT_UPDATES_DIRECTORY = path.join(os.tmpdir(), TestUtil_1.TestUtil.getPluginName(), "updates");
var DEFAULT_PLUGIN_PATH = path.join(__dirname, "../..");
var NPM_PLUGIN_PATH = TestUtil_1.TestUtil.getPluginName();
var SETUP_FLAG_NAME = "--setup";
var DEFAULT_PLUGIN_TGZ_NAME = TestUtil_1.TestUtil.getPluginName() + "-" + TestUtil_1.TestUtil.getPluginVersion() + ".tgz";
// CONST VARIABLES
exports.TestAppName = "TestCodePush";
exports.TestNamespace = "com.testcodepush";
exports.AcquisitionSDKPluginName = "code-push";
exports.templatePath = path.join(__dirname, "../../test/template");
exports.thisPluginInstallString = TestUtil_1.TestUtil.resolveBooleanVariables(process.env.NPM) ? `npm install ${NPM_PLUGIN_PATH}` : `npm pack ${DEFAULT_PLUGIN_PATH} && npm install ${DEFAULT_PLUGIN_TGZ_NAME} && npm link`;
exports.testRunDirectory = process.env.RUN_DIR ? process.env.RUN_DIR: DEFAULT_TEST_RUN_DIRECTORY;
exports.updatesDirectory = process.env.UPDATE_DIR ? process.env.UPDATE_DIR : DEFAULT_UPDATES_DIRECTORY;
exports.onlyRunCoreTests = TestUtil_1.TestUtil.resolveBooleanVariables(process.env.CORE);
exports.shouldSetup = TestUtil_1.TestUtil.readMochaCommandLineFlag(SETUP_FLAG_NAME);
exports.restartEmulators = TestUtil_1.TestUtil.resolveBooleanVariables(process.env.CLEAN);
