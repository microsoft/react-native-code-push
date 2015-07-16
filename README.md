hybrid-mobile-deploy-react-native
===

React Native module for deploying script updates

Running the Example
---

* Make sure you have https://github.com/Microsoft/hybrid-mobile-deploy cloned beside the react-native project in a folder called `website`. This is hacky, and will be cleaned up as soon as React Native's packager supports symlinks.
* Start the server with `gulp serve`, after installing the prerequisites described in the [project readme](https://github.com/Microsoft/hybrid-mobile-deploy/blob/master/README.md)
* `cd` into `Examples/HybridMobileDeployCompanion` and run `npm install`
* Open `index.ios.js` and add a deployment key (generate one using the UI at http://localhost:4000/)
* Open `HybridMobileDeployCompanion.xcodeproj` in Xcode
* Launch the project
