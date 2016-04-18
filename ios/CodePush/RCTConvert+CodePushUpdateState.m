#import "CodePush.h"
#import "RCTConvert.h"

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushUpdateState"
@implementation RCTConvert (CodePushUpdateState)

RCT_ENUM_CONVERTER(CodePushUpdateState, (@{ @"codePushUpdateStateRunning": @(CodePushUpdateStateRunning),
                                            @"codePushUpdateStatePending": @(CodePushUpdateStatePending),
                                            @"codePushUpdateStateLatest": @(CodePushUpdateStateLatest)
                                          }),
                   CodePushUpdateStateRunning, // Default enum value
                   integerValue)

@end