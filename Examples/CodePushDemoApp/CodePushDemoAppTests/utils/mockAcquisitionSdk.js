var assert = require("assert");

function createMockAcquisitionSdk(serverPackage, localPackage, expectedDeploymentKey) {
  var AcquisitionManager = function (httpRequester, configuration) {
    expectedDeploymentKey && assert.equal(expectedDeploymentKey, configuration.deploymentKey, "checkForUpdate did not initialize Acquisition SDK with the expected deployment key");
  }
  AcquisitionManager.prototype.queryUpdateWithCurrentPackage = function (queryPackage, callback) {
    if (localPackage) {
      localPackage.appVersion = queryPackage.appVersion;
      assert.deepEqual(queryPackage, localPackage, "checkForUpdate did not attach current package info to the acquisition request");
    }
    callback(/*err:*/ null, serverPackage);
  };
  return AcquisitionManager;
}

module.exports = createMockAcquisitionSdk;


