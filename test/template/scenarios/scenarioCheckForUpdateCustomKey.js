var CodePushWrapper = require("../codePushWrapper.js");

module.exports = {
    startTest: function (testApp) {
        CodePushWrapper.checkForUpdate(testApp, undefined, undefined, "CUSTOM-DEPLOYMENT-KEY");
    },

    getScenarioName: function () {
        return "Check for Update Custom Key";
    }
};