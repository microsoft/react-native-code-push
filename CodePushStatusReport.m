#import "CodePush.h"

static NSString *const LastDeploymentReportKey = @"CODE_PUSH_LAST_DEPLOYMENT_REPORT";
static NSString *const DeploymentKeyKey = @"deploymentKey";
static NSString *const LabelKey = @"label";

@implementation CodePushStatusReport

+ (NSString *)getDeploymentKeyFromStatusReportIdentifier:(NSString *)statusReportIdentifier
{
    return [[statusReportIdentifier componentsSeparatedByString:@":"] firstObject];
}

+ (NSString *)getPackageStatusReportIdentifier:(NSDictionary *)package
{
    // Because deploymentKeys can be dynamically switched, we use a
    // combination of the deploymentKey and label as the packageIdentifier.
    NSString *deploymentKey = [package objectForKey:DeploymentKeyKey];
    NSString *label = [package objectForKey:LabelKey];
    if (deploymentKey && label) {
        return [[deploymentKey stringByAppendingString:@":"] stringByAppendingString:label];
    } else {
        return nil;
    }
}

+ (NSString *)getPreviousStatusReportIdentifier
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSString *sentStatusReportIdentifier = [preferences objectForKey:LastDeploymentReportKey];
    return sentStatusReportIdentifier;
}

+ (NSString *)getVersionLabelFromStatusReportIdentifier:(NSString *)statusReportIdentifier
{
    return [[statusReportIdentifier componentsSeparatedByString:@":"] lastObject];
}

+ (BOOL)isStatusReportIdentifierCodePushLabel:(NSString *)statusReportIdentifier
{
    return statusReportIdentifier != nil && [statusReportIdentifier containsString:@":"];
}

+ (void)recordDeploymentStatusReported:(NSString *)appVersionOrPackageIdentifier
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    [preferences setValue:appVersionOrPackageIdentifier forKey:LastDeploymentReportKey];
    [preferences synchronize];
}

@end