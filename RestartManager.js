let NativeCodePush = require("react-native").NativeModules.CodePush;

const RestartManager = (() => {
    let _allowed = true;
    let restartPending = false;

    function tryRestart() {
        if (restartPending && _allowed == true) {
            NativeCodePush.restartApp(true);
        }
    }

    function requestRestart() {
        restartPending = true;
        tryRestart();
    }

    function allow() {
        _allowed = true;
        tryRestart();
    }

    function allowed() {
        return _allowed
    }

    function disallow() {
        _allowed = false;
    }

    return {
        allow,
        disallow,
        allowed,
        requestRestart
    };
})();

module.exports = RestartManager;
