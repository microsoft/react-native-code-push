#import "CodePush.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#else
#import "RCTConvert.h"
#endif

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushInstallMode"
@implementation RCTConvert (CodePushInstallMode)

RCT_ENUM_CONVERTER(CodePushInstallMode, (@{ @"codePushInstallModeImmediate": @(CodePushInstallModeImmediate),
                                            @"codePushInstallModeOnNextRestart": @(CodePushInstallModeOnNextRestart),
                                            @"codePushInstallModeOnNextResume": @(CodePushInstallModeOnNextResume),
                                            @"codePushInstallModeOnNextSuspend": @(CodePushInstallModeOnNextSuspend) }),
                   CodePushInstallModeImmediate, // Default enum value
                   integerValue)

@end
