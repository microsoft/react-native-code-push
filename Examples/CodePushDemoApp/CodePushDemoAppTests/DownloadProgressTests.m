#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTAssert.h"
#import "CodePush.h"

#define FB_REFERENCE_IMAGE_DIR "\"$(SOURCE_ROOT)/$(PROJECT_NAME)Tests/ReferenceImages\""

@interface DownloadProgressTests : XCTestCase

@end

@implementation DownloadProgressTests
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
    _runner = RCTInitRunnerForApp(@"CodePushDemoAppTests/DownloadProgressTests/DownloadProgressTestApp", nil);
}

#pragma mark Logic Tests
- (void)testDownloadProgress
{
  
    [_runner runTest:_cmd module:@"DownloadProgressTest"];
}

@end
