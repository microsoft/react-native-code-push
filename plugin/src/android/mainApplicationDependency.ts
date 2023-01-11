import { ConfigPlugin, withMainApplication } from '@expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

/**
 * Update `<project>/build.gradle` by adding the codepush.gradle file
 * as an additional build task definition underneath react.gradle
 */

function applyImplementation(
  mainApplication: string,
  find: string,
  add: string,
  replace?: boolean
) {
  // Make sure the project does not have the settings already
  if (!mainApplication.includes(add)) {
    if (replace) return mainApplication.replace(find, add)
    else return mainApplication.replace(find, `${find}\n${add}`)
  }

  return mainApplication
}

export const withAndroidMainApplicationDependency: ConfigPlugin<PluginConfigType> = (config) => {
  return withMainApplication(config, (config) => {
    config.modResults.contents = applyImplementation(
      config.modResults.contents,
      'import expo.modules.ReactNativeHostWrapper;',
      `\nimport com.microsoft.codepush.react.CodePush;`
    )
    config.modResults.contents = applyImplementation(
      config.modResults.contents,
      `new ReactNativeHost(this) {`,
      `
    @Override
    protected String getJSBundleFile() {
      return CodePush.getJSBundleFile();
    }\n`
    )
    return config
  })
}
