#import "CodePush.h"

// These constants represent valid deployment statuses
NSString *const DeploymentFailed = @"DeploymentFailed";
NSString *const DeploymentSucceeded = @"DeploymentSucceeded";

NSString *const CodePushSyncStatusChangedNotification = @"CodePushSyncStatusChangedNotification";

// These keys are used to inspect/augment the metadata
// that is associated with the package.
NSString *const AppVersionKey = @"appVersion";
NSString *const PackageHashKey = @"packageHash";
NSString *const LabelKey = @"label";
NSString *const PackageIsPendingKey = @"isPending";
NSString *const FailedInstallKey = @"failedInstall";
NSString *const IsFirstRunKey = @"isFirstRun";
NSString *const IsDebugOnlyKey = @"_isDebugOnly";
NSString *const UpdateInfoKey = @"updateInfo";
NSString *const IsCompanionKey = @"isCompanion";
NSString *const IsAvailableKey = @"isAvailable";
NSString *const UpdateAppVersionKey = @"updateAppVersion";
NSString *const DescriptionKey = @"description";
NSString *const IsMandatoryKey = @"isMandatory";
NSString *const PackageSizeKey = @"packageSize";
NSString *const DownloadUrlKey = @"downloadURL";
NSString *const DownloadUrRemotePackageKey = @"downloadUrl"; //there is a mismatch in this, so service require to send with lower case
NSString *const SyncStatusKey = @"syncStatus";

// This keys for associated CodePush config values
NSString * const AppVersionConfigKey = @"appVersion";
NSString * const BuildVersionConfigKey = @"buildVersion";
NSString *const ServerURLConfigKey = @"serverUrl";
NSString *const DeploymentKeyConfigKey = @"deploymentKey";
NSString *const ClientUniqueIDConfigKey = @"clientUniqueId";
NSString *const IgonreAppVersionConfigKey = @"_ignoreAppVersion";

NSString *const MinimumBackgroundDurationKey = @"minimumBackgroundDuration";
NSString *const MandatoryInstallModeKey = @"mandatoryInstallMode";
NSString *const IgnoreFailedUpdatesKey = @"ignoreFailedUpdates";
NSString *const InstallModeKey = @"installMode";
NSString *const UpdateDialogKey = @"updateDialog";


NSString *const BinaryBundleDateKey = @"binaryDate";
NSString *const StatusKey = @"status";
NSString *const PreviousDeploymentKey = @"previousDeploymentKey";
NSString *const PreviousLabelOrAppVersionKey = @"previousLabelOrAppVersion";
