var CodePushWrapper = require("../codePushWrapper.js");
import CodePush from "react-native-code-push";

module.exports = {
    startTest: function (testApp) {
        CodePushWrapper.sync(testApp, undefined, undefined,
            {
                installMode: CodePush.InstallMode.IMMEDIATE,
                mandatoryInstallMode: CodePush.InstallMode.ON_NEXT_RESTART
            });
    },

    getScenarioName: function () {
        return "Sync Mandatory Restart";
    }
};