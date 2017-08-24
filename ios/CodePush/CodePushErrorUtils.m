#import "CodePush.h"

@implementation CodePushErrorUtils

static NSString *const CodePushErrorDomain = @"CodePushError";
static const int CodePushErrorCode = -1;

+ (NSError *)errorWithMessage:(NSString *)errorMessage
{
    return [NSError errorWithDomain:CodePushErrorDomain
                               code:CodePushErrorCode
                           userInfo:@{ NSLocalizedDescriptionKey: NSLocalizedString(errorMessage, nil) }];
}

+ (BOOL)isCodePushError:(NSError *)err
{
    return err != nil && [CodePushErrorDomain isEqualToString:err.domain];
}

@end