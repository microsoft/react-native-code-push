import CodePush from "react-native-code-push";

// This module wraps CodePush API calls to add test message callbacks to every function for simpler test code.

module.exports = {
    checkForUpdate: function(testApp, onSuccess, onError, deploymentKey) {
        return CodePush.checkForUpdate(deploymentKey)
            .then((remotePackage) => {
                testApp.checkUpdateSuccess(remotePackage);
                return onSuccess && onSuccess(remotePackage);
            }, (error) => {
                testApp.checkUpdateError(error);
                return onError && onError(error);
            });
    },
    
    download: function(testApp, onSuccess, onError, remotePackage) {
        return remotePackage.download()
            .then((localPackage) => {
                testApp.downloadSuccess(localPackage);
                return onSuccess && onSuccess(localPackage);
            }, (error) => {
                testApp.downloadError(error);
                return onError && onError(error);
            });
    },
    
    install: function(testApp, onSuccess, onError, installMode, minBackgroundDuration, localPackage) {
        return localPackage.install(installMode, minBackgroundDuration)
            .then(() => {
                testApp.installSuccess();
                return onSuccess && onSuccess();
            }, () => {
                testApp.installError();
                return onError && onError();
            });
    },
    
    checkAndInstall: function(testApp, onSuccess, onError, installMode, minBackgroundDuration) {
        return this.checkForUpdate(testApp,
            this.download.bind(undefined, testApp,
                this.install.bind(undefined, testApp, undefined, undefined, installMode, minBackgroundDuration),
                undefined));
    },
    
    sync: function(testApp, onSyncStatus, onSyncError, options) {
        return CodePush.sync(options)
            .then((status) => {
                testApp.onSyncStatus(status);
                return onSyncStatus(status);
            }, (error) => {
                testApp.onSyncError(error);
                return onSyncError(error);
            });
    }
}