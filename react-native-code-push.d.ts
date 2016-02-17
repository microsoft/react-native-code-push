// Type definitions for React Native CodePush pligin.
// Project: https://github.com/Microsoft/react-native-code-push

/// <reference path="../react-native/react-native.d.ts" />

import ReactNativePromise = __React.Promise;

type DowloadProgressCallback = (progress: DownloadProgress) => void;
type SyncStatusChangedCallback = (status: CodePush.SyncStatus) => void;

interface DownloadProgress {
    /**
     * The total number of bytes expected to be received for this update.
     */
    totalBytes: number;
    
    /**
     * The number of bytes downloaded thus far.
     */
    receivedBytes: number;
}

interface LocalPackage extends Package {
    /**
     * Installs the update by saving it to the location on disk where the runtime expects to find the latest version of the app.
     * 
     * @param installMode Indicates when you would like the update changes to take affect for the end-user.
     */
    install(installMode: CodePush.InstallMode): ReactNativePromise<void>;
}
    
interface Package {
    /**
     * The app binary version that this update is dependent on. This is the value that was
     * specified via the appStoreVersion parameter when calling the CLI's release command.
     */
    appVersion: string;
    
    /**
     * The deployment key that was used to originally download this update.
     */
    deploymentKey: string;
    
    /**
     * The description of the update. This is the same value that you specified in the CLI when you released the update.
     */
    description: string;
    
    /**
     * Indicates whether this update has been previously installed but was rolled back.
     */
    failedInstall: boolean;
    
    /**
     * Indicates whether this is the first time the update has been run after being installed.
     */
    isFirstRun: boolean;
    
    /**
     * Indicates whether the update is considered mandatory. This is the value that was specified in the CLI when the update was released.
     */
    isMandatory: boolean;
    
    /**
     * Indicates whether this update is in a "pending" state. When true, that means the update has been downloaded and installed, but the app restart
     * needed to apply it hasn't occurred yet, and therefore, its changes aren't currently visible to the end-user. 
     */
    isPending: boolean;
    
    /**
     * The internal label automatically given to the update by the CodePush server. This value uniquely identifies the update within its deployment.
     */
    label: string;
    
    /**
     * The SHA hash value of the update.
     */
    packageHash: string;
    
    /**
     * The size of the code contained within the update, in bytes.
     */
    packageSize: number;
}
    
interface RemotePackage extends Package {
    /**
     * Downloads the available update from the CodePush service. 
     * 
     * @param downloadProgressCallback An optional callback that allows tracking the progress of the update while it is being downloaded.
     */
    download(downloadProgressCallback?: DowloadProgressCallback): ReactNativePromise<LocalPackage>;
    
    /**
     * The URL at which the package is available for download.
     */
    downloadUrl: string;
}
    
interface SyncOptions {
    /**
     * Specifies the deployment key you want to query for an update against. By default, this value is derived from the Info.plist
     * file (iOS) and MainActivity.java file (Android), but this option allows you to override it from the script-side if you need to
     * dynamically use a different deployment for a specific call to sync.
     */
    deploymentKey?: string;
    
    /**
     * Indicates when you would like to "install" the update after downloading it, which includes reloading the JS bundle in order for
     * any changes to take affect. Defaults to codePush.InstallMode.ON_NEXT_RESTART.
     */
    installMode?: CodePush.InstallMode;
    
    /**
     * An "options" object used to determine whether a confirmation dialog should be displayed to the end user when an update is available,
     * and if so, what strings to use. Defaults to null, which has the effect of disabling the dialog completely. Setting this to any truthy
     * value will enable the dialog with the default strings, and passing an object to this parameter allows enabling the dialog as well as
     * overriding one or more of the default strings.
     */
    updateDialog?: UpdateDialog;
}
    
interface UpdateDialog {
    /**
     * Indicates whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user. Defaults to false.
     */
    appendReleaseDescription?: boolean;
    
    /**
     * Indicates the string you would like to prefix the release description with, if any, when
     * displaying the update notification to the end user. Defaults to " Description: "
     */
    descriptionPrefix?: string;
    
    /**
     * The text to use for the button the end user must press in order to install a mandatory update. Defaults to "Continue".
     */
    mandatoryContinueButtonLabel?: string;
    
