var CodePushWrapper = require("../codePushWrapper.js");

module.exports = {
    startTest: function (testApp) {
        testApp.readyAfterUpdate();
    },

    getScenarioName: function () {
        return "Bad Update";
    }
};