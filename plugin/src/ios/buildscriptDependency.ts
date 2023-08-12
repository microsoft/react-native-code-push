import { ConfigPlugin, withInfoPlist } from 'expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

// Pass `<string>` to specify that this plugin requires a string property.
export const withIosBuildscriptDependency: ConfigPlugin<PluginConfigType> = (config, props) => {
  if (!props.ios.CodePushDeploymentKey) return config
  return withInfoPlist(config, (config) => {
    config.modResults.CodePushDeploymentKey = props.ios.CodePushDeploymentKey
    return config
  })
}
