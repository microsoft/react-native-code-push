//
//  JWTAlgorithmHS384.m
//  JWT
//
//  Created by Lobanov Dmitry on 06-02-16.
//  Copyright (c) 2016 lolgear. All rights reserved.
//

#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonHMAC.h>

#import "JWTAlgorithmHS384.h"

@implementation JWTAlgorithmHS384

- (size_t)ccSHANumberDigestLength {
    return CC_SHA384_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA384;
}

- (NSString *)name;
{
    return @"HS384";
}

@end