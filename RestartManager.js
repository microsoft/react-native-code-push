const log = require("./logging");
const NativeCodePush = require("react-native").NativeModules.CodePush;
const CodePush = require("./CodePush");

const RestartManager = (() => {
    let _inProgress = false;
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
            if (_inProgress) {
                log("A restart request is already in progress or queued");
                return;
            }
            // The restart won't execute if `onlyIfUpdateIsPending === true` and there is no pending update.
            _inProgress = !onlyIfUpdateIsPending || !!(NativeCodePush.getUpdateMetadata(CodePush.UpdateState.PENDING));
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