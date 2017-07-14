#import "CodePush.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#else
#import "RCTConvert.h"
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