    /**
     * The text used as the body of an update notification, when the update is specified as mandatory.
     * Defaults to "An update is available that must be installed.".
     */
    mandatoryUpdateMessage?: string;
    
    /**
     * The text to use for the button the end user can press in order to ignore an optional update that is available. Defaults to "Ignore".
     */
    optionalIgnoreButtonLabel?: string;
    
    /**
     * The text to use for the button the end user can press in order to install an optional update. Defaults to "Install".
     */
    optionalInstallButtonLabel?: string;
    
    /**
     * The text used as the body of an update notification, when the update is optional. Defaults to "An update is available. Would you like to install it?".
     */
    optionalUpdateMessage?: string;
    
    /**
     * The text used as the header of an update notification that is displayed to the end user. Defaults to "Update available".
     */
    title?: string;
}
      
declare namespace CodePush { 
    /**
     * Represents the default settings that will be used by the sync method if
     * an update dialog is configured to be displayed.
     */
    var DEFAULT_UPDATE_DIALOG: UpdateDialog;
    
    /**
     * Asks the CodePush service whether the configured app deployment has an update available.
     * 
     * @param deploymentKey The deployment key to use to query the CodePush server for an update.
     */
    function checkForUpdate(deploymentKey?: string): ReactNativePromise<RemotePackage>;  
    
    /**
     * Retrieves the metadata about the currently installed update (e.g. description, installation time, size).
     */
    function getCurrentPackage(): ReactNativePromise<LocalPackage>;
    
    /**
     * Notifies the CodePush runtime that an installed update is considered successful.
     */
    function notifyApplicationReady(): ReactNativePromise<void>;
    
    /**
     * Immediately restarts the app.
     * 
     * @param onlyIfUpdateIsPending Indicates whether you want the restart to no-op if there isn't currently a pending update.
     */
    function restartApp(onlyIfUpdateIsPending?: boolean): void;
    
    /**
     * Allows checking for an update, downloading it and installing it, all with a single call.
     * 
     * @param options Options used to configure the end-user update experience (e.g. show an prompt?, install the update immediately?).
     * @param syncStatusChangedCallback An optional callback that allows tracking the status of the sync operation, as opposed to simply checking the resolved state via the returned Promise.
     * @param downloadProgressCallback An optional callback that allows tracking the progress of an update while it is being downloaded.
     */
    function sync(options?: SyncOptions, syncStatusChangedCallback?: SyncStatusChangedCallback, downloadProgressCallback?: DowloadProgressCallback): __React.Promise<SyncStatus>;

    /**
     * Indicates when you would like an installed update to actually be applied.
     */
    enum InstallMode {
        /**
         * Indicates that you want to install the update and restart the app immediately. 
         */
        IMMEDIATE,
        
        /**
         * Indicates that you want to install the update, but not forcibly restart the app. 
         */
        ON_NEXT_RESTART,
        
        /**
         * Indicates that you want to install the update, but don't want to restart the
         * app until the next time the end user resumes it from the background.
         */
        ON_NEXT_RESUME
    }
    
    enum SyncStatus {
        /**
         * The CodePush server is being queried for an update.
         */
        CHECKING_FOR_UPDATE,
        
        /**
         * An update is available, and a confirmation dialog was shown
         * to the end user. (This is only applicable when the updateDialog is used)
         */
        AWAITING_USER_ACTION,
        
        /**
         * An available update is being downloaded from the CodePush server.
         */
        DOWNLOADING_PACKAGE,
        
        /**
         * An available update was downloaded and is about to be installed.
         */
        INSTALLING_UPDATE,
        
        /** 
         * The app is up-to-date with the CodePush server.
         */
        UP_TO_DATE,
        
        /**
         * The app had an optional update which the end user chose to ignore.
         * (This is only applicable when the updateDialog is used)
         */
        UPDATE_IGNORED,
        
        /**
         * An available update has been installed and will be run either immediately after the
         * syncStatusChangedCallback function returns or the next time the app resumes/restarts,
         * depending on the InstallMode specified in SyncOptions
         */
        UPDATE_INSTALLED,
        
        /**
         * There is an ongoing sync operation running which prevents the current call from being executed.
         */
        SYNC_IN_PROGRESS,
        
        /**
         * The sync operation encountered an unknown error.
         */
        UNKNOWN_ERROR
    }
}

declare module "react-native-code-push" {
    export default CodePush;
}