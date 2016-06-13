const log = require("./logging");
const NativeCodePush = require("react-native").NativeModules.CodePush;

const RestartManager = (() => {
    let _allowed = true;
    let _restartPending = false;

    function allow() {
        log("Re-allowing restarts");
        _allowed = true;
        
        if (_restartPending) {
            log("Executing pending restart");
            restartApp(true);
        }
    }
    
    function clearPendingRestart() {
        _restartPending = false;
    }
    
    function disallow() {
        log("Disallowing restarts");
        _allowed = false;
    }

    function restartApp(onlyIfUpdateIsPending = false) {
        if (_allowed) {
            NativeCodePush.restartApp(onlyIfUpdateIsPending);
            log("Restarting app");
        } else {
            log("Restart request queued until restarts are re-allowed");
            _restartPending = true;
            return true;
        }
    }

    return {
        allow,
        clearPendingRestart,        
        disallow,
        restartApp
    };
})();

module.exports = RestartManager;