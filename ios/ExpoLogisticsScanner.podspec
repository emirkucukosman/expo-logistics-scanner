Pod::Spec.new do |s|
  s.name           = 'ExpoLogisticsScanner'
  s.version        = '1.0.0'
  s.summary        = 'High-performance barcode scanning for Expo logistics apps'
  s.description    = 'Native AVFoundation and Vision-based CODE_128 scanning for React Native and Expo.'
  s.author         = 'Emir Kucukosman'
  s.homepage       = 'https://docs.expo.dev/modules/'
  s.platforms      = {
    :ios => '16.4',
    :tvos => '16.4'
  }
  s.source         = { git: '' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = "**/*.{h,m,mm,swift,hpp,cpp}"
end
