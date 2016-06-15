// Type definitions for Apache Cordova CodePush plugin.
// Project: https://github.com/Microsoft/cordova-plugin-code-push
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
// Licensed under the MIT license.

declare module Http {
    export const enum Verb {
        GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
    }

    export interface Response {
        statusCode: number;
        body?: string;
    }

    export interface Requester {
        request(verb: Verb, url: string, callback: Callback<Response>): void;
        request(verb: Verb, url: string, requestBody: string, callback: Callback<Response>): void;
    }
}

interface Window {
    codePush: CodePushCordovaPlugin;
}

/**
 * Defines a package. All fields are non-nullable, except when retrieving the currently running package on the first run of the app,
 * in which case only the appVersion is compulsory.
 *
 * !! THIS TYPE IS READ FROM NATIVE CODE AS WELL. ANY CHANGES TO THIS INTERFACE NEEDS TO BE UPDATED IN NATIVE CODE !!
 */
interface IPackage {
    deploymentKey: string;
    description: string;
    label: string;
    appVersion: string;
    isMandatory: boolean;
    packageHash: string;
    packageSize: number;
    failedInstall: boolean;
}

/**
 * Defines a remote package, which represents an update package available for download.
 */
interface IRemotePackage extends IPackage {
    /**
     * The URL at which the package is available for download.
     */
    downloadUrl: string;

    /**
     * Downloads the package update from the CodePush service.
     *
     * @param downloadSuccess Called with one parameter, the downloaded package information, once the download completed successfully.
     * @param downloadError Optional callback invoked in case of an error.
     * @param downloadProgress Optional callback invoked during the download process. It is called several times with one DownloadProgress parameter.
     */
    download(downloadSuccess: SuccessCallback<ILocalPackage>, downloadError?: ErrorCallback, downloadProgress?: SuccessCallback<DownloadProgress>): void;

    /**
     * Aborts the current download session, previously started with download().
     *
     * @param abortSuccess Optional callback invoked if the abort operation succeeded.
     * @param abortError Optional callback invoked in case of an error.
     */
    abortDownload(abortSuccess?: SuccessCallback<void>, abortError?: ErrorCallback): void;
}

/**
 * Defines a local package.
 *
 * !! THIS TYPE IS READ FROM NATIVE CODE AS WELL. ANY CHANGES TO THIS INTERFACE NEEDS TO BE UPDATED IN NATIVE CODE !!
 */
interface ILocalPackage extends IPackage {
    /**
     * The local storage path where this package is located.
     */
    localPath: string;

    /**
     * Indicates if the current application run is the first one after the package was applied.
     */
    isFirstRun: boolean;

    /**
     * Applies this package to the application. The application will be reloaded with this package and on every application launch this package will be loaded.
     * On the first run after the update, the application will wait for a codePush.notifyApplicationReady() call. Once this call is made, the install operation is considered a success.
     * Otherwise, the install operation will be marked as failed, and the application is reverted to its previous version on the next run.
     *
     * @param installSuccess Callback invoked if the install operation succeeded.
     * @param installError Optional callback inovoked in case of an error.
     * @param installOptions Optional parameter used for customizing the installation behavior.
     */
    install(installSuccess: SuccessCallback<void>, errorCallback?: ErrorCallback, installOptions?: InstallOptions): void;
}

/**
 * Decomposed static side of RemotePackage.
 * For Class Decomposition guidelines see http://www.typescriptlang.org/Handbook#writing-dts-files-guidelines-and-specifics
 */
interface RemotePackage_Static {
    new (): IRemotePackage;
}

/**
 * Decomposed static side of LocalPackage.
 * For Class Decomposition guidelines see http://www.typescriptlang.org/Handbook#writing-dts-files-guidelines-and-specifics
 */
interface LocalPackage_Static {
    new (): ILocalPackage;
}

declare var RemotePackage: RemotePackage_Static;
declare var LocalPackage: LocalPackage_Static;

/**
 * Defines the JSON format of the current package information file.
 * This file is stored in the local storage of the device and persists between store updates and code-push updates.
 *
 * !! THIS FILE IS READ FROM NATIVE CODE AS WELL. ANY CHANGES TO THIS INTERFACE NEEDS TO BE UPDATED IN NATIVE CODE !!
 */
interface IPackageInfoMetadata extends ILocalPackage {
    nativeBuildTime: string;
}

interface NativeUpdateNotification {
    updateAppVersion: boolean;   // Always true
    appVersion: string;
}

interface Callback<T> { (error: Error, parameter: T): void; }
interface SuccessCallback<T> { (result?: T): void; }
interface ErrorCallback { (error?: Error): void; }

interface Configuration {
    appVersion: string;
    clientUniqueId: string;
    deploymentKey: string;
    serverUrl: string;
    ignoreAppVersion?: boolean
}

