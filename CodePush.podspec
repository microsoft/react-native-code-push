require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|

  s.name           = 'CodePush'
  s.version        = package['version'].sub('-beta', '')
  s.summary        = package['description']
  s.author         = package['author']
  s.license        = package['license']
  s.homepage       = package['homepage']
  s.source         = { :git => 'https://github.com/Microsoft/react-native-code-push.git', :tag => "v#{s.version}-beta"}
  s.platform       = :ios, '7.0'
  s.preserve_paths = '*.js'
  s.library        = 'z'

  s.dependency 'React'

  s.subspec 'Core' do |ss|
    ss.source_files = 'ios/CodePush/*.{h,m}'
    ss.public_header_files = ['ios/CodePush/CodePush.h']
  end

  s.subspec 'SSZipArchive' do |ss|
    ss.source_files = 'ios/CodePush/SSZipArchive/*.{h,m}', 'ios/CodePush/SSZipArchive/aes/*.{h,c}', 'ios/CodePush/SSZipArchive/minizip/*.{h,c}'
    ss.private_header_files = 'ios/CodePush/SSZipArchive/*.h', 'ios/CodePush/SSZipArchive/aes/*.h', 'ios/CodePush/SSZipArchive/minizip/*.h'
  end

end