Pod::Spec.new do |s|

  s.name                = 'CodePush'
  s.version             = '1.7.4-beta'
  s.summary             = 'React Native plugin for the CodePush service'
  s.author              = 'Microsoft Corporation'
  s.license             = 'MIT'
  s.homepage            = 'http://microsoft.github.io/code-push/'
  s.source              = { :git => 'https://github.com/Microsoft/react-native-code-push.git', :tag => "v#{s.version}" }
  s.platform            = :ios, '7.0'
  s.source_files        = 'ios/CodePush/*.{h,m}', 'ios/CodePush/SSZipArchive/*.{h,m}', 'ios/CodePush/SSZipArchive/aes/*.{h,c}', 'ios/CodePush/SSZipArchive/minizip/*.{h,c}'
  s.public_header_files = 'ios/CodePush/CodePush.h'
  s.preserve_paths      = '*.js'
  s.library             = 'z'
  s.dependency 'React'

end