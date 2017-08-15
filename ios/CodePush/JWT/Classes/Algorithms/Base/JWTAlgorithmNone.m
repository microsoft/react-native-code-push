//
//  JWTAlgorithmNone.m
//  JWT
//
//  Created by Lobanov Dmitry on 16.10.15.
//  Copyright © 2015 Karma. All rights reserved.
//

#import "JWTAlgorithmNone.h"
NSString *const JWTAlgorithmNameNone = @"none";

@implementation JWTAlgorithmNone

- (NSString *)name {
    return JWTAlgorithmNameNone;
}

- (NSData *)encodePayload:(NSString *)theString withSecret:(NSString *)theSecret {
    return [NSData data];
}

- (NSData *)encodePayloadData:(NSData *)theStringData withSecret:(NSData *)theSecretData
{
    return [NSData data];
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKey:(NSString *)verificationKey
{
    //if a secret is provided, this isn't the None algorithm
    if (verificationKey && ![verificationKey isEqualToString:@""]) {
        return NO;
    }
    
    //If the signature isn't blank, this isn't the None algorithm
    if (signature && ![signature isEqualToString:@""]) {
        return NO;
    }
    
    return YES;
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKeyData:(NSData *)verificationKeyData
{
    if (verificationKeyData && verificationKeyData.length > 0) {
        return NO;
    }
    
    if (signature && signature.length > 0) {
        return NO;
    }
    
    return YES;
}

@end
