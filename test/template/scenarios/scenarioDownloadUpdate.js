var CodePushWrapper = require("../codePushWrapper.js");

module.exports = {
    startTest: function (testApp) {
        CodePushWrapper.checkForUpdate(testApp,
            CodePushWrapper.download.bind(undefined, testApp, undefined, undefined));
    },

    getScenarioName: function () {
        return "Download Update";
    }
};