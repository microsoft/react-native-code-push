#import "CodePush.h"
#import "RCTConvert.h"

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushRestartMode"
@implementation RCTConvert (CodePushRestartMode)

RCT_ENUM_CONVERTER(CodePushRestartMode, (@{ @"codePushRestartModeNone": @(CodePushRestartModeNone),
                                            @"codePushRestartModeImmediate": @(CodePushRestartModeImmediate),
                                            @"codePushRestartModeOnNextResume": @(CodePushRestartModeOnNextResume) }),
                   CodePushRestartModeImmediate, // Default enum value
                   integerValue)

@end