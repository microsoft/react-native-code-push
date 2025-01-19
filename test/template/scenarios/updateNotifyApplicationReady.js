var CodePushWrapper = require("../codePushWrapper.js");
import CodePush from "@code-push-next/react-native-code-push";

module.exports = {
    startTest: function (testApp) {
        testApp.readyAfterUpdate();
        CodePush.notifyAppReady();
    },

    getScenarioName: function () {
        return "Good Update";
    }
};