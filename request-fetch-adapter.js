let request = async (verb, url, body, callback) => {
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
        let response = await fetch(url, {
            method: verb,
            headers: headers,
            body: body
        });
        
        let statusCode = response.status;
        let body = await response.text();
        callback(null, { statusCode, body });
    } catch (err) {
        callback(err);
    }
}
