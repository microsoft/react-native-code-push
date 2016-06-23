const log = require("./logging");
const NativeCodePush = require("react-native").NativeModules.CodePush;
const CodePush = require("./CodePush");

const RestartManager = (() => {
    let _inProgressPromise = null;
    let _inProgressOnUpdateOnly = false;
    
    let _allowed = true;
    let _restartPending = false;
    let _restartPendingOnUpdateOnly = false;

    function allow() {
        log("Re-allowing restarts");
        _allowed = true;
        
        if (_restartPending) {
            log("Executing pending restart");
            restartApp(_restartPendingOnUpdateOnly);
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
        (async function(onlyIfUpdateIsPending) {
            var didRestartSucceed = false;
            
            if (_restartPending) {
                _restartPendingOnUpdateOnly = _restartPendingOnUpdateOnly && onlyIfUpdateIsPending;
                log("Restart request queued until restarts are re-allowed");
                return;
            }
            
            if (!!_inProgressPromise) {
                didRestartSucceed = await _inProgressPromise;
                if (didRestartSucceed) {
                    log("A restart is already in progress.");
                    return;
                }
            }
            
            _inProgressPromise = new Promise(async function(resolve, reject) {
                resolve(await restartAppInternal(onlyIfUpdateIsPending));
            });
            _inProgressOnUpdateOnly = onlyIfUpdateIsPending;
            
            didRestartSucceed = await _inProgressPromise;
            if (!didRestartSucceed) _inProgressPromise = null;
        })(onlyIfUpdateIsPending);
    };
    
    async function restartAppInternal(onlyIfUpdateIsPending = false) {
        if (_allowed) {
            var didRestartSucceed = await NativeCodePush.restartApp(onlyIfUpdateIsPending);
            log("Restarting app");
            return didRestartSucceed;
        } else {
            log("Restart request queued until restarts are re-allowed");
            _restartPending = true;
            _restartPendingOnUpdateOnly = onlyIfUpdateIsPending;
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