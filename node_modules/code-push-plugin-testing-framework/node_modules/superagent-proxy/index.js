
/**
 * Module dependencies.
 */

var proxyAgent = require('proxy-agent');
var debug = require('debug')('superagent-proxy');

/**
 * Module exports.
 */

module.exports = setup;

/**
 * Adds a `.proxy(uri)` function to the "superagent" module's Request class.
 *
 * ``` js
 * var request = require('superagent');
 * require('superagent-proxy')(request);
 *
 * request
 *   .get(uri)
 *   .proxy(uri)
 *   .end(fn);
 * ```
 *
 * Or, you can pass in a `superagent.Request` instance, and it's like calling the
 * `.proxy(uri)` function on it, but without extending the prototype:
 *
 * ``` js
 * var request = require('superagent');
 * var proxy = require('superagent-proxy');
 *
 * proxy(request.get(uri), uri).end(fn);
 * ```
 *
 * @param {Object} superagent The `superagent` exports object
 * @api public
 */

function setup (superagent, uri) {
  var Request = superagent.Request;
  if (Request) {
    // the superagent exports object - extent Request with "proxy"
    Request.prototype.proxy = proxy;
    return superagent;
  } else {
    // assume it's a `superagent.Request` instance
    return proxy.call(superagent, uri);
  }
}

/**
 * Sets the proxy server to use for this HTTP(s) request.
 *
 * @param {String} uri proxy url
 * @api public
 */

function proxy (uri) {
  debug('Request#proxy(%o)', uri);

  // we need to observe the `url` field from now on... Superagent sometimes
  // re-uses the `req` instance but changes its `url` field (i.e. in the case of
  // a redirect), so when that happens we need to potentially re-set the proxy
  // agent
  setupUrl(this);

  // attempt to get a proxying `http.Agent` instance
  var agent = proxyAgent(uri);

  // if we have an `http.Agent` instance then call the .agent() function
  if (agent) this.agent(agent);

  // store the proxy URI in case of changes to the `url` prop in the future
  this._proxyUri = uri;

  return this;
}

/**
 * Sets up a get/set descriptor for the `url` property of the provided `req`
 * Request instance. This is so that we can re-run the "proxy agent" logic when
 * the `url` field is changed, i.e. during a 302 Redirect scenario.
 *
 * @api private
 */

function setupUrl (req) {
  var desc = Object.getOwnPropertyDescriptor(req, 'url');
  if (desc.get == getUrl && desc.set == setUrl) return; // already patched

  // save current value
  req._url = req.url;

  desc.get = getUrl;
  desc.set = setUrl;
  delete desc.value;
  delete desc.writable;

  Object.defineProperty(req, 'url', desc);
  debug('patched superagent Request "url" property for changes');
}

/**
 * `url` property getter.
 *
 * @api protected
 */

function getUrl () {
  return this._url;
}

/**
 * `url` property setter.
 *
 * @api protected
 */

function setUrl (v) {
  debug('set `.url`: %o', v);
  this._url = v;
  proxy.call(this, this._proxyUri);
}
