module.exports.request = function request(verb, url, body, callback) {
    if (typeof body === "function") {
        callback = body;
        body = null;
    }

    var headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    };

    if (body && typeof body === "object") {
        body = JSON.stringify(body);
    }

    var statusCode;

    fetch(url, {
        method: verb,
        headers: headers,
        body: body
    }).then(function(response) {
        statusCode = response.status;
        return response.text();
    }).then(function(body) {
        callback(null, {statusCode: statusCode, body: body});
    }).catch(function(err) {
        callback(err);
    });
}
