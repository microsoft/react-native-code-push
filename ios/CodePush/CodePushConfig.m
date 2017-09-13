#import "CodePush.h"
#import <UIKit/UIKit.h>

@implementation CodePushConfig {
    NSMutableDictionary *_configDictionary;
}

static CodePushConfig *_currentConfig;

static NSString * const AppVersionConfigKey = @"appVersion";
static NSString * const BuildVersionConfigKey = @"buildVersion";
static NSString * const ClientUniqueIDConfigKey = @"clientUniqueId";
static NSString * const DeploymentKeyConfigKey = @"deploymentKey";
static NSString * const ServerURLConfigKey = @"serverUrl";
static NSString * const PublicKeyKey = @"publicKey";

+ (instancetype)current
{
    return _currentConfig;
}

+ (void)initialize
{
    if (self == [CodePushConfig class]) {
        _currentConfig = [[CodePushConfig alloc] init];
    }
}

- (instancetype)init
{
    self = [super init];
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];

    NSString *appVersion = [infoDictionary objectForKey:@"CFBundleShortVersionString"];
    NSString *buildVersion = [infoDictionary objectForKey:(NSString *)kCFBundleVersionKey];
    NSString *deploymentKey = [infoDictionary objectForKey:@"CodePushDeploymentKey"];
    NSString *serverURL = [infoDictionary objectForKey:@"CodePushServerURL"];
    NSString *publicKey = [infoDictionary objectForKey:@"CodePushPublicKey"];
    
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    NSString *clientUniqueId = [userDefaults stringForKey:ClientUniqueIDConfigKey];
    if (clientUniqueId == nil) {
        clientUniqueId = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
        [userDefaults setObject:clientUniqueId forKey:ClientUniqueIDConfigKey];
        [userDefaults synchronize];
    }

    if (!serverURL) {
        serverURL = @"https://codepush.azurewebsites.net/";
    }

    _configDictionary = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                            appVersion,AppVersionConfigKey,
                            buildVersion,BuildVersionConfigKey,
                            serverURL,ServerURLConfigKey,
                            clientUniqueId,ClientUniqueIDConfigKey,
                            deploymentKey,DeploymentKeyConfigKey,
                            publicKey,PublicKeyKey,
                            nil];

    return self;
}

- (NSString *)appVersion
{
    return [_configDictionary objectForKey:AppVersionConfigKey];
}

- (NSString *)buildVersion
{
    return [_configDictionary objectForKey:BuildVersionConfigKey];
}

- (NSDictionary *)configuration
{
    return _configDictionary;
}

- (NSString *)deploymentKey
{
    return [_configDictionary objectForKey:DeploymentKeyConfigKey];
}

- (NSString *)serverURL
{
    return [_configDictionary objectForKey:ServerURLConfigKey];
}

- (NSString *)clientUniqueId
{
    return [_configDictionary objectForKey:ClientUniqueIDConfigKey];
}

- (NSString *)publicKey
{
    return [_configDictionary objectForKey:PublicKeyKey];
}

- (void)setAppVersion:(NSString *)appVersion
{
    [_configDictionary setValue:appVersion forKey:AppVersionConfigKey];
}

- (void)setDeploymentKey:(NSString *)deploymentKey
{
    [_configDictionary setValue:deploymentKey forKey:DeploymentKeyConfigKey];
}

- (void)setServerURL:(NSString *)serverURL
{
    [_configDictionary setValue:serverURL forKey:ServerURLConfigKey];
}

//no setter for PublicKey, because it's need to be hard coded within Info.plist for safety

@end
