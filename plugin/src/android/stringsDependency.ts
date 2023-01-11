import { AndroidConfig, ConfigPlugin, withStringsXml } from '@expo/config-plugins'
import { ResourceXML } from '@expo/config-plugins/build/android/Resources'

import { PluginConfigType } from '../pluginConfig'

/**
 * Update `<project>/settings.gradle` by adding react-native-code-push
 */

function setStrings(strings: ResourceXML, value: string) {
  // Helper to add string.xml JSON items or overwrite existing items with the same name.
  return AndroidConfig.Strings.setStringItem(
    [
      // XML represented as JSON
      // <string moduleConfig="true" name="CodePushDeploymentKey">value</string>
      { $: { name: 'CodePushDeploymentKey' }, _: value },
    ],
    strings
  )
}

export const withAndroidStringsDependency: ConfigPlugin<PluginConfigType> = (config, props) => {
  return withStringsXml(config, (config) => {
    config.modResults = setStrings(config.modResults, props.android.CodePushDeploymentKey)
    return config
  })
}
