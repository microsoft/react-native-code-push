require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|

  s.name                = 'CodePush'
  s.version             = package['version'].sub('-beta', '')
  s.summary             = 'React Native module for the CodePush service'
  s.author              = 'Microsoft Corporation'
  s.license             = 'MIT'
  s.homepage            = 'http://microsoft.github.io/code-push/'
  s.source              = { :git => 'https://github.com/Microsoft/react-native-code-push.git', :tag => "v#{s.version}-beta" }
  s.platform            = :ios, '7.0'
  s.public_header_files = 'ios/CodePush/CodePush.h'
  s.preserve_paths      = '*.js'
  s.library             = 'z'
  s.dependency 'React'
  
  s.default_subspec = 'Full'
  
  s.subspec 'Full' do |ss|
    ss.source_files = 'ios/CodePush/*.{h,m}', 'ios/CodePush/SSZipArchive/*.{h,m}', 'ios/CodePush/SSZipArchive/aes/*.{h,c}', 'ios/CodePush/SSZipArchive/minizip/*.{h,c}'    
  end
  
  s.subspec 'NoZip' do |ss|
    ss.source_files = 'ios/CodePush/*.{h,m}'    
  end
  
end