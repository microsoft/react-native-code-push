let codePushOptions = { updateDialog: true, installMode: codePush.InstallMode.IMMEDIATE, checkFrequency: codePush.CheckFrequency.ON_APP_RESUME};
codePush.SyncStatus.DOWNLOADING_BIN_PACKAGE = -100

class App extends Component {
  constructor(props) {
    super(props)
    this.state = {
      codepushStatus: codePush.SyncStatus.CHECKING_FOR_UPDATE,
      codepushStatusMessage: "Checking for updates",
      progress: {
        receivedBytes: 0,
        totalBytes: 1
      },
      update: {
        appVersion: 1.0 // Current installed binary version
      }
    }
  }

  codePushStatusDidChange(status) {
      var message = ""
      switch(status) {
          case codePush.SyncStatus.CHECKING_FOR_UPDATE:
              console.log("Checking for updates");
              message = "Checking for updates"
          case codePush.SyncStatus.DOWNLOADING_PACKAGE:
              console.log("Downloading package.");
              message = "Downloading package"
          case codePush.SyncStatus.INSTALLING_UPDATE:
              console.log("Installing update.");
              message = "Installing update"
          case codePush.SyncStatus.UP_TO_DATE:
              console.log("Up-to-date.");
              message = "Up-to-date"
          case codePush.SyncStatus.UPDATE_INSTALLED:
              console.log("Update installed.");
              message = "Update installed"
      }
      if(this.state.codepushStatus != codePush.SyncStatus.DOWNLOADING_BIN_PACKAGE) {
        this.setState({codepushStatus: status, codepushStatusMessage: message})
      }
  }

  codePushDownloadDidProgress(progress) {
      this.setState({codepushStatusMessage: "Downloading updates", progress: progress})
  }

  codePushOnBinaryVersionMismatch(update) {
    if(update && update.updateAppVersion) {
      alert("You are running on version " + Config.VERSION_NAME + "\nApp has been updated to " + update.appVersion + " \nDownload latest APP and install")
      this.setState({codepushStatus: codePush.SyncStatus.DOWNLOADING_BIN_PACKAGE, codepushStatusMessage: "Download newer version of your " + Config.BUILD_NAME + " app", update: update})
    }
  }

  renderDownloadBin() {
    if("<Url to get the APK from AWS S3> is defined") {
      return(
          <TouchableOpacity style={{backgroundColor: "#0fb14a", padding: 10, marginTop: 20}} onPress={() => Linking.openURL("<Url to get the APK from AWS S3>")}>
            <View>
              <Text style={{textAlign:'center',
                            fontSize:14,
                            color:"#ffffff",
                            fontFamily: "Roboto-Medium"}}>Download Now</Text>
            </View>
          </TouchableOpacity>
        )
    }else {
      return (
          <View>
              <Text style={{textAlign:'center',
                            fontSize:14,
                            color:"#ffffff",
                            fontFamily: "Roboto-Medium"}}>Please download the latest APK</Text>
            </View>
        )
    }
  }

  render() {
    if(((this.state.codepushStatus == codePush.SyncStatus.DOWNLOADING_BIN_PACKAGE) && this.state.update)) {
      console.log(this.state.update)
      return (
        <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
          <Text>Installed Version: {Config.VERSION_NAME}</Text>
          <Text>New Version: {this.state.update.appVersion}</Text>
          {this.renderDownloadBin()}
        </View>
      )
    }else {
      return (
          <Provider store={store}>
            <AppContainer />
          </Provider>
        )
    }
  }
}
if(Config.CODE_PUSH_ENABLE) {
  App = codePush(codePushOptions)(App);
}