#import "CodePush.h"

static NSString *const DeploymentFailed = @"DeploymentFailed";
static NSString *const DeploymentKeyKey = @"deploymentKey";
static NSString *const DeploymentSucceeded = @"DeploymentSucceeded";
static NSString *const LabelKey = @"label";
static NSString *const LastDeploymentReportKey = @"CODE_PUSH_LAST_DEPLOYMENT_REPORT";

@implementation CodePushTelemetryManager

+ (NSDictionary *)getBinaryUpdateReport:(NSString *)appVersion
{
    NSString *previousStatusReportIdentifier = [self getPreviousStatusReportIdentifier];
    if (previousStatusReportIdentifier == nil) {
        [self recordDeploymentStatusReported:appVersion];
        return @{ @"appVersion": appVersion };
    } else if (![previousStatusReportIdentifier isEqualToString:appVersion]) {
        [self recordDeploymentStatusReported:appVersion];
        if ([self isStatusReportIdentifierCodePushLabel:previousStatusReportIdentifier]) {
            NSString *previousDeploymentKey = [self getDeploymentKeyFromStatusReportIdentifier:previousStatusReportIdentifier];
            NSString *previousLabel = [self getVersionLabelFromStatusReportIdentifier:previousStatusReportIdentifier];
            return @{
                      @"appVersion": appVersion,
                      @"previousDeploymentKey": previousDeploymentKey,
                      @"previousLabelOrAppVersion": previousLabel
                    };
        } else {
            // Previous status report was with a binary app version.
            return @{
                      @"appVersion": appVersion,
                      @"previousLabelOrAppVersion": previousStatusReportIdentifier
                    };
        }
    }
    
    return nil;
}

+ (NSDictionary *)getRollbackReport:(NSDictionary *)lastFailedPackage
{
    return @{
              @"package": lastFailedPackage,
              @"status": DeploymentFailed
            };
}

+ (NSDictionary *)getUpdateReport:(NSDictionary *)currentPackage
{
    NSString *currentPackageIdentifier = [self getPackageStatusReportIdentifier:currentPackage];
    NSString *previousStatusReportIdentifier = [self getPreviousStatusReportIdentifier];
    if (currentPackageIdentifier) {
        if (previousStatusReportIdentifier == nil) {
            [self recordDeploymentStatusReported:currentPackageIdentifier];
            return @{
                      @"package": currentPackage,
                      @"status": DeploymentSucceeded
                    };
        } else if (![previousStatusReportIdentifier isEqualToString:currentPackageIdentifier]) {
            [self recordDeploymentStatusReported:currentPackageIdentifier];
            if ([self isStatusReportIdentifierCodePushLabel:previousStatusReportIdentifier]) {
                NSString *previousDeploymentKey = [self getDeploymentKeyFromStatusReportIdentifier:previousStatusReportIdentifier];
                NSString *previousLabel = [self getVersionLabelFromStatusReportIdentifier:previousStatusReportIdentifier];
                return @{
                          @"package": currentPackage,
                          @"status": DeploymentSucceeded,
                          @"previousDeploymentKey": previousDeploymentKey,
                          @"previousLabelOrAppVersion": previousLabel
                        };
            } else {
                // Previous status report was with a binary app version.
                return @{
                          @"package": currentPackage,
                          @"status": DeploymentSucceeded,
                          @"previousLabelOrAppVersion": previousStatusReportIdentifier
                        };
            }
        }
    }
    
    return nil;
}

#pragma mark - private methods

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