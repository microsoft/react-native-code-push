#import "CodePush.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#else
#import "RCTConvert.h"
#endif

// Extending the RCTConvert class allows the React Native
// bridge to handle args of type "CodePushSyncStatus"
@implementation RCTConvert (CodePushSyncStatus)

RCT_ENUM_CONVERTER(CodePushSyncStatus, (@{ @"codePushSyncStatusUpToDate": @(CodePushSyncStatusUP_TO_DATE),
                                            @"codePushSyncStatusUpdateInstalled": @(CodePushSyncStatusUPDATE_INSTALLED),
                                            @"codePushSyncStatusUpdateIgnored": @(CodePushSyncStatusUPDATE_IGNORED),
                                            @"codePushSyncStatusUnknownError": @(CodePushSyncStatusUNKNOWN_ERROR),
                                            @"codePushSyncStatusSyncInProgress": @(CodePushSyncStatusSYNC_IN_PROGRESS),
                                            @"codePushSyncStatusCheckingForUpdate": @(CodePushSyncStatusCHECKING_FOR_UPDATE),
                                            @"codePushSyncStatusAwaitingUserAction": @(CodePushSyncStatusAWAITING_USER_ACTION),
                                            @"codePushSyncStatusDownloadingPackage": @(CodePushSyncStatusDOWNLOADING_PACKAGE),
                                            @"codePushSyncStatusInstallingUpdate": @(CodePushSyncStatusINSTALLING_UPDATE),
                                            }),
                   CodePushSyncStatusUP_TO_DATE, // Default enum value
                   integerValue)

@end
