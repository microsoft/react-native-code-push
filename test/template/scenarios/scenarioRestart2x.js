var CodePushWrapper = require("../codePushWrapper.js");
import CodePush from "react-native-code-push";

module.exports = {
    startTest: function (testApp) {
        CodePush.restartApp(true);
        CodePushWrapper.checkAndInstall(testApp,
            () => {
                CodePush.restartApp(true);
            }
        );
    },

    getScenarioName: function () {
        return "Restart2x";
    }
};