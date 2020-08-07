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
	     * The command line flag used to determine whether or not this platform should run.
	     * Runs when the flag is present, doesn't run otherwise.
	     */
	    getCommandLineFlagName(): string;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
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
	     * Returns the target emulator, which is specified through the command line.
	     */
	    getTargetEmulator(): Q.Promise<string>;
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<void>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<void>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<void>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<void>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<void>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<void>;
	}
	/**
	 * Android implementations of IPlatform.
	 */
	export class Android implements IPlatform {
	    private emulatorManager;
	    private serverUrl;
	    constructor(emulatorManager: IEmulatorManager);
	    /**
	     * Gets the platform name. (e.g. "android" for the Android platform).
	     */
	    getName(): string;
	    /**
	     * The command line flag used to determine whether or not this platform should run.
	     * Runs when the flag is present, doesn't run otherwise.
	     */
	    getCommandLineFlagName(): string;
	    private static DEFAULT_ANDROID_SERVER_URL;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
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
	 * IOS implementation of IPlatform.
	 */
	export class IOS implements IPlatform {
	    private emulatorManager;
	    private serverUrl;
	    constructor(emulatorManager: IEmulatorManager);
	    /**
	     * Gets the platform name. (e.g. "android" for the Android platform).
	     */
	    getName(): string;
	    /**
	     * The command line flag used to determine whether or not this platform should run.
	     * Runs when the flag is present, doesn't run otherwise.
	     */
	    getCommandLineFlagName(): string;
	    private static DEFAULT_IOS_SERVER_URL;
	    /**
	     * Gets the server url used for testing.
	     */
	    getServerUrl(): string;
	    /**
	     * Gets an IEmulatorManager that is used to control the emulator during the tests.
	     */
	    getEmulatorManager(): IEmulatorManager;
	    /**
	     * Gets the default deployment key.
	     */
	    getDefaultDeploymentKey(): string;
	}
	export class AndroidEmulatorManager implements IEmulatorManager {
	    private targetEmulator;
	    /**
	     * Returns the target emulator, which is specified through the command line.
	     */
	    getTargetEmulator(): Q.Promise<string>;
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<void>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<void>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<void>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<void>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<void>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<void>;
	}
	export class IOSEmulatorManager implements IEmulatorManager {
	    private targetEmulator;
	    /**
	     * Returns the target emulator, which is specified through the command line.
	     */
	    getTargetEmulator(): Q.Promise<string>;
	    /**
	     * Boots the target emulator.
	     */
	    bootEmulator(restartEmulators: boolean): Q.Promise<void>;
	    /**
	     * Launches an already installed application by app id.
	     */
	    launchInstalledApplication(appId: string): Q.Promise<void>;
	    /**
	     * Ends a running application given its app id.
	     */
	    endRunningApplication(appId: string): Q.Promise<void>;
	    /**
	     * Restarts an already installed application by app id.
	     */
	    restartApplication(appId: string): Q.Promise<void>;
	    /**
	     * Navigates away from the current app, waits for a delay (defaults to 1 second), then navigates to the specified app.
	     */
	    resumeApplication(appId: string, delayBeforeResumingMs?: number): Q.Promise<void>;
	    /**
	     * Prepares the emulator for a test.
	     */
	    prepareEmulatorForTest(appId: string): Q.Promise<void>;
	    /**
	     * Uninstalls the app from the emulator.
	     */
	    uninstallApplication(appId: string): Q.Promise<void>;
	}

}
declare module 'code-push-plugin-testing-framework/script/projectManager' {
	import Q = require("q");
	import platform = require('code-push-plugin-testing-framework/script/platform');
	/**
	 * In charge of project related operations.
	 */
	export class ProjectManager {
	    static DEFAULT_APP_VERSION: string;
	    private static NOT_IMPLEMENTED_ERROR_MSG;
	    /**
	     * Returns the name of the plugin being tested, for example Cordova or React-Native.
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
	    setupProject(projectDirectory: string, templatePath: string, appName: string, appNamespace: string, version?: string): Q.Promise<void>;
	    /**
	     * Sets up the scenario for a test in an already existing project.
	     *
	     * Overwrite this in your implementation!
	     */
	    setupScenario(projectDirectory: string, appId: string, templatePath: string, jsPath: string, targetPlatform: platform.IPlatform, version?: string): Q.Promise<void>;
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
	    preparePlatform(projectDirectory: string, targetPlatform: platform.IPlatform): Q.Promise<void>;
	    /**
	     * Cleans up a specific platform after tests.
	     *
	     * Overwrite this in your implementation!
	     */
	    cleanupAfterPlatform(projectDirectory: string, targetPlatform: platform.IPlatform): Q.Promise<void>;
	    /**
	     * Runs the test app on the given target / platform.
	     *
	     * Overwrite this in your implementation!
	     */
	    runApplication(projectDirectory: string, targetPlatform: platform.IPlatform): Q.Promise<void>;
	}
	/**
	 * Wrapper for ProjectManager.setupScenario in the TestRun directory.
	 */
	export function setupTestRunScenario(projectManager: ProjectManager, targetPlatform: platform.IPlatform, scenarioJsPath: string, version?: string): Q.Promise<void>;
	/**
	 * Creates an update and zip for the test app using the specified scenario and version.
	 */
	export function setupUpdateScenario(projectManager: ProjectManager, targetPlatform: platform.IPlatform, scenarioJsPath: string, version: string): Q.Promise<string>;

}
declare module 'code-push-plugin-testing-framework/script/test' {
	import Platform = require('code-push-plugin-testing-framework/script/platform');
	import { ProjectManager } from 'code-push-plugin-testing-framework/script/projectManager';
	/**
	 * Call this function to initialize the automated tests.
	 */
	export function initializeTests(projectManager: ProjectManager, supportedTargetPlatforms: Platform.IPlatform[], describeTests: (projectManager: ProjectManager, targetPlatform: Platform.IPlatform) => void): void;

}
declare module 'code-push-plugin-testing-framework/script/serverUtil' {
	import platform = require('code-push-plugin-testing-framework/script/platform');
	import Q = require("q");
	/** The server to respond to requests from the app. */
	export var server: any;
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
	/**
	 * Sets up the server that the test app uses to send test messages and check for and download updates.
	 */
	export function setupServer(targetPlatform: platform.IPlatform): void;
	/**
	 * Closes the server.
	 */
	export function cleanupServer(): void;
	/**
	 * Class used to mock the codePush.checkForUpdate() response from the server.
	 */
	export class CheckForUpdateResponseMock {
		download_url: string;
		is_available: boolean;
		should_run_binary_version: boolean;
		package_size: number;
		update_app_version: boolean;
		target_binary_range: string;
		is_disabled: boolean;
		description: string;
		label: string;
		package_hash: string;
		is_mandatory: boolean;
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
	 * Returns a default empty response to give to the app in a checkForUpdate request
	 */
	export function createDefaultResponse(): CheckForUpdateResponseMock;
	/**
	 * Returns a default update response to give to the app in a checkForUpdate request
	 */
	export function createUpdateResponse(mandatory?: boolean, targetPlatform?: platform.IPlatform, randomHash?: boolean): CheckForUpdateResponseMock;
	/**
	 * Returns a promise that waits for the next set of test messages sent by the app and resolves if that they are equal to the expected messages or rejects if they are not.
	 */
	export function expectTestMessages(expectedMessages: (string | AppMessage)[]): Q.Promise<void>;
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
declare module 'code-push-plugin-testing-framework/script/testBuilder' {
	import Platform = require('code-push-plugin-testing-framework/script/platform');
	import { ProjectManager } from 'code-push-plugin-testing-framework/script/projectManager';
	export class TestBuilder {
	    static describe: ITestBuilderContextDefintion;
	    static it: ITestBuilderTestDefinition;
	}
	/** Singleton class for TestBuilder.describe to use internally to define the context. */
	export class TestContext {
	    static projectManager: ProjectManager;
	    static targetPlatform: Platform.IPlatform;
	}
	export interface ITestBuilderContextDefintion {
	    (description: string, spec: () => void, scenarioPath?: string): void;
	    only(description: string, spec: () => void, scenarioPath?: string): void;
	    skip(description: string, spec: () => void, scenarioPath?: string): void;
	}
	export interface ITestBuilderTestDefinition {
	    (expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
	    only(expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
	    skip(expectation: string, isCoreTest: boolean, assertion: (done: Mocha.Done) => void): void;
	}

}
declare module 'code-push-plugin-testing-framework/script/testConfig' {
	export const TestAppName: string;
	export const TestNamespace: string;
	export const AcquisitionSDKPluginName: string;
	export const templatePath: string;
	export const thisPluginInstallString: string;
	export const testRunDirectory: string;
	export const updatesDirectory: string;
	export const onlyRunCoreTests: boolean;
	export const shouldSetup: boolean;
	export const restartEmulators: boolean;

}
declare module 'code-push-plugin-testing-framework/script/testUtil' {
	import Q = require("q");
	export class TestUtil {
	    static ANDROID_KEY_PLACEHOLDER: string;
	    static IOS_KEY_PLACEHOLDER: string;
	    static SERVER_URL_PLACEHOLDER: string;
	    static INDEX_JS_PLACEHOLDER: string;
	    static CODE_PUSH_APP_VERSION_PLACEHOLDER: string;
	    static CODE_PUSH_TEST_APP_NAME_PLACEHOLDER: string;
	    static CODE_PUSH_APP_ID_PLACEHOLDER: string;
	    static PLUGIN_VERSION_PLACEHOLDER: string;
	    /**
	     * Reads a command line option passed to mocha and returns a default if unspecified.
	     */
	    static readMochaCommandLineOption(optionName: string, defaultValue?: string): string;
	    /**
	     * Reads command line options passed to mocha.
	     */
	    static readMochaCommandLineFlag(optionName: string): boolean;
	    /**
	     * Executes a child process and returns a promise that resolves with its output or rejects with its error.
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
	        noLogCommand?: boolean;
	        noLogStdOut?: boolean;
	        noLogStdErr?: boolean;
	    }): Q.Promise<string>;
	    /**
	     * Returns the name of the plugin that is being tested.
	     */
	    static getPluginName(): string;
	    /**
	     * Replaces a regex in a file with a given string.
	     */
	    static replaceString(filePath: string, regex: string, replacement: string): void;
	    /**
	     * Copies a file from a given location to another.
	     */
	    static copyFile(source: string, destination: string, overwrite: boolean): Q.Promise<void>;
	    /**
	     * Archives the contents of sourceFolder and puts it in an archive at archivePath in targetFolder.
	     */
	    static archiveFolder(sourceFolder: string, targetFolder: string, archivePath: string, isDiff: boolean): Q.Promise<string>;
	}

}
declare module 'code-push-plugin-testing-framework/script/index' {
	import * as Platform from 'code-push-plugin-testing-framework/script/platform';
	import * as PluginTestingFramework from 'code-push-plugin-testing-framework/script/test';
	import { ProjectManager, setupTestRunScenario, setupUpdateScenario } from 'code-push-plugin-testing-framework/script/projectManager';
	import * as ServerUtil from 'code-push-plugin-testing-framework/script/serverUtil';
	import { TestBuilder } from 'code-push-plugin-testing-framework/script/testBuilder';
	import * as TestConfig from 'code-push-plugin-testing-framework/script/testConfig';
	import { TestUtil } from 'code-push-plugin-testing-framework/script/testUtil';
	export { Platform, PluginTestingFramework, ProjectManager, setupTestRunScenario, setupUpdateScenario, ServerUtil, TestBuilder, TestConfig, TestUtil };

}
declare module 'code-push-plugin-testing-framework' {
	import main = require('code-push-plugin-testing-framework/script/index');
	export = main;
}
