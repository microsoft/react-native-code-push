/**
 * @providesModule HybridMobileDeploy
 * @flow
 */

'use strict';

var NativeHybridMobileDeploy = require('NativeModules').HybridMobileDeploy;
var semver = require('semver');
var serverUrl;
var appName;

var HybridMobileDeploy = {
  checkForUpdate: function(version, callback) {
    var url = serverUrl + 'latest/' + appName;
    fetch(url)
      .then(response => response.json())
      .done(latest => {
        if (semver.gt(latest.version, version)) {
          callback(undefined, latest);
        } else {
          callback(undefined, false);
        }
      }, err => {
        callback(err);
      });
  },
  pollForUpdate: function(version, intervalDelay, callback) {
    var interval;
    var checkUpdate = () => {
      this.checkForUpdate(version, (err, update) => {
        if (err) {
          callback(err);
        } else if (update) {
          interval && clearInterval(interval);
          callback(undefined, update);
        }
      });
    };
    interval = setInterval(checkUpdate, intervalDelay);
    checkUpdate();
  },
  getAvailableUpdates: function(callback) {
    var url = serverUrl + 'updates/' + appName;
    console.log(url);
    fetch(url)
      .then((response) => {
        console.log(response);
        return response.json()
      })
      .then((value) => {
        console.log(value);
        return value;
      })
      .then(callback);
  },
  installUpdate: function(update) {
    NativeHybridMobileDeploy.installUpdateFromUrl(update.updateUrl, update.bundleName, (err) => console.log(err), () => console.log("success"));
  }
};

module.exports = function(server, app) {
  serverUrl = server;
  appName = app;
  return HybridMobileDeploy; 
};
