#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTAssert.h"
#import "CodePush.h"

#define FB_REFERENCE_IMAGE_DIR "\"$(SOURCE_ROOT)/$(PROJECT_NAME)Tests/ReferenceImages\""

@interface CheckForUpdateTests : XCTestCase

@end

@implementation CheckForUpdateTests
{
    RCTTestRunner *_runner;
}

- (void)setUp
{
#if __LP64__
    RCTAssert(false, @"Tests should be run on 32-bit device simulators (e.g. iPhone 5)");
#endif
  
    NSOperatingSystemVersion version = [[NSProcessInfo processInfo] operatingSystemVersion];
    RCTAssert(version.majorVersion == 8 || version.minorVersion == 3, @"Tests should be run on iOS 8.3, found %zd.%zd.%zd", version.majorVersion, version.minorVersion, version.patchVersion);
    [CodePush setUsingTestConfiguration:YES];
    [CodePush clearUpdates];
    _runner = RCTInitRunnerForApp(@"CodePushDemoAppTests/CheckForUpdateTests/CheckForUpdateTestApp", nil);
}

#pragma mark Logic Tests

- (void)testFirstUpdate
{
    [_runner runTest:_cmd
              module:@"FirstUpdateTest"];
}

- (void)testNewUpdate
{
    [_runner runTest:_cmd
              module:@"NewUpdateTest"];
}

- (void)testNoRemotePackage
{
  
    [_runner runTest:_cmd module:@"NoRemotePackageTest"];
}

- (void)testRemotePackageAppVersionNewer
{
    [_runner runTest:_cmd
              module:@"RemotePackageAppVersionNewerTest"];
}

- (void)testSamePackage
{
  [_runner runTest:_cmd
            module:@"SamePackageTest"];
}

- (void)testSwitchDeploymentKey
{
    [_runner runTest:_cmd
              module:@"SwitchDeploymentKeyTest"];
}

@end
