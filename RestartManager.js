let log = require('./logging');
let NativeCodePush = require("react-native").NativeModules.CodePush;

const RestartManager = (() => {
    let _allowed = true;
    let _restartPending = false;

    function restartApp(onlyIfUpdateIsPending = false) {
        if (_allowed) {
            NativeCodePush.restartApp(onlyIfUpdateIsPending);
        } else {
            log("restart not allowed");
            _restartPending = true;
        }
    }

    function allow() {
        log("allow restart");
        _allowed = true;
        if (_restartPending) {
            log("executing pending restart");
            restartApp(true);
        }
    }

    function disallow() {
        log("disallow restart");
        _allowed = false;
    }

    return {
        allow,
        disallow,
        restartApp,
    };
})();

module.exports = RestartManager;
