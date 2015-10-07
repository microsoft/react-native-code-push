#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTBridge.h"
#import "RCTBridgeModule.h"
#import "RCTDefines.h"
#import "RCTRootView.h"
#import "RCTRedBox.h"
#import "RCTAssert.h"
#import "RCTLog.h"

#define FB_REFERENCE_IMAGE_DIR "\"$(SOURCE_ROOT)/$(PROJECT_NAME)Tests/ReferenceImages\""

#define TIMEOUT_SECONDS 60
#define TEXT_TO_LOOK_FOR @"If you see this, you have successfully installed an update!"

@interface ApplyUpdateTests : XCTestCase

@end

@implementation ApplyUpdateTests
{
  RCTTestRunner *_runner;
  NSString* app;
}

- (void)setUp
{
  app = @"CodePushDemoAppTests/ApplyUpdateTests/ApplyUpdateTestApp.ios";
#if __LP64__
  RCTAssert(false, @"Tests should be run on 32-bit device simulators (e.g. iPhone 5)");
#endif
  
  NSOperatingSystemVersion version = [[NSProcessInfo processInfo] operatingSystemVersion];
  RCTAssert(version.majorVersion == 8 || version.minorVersion == 3, @"Tests should be run on iOS 8.3, found %zd.%zd.%zd", version.majorVersion, version.minorVersion, version.patchVersion);
}


- (BOOL)findSubviewInView:(UIView *)view matching:(BOOL(^)(UIView *view))test
{
  if (test(view)) {
    return YES;
  }
  for (UIView *subview in [view subviews]) {
    if ([self findSubviewInView:subview matching:test]) {
      return YES;
    }
  }
  return NO;
}

#pragma mark Logic Tests
- (void)testDownloadAndApplyUpdate
{
  NSString *sanitizedAppName = [app stringByReplacingOccurrencesOfString:@"/" withString:@"-"];
  sanitizedAppName = [sanitizedAppName stringByReplacingOccurrencesOfString:@"\\" withString:@"-"];
  NSURL* scriptURL;
  
#if RUNNING_ON_CI
  scriptURL = [[NSBundle bundleForClass:[RCTBridge class]] URLForResource:@"main" withExtension:@"jsbundle"];
  RCTAssert(_scriptURL != nil, @"Could not locate main.jsBundle");
#else
  scriptURL = [NSURL URLWithString:[NSString stringWithFormat:@"http://localhost:8081/%@.includeRequire.runModule.bundle?dev=true", app]];
#endif
  
  RCTBridge *bridge = [[RCTBridge alloc] initWithBundleURL:scriptURL
                                            moduleProvider:nil
                                             launchOptions:nil];
  RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:bridge
                                                   moduleName:@"DownloadAndApplyUpdateTest"
                                                   initialProperties:nil];

  NSDate *date = [NSDate dateWithTimeIntervalSinceNow:TIMEOUT_SECONDS];
  BOOL foundElement = NO;
  
  __block NSString *redboxError = nil;
  RCTSetLogFunction(^(RCTLogLevel level, NSString *fileName, NSNumber *lineNumber, NSString *message) {
    if (level >= RCTLogLevelError) {
      redboxError = message;
    }
  });
  
  while ([date timeIntervalSinceNow] > 0 && !foundElement && !redboxError) {
    [[NSRunLoop mainRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
    [[NSRunLoop mainRunLoop] runMode:NSRunLoopCommonModes beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
    
    UIViewController *vc = [UIApplication sharedApplication].delegate.window.rootViewController;
    foundElement = [self findSubviewInView:vc.view matching:^BOOL(UIView *view) {
      if ([NSStringFromClass([view class]) isEqualToString:@"RCTText"]){
        NSString *text = [(id)view textStorage].string;
        if ([text isEqualToString:TEXT_TO_LOOK_FOR]) {
          return YES;
        }
      }
      
      return NO;
    }];
  }
  
  XCTAssertNil(redboxError, @"RedBox error: %@", redboxError);
  XCTAssertTrue(foundElement, @"Cound't find element with text '%@' in %d seconds", TEXT_TO_LOOK_FOR, TIMEOUT_SECONDS);
  
}


@end
