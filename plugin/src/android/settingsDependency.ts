import { ConfigPlugin, withSettingsGradle } from '@expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

/**
 * Update `<project>/settings.gradle` by adding react-native-code-push
 */

function applySettings(gradleSettings: string) {
  const codePushSettings = `include ':app', ':react-native-code-push'\nproject(':react-native-code-push').projectDir = new File(["node", "--print", "require.resolve('react-native-code-push/package.json')"].execute(null, rootDir).text.trim(), "../android/app")`

  // Make sure the project does not have the settings already
  if (!gradleSettings.includes(`include ':app', ':react-native-code-push'`)) {
    return gradleSettings + codePushSettings
  }

  return gradleSettings
}

export const withAndroidSettingsDependency: ConfigPlugin<PluginConfigType> = (config) => {
  return withSettingsGradle(config, (config) => {
    config.modResults.contents = applySettings(config.modResults.contents)
    return config
  })
}
