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
        method: getHttpMethodName(verb),
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

function getHttpMethodName(verb) {
  // Note: This should stay in sync with the enum definition in
  // https://github.com/Microsoft/code-push/blob/master/sdk/script/acquisition-sdk.ts#L6
  return [
    "GET",
    "HEAD",
    "POST",
    "PUT",
    "DELETE",
    "TRACE",
    "OPTIONS",
    "CONNECT",
    "PATCH"
  ][verb];
}