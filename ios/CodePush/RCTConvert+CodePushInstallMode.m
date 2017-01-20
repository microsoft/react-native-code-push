#import "CodePush.h"

#if __has_include("RCTConvert.h")
#import "RCTConvert.h"
#else
#import <React/RCTConvert.h>
#endif

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushInstallMode"
@implementation RCTConvert (CodePushInstallMode)

RCT_ENUM_CONVERTER(CodePushInstallMode, (@{ @"codePushInstallModeImmediate": @(CodePushInstallModeImmediate),
                                            @"codePushInstallModeOnNextRestart": @(CodePushInstallModeOnNextRestart),
                                            @"codePushInstallModeOnNextResume": @(CodePushInstallModeOnNextResume) }),
                   CodePushInstallModeImmediate, // Default enum value
                   integerValue)

@end
