declare module 'code-push-plugin-testing-framework/script/platform' {
	import Q = require("q");
	/**
	 * Defines a platform supported by CodePush.
	 */
	export interface IPlatform {
	    /**
	     * Gets the platform name. (e.g. "android" for the Android platform).
	     */
	    getName(): string;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
	    /**
	     * Gets the root of the platform www folder used for creating update packages.
	     */
	    getPlatformWwwPath(projectDirectory: string): string;
	    /**
	     * Gets an IEmulatorManager that is used to control the emulator during the tests.
	     */
	    getEmulatorManager(): IEmulatorManager;
	    /**
	     * Gets the default deployment key.
	     */
	    getDefaultDeploymentKey(): string;
	}
	/**
	 * Manages the interaction with the emulator.
	 */
	export interface IEmulatorManager {
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<string>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<string>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<string>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<string>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs: number): Q.Promise<string>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<string>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<string>;
	}
	/**
	 * Android implementations of IPlatform.
	 */
	export class Android implements IPlatform {
	    private static instance;
	    private emulatorManager;
	    private serverUrl;
	    constructor(emulatorManager: IEmulatorManager);
	    static getInstance(): Android;
	    getName(): string;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
	    getPlatformWwwPath(projectDirectory: string): string;
	    getEmulatorManager(): IEmulatorManager;
	    getDefaultDeploymentKey(): string;
	}
	/**
	 * IOS implementation of IPlatform.
	 */
	export class IOS implements IPlatform {
	    private static instance;
	    private emulatorManager;
	    private serverUrl;
	    constructor(emulatorManager: IEmulatorManager);
	    static getInstance(): IOS;
	    getName(): string;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
	    getPlatformWwwPath(projectDirectory: string): string;
	    getEmulatorManager(): IEmulatorManager;
	    getDefaultDeploymentKey(): string;
	}
	export class IOSEmulatorManager implements IEmulatorManager {
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<string>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<string>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<string>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<string>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<string>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<string>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<string>;
	}
	export class AndroidEmulatorManager implements IEmulatorManager {
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<string>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<string>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<string>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<string>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<string>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<string>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<string>;
	}
	/**
	 * Supported platforms resolver.
	 */
	export class PlatformResolver {
	    private static supportedPlatforms;
	    /**
	     * Given the cordova name of a platform, this method returns the IPlatform associated with it.
	     */
	    static resolvePlatforms(cordovaPlatformNames: string[]): IPlatform[];
	    /**
	     * Given the cordova name of a platform, this method returns the IPlatform associated with it.
	     */
	    static resolvePlatform(cordovaPlatformName: string): IPlatform;
	}

}
declare module 'code-push-plugin-testing-framework/script/projectManager' {
	import Q = require("q");
	import platform = require('code-push-plugin-testing-framework/script/platform');
	/**
	 * In charge of project related operations.
	 */
	export class ProjectManager {
	    static ANDROID_KEY_PLACEHOLDER: string;
	    static IOS_KEY_PLACEHOLDER: string;
	    static SERVER_URL_PLACEHOLDER: string;
	    static INDEX_JS_PLACEHOLDER: string;
	    static CODE_PUSH_APP_VERSION_PLACEHOLDER: string;
	    static CODE_PUSH_APP_ID_PLACEHOLDER: string;
	    static PLUGIN_VERSION_PLACEHOLDER: string;
	    static DEFAULT_APP_VERSION: string;
	    private static NOT_IMPLEMENTED_ERROR_MSG;
	    /**
	     * Returns the name of the plugin being tested, ie Cordova or React-Native.
	     *
	     * Overwrite this in your implementation!
	     */
	    getPluginName(): string;
	    /**
	     * Creates a new test application at the specified path, and configures it
	     * with the given server URL, android and ios deployment keys.
	     *
	     * Overwrite this in your implementation!
	     */
	    setupProject(projectDirectory: string, templatePath: string, appName: string, appNamespace: string, version?: string): Q.Promise<string>;
	    /**
	     * Sets up the scenario for a test in an already existing project.
	     *
	     * Overwrite this in your implementation!
	     */
	    setupScenario(projectDirectory: string, appId: string, templatePath: string, jsPath: string, targetPlatform: platform.IPlatform, version?: string): Q.Promise<string>;
	    /**
	     * Creates a CodePush update package zip for a project.
	     *
	     * Overwrite this in your implementation!
	     */
	    createUpdateArchive(projectDirectory: string, targetPlatform: platform.IPlatform, isDiff?: boolean): Q.Promise<string>;
	    /**
	     * Prepares a specific platform for tests.
	     *
	     * Overwrite this in your implementation!
	     */
	    preparePlatform(projectFolder: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Cleans up a specific platform after tests.
	     *
	     * Overwrite this in your implementation!
	     */
	    cleanupAfterPlatform(projectFolder: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Runs the test app on the given target / platform.
	     *
	     * Overwrite this in your implementation!
	     */
	    runPlatform(projectFolder: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Launch the test app on the given target / platform.
	     */
	    launchApplication(appNamespace: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Kill the test app on the given target / platform.
	     */
	    endRunningApplication(appNamespace: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appNamespace: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appNamespace: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Stops and restarts an application specified by its namespace identifier.
	     */
	    restartApplication(appNamespace: string, targetPlatform: platform.IPlatform): Q.Promise<string>;
	    /**
	     * Navigates away from the application and then navigates back to it.
	     */
	    resumeApplication(appNamespace: string, targetPlatform: platform.IPlatform, delayBeforeResumingMs?: number): Q.Promise<string>;
	    /**
	     * Executes a child process and logs its output to the console and returns its output in the promise as a string
	     */
	    static execChildProcess(command: string, options?: {
	        cwd?: string;
	        stdio?: any;
	        customFds?: any;
	        env?: any;
	        encoding?: string;
	        timeout?: number;
	        maxBuffer?: number;
	        killSignal?: string;
	    }, logOutput?: boolean): Q.Promise<string>;
	    /**
	     * Replaces a regex in a file with a given string.
	     */
	    static replaceString(filePath: string, regex: string, replacement: string): void;
	    /**
	     * Copies a file from a given location to another.
	     */
	    static copyFile(source: string, destination: string, overwrite: boolean): Q.Promise<void>;
	}

}
declare module 'code-push-plugin-testing-framework/script/testUtil' {
	import Q = require("q");
	export class TestUtil {
	    static ANDROID_PLATFORM_OPTION_NAME: string;
	    static ANDROID_SERVER_URL: string;
	    static defaultAndroidServerUrl: string;
	    static ANDROID_EMULATOR: string;
	    static defaultAndroidEmulator: string;
	    static IOS_PLATFORM_OPTION_NAME: string;
	    static IOS_SERVER_URL: string;
	    static defaultIOSServerUrl: string;
	    static IOS_EMULATOR: string;
	    static SHOULD_USE_WKWEBVIEW: string;
	    static templatePath: string;
	    static thisPluginPath: string;
	    static TEST_RUN_DIRECTORY: string;
	    private static defaultTestRunDirectory;
	    static TEST_UPDATES_DIRECTORY: string;
	    private static defaultUpdatesDirectory;
	    static CORE_TESTS_ONLY: string;
	    static PULL_FROM_NPM: string;
	    static SETUP: string;
	    static RESTART_EMULATORS: string;
	    /**
	     * Reads the directory in which the test project is.
	     */
	    static readTestRunDirectory(): string;
	    /**
	     * Reads the directory in which the test project for updates is.
	     */
	    static readTestUpdatesDirectory(): string;
	    /**
	     * Reads the path of the plugin (whether or not we should use the local copy or pull from npm)
	     */
	    static readPluginPath(): string;
	    /**
	     * Reads the Android server url to use
	     */
	    static readAndroidServerUrl(): string;
	    /**
	     * Reads the iOS server url to use
	     */
	    static readIOSServerUrl(): string;
	    /**
	     * Reads the Android emulator to use
	     */
	    static readAndroidEmulator(): string;
	    /**
	     * Reads the iOS emulator to use
	     */
	    static readIOSEmulator(): Q.Promise<string>;
	    /**
	     * Reads whether or not emulators should be restarted.
	     */
	    static readRestartEmulators(): boolean;
	    /**
	     * Reads whether or not only core tests should be run.
	     */
	    static readCoreTestsOnly(): boolean;
	    /**
	     * Reads whether or not to setup the test project directories.
	     */
	    static readShouldSetup(): boolean;
	    /**
	     * Reads the test target platforms.
	     */
	    static readTargetPlatforms(): string[];
	    /**
	     * Reads if we should use the WkWebView or the UIWebView or run tests for both.
	     * 0 for UIWebView, 1 for WkWebView, 2 for both
	     */
	    static readShouldUseWkWebView(): number;
	    /**
	     * Reads command line options passed to mocha.
	     */
	    private static readMochaCommandLineOption(optionName);
	    /**
	     * Reads command line options passed to mocha.
	     */
	    private static readMochaCommandLineFlag(optionName);
	    /**
	     * Executes a child process returns its output as a string.
	     */
	    static getProcessOutput(command: string, options?: {
	        cwd?: string;
	        stdio?: any;
	        customFds?: any;
	        env?: any;
	        encoding?: string;
	        timeout?: number;
	        maxBuffer?: number;
	        killSignal?: string;
	    }, logOutput?: boolean): Q.Promise<string>;
	}

}
declare module 'code-push-plugin-testing-framework/script/serverUtil' {
	/**
	 * Class used to mock the codePush.checkForUpdate() response from the server.
	 */
	export class CheckForUpdateResponseMock {
	    downloadURL: string;
	    isAvailable: boolean;
	    packageSize: number;
	    updateAppVersion: boolean;
	    appVersion: string;
	    description: string;
	    label: string;
	    packageHash: string;
	    isMandatory: boolean;
	}
	/**
	 * The model class of the codePush.checkForUpdate() request to the server.
	 */
	export class UpdateCheckRequestMock {
	    deploymentKey: string;
	    appVersion: string;
	    packageHash: string;
	    isCompanion: boolean;
	}
	/**
	 * Contains all the messages sent from the application to the mock server during tests.
	 */
	export class TestMessage {
	    static CHECK_UP_TO_DATE: string;
	    static CHECK_UPDATE_AVAILABLE: string;
	    static CHECK_ERROR: string;
	    static DOWNLOAD_SUCCEEDED: string;
	    static DOWNLOAD_ERROR: string;
	    static UPDATE_INSTALLED: string;
	    static INSTALL_ERROR: string;
	    static DEVICE_READY_AFTER_UPDATE: string;
	    static UPDATE_FAILED_PREVIOUSLY: string;
	    static NOTIFY_APP_READY_SUCCESS: string;
	    static NOTIFY_APP_READY_FAILURE: string;
	    static SKIPPED_NOTIFY_APPLICATION_READY: string;
	    static SYNC_STATUS: string;
	    static RESTART_SUCCEEDED: string;
	    static RESTART_FAILED: string;
	    static PENDING_PACKAGE: string;
	    static CURRENT_PACKAGE: string;
	    static SYNC_UP_TO_DATE: number;
	    static SYNC_UPDATE_INSTALLED: number;
	    static SYNC_UPDATE_IGNORED: number;
	    static SYNC_ERROR: number;
	    static SYNC_IN_PROGRESS: number;
	    static SYNC_CHECKING_FOR_UPDATE: number;
	    static SYNC_AWAITING_USER_ACTION: number;
	    static SYNC_DOWNLOADING_PACKAGE: number;
	    static SYNC_INSTALLING_UPDATE: number;
	}
	/**
	 * Contains all the messages sent from the mock server back to the application during tests.
	 */
	export class TestMessageResponse {
	    static SKIP_NOTIFY_APPLICATION_READY: string;
	}
	/**
	 * Defines the messages sent from the application to the mock server during tests.
	 */
	export class AppMessage {
	    message: string;
	    args: any[];
	    constructor(message: string, args: any[]);
	    static fromString(message: string): AppMessage;
	}
	/**
	 * Checks if two messages are equal.
	 */
	export function areEqual(m1: AppMessage, m2: AppMessage): boolean;

}
declare module 'code-push-plugin-testing-framework/script/test' {
	import platform = require('code-push-plugin-testing-framework/script/platform');
	import Q = require("q");
	import tm = require('code-push-plugin-testing-framework/script/projectManager');
	import tu = require('code-push-plugin-testing-framework/script/testUtil');
	import su = require('code-push-plugin-testing-framework/script/serverUtil');
	export const TestAppName: string;
	export const TestNamespace: string;
	export const AcquisitionSDKPluginName: string;
	/** Response the server gives the next update check request */
	export var updateResponse: any;
	/** Response the server gives the next test message request */
	export var testMessageResponse: any;
	/** Called after the next test message request */
	export var testMessageCallback: (requestBody: any) => void;
	/** Called after the next update check request */
	export var updateCheckCallback: (requestBody: any) => void;
	/** Location of the update package given in the update check response */
	export var updatePackagePath: string;
	export var testUtil: typeof tu.TestUtil;
	export var templatePath: string;
	export var thisPluginPath: string;
	export var testRunDirectory: string;
	export var updatesDirectory: string;
	export var onlyRunCoreTests: boolean;
	export var targetPlatforms: platform.IPlatform[];
	export var shouldUseWkWebView: number;
	export var shouldSetup: boolean;
	export var restartEmulators: boolean;
	export interface TestBuilder {
	    /**
	     * Called to create the test suite by the initializeTests function
	     *
	     * coreTestsOnly - Whether or not only core tests are to be run
	     * projectManager - The projectManager instance that these tests are being run with
	     * targetPlatform - The platform that these tests are going to be run on
	     */
	    create(coreTestsOnly: boolean, projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform): void;
	}
	/** Use this class to create a mocha.describe that contains additional tests */
	export class TestBuilderDescribe implements TestBuilder {
	    /** The name passed to the describe */
	    private describeName;
	    /** The path to the scenario that will be loaded by the test app for the nested TestBuilder objects */
	    private scenarioPath;
	    /** An array of nested TestBuilder objects that this describe contains */
	    private testBuilders;
	    /** Whether or not this.testBuilders directly contains any TestBuildIt objects */
	    private hasIts;
	    /**
	     * describeName - used as the description in the call to describe
	     * scenarioPath - if specified, will be set up before the tests run
	     * testBuilders - the testBuilders to create within this describe call
	     */
	    constructor(describeName: string, testBuilders: TestBuilder[], scenarioPath?: string);
	    /**
	     * Called to create the test suite by the initializeTests function
	     *
	     * coreTestsOnly - Whether or not only core tests are to be run
	     * projectManager - The projectManager instance that these tests are being run with
	     * targetPlatform - The platform that these tests are going to be run on
	     */
	    create(coreTestsOnly: boolean, projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform): void;
	}
	/** Use this class to create a test through mocha.it */
	export class TestBuilderIt implements TestBuilder {
	    /** The name of the test */
	    private testName;
	    /** The test to be run */
	    private test;
	    /** Whether or not the test should be run when "--core" is supplied */
	    private isCoreTest;
	    /**
	     * testName - used as the expectation in the call to it
	     * test - the test to provide to it
	     * isCoreTest - whether or not the test should run when "--core" is supplied
	     */
	    constructor(testName: string, test: (projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform, done: MochaDone) => void, isCoreTest: boolean);
	    /**
	     * Called to create the test suite by the initializeTests function
	     *
	     * coreTestsOnly - Whether or not only core tests are to be run
	     * projectManager - The projectManager instance that these tests are being run with
	     * targetPlatform - The platform that these tests are going to be run on
	     */
	    create(coreTestsOnly: boolean, projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform): void;
	}
	/**
	 * Returns a default empty response to give to the app in a checkForUpdate request
	 */
	export function createDefaultResponse(): su.CheckForUpdateResponseMock;
	/**
	 * Returns a default update response to give to the app in a checkForUpdate request
	 */
	export function createMockResponse(mandatory?: boolean): su.CheckForUpdateResponseMock;
	/**
	 * Returns a default update response with a download URL and random package hash.
	 */
	export function getMockResponse(targetPlatform: platform.IPlatform, mandatory?: boolean, randomHash?: boolean): su.CheckForUpdateResponseMock;
	/**
	 * Wrapper for ProjectManager.setupScenario
	 */
	export function setupScenario(projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform, scenarioJsPath: string, version?: string): Q.Promise<string>;
	/**
	 * Creates an update and zip for the test app using the specified scenario and version
	 */
	export function createUpdate(projectManager: tm.ProjectManager, targetPlatform: platform.IPlatform, scenarioJsPath: string, version: string): Q.Promise<string>;
	/**
	 * Waits for the next set of test messages sent by the app and asserts that they are equal to the expected messages
	 */
	export function verifyMessages(expectedMessages: (string | su.AppMessage)[], deferred: Q.Deferred<void>): (requestBody: any) => void;
	/**
	 * Call this function with a ProjectManager and an array of TestBuilderDescribe objects to run tests
	 */
	export function initializeTests(projectManager: tm.ProjectManager, tests: TestBuilderDescribe[]): void;

}
declare module 'code-push-plugin-testing-framework/script/index' {
	import * as Platform from 'code-push-plugin-testing-framework/script/platform';
	import * as PluginTestingFramework from 'code-push-plugin-testing-framework/script/test';
	import { ProjectManager } from 'code-push-plugin-testing-framework/script/projectManager';
	import * as ServerUtil from 'code-push-plugin-testing-framework/script/serverUtil';
	import * as TestUtil from 'code-push-plugin-testing-framework/script/testUtil';
	export { Platform, PluginTestingFramework, ProjectManager, ServerUtil, TestUtil };

}
declare module 'code-push-plugin-testing-framework' {
	import main = require('code-push-plugin-testing-framework/script/index');
	export = main;
}
