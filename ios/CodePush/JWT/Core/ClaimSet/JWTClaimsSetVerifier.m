//
//  JWTClaimsSetVerifier.m
//  JWT
//
//  Created by Lobanov Dmitry on 13.02.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTClaimsSetVerifier.h"
#import "JWTClaimsSetSerializer.h"
#import "JWTClaim.h"

@implementation JWTClaimsSetVerifier

+ (BOOL)verifyDictionary:(NSDictionary *)dictionary withTrustedDictionary:(NSDictionary *)trustedDictionary byKey:(NSString *)key {
    NSObject *value = dictionary[key];
    NSObject *trustedValue = trustedDictionary[key];
    
    BOOL result = YES;
    
    if (trustedValue) {
        result = [[JWTClaim claimByName:key] verifyValue:value withTrustedValue:trustedValue];
    }
    
    return result;
}

+ (BOOL)verifyClaimsSetDictionary:(NSDictionary *)theClaimsSetDictionary withTrustedClaimsSetDictionary:(NSDictionary *)trustedClaimsSetDictionary {
    
    NSArray *claimsSets = [JWTClaimsSetSerializer claimsSetKeys];
    
    if (!trustedClaimsSetDictionary) {
        return YES;
    }
    
    if (!theClaimsSetDictionary) {
        return NO;
    }
    
    BOOL result = YES;
    
    for (NSString *key in claimsSets) {
        result = result && [self verifyDictionary:theClaimsSetDictionary withTrustedDictionary:trustedClaimsSetDictionary byKey:key];
    }
    
    return result;
}


+ (BOOL)verifyClaimsSet:(JWTClaimsSet *)theClaimsSet withTrustedClaimsSet:(JWTClaimsSet *)trustedClaimsSet {
    
    NSDictionary *dictionary = [JWTClaimsSetSerializer dictionaryWithClaimsSet:theClaimsSet];
    
    NSDictionary *trustedDictionary = [JWTClaimsSetSerializer dictionaryWithClaimsSet:trustedClaimsSet];
    
    NSArray *claimsSets = [JWTClaimsSetSerializer claimsSetKeys];
    
    BOOL result = YES;
    for (NSString *key in claimsSets) {
        result = result && [self verifyDictionary:dictionary withTrustedDictionary:trustedDictionary byKey:key];
    }
    
    return result;
}

+ (BOOL)verifyClaimsSetDictionary:(NSDictionary *)theClaimsSetDictionary withTrustedClaimsSet:(JWTClaimsSet *)trustedClaimsSet {
    NSDictionary *trustedDictionary = [JWTClaimsSetSerializer dictionaryWithClaimsSet:trustedClaimsSet];

    return [self verifyClaimsSetDictionary:theClaimsSetDictionary withTrustedClaimsSetDictionary:trustedDictionary];
}

@end