declare class AcquisitionStatus {
    static DeploymentSucceeded: string;
    static DeploymentFailed: string;
}

declare class AcquisitionManager {
    constructor(httpRequester: Http.Requester, configuration: Configuration);
    public queryUpdateWithCurrentPackage(currentPackage: IPackage, callback?: Callback<IRemotePackage | NativeUpdateNotification>): void;
    public reportStatusDeploy(pkg?: IPackage, status?: string, previousLabelOrAppVersion?: string, previousDeploymentKey?: string, callback?: Callback<void>): void;
    public reportStatusDownload(pkg: IPackage, callback?: Callback<void>): void;
}

interface CodePushCordovaPlugin {

    /**
     * Get the current package information.
     *
     * @param packageSuccess Callback invoked with the currently deployed package information.
     * @param packageError Optional callback invoked in case of an error.
     */
    getCurrentPackage(packageSuccess: SuccessCallback<ILocalPackage>, packageError?: ErrorCallback): void;

    /**
     * Gets the pending package information, if any. A pending package is one that has been installed but the application still runs the old code.
     * This happends only after a package has been installed using ON_NEXT_RESTART or ON_NEXT_RESUME mode, but the application was not restarted/resumed yet.
     */
    getPendingPackage(packageSuccess: SuccessCallback<ILocalPackage>, packageError?: ErrorCallback): void;

    /**
     * Checks with the CodePush server if an update package is available for download.
     *
     * @param querySuccess Callback invoked in case of a successful response from the server.
     *                     The callback takes one RemotePackage parameter. A non-null package is a valid update.
     *                     A null package means the application is up to date for the current native application version.
     * @param queryError Optional callback invoked in case of an error.
     * @param deploymentKey Optional deployment key that overrides the config.xml setting.
     */
    checkForUpdate(querySuccess: SuccessCallback<IRemotePackage>, queryError?: ErrorCallback, deploymentKey?: string): void;

    /**
     * Notifies the plugin that the update operation succeeded and that the application is ready.
     * Calling this function is required on the first run after an update. On every subsequent application run, calling this function is a noop.
     * If using sync API, calling this function is not required since sync calls it internally.
     *
     * @param notifySucceeded Optional callback invoked if the plugin was successfully notified.
     * @param notifyFailed Optional callback invoked in case of an error during notifying the plugin.
     */
    notifyApplicationReady(notifySucceeded?: SuccessCallback<void>, notifyFailed?: ErrorCallback): void;

    /**
     * Reloads the application. If there is a pending update package installed using ON_NEXT_RESTART or ON_NEXT_RESUME modes, the update
     * will be immediately visible to the user. Otherwise, calling this function will simply reload the current version of the application.
     */
    restartApplication(installSuccess: SuccessCallback<void>, errorCallback?: ErrorCallback): void;

    /**
     * Convenience method for installing updates in one method call.
     * This method is provided for simplicity, and its behavior can be replicated by using window.codePush.checkForUpdate(), RemotePackage's download() and LocalPackage's install() methods.
     *
     * The algorithm of this method is the following:
     * - Checks for an update on the CodePush server.
     * - If an update is available
     *         - If the update is mandatory and the alertMessage is set in options, the user will be informed that the application will be updated to the latest version.
     *           The update package will then be downloaded and applied.
     *         - If the update is not mandatory and the confirmMessage is set in options, the user will be asked if they want to update to the latest version.
     *           If they decline, the syncCallback will be invoked with SyncStatus.UPDATE_IGNORED.
     *         - Otherwise, the update package will be downloaded and applied with no user interaction.
     * - If no update is available on the server, or if a previously rolled back update is available and the ignoreFailedUpdates is set to true, the syncCallback will be invoked with the SyncStatus.UP_TO_DATE.
     * - If an error occurs during checking for update, downloading or installing it, the syncCallback will be invoked with the SyncStatus.ERROR.
     *
     * @param syncCallback Optional callback to be called with the status of the sync operation.
     *                     The callback will be called only once, and the possible statuses are defined by the SyncStatus enum.
     * @param syncOptions Optional SyncOptions parameter configuring the behavior of the sync operation.
     * @param downloadProgress Optional callback invoked during the download process. It is called several times with one DownloadProgress parameter.
     *
     */
    sync(syncCallback?: SuccessCallback<SyncStatus>, syncOptions?: SyncOptions, downloadProgress?: SuccessCallback<DownloadProgress>): void;
}

/**
 * Defines the possible result statuses of the window.codePush.sync operation.
 */
declare enum SyncStatus {
    /**
     * The application is up to date.
     */
    UP_TO_DATE,

