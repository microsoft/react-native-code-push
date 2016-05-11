/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, {
  AppRegistry,
  Component,
  StyleSheet,
  Text,
  View
} from 'react-native';

import CodePush from "react-native-code-push";

var testScenario = require("./CODE_PUSH_INDEX_JS_PATH");

var RNTestTester = React.createClass({
    // CodePush API Callbacks
    
    // checkForUpdate
    checkUpdateSuccess(remotePackage) {
        if (remotePackage) {
            if (!remotePackage.failedInstall) {
                this.setStateAndSendMessage("There is an update available. Remote package:" + JSON.stringify(remotePackage), "CHECK_UPDATE_AVAILABLE");
            } else {
                this.setStateAndSendMessage("An update is available but failed previously. Remote package:" + JSON.stringify(remotePackage), "UPDATE_FAILED_PREVIOUSLY");
            }
        } else {
            this.setStateAndSendMessage("The application is up to date.", "CHECK_UP_TO_DATE");
        }
    },
    checkUpdateError(error) {
        this.setStateAndSendMessage("An error occured while checking for updates.", "CHECK_ERROR");
    },
    
    // remotePackage.download
    downloadSuccess(localPackage) {
        this.setStateAndSendMessage("Download succeeded.", "DOWNLOAD_SUCCEEDED", [localPackage]);
    },
    downloadError(error) {
        this.setStateAndSendMessage("Download error.", "DOWNLOAD_ERROR");
    },
    
    // localPackage.install
    installSuccess() {
        this.setStateAndSendMessage("Update installed.", "UPDATE_INSTALLED");
    },
    installError() {
        this.setStateAndSendMessage("Install error.", "INSTALL_ERROR");
    },
    
    // sync
    onSyncStatus(status) {
        this.setStateAndSendMessage("Sync status " + status + " received.", "SYNC_STATUS", [status]);
    },
    onSyncError(error) {
        this.setStateAndSendMessage("Sync error.", "SYNC_ERROR");
    },
    
    
    // Test Output Methods
    
    readyAfterUpdate(callback) {
        this.setStateAndSendMessage("Ready after update.", "DEVICE_READY_AFTER_UPDATE", callback);
    },
    
    sendCurrentAndPendingPackage() {
        return CodePush.getUpdateMetadata(CodePush.UpdateState.PENDING)
            .then((pendingPackage) => {
                this.setStateAndSendMessage("Pending package: " + pendingPackage, "PENDING_PACKAGE", [pendingPackage ? pendingPackage.packageHash : null]);
                return CodePush.getUpdateMetadata(CodePush.UpdateState.RUNNING);
            })
            .then((currentPackage) => {
                this.setStateAndSendMessage("Current package: " + currentPackage, "CURRENT_PACKAGE", [currentPackage ? currentPackage.packageHash : null]);
            });
    },
    
    setStateAndSendMessage(message, testMessage, args) {
        this.setState({
            message: this.state.message + "\n...\n" + message
        });
        this.sendTestMessage(testMessage, args);
    },
    
    sendTestMessage(message, args, callback) {
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function () {
            if (xhr.readyState == 4 && xhr.status == 200) {
                callback && callback(xhr.response);
            }
        };
        
        xhr.open("POST", "CODE_PUSH_SERVER_URL/reportTestMessage", true);
        var body = JSON.stringify({ message: message, args: args});
        console.log("Sending test message body: " + body);

        xhr.setRequestHeader("Content-type", "application/json");

        xhr.send(body);
    },
    
    
    // Test Setup Methods
    
    componentDidMount() {
        testScenario.startTest(this);
    },
    
    getInitialState() {
        return {
            message: ""
        };
    },

    render() {
        return (
        <View style={styles.container}>
            <Text style={styles.welcome}>
            CodePush React-Native Plugin Tests
            </Text>
            <Text style={styles.instructions}>
            {testScenario.getScenarioName()}
            </Text>
            <Text style={styles.instructions}>
            {this.state.message}
            </Text>
        </View>
        );
    }
});

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5FCFF',
    },
    welcome: {
        fontSize: 20,
        textAlign: 'center',
        margin: 10,
    },
    instructions: {
        textAlign: 'center',
        color: '#333333',
        marginBottom: 5,
    },
});

AppRegistry.registerComponent('RNTestTester', () => RNTestTester);