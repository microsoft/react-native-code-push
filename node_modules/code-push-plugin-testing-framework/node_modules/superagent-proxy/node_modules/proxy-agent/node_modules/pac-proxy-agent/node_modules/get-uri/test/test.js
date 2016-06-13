
/**
 * Module dependencies.
 */

var fs = require('fs');
var st = require('st');
var ftpd = require('ftpd');
var http = require('http');
var https = require('https');
var getUri = require('../');
var assert = require('assert');
var streamToArray = require('stream-to-array');

describe('get-uri', function () {

  describe('bad input', function () {
    it('should throw a TypeError when callback function is given', function () {
      assert.throws(function () {
        getUri();
      }, TypeError);
    });

    it('should return a TypeError when no URI is given', function (done) {
      getUri(null, function (err) {
        assert.equal(err.name, 'TypeError');
        done();
      });
    });
  });

});
