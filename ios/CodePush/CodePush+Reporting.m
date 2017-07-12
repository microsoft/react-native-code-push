#import "CodePush+Reporting.h"
#import "CodePush.h"

@implementation CodePush (Reporting)

+ (void)reportStatusDownload:(NSDictionary *)updatePackage withConfiguraton:(NSDictionary*)config
{
    CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:config];
    NSError *error;
    [aquisitionSdk reportStatusDownload:updatePackage error:&error];
}

+ (void)reportStatusDeploy:(NSDictionary *)statusReport withConfiguraton:(NSDictionary*)config
{
    NSMutableDictionary *configuration = [config mutableCopy];
    NSString *prevLabelOrAppVersion = [statusReport objectForKey:PreviousLabelOrAppVersionKey];
    NSString *prevDeploymentKey = [statusReport objectForKey:PreviousDeploymentKey];
    NSError *error;
    
    CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:configuration];
    
    if (!prevDeploymentKey){
        prevDeploymentKey = [configuration objectForKey:DeploymentKeyConfigKey];
    }
    
    if ([statusReport objectForKey:AppVersionKey]){
        CPLog(@"Reporting binary update  %@", [statusReport objectForKey:AppVersionKey]);
        
        [aquisitionSdk reportStatusDeploy:nil
                               withStatus:nil
                previousLabelOrAppVersion:prevLabelOrAppVersion
                    previousDeploymentKey:prevDeploymentKey
                                    error:&error];
    } else {
        NSDictionary *package = [statusReport objectForKey:@"package"];
        NSString *label = [package objectForKey:LabelKey];
        NSString *reportStatus = [statusReport objectForKey:StatusKey];
        
        if ([reportStatus  isEqual: DeploymentSucceeded]){
            CPLog(@"Reporting CodePush update success %@", label);
        } else {
            CPLog(@"Reporting CodePush update rollback %@", label);
        }
        
        NSString *configDeploymentKey = [package objectForKey:DeploymentKeyConfigKey];
        [configuration setObject:configDeploymentKey forKey:DeploymentKeyConfigKey];
        
        [aquisitionSdk reportStatusDeploy:package
                               withStatus:reportStatus
                previousLabelOrAppVersion:prevLabelOrAppVersion
                    previousDeploymentKey:prevDeploymentKey
                                    error:&error];
        
    }
    
    if (error){
        //TODO: add retry logic?
        CPLog(@"Something went wrong, status report was not reported");
    } else {
        [CodePushTelemetryManager recordStatusReported:statusReport];
    }
}

@end
