//
//  JWTAlgorithmHS512.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonHMAC.h>

#import "JWTAlgorithmHS256.h"

@implementation JWTAlgorithmHS256

- (size_t)ccSHANumberDigestLength {
    return CC_SHA256_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA256;
}

- (NSString *)name;
{
    return @"HS256";
}

@end