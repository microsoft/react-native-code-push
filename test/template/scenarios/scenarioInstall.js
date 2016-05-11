var CodePushWrapper = require("./codePushWrapper.js");

module.exports = {
    startTest: function(testApp) {
        CodePushWrapper.checkAndInstall(testApp);
    },
    
    getScenarioName: function() {
        return "Install";
    }
};