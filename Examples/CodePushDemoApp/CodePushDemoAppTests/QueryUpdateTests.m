/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <RCTTest/RCTTestRunner.h>

#import "RCTAssert.h"

@interface QueryUpdateTests : XCTestCase

@end

@implementation QueryUpdateTests
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
  _runner = RCTInitRunnerForApp(@"CodePushDemoAppTests/QueryUpdateTests/QueryUpdateTestApp", nil);
}

#pragma mark Logic Tests
- (void)testNoRemotePackage
{
  
  [_runner runTest:_cmd module:@"NoRemotePackageTest"];
}

- (void)testNoRemotePackageWithSameAppVersion
{
  [_runner runTest:_cmd
            module:@"NoRemotePackageWithSameAppVersionTest"];
}

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

- (void)testSamePackage
{
  [_runner runTest:_cmd
            module:@"SamePackageTest"];
}


@end
