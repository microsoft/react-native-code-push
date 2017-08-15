//
//  JWTClaim.h
//  JWT
//
//  Created by Lobanov Dmitry on 13.02.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface JWTClaim : NSObject

+ (NSString *)name;
+ (instancetype)claimByName:(NSString *)name;
+ (BOOL)verifyValue:(NSObject *)value withTrustedValue:(NSObject *)trustedValue;
- (BOOL)verifyValue:(NSObject *)value withTrustedValue:(NSObject *)trustedValue;

@end
