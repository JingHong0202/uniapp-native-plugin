#
# Be sure to run `pod lib lint su-testmodule.podspec' to ensure this is a
# valid spec before submitting.
#
# Any lines starting with a # are optional, but their use is encouraged
# To learn more about a Podspec see https://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
  s.name             = 'surprise-io'
  s.version          = '0.0.1'
  s.summary          = 'uni-app IOS端本地文件选择器插件'

# This description is used to generate tags and improve search results.
#   * Think: What does it do? Why did you write it? What is the focus?
#   * Try to keep it short, snappy and to the point.
#   * Write the description between the DESC delimiters below.
#   * Finally, don't worry about the indent, CocoaPods strips it!

#   s.description      = <<-DESC
# TODO: Add long description of the pod here.
#                        DESC

  s.homepage         = 'https://github.com/JingHong0202/uniapp-native-plugin/tree/surprise-io'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'surprise-io' => 'surprise' }
  s.source           = { :git => 'https://github.com/JingHong0202/uniapp-native-plugin.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'

  s.ios.deployment_target = '9.0'

  s.source_files = 'surprise-io/Classes/**/*'
  
  s.static_framework = true

  s.xcconfig = {
    'USER_HEADER_SEARCH_PATHS'=> [
      '"$(SRCROOT)/../../SDK/inc"/**'
    ]
  }

  s.user_target_xcconfig = {
    'ENABLE_BITCODE' => 'NO'
  }
  # s.resource_bundles = {
  #   'su-testmodule' => ['su-testmodule/Assets/*.png']
  # }

  # s.public_header_files = 'Pod/Classes/**/*.h'
  # s.frameworks = 'UIKit', 'MapKit'
  # s.dependency 'AFNetworking', '~> 2.3'
end
