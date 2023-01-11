import { ConfigPlugin, createRunOncePlugin } from '@expo/config-plugins'

import {
  withAndroidBuildscriptDependency,
  withAndroidMainApplicationDependency,
  withAndroidSettingsDependency,
  withAndroidStringsDependency,
} from './android'
import { withIosAppDelegateDependency, withIosBuildscriptDependency } from './ios'
import { PluginConfigType } from './pluginConfig'

/**
 * A config plugin for configuring `react-native-code-push`
 */
const withRnCodepush: ConfigPlugin<PluginConfigType> = (config, props) => {
  config = withAndroidBuildscriptDependency(config, props)
  config = withAndroidSettingsDependency(config, props)
  config = withAndroidStringsDependency(config, props)
  config = withAndroidMainApplicationDependency(config, props)
  // plugins order matter: the later one would run first
  config = withIosBuildscriptDependency(config, props)
  config = withIosAppDelegateDependency(config, props)

  return config
}

const pak = require('react-native-code-push/package.json')
export default createRunOncePlugin(withRnCodepush, pak.name, pak.version)
