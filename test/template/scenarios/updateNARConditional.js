var CodePushWrapper = require("../codePushWrapper.js");
import CodePush from "react-native-code-push";

module.exports = {
    startTest: function (testApp) {
        testApp.readyAfterUpdate((responseBody) => {
            if (responseBody !== "SKIP_NOTIFY_APPLICATION_READY") {
                CodePush.notifyAppReady();
                CodePushWrapper.checkAndInstall(testApp, undefined, undefined, CodePush.InstallMode.ON_NEXT_RESTART);
            } else {
                testApp.setStateAndSendMessage("Skipping notifyApplicationReady!", "SKIPPED_NOTIFY_APPLICATION_READY");
            }
        });
    },

    getScenarioName: function () {
        return "Conditional Update";
    }
};