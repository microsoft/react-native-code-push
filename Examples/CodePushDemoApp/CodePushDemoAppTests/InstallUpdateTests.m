#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTAssert.h"
#import "RCTRootView.h"
#import "RCTText.h"
#import "CodePush.h"

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
    [CodePush setUsingTestConfiguration:YES];
}

#pragma mark Logic Tests
- (void)testInstallModeImmediate
{
    [self runTest:@"InstallModeImmediateTest"];
}

- (void)testInstallModeOnNextResume
{
  [self runTest:@"InstallModeOnNextResumeTest"];
}

- (void)testInstallModeOnNextRestart
{
  [self runTest:@"InstallModeOnNextRestartTest"];
}

- (void)testIsFirstRun
{
  [self runTest:@"IsFirstRunTest"];
}

- (void)testNotifyApplicationReady
{
  [self runTest:@"NotifyApplicationReadyTest"];
}

- (void)testRollback
{
  [self runTest:@"RollbackTest"];
}

- (void)testIsFailedUpdate
{
  [self runTest:@"IsFailedUpdateTest"];
}

- (void)testIsPending
{
  [self runTest:@"IsPendingTest"];
}

- (void)runTest:(NSString *)testName
{
    [CodePush clearUpdates];
    RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:[NSURL URLWithString:[NSString stringWithFormat:@"http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/testcases/%@.bundle?platform=ios&dev=true", testName]]
                                                        moduleName:testName
                                                 initialProperties:nil
                                                     launchOptions:nil];
    rootView.frame = CGRectMake(0, 0, 320, 2000); // Constant size for testing on multiple devices
    UIViewController *vc = [UIApplication sharedApplication].delegate.window.rootViewController;
    vc.view = [UIView new];
    [vc.view addSubview:rootView];
    while (![self foundTestPassedText:vc.view]) {
        [[NSRunLoop mainRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
        [[NSRunLoop mainRunLoop] runMode:NSRunLoopCommonModes beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
    }
}

- (BOOL)foundTestPassedText:(UIView *)view {
    BOOL foundText = NO;
    NSArray *subviews = [view subviews];
    if ([subviews count] == 0) {
        if ([view isKindOfClass:[RCTText class]] && [[((RCTText *)view) textStorage].string isEqualToString:@"Test Passed!"]) {
            return YES;
        }
    
        return NO;
    }
  
    for (UIView *subview in subviews) {
        foundText = [self foundTestPassedText:subview];
        if (foundText) {
            break;
        }
    }
  
    return foundText;
}

@end
