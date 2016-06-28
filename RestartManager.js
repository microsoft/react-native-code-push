const log = require("./logging");
const NativeCodePush = require("react-native").NativeModules.CodePush;

const RestartManager = (() => {
    let _allowed = true;
    let _restartInProgress = false;
    let _restartQueue = [];

    function allow() {
        log("Re-allowing restarts");
        _allowed = true;

        if (_restartQueue.length) {
            log("Executing pending restart");
            restartApp(_restartQueue.shift(1));
        }
    }

    function clearPendingRestart() {
        _restartQueue = [];
    }

    function disallow() {
        log("Disallowing restarts");
        _allowed = false;
    }

    async function restartApp(onlyIfUpdateIsPending = false) {
        if (_restartInProgress) {
            log("Restart request queued until the current restart is completed");
            _restartQueue.push(onlyIfUpdateIsPending);
        } else if (!_allowed) {
            log("Restart request queued until restarts are re-allowed");
            _restartQueue.push(onlyIfUpdateIsPending);
        } else {
            _restartInProgress = true;
            if (await NativeCodePush.restartApp(onlyIfUpdateIsPending)) {
                // The app has already restarted, so there is no need to
                // process the remaining queued restarts.
                log("Restarting app");
                return;
            }

            _restartInProgress = false;
            if (_restartQueue.length) {
                restartApp(_restartQueue.shift(1));
            }
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
