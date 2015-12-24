"use strict";

import React, {
  AppRegistry,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

let TESTS = [
  require("./testcases/DownloadProgressTest")
];

TESTS.forEach(
  (test) => AppRegistry.registerComponent(test.displayName, () => test)
);

let DownloadProgressTestApp = React.createClass({
  getInitialState() {
    return {
      test: null,
    };
  },
  render() {
    if (this.state.test) {
      return (
        <ScrollView>
          <this.state.test />
        </ScrollView>
      );
    }
    return (
      <View style={styles.container}>
        <Text style={styles.row}>
          DownloadProgress Tests
        </Text>
        <View style={styles.separator} />
        <ScrollView>
          {TESTS.map((test) => [
            <TouchableOpacity
              onPress={() => this.setState({test})}
              style={styles.row}>
              <Text style={styles.testName}>
                {test.displayName}
              </Text>
              <Text style={styles.testDescription}>
                {test.description}
              </Text>
            </TouchableOpacity>,
            <View style={styles.separator} />
          ])}
        </ScrollView>
      </View>
    );
  }
});

let styles = StyleSheet.create({
  container: {
    backgroundColor: "white",
    marginTop: 40,
    margin: 15,
  },
  row: {
    padding: 10,
  },
  testName: {
    fontWeight: "500",
  },
  separator: {
    height: 1,
    backgroundColor: "#bbbbbb",
  }
});

AppRegistry.registerComponent("DownloadProgressTestApp", () => DownloadProgressTestApp);