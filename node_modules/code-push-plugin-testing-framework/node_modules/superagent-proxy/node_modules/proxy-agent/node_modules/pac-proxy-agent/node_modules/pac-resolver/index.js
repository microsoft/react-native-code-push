
/**
 * Module dependencies.
 */

var co = require('co');
var vm = require('vm');
var thunkify = require('thunkify');
var degenerator = require('degenerator');
var regenerator = require('regenerator');

/**
 * Built-in PAC functions.
 */

var dateRange = require('./dateRange');
var dnsDomainIs = require('./dnsDomainIs');
var dnsDomainLevels = require('./dnsDomainLevels');
var dnsResolve = require('./dnsResolve');
var isInNet = require('./isInNet');
var isPlainHostName = require('./isPlainHostName');
var isResolvable = require('./isResolvable');
var localHostOrDomainIs = require('./localHostOrDomainIs');
var myIpAddress = require('./myIpAddress');
var shExpMatch = require('./shExpMatch');
var timeRange = require('./timeRange');
var weekdayRange = require('./weekdayRange');

/**
 * Module exports.
 */

module.exports = generate;

// cache the `facebook/regenerator` wrap function for the Generator object.
var rg = vm.runInNewContext(regenerator.compile('', { includeRuntime: true }).code + ';regeneratorRuntime');

/**
 * Returns an asyncronous `FindProxyForURL` function from the
 * given JS string (from a PAC file).
 *
 * @param {String} str JS string
 * @param {Object} opts optional "options" object
 * @return {Function} async resolver function
 */

function generate (str, opts) {
  var i;

  // the sandbox to use for the vm
  var sandbox = {
    dateRange: dateRange,
    dnsDomainIs: dnsDomainIs,
    dnsDomainLevels: dnsDomainLevels,
    dnsResolve: dnsResolve,
    isInNet: isInNet,
    isPlainHostName: isPlainHostName,
    isResolvable: isResolvable,
    localHostOrDomainIs: localHostOrDomainIs,
    myIpAddress: myIpAddress,
    shExpMatch: shExpMatch,
    timeRange: timeRange,
    weekdayRange: weekdayRange
  };

  // copy the properties from the user-provided `sandbox` onto ours
  if (opts && opts.sandbox) {
    for (i in opts.sandbox) {
      sandbox[i] = opts.sandbox[i];
    }
  }

  // construct the array of async function names to add `yield` calls to.
  // user-provided async functions added to the `sandbox` must have an
  // `async = true` property set on the function instance
  var names = [];
  for (i in sandbox) {
    if (sandbox[i].async) {
      names.push(i);
      sandbox[i] = thunkify(sandbox[i]);
    }
  }
  //console.log(names);

  // for `facebook/regenerator`
  sandbox.regeneratorRuntime = rg;

  // convert the JS FindProxyForURL function into a generator function
  var js = degenerator(str, names);

  // use `facebook/regenerator` for node < v0.11 support
  // TODO: don't use regenerator if native generators are supported...
  js = regenerator.compile(js, { includeRuntime: false }).code;

  // filename of the pac file for the vm
  var filename = opts && opts.filename ? opts.filename : 'proxy.pac';

  // evaluate the JS string and extract the FindProxyForURL generator function
  var fn = vm.runInNewContext(js + ';FindProxyForURL', sandbox, filename);
  if ('function' != typeof fn) {
    throw new TypeError('PAC file JavaScript contents must define a `FindProxyForURL` function');
  }

  // return the async resolver function
  var resolver = co(fn);

  return function FindProxyForURL (url, host, fn) {
    resolver(url, host, fn);
  };
}
