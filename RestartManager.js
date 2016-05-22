let NativeCodePush = require("react-native").NativeModules.CodePush;

const RestartManager = (() => {
    let _allowed = true;

    function restartApp(onlyIfUpdateIsPending = false) {
        if (_allowed) {
            NativeCodePush.restartApp(onlyIfUpdateIsPending);
        }
    }

    function allow() {
        _allowed = true;
        restartApp(true);
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
        restartApp,
    };
})();

module.exports = RestartManager;
