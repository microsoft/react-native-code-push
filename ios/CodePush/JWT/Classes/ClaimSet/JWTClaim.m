//
//  JWTClaim.m
//  JWT
//
//  Created by Lobanov Dmitry on 13.02.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTClaim.h"

// TODO(3.0): Claim aud should check include in collection?
// Add claims specification tests.
// "iss" (Issuer) Claim
// "sub" (Subject) Claim
// "aud" (Audience) Claim
// "exp" (Expiration Time) Claim
// "nbf" (Not Before) Claim
// "iat" (Issued At) Claim
// "jti" (JWT ID) Claim
// "typ" (Type) Claim

@interface JWTClaimIssuer : JWTClaim

@end

@implementation JWTClaimIssuer

+ (NSString *)name {
	return @"iss";
}

+ (BOOL)verifyValue:(NSString *)value withTrustedValue:(NSString *)trustedValue {
    return [trustedValue isEqualToString:value];
}

@end

@interface JWTClaimSubject : JWTClaim

@end

@implementation JWTClaimSubject

+ (NSString *)name {
	return @"sub";
}

+ (BOOL)verifyValue:(NSString *)value withTrustedValue:(NSString *)trustedValue {
    return [trustedValue isEqualToString:value];
}

@end

@interface JWTClaimAudience : JWTClaim

@end

// TODO: add array support later
@implementation JWTClaimAudience

+ (NSString *)name {
	return @"aud";
}

+ (BOOL)verifyValue:(NSString *)value withTrustedValue:(NSString *)trustedValue {
    return [trustedValue isEqualToString:value];
}

@end

@interface JWTClaimExpirationTime : JWTClaim

@end

@implementation JWTClaimExpirationTime

+ (NSString *)name {
	return @"exp";
}

+ (BOOL)verifyValue:(NSDate *)value withTrustedValue:(NSDate *)trustedValue {
    // trustedValue - current date
    // value - expiration date
    // trustedValue < value, so
    return [trustedValue compare:value] == NSOrderedAscending;
}

@end

@interface JWTClaimNotBefore : JWTClaim

@end

@implementation JWTClaimNotBefore

+ (NSString *)name {
	return @"nbf";
}

+ (BOOL)verifyValue:(NSDate *)value withTrustedValue:(NSDate *)trustedValue {
    // trustedValue - current date
    // value - start date
    // value <= trustedValue, so
    // trustedValue >= value, which means:
    // !(trustedValue < value) or NOT OrderedAscending
    return [trustedValue compare:value] != NSOrderedAscending;
}

@end

@interface JWTClaimIssuedAt : JWTClaim

@end

@implementation JWTClaimIssuedAt

+ (NSString *)name {
	return @"iat";
}

+ (BOOL)verifyValue:(NSDate *)value withTrustedValue:(NSDate *)trustedValue {
    // trustedValue - current date
    // value - issued at date
    // value < trustedValue, so
    // trustedValue > value
    return [trustedValue compare:value] == NSOrderedDescending;
}

@end

@interface JWTClaimJWTID : JWTClaim

@end

@implementation JWTClaimJWTID

+ (NSString *)name {
	return @"jti";
}

+ (BOOL)verifyValue:(NSString *)value withTrustedValue:(NSString *)trustedValue {
    return [trustedValue isEqualToString:value];
}

@end

@interface JWTClaimType : JWTClaim

@end

@implementation JWTClaimType

+ (NSString *)name {
    return @"typ";
}

+ (BOOL)verifyValue:(NSString *)value withTrustedValue:(NSString *)trustedValue {
    return [trustedValue isEqualToString:value];
}

@end


@implementation JWTClaim
+ (NSString *)name {
    return @"";
}

+ (NSDictionary *)claimsAndNames {
    static NSDictionary *dictionary = nil;
    return dictionary ? dictionary : (dictionary = @{
		[JWTClaimIssuer name] : [JWTClaimIssuer new],
		[JWTClaimSubject name] : [JWTClaimSubject new],
		[JWTClaimAudience name] : [JWTClaimAudience new],
		[JWTClaimExpirationTime name] : [JWTClaimExpirationTime new],
		[JWTClaimNotBefore name] : [JWTClaimNotBefore new],
		[JWTClaimIssuedAt name] : [JWTClaimIssuedAt new],
		[JWTClaimJWTID name] : [JWTClaimJWTID new],
		[JWTClaimType name] : [JWTClaimType new]
	}, dictionary);
}

+ (instancetype)claimByName:(NSString *)name {    
    return [self claimsAndNames][name];
}

+ (BOOL)verifyValue:(NSObject *)value withTrustedValue:(NSObject *)trustedValue {
	return NO;
}

- (BOOL)verifyValue:(NSObject *)value withTrustedValue:(NSObject *)trustedValue {
    return [self.class verifyValue:value withTrustedValue:trustedValue];
}

@end

