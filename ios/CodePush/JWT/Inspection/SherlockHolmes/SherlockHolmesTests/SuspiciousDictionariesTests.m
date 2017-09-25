//
//  SuspiciousDictionariesTests.m
//  SherlockHolmes
//
//  Created by Lobanov Dmitry on 06.06.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <XCTest/XCTest.h>
#import <Kiwi/Kiwi.h>
#import <JWT/JWT.h>
#import <JWT/JWTAlgorithmFactory.h>
#import <JWT/JWTClaimsSetSerializer.h>
#import <Base64/MF_Base64Additions.h>

SPEC_BEGIN(SuspiciousDictionariesTests)

// iPhone 5, 9.3, Xcode 7.3.1
// Cmd-U - U
// Cmd-K - K
// Sequence: (U-U-U-K-U-K-U-) or shuffle
// Repeat until failure.
describe(@"encoding", ^{
    context(@"claims set", ^{
        it(@"decode claims set and verify it correctly", ^{
            NSString *algorithmName = @"HS256";
            NSString *secret = @"secret";
            JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
            claimsSet.issuer = @"Facebook";
            claimsSet.subject = @"Token";
            claimsSet.audience = @"http://yourkarma.com";
            claimsSet.expirationDate = [NSDate distantFuture];
            claimsSet.notBeforeDate = [NSDate distantPast];
            claimsSet.issuedAt = [NSDate date];
            claimsSet.identifier = @"thisisunique";
            claimsSet.type = @"test";
            
            
            NSDictionary *payload = [JWTClaimsSetSerializer dictionaryWithClaimsSet:claimsSet];//@{@"key": @"value"};
            NSDictionary *headers = @{@"header" : @"value"};
            
            NSMutableDictionary *allHeaders = [@{@"typ":@"JWT", @"alg":algorithmName} mutableCopy];
            
            [allHeaders addEntriesFromDictionary:headers];
            
            NSString *headerSegment = [[NSJSONSerialization dataWithJSONObject:allHeaders options:0 error:nil] base64UrlEncodedString];
            
            NSString *payloadSegment = [[NSJSONSerialization dataWithJSONObject:payload options:0 error:nil] base64UrlEncodedString];
            
            NSString *signingInput = [@[headerSegment, payloadSegment] componentsJoinedByString:@"."];
            
            NSString *signingOutput = [[[JWTAlgorithmFactory algorithmByName:algorithmName] encodePayload:signingInput withSecret:secret] base64UrlEncodedString];
            
            NSString *jwt = [@[headerSegment, payloadSegment, signingOutput] componentsJoinedByString:@"."];
            JWTClaimsSet *trustedClaimsSet = claimsSet.copy;
            trustedClaimsSet.expirationDate = [NSDate date];
            trustedClaimsSet.notBeforeDate = [NSDate date];
            trustedClaimsSet.issuedAt = [NSDate date];
            JWTBuilder *builder = [JWT decodeMessage:jwt].secret(secret).claimsSet(trustedClaimsSet).algorithmName(algorithmName);
            NSDictionary *info = builder.decode;
            
            NSLog(@"info is: %@", info);
            NSLog(@"error is: %@", builder.jwtError);
            
            BOOL noError = builder.jwtError == nil;
            
            [[@(noError) should] equal:@(YES)];
            [[info[@"payload"] should] equal:payload];
            [[info[@"header"] should] equal:allHeaders];
        });
    });
});

SPEC_END
