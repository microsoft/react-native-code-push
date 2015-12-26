"use strict";

module.exports = {
  async request(verb, url, body, callback) {
    if (typeof body === "function") {
      callback = body;
      body = null;
    }

    var headers = {
      "Accept": "application/json",
      "Content-Type": "application/json"
    };

    if (body && typeof body === "object") {
      body = JSON.stringify(body);
    }

    try {
      const response = await fetch(url, {
        method: verb,
        headers: headers,
        body: body
      });
        
      const statusCode = response.status;
      const body = await response.text();
      callback(null, { statusCode, body });
    } catch (err) {
      callback(err);
    }
  }
};
