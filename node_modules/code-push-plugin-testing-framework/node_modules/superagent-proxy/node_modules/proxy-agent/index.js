
/**
 * Module dependencies.
 */

var url = require('url');
var LRU = require('lru-cache');
var extend = require('extend');
var Agent = require('agent-base');
var inherits = require('util').inherits;
var debug = require('debug')('proxy-agent');

var PacProxyAgent = require('pac-proxy-agent');
var HttpProxyAgent = require('http-proxy-agent');
var HttpsProxyAgent = require('https-proxy-agent');
var SocksProxyAgent = require('socks-proxy-agent');

/**
 * Module exports.
 */

exports = module.exports = ProxyAgent;

/**
 * Number of `http.Agent` instances to cache.
 *
 * This value was arbitrarily chosen... a better
 * value could be conceived with some benchmarks.
 */

var cacheSize = 20;

/**
 * Cache for `http.Agent` instances.
 */

exports.cache = new LRU(cacheSize);

/**
 * Built-in proxy types.
 */

exports.proxies = Object.create(null);
exports.proxies.http = httpOrHttpsProxy;
exports.proxies.https = httpOrHttpsProxy;
exports.proxies.socks = SocksProxyAgent;
exports.proxies.socks4 = SocksProxyAgent;
exports.proxies.socks4a = SocksProxyAgent;
exports.proxies.socks5 = SocksProxyAgent;
exports.proxies.socks5h = SocksProxyAgent;

PacProxyAgent.protocols.forEach(function (protocol) {
  exports.proxies['pac+' + protocol] = PacProxyAgent;
});

function httpOrHttpsProxy (opts, secureEndpoint) {
  if (secureEndpoint) {
    // HTTPS
    return new HttpsProxyAgent(opts);
  } else {
    // HTTP
    return new HttpProxyAgent(opts);
  }
}

/**
 * Attempts to get an `http.Agent` instance based off of the given proxy URI
 * information, and the `secure` flag.
 *
 * An LRU cache is used, to prevent unnecessary creation of proxy
 * `http.Agent` instances.
 *
 * @param {String} uri proxy url
 * @param {Boolean} secure true if this is for an HTTPS request, false for HTTP
 * @return {http.Agent}
 * @api public
 */

function ProxyAgent (opts) {
  if (!(this instanceof ProxyAgent)) return new ProxyAgent(opts);
  if ('string' == typeof opts) opts = url.parse(opts);
  if (!opts) throw new TypeError('an HTTP(S) proxy server `host` and `protocol` must be specified!');
  debug('creating new ProxyAgent instance: %o', opts);
  Agent.call(this, connect);

  var proxies;
  if (opts.proxies) {
    proxies = extend(Object.create(exports.proxies), opts.proxies);
  } else {
    proxies = exports.proxies;
  }

  // get the requested proxy "protocol"
  var protocol = opts.protocol;
  if (!protocol) {
    throw new TypeError('You must specify a string "protocol" for the ' +
                        'proxy type (' + types().join(', ') + ')');
  }

  // strip the trailing ":" if present
  if (':' == protocol[protocol.length - 1]) {
    protocol = protocol.substring(0, protocol.length - 1);
  }

  // get the proxy `http.Agent` creation function
  var proxyFn = proxies[protocol];
  if ('function' != typeof proxyFn) {
    throw new TypeError('unsupported proxy protocol: "' + protocol + '"');
  }

  this.proxy = opts;
  // format the proxy info back into a URI, since an opts object
  // could have been passed in originally. This generated URI is used
  // as part of the "key" for the LRU cache
  this.proxyUri = url.format({
    protocol: protocol + ':',
    slashes: true,
    hostname: opts.hostname || opts.host,
    port: opts.port
  });
  this.proxyFn = proxyFn;
}
inherits(ProxyAgent, Agent);

/**
 *
 */

function connect (req, opts, fn) {
  // create the "key" for the LRU cache
  var key = this.proxyUri;
  if (opts.secureEndpoint) key += ' secure';

  // attempt to get a cached `http.Agent` instance first
  var agent = exports.cache.get(key);
  if (!agent) {
    // get an `http.Agent` instance from protocol-specific agent function
    agent = this.proxyFn(this.proxy, opts.secureEndpoint);
    if (agent) exports.cache.set(key, agent);
  } else {
    debug('cache hit with key: %o', key);
  }

  // XXX: agent.callback() is an agent-base-ism
  // TODO: add support for generic `http.Agent` instances by calling
  // agent.addRequest(), but with support for <= 0.10.x and >= 0.12.x
  agent.callback(req, opts, fn);
}

/**
 * Returns an Array of supported protocol string names.
 *
 * @return {Array}
 * @api private
 */

function types () {
  var rtn = [];
  // not using Object.keys() so that we get any
  // potential prototype values as well
  for (var type in exports.proxies) rtn.push(type);
  return rtn;
}
