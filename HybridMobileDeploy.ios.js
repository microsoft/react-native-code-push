/**
 * @providesModule HybridMobileDeploy
 * @flow
 */
'use strict';

var NativeHybridMobileDeploy = require('NativeModules').HybridMobileDeploy;
var invariant = require('invariant');

/**
 * High-level docs for the HybridMobileDeploy iOS API can be written here.
 */

var HybridMobileDeploy = {
  test: function() {
    NativeHybridMobileDeploy.test();
  }
};

module.exports = HybridMobileDeploy;
