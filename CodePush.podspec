require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'CodePush'
  s.version        = package['version'].gsub(/v|-beta/, '')
  s.summary        = package['description']
  s.author         = package['author']
  s.license        = package['license']
  s.homepage       = package['homepage']
  s.source         = { :git => 'https://github.com/Microsoft/react-native-code-push.git', :tag => "v#{s.version}"}
  s.ios.deployment_target = '7.0'
  s.tvos.deployment_target = '9.0'
  s.preserve_paths = '*.js'
  s.library        = 'z'
  s.source_files = 'ios/CodePush/*.{h,m}'
  s.public_header_files = ['ios/CodePush/CodePush.h']

  # Note: Even though there are copy/pasted versions of some of these dependencies in the repo, 
  # we explicitly let CocoaPods pull in the versions below so all dependencies are resolved and 
  # linked properly at a parent workspace level.
  s.dependency 'React'
  s.dependency 'SSZipArchive', '~> 2.1'
  s.dependency 'JWT', '~> 3.0.0-beta.7'
  s.dependency 'Base64', '~> 1.1'
end
