//
//  JWTAlgorithmHS512.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonHMAC.h>

#import "JWTAlgorithmHS512.h"

@implementation JWTAlgorithmHS512

- (size_t)ccSHANumberDigestLength {
    return CC_SHA512_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA512;
}

- (NSString *)name;
{
    return @"HS512";
}

@end