#import "CodePush.h"

#if __has_include("RCTConvert.h")
#import "RCTConvert.h"
#else
#import <React/RCTConvert.h>
#endif

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
