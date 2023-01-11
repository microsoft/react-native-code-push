import { ConfigPlugin, withInfoPlist } from 'expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

// Use these imports in SDK 46 and lower
// import { ConfigPlugin, InfoPlist, withInfoPlist } from '@expo/config-plugins';
// import { ExpoConfig } from '@expo/config-types';

// Pass `<string>` to specify that this plugin requires a string property.
export const withIosBuildscriptDependency: ConfigPlugin<PluginConfigType> = (config, props) => {
  if (!props.ios.CodePushDeploymentKey) return config
  return withInfoPlist(config, (config) => {
    config.modResults.CodePushDeploymentKey = props.ios.CodePushDeploymentKey
    return config
  })
}
