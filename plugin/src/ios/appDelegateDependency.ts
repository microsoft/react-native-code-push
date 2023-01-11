import { ConfigPlugin, withAppDelegate } from 'expo/config-plugins'

import { PluginConfigType } from '../pluginConfig'

// Use these imports in SDK 46 and lower
// import { ConfigPlugin, InfoPlist, withInfoPlist } from '@expo/config-plugins';
// import { ExpoConfig } from '@expo/config-types';

function applyImplementation(appDelegate: string, find: string, add: string, replace?: boolean) {
  // Make sure the project does not have the settings already
  if (!appDelegate.includes(add)) {
    if (replace) return appDelegate.replace(find, add)
    else return appDelegate.replace(find, `${find}\n${add}`)
  }

  return appDelegate
}

export const withIosAppDelegateDependency: ConfigPlugin<PluginConfigType> = (config, props) => {
  return withAppDelegate(config, (config) => {
    config.modResults.contents = applyImplementation(
      config.modResults.contents,
      `#import "AppDelegate.h"`,
      `#import <CodePush/CodePush.h>`
    )
    config.modResults.contents = applyImplementation(
      config.modResults.contents,
      `return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];`,
      `return [CodePush bundleURL];`,
      true
    )
    return config
  })
}