    /**
     * An update is available, it has been downloaded, unzipped and copied to the deployment folder.
     * After the completion of the callback invoked with SyncStatus.UPDATE_INSTALLED, the application will be reloaded with the updated code and resources.
     */
    UPDATE_INSTALLED,

    /**
     * An optional update is available, but the user declined to install it. The update was not downloaded.
     */
    UPDATE_IGNORED,

    /**
     * An error happened during the sync operation. This might be an error while communicating with the server, downloading or unziping the update.
     * The console logs should contain more information about what happened. No update has been applied in this case.
     */
    ERROR,

    /**
     * There is an ongoing sync in progress, so this attempt to sync has been aborted.
     */
    IN_PROGRESS,

    /**
     * Intermediate status - the plugin is about to check for updates.
     */
    CHECKING_FOR_UPDATE,

    /**
     * Intermediate status - a user dialog is about to be displayed. This status will be reported only if user interaction is enabled.
     */
    AWAITING_USER_ACTION,

    /**
     * Intermediate status - the update package is about to be downloaded.
     */
    DOWNLOADING_PACKAGE,

    /**
     * Intermediate status - the update package is about to be installed.
     */
    INSTALLING_UPDATE
}

/**
 * Defines the available install modes for updates.
 */
declare enum InstallMode {
    /**
     * The update will be applied to the running application immediately. The application will be reloaded with the new content immediately.
     */
    IMMEDIATE,

    /**
     * The update is downloaded but not installed immediately. The new content will be available the next time the application is started.
     */
    ON_NEXT_RESTART,

    /**
     * The udpate is downloaded but not installed immediately. The new content will be available the next time the application is resumed or restarted, whichever event happends first.
     */
    ON_NEXT_RESUME
}

/**
 * Defines the install operation options.
 */
interface InstallOptions {
    /**
     * Used to specify the InstallMode used for the install operation. This is optional and defaults to InstallMode.ON_NEXT_RESTART.
     */
    installMode?: InstallMode;

    /**
     * If installMode === ON_NEXT_RESUME, the minimum amount of time (in seconds) which needs to pass with the app in the background before an update install occurs when the app is resumed.
     */
    minimumBackgroundDuration?: number;
    
    /**
     * Used to specify the InstallMode used for the install operation if the update is mandatory. This is optional and defaults to InstallMode.IMMEDIATE.
     */
    mandatoryInstallMode?: InstallMode;
}

/**
 * Defines the sync operation options.
 */
interface SyncOptions extends InstallOptions {
    /**
     * Optional boolean flag. If set, previous updates which were rolled back will be ignored. Defaults to true.
     */
    ignoreFailedUpdates?: boolean;

    /**
     * Used to enable, disable or customize the user interaction during sync.
     * If set to false, user interaction will be disabled. If set to true, the user will be alerted or asked to confirm new updates, based on whether the update is mandatory.
     * To customize the user dialog, this option can be set to a custom UpdateDialogOptions instance.
     */
    updateDialog?: boolean | UpdateDialogOptions;

    /**
     * Overrides the config.xml deployment key when checking for updates.
     */
    deploymentKey?: string;
}

/**
 * Defines the configuration options for the alert or confirmation dialog
 */
interface UpdateDialogOptions {
    /**
     * If a mandatory update is available and this option is set, the message will be displayed to the user in an alert dialog before downloading and installing the update.
     * The user will not be able to cancel the operation, since the update is mandatory.
     */
    mandatoryUpdateMessage?: string;

    /**
     * If an optional update is available and this option is set, the message will be displayed to the user in a confirmation dialog.
     * If the user confirms the update, it will be downloaded and installed. Otherwise, the update update is not downloaded.
     */
    optionalUpdateMessage?: string;

    /**
     * The title of the dialog box used for interacting with the user in case of a mandatory or optional update.
     * This title will only be used if at least one of mandatoryUpdateMessage or optionalUpdateMessage options are set.
     */
    updateTitle?: string;

    /**
     * The label of the confirmation button in case of an optional update.
     */
    optionalInstallButtonLabel?: string;

    /**
     * The label of the cancel button in case of an optional update.
     */
    optionalIgnoreButtonLabel?: string;

    /**
     * The label of the continue button in case of a mandatory update.
     */
    mandatoryContinueButtonLabel?: string;

    /**
     * Flag indicating if the update description provided by the CodePush server should be displayed in the dialog box appended to the update message.
     */
    appendReleaseDescription?: boolean;

    /**
     * Optional prefix to add to the release description.
     */
    descriptionPrefix?: string;
}

/**
 * Defines the JSON format of the package diff manifest file.
 */
interface IDiffManifest {
    deletedFiles: string[];
}

/**
 * Defines the format of the DownloadProgress object, used to send periodical update notifications on the progress of the update download.
 */
interface DownloadProgress {
    totalBytes: number;
    receivedBytes: number;
}