import { ConfigPlugin, withAppBuildGradle } from '@expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

/**
 * Update `<project>/build.gradle` by adding the codepush.gradle file
 * as an additional build task definition underneath react.gradle
 */

function applyImplementation(appBuildGradle: string) {
  const codePushImplementation = `apply from: new File(["node", "--print", "require.resolve('react-native-code-push/package.json')"].execute(null, rootDir).text.trim()).getParentFile().getAbsolutePath() + "/android/codepush.gradle"`

  // Make sure the project does not have the dependency already
  if (!appBuildGradle.includes(codePushImplementation)) {
    if (appBuildGradle.includes('apply from: new File(reactNativeRoot, "react.gradle")')) {
      return appBuildGradle.replace(
        'apply from: new File(reactNativeRoot, "react.gradle")',
        `apply from: new File(reactNativeRoot, "react.gradle")\n${codePushImplementation}`
      )
    } else if (
      appBuildGradle.includes('apply from: "../../node_modules/react-native/react.gradle"')
    ) {
      return appBuildGradle.replace(
        'apply from: "../../node_modules/react-native/react.gradle"',
        `apply from: "../../node_modules/react-native/react.gradle"\n${codePushImplementation}`
      )
    }
  }

  return appBuildGradle
}
export const withAndroidBuildscriptDependency: ConfigPlugin<PluginConfigType> = (config) => {
  return withAppBuildGradle(config, (config) => {
    config.modResults.contents = applyImplementation(config.modResults.contents)
    return config
  })
}
