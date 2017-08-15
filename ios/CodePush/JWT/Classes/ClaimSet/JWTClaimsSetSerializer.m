//
//  JWTClaimsSetSerializer.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import "JWTClaimsSetSerializer.h"

@implementation JWTClaimsSetSerializer

+ (NSArray *)claimsSetKeys
{
    return @[@"iss", @"sub", @"aud", @"exp", @"nbf", @"iat", @"jti", @"typ"];
}

+ (NSDictionary *)dictionaryWithClaimsSet:(JWTClaimsSet *)theClaimsSet;
{
    NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
    [self dictionary:dictionary setObjectIfNotNil:theClaimsSet.issuer forKey:@"iss"];
    [self dictionary:dictionary setObjectIfNotNil:theClaimsSet.subject forKey:@"sub"];
    [self dictionary:dictionary setObjectIfNotNil:theClaimsSet.audience forKey:@"aud"];
    [self dictionary:dictionary setDateIfNotNil:theClaimsSet.expirationDate forKey:@"exp"];
    [self dictionary:dictionary setDateIfNotNil:theClaimsSet.notBeforeDate forKey:@"nbf"];
    [self dictionary:dictionary setDateIfNotNil:theClaimsSet.issuedAt forKey:@"iat"];
    [self dictionary:dictionary setObjectIfNotNil:theClaimsSet.identifier forKey:@"jti"];
    [self dictionary:dictionary setObjectIfNotNil:theClaimsSet.type forKey:@"typ"];
    return [dictionary copy];
}

+ (JWTClaimsSet *)claimsSetWithDictionary:(NSDictionary *)theDictionary;
{
    JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
    claimsSet.issuer = [theDictionary objectForKey:@"iss"];
    claimsSet.subject = [theDictionary objectForKey:@"sub"];
    claimsSet.audience = [theDictionary objectForKey:@"aud"];
    claimsSet.expirationDate = [NSDate dateWithTimeIntervalSince1970:[[theDictionary objectForKey:@"exp"] doubleValue]];
    claimsSet.notBeforeDate = [NSDate dateWithTimeIntervalSince1970:[[theDictionary objectForKey:@"nbf"] doubleValue]];
    claimsSet.issuedAt = [NSDate dateWithTimeIntervalSince1970:[[theDictionary objectForKey:@"iat"] doubleValue]];
    claimsSet.identifier = [theDictionary objectForKey:@"jti"];
    claimsSet.type = [theDictionary objectForKey:@"typ"];
    return claimsSet;
}

+ (void)dictionary:(NSMutableDictionary *)theDictionary setObjectIfNotNil:(id)theObject forKey:(id<NSCopying>)theKey;
{
    if (!theObject)
        return;
    
    [theDictionary setObject:theObject forKey:theKey];
}

+ (void)dictionary:(NSMutableDictionary *)theDictionary setDateIfNotNil:(NSDate*)date forKey:(id<NSCopying>)theKey;
{
    if (!date)
        return;
    NSNumber* value = @((unsigned long)[date timeIntervalSince1970]);
    
    [theDictionary setObject:value forKey:theKey];
}


@end
