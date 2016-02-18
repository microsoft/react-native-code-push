#import "CodePush.h"
#import "RCTConvert.h"

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushInstallMode"
@implementation RCTConvert (CodePushInstallMode)

RCT_ENUM_CONVERTER(CodePushInstallMode, (@{ @"codePushInstallModeImmediate": @(CodePushInstallModeImmediate),
                                            @"codePushInstallModeOnNextRestart": @(CodePushInstallModeOnNextRestart),
                                            @"codePushInstallModeOnNextResume": @(CodePushInstallModeOnNextResume) }),
                   CodePushInstallModeImmediate, // Default enum value
                   integerValue)

@end