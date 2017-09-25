//
//  JWTClaimsSerializerSpec.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright 2013 Karma. All rights reserved.
//

#import <Kiwi/Kiwi.h>

#import "JWTClaimsSetSerializer.h"

SPEC_BEGIN(JWTClaimsSerializerSpec)

context(@"serialization", ^{
    __block JWTClaimsSet *claimsSet;
    __block NSDictionary *serialized;

    __block NSInteger expirationDateTS = 1234567;
    __block NSInteger notBeforeDateTS = 1234321;
    __block NSInteger issuedAtTS = 1234333;
    
    beforeEach(^{
        
        claimsSet = [[JWTClaimsSet alloc] init];
        claimsSet.issuer = @"Facebook";
        claimsSet.subject = @"Token";
        claimsSet.audience = @"https://jwt.io";
        claimsSet.expirationDate = [NSDate dateWithTimeIntervalSince1970:expirationDateTS];
        claimsSet.notBeforeDate = [NSDate dateWithTimeIntervalSince1970:notBeforeDateTS];
        claimsSet.issuedAt = [NSDate dateWithTimeIntervalSince1970:issuedAtTS];
        claimsSet.identifier = @"thisisunique";
        claimsSet.type = @"test";
        claimsSet.scope = @"https://www.googleapis.com/auth/devstorage.read_write";
        serialized = [JWTClaimsSetSerializer dictionaryWithClaimsSet:claimsSet];
        
    });

    it(@"number of serialized values", ^{
        [[serialized should] haveCountOf:9];
    });
    
    it(@"serializes the issuer property", ^{
        [[serialized should] haveValue:claimsSet.issuer forKey:@"iss"];
    });
    
    it(@"serializes the subject property", ^{
        [[serialized should] haveValue:claimsSet.subject forKey:@"sub"];
    });
    
    it(@"serializes the audience property", ^{
        [[serialized should] haveValue:claimsSet.audience forKey:@"aud"];
    });
    
    it(@"serializes the expiration date property", ^{
        [[serialized should] haveValue:@(expirationDateTS) forKey:@"exp"];
    });
    
    it(@"serializes the not before date property", ^{
        [[serialized should] haveValue:@(notBeforeDateTS)  forKey:@"nbf"];
    });
    
    it(@"serializes the issued at property", ^{
        [[serialized should] haveValue:@(issuedAtTS) forKey:@"iat"];
    });
    
    it(@"serializes the JWT ID property", ^{
        [[serialized should] haveValue:claimsSet.identifier forKey:@"jti"];

    });

    it(@"serializes the type property", ^{
        [[serialized should] haveValue:claimsSet.type forKey:@"typ"];

    });

    it(@"serializes the scope property", ^{
        [[serialized should] haveValue:claimsSet.scope forKey:@"scope"];

    });
});

context(@"deserialization", ^{
    __block JWTClaimsSet *deserialized;
    __block NSDictionary *serialized;
    
    beforeEach(^{
        serialized = @{
            @"iss": @"Facebook",
            @"sub": @"Token",
            @"aud": @"https://jwt.io",
            @"exp": @(64092211200),
            @"nbf": @(-62135769600),
            @"iat": @(1370005175),
            @"jti": @"thisisunique",
            @"typ": @"test",
            @"scope": @"https://www.googleapis.com/auth/devstorage.read_write"
        };
        deserialized = [JWTClaimsSetSerializer claimsSetWithDictionary:serialized];
    });
    
    it(@"deserializes the issuer property", ^{
        [[deserialized.issuer should] equal:[serialized objectForKey:@"iss"]];
    });
    
    it(@"deserializes the subject property", ^{
        [[deserialized.subject should] equal:[serialized objectForKey:@"sub"]];
    });
    
    it(@"deserializes the audience property", ^{
        [[deserialized.audience should] equal:[serialized objectForKey:@"aud"]];
    });
    
    it(@"deserializes the expiration date property", ^{
        [[deserialized.expirationDate should] equal:[NSDate dateWithTimeIntervalSince1970:[[serialized objectForKey:@"exp"] doubleValue]]];
    });
    
    it(@"deserializes the not before date property", ^{
        [[deserialized.notBeforeDate should] equal:[NSDate dateWithTimeIntervalSince1970:[[serialized objectForKey:@"nbf"] doubleValue]]];
    });
    
    it(@"deserializes the issued at property", ^{
        [[deserialized.issuedAt should] equal:[NSDate dateWithTimeIntervalSince1970:[[serialized objectForKey:@"iat"] doubleValue]]];
    });
    
    it(@"deserializes the JWT ID property", ^{
        [[deserialized.identifier should] equal:[serialized objectForKey:@"jti"]];
    });

    it(@"deserializes the type property", ^{
        [[deserialized.type should] equal:[serialized objectForKey:@"typ"]];
    });

    it(@"deserializes the scope property", ^{
        [[deserialized.scope should] equal:[serialized objectForKey:@"scope"]];
    });
});
        

SPEC_END


