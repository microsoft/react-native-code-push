import assert from "assert";

function createMockAcquisitionSdk(serverPackage, localPackage, expectedDeploymentKey) {
  let AcquisitionManager = (httpRequester, configuration) => {
    expectedDeploymentKey && assert.equal(expectedDeploymentKey, configuration.deploymentKey, "checkForUpdate did not initialize Acquisition SDK with the expected deployment key");
  };
  
  AcquisitionManager.prototype.queryUpdateWithCurrentPackage = (queryPackage, callback) => {
    if (localPackage) {
      localPackage.appVersion = queryPackage.appVersion;
      assert.deepEqual(queryPackage, localPackage, "checkForUpdate did not attach current package info to the acquisition request");
    }
    callback(/*err:*/ null, serverPackage);
  };
  
  AcquisitionManager.prototype.reportStatus = (status, message, callback) => {
    // No-op and return success.
    callback(null, null);
  };
  
  return AcquisitionManager;
}

export default createMockAcquisitionSdk;