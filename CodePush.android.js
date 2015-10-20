/**
 * @providesModule CodePush-android
 * @flow
 */

'use strict';

var extend = require("extend");
var NativeCodePush = require('react-native').NativeModules.CodePush;
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var requestFetchAdapter = require("./request-fetch-adapter.js");

var getConfiguration = (() => {
  var config;
  return () => {
    if(config){
      return Promise.resolve(config);
    } else {
      return new Promise((resolve, reject) => {   
        NativeCodePush.getConfiguration(function(configuration){
          config = configuration;
          resolve(config);
        });
      });
    }
  }
})();

var getSdk = (() => {
  var sdk;
  return () => {
    if (sdk) {
      Promise.resolve(sdk);
    } else {
      return getConfiguration()
        .then((configuration) => {
          sdk = new Sdk(requestFetchAdapter, configuration);
          return sdk;
        });
    }
  }
})();

function checkForUpdate() {
  var config;
  var sdk;
  return getConfiguration()
    .then((configResult) => {
      config = configResult;
      return getSdk();
    })
    .then((sdkResult) => {
      sdk = sdkResult;
      return getCurrentPackage();
    })
    .then((localPackage) => {
      var queryPackage = {appVersion: config.appVersion};
      if (localPackage && localPackage.appVersion === config.appVersion) {
        queryPackage = localPackage;
      }
      
      return new Promise((resolve, reject) => {
        sdk.queryUpdateWithCurrentPackage(queryPackage, (err, update) => {
          if (err) return reject(err);
          if (update) {
            resolve(extend({}, update, {
              download: function () {
                return new Promise((resolve, reject) => {
                  NativeCodePush.downloadUpdate(update,
                    () => {
                      resolve(extend({}, {
                        apply: () => {
                          NativeCodePush.applyUpdate();
                        }
                      }));
                    },
                    (error) => {
                      reject(error);
                    }
                  );
                });
              }
            }));
          } else {
            resolve(update);
          }
        });
      });
    });
}  

function getCurrentPackage() {
  return new Promise((resolve, reject) => {   
    NativeCodePush.getCurrentPackage(
      (localPackage) => {
        resolve(localPackage);
      },
      (error) => {
        reject(error);
      }
    );
  });
}

var CodePush = {
  getConfiguration: getConfiguration,
  checkForUpdate: checkForUpdate,
  getCurrentPackage: getCurrentPackage
};

module.exports = CodePush;