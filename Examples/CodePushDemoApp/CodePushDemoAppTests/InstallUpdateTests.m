#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTAssert.h"

#define FB_REFERENCE_IMAGE_DIR "\"$(SOURCE_ROOT)/$(PROJECT_NAME)Tests/ReferenceImages\""

@interface InstallUpdateTests : XCTestCase

@end

@implementation InstallUpdateTests
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
  _runner = RCTInitRunnerForApp(@"CodePushDemoAppTests/InstallUpdateTests/InstallUpdateTestApp", nil);
}

#pragma mark Logic Tests
- (void)testDownloadAndInstallUpdate
{
  
  [_runner runTest:_cmd module:@"DownloadAndInstallUpdateTest"];
}

@end
