var CodePushWrapper = require("../codePushWrapper.js");

module.exports = {
    startTest: function (testApp) {
        CodePushWrapper.checkForUpdate(testApp);
    },

    getScenarioName: function () {
        return "Check for Update";
    }
}; 