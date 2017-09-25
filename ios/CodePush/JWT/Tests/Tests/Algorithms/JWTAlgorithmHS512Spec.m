//
//  JWTAlgorithmHS512.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright 2013 Karma. All rights reserved.
//

#import <Kiwi/Kiwi.h>
#import <Base64/MF_Base64Additions.h>

#import <JWT/JWTAlgorithmHSBase.h>

static NSString *dataAlgorithmKey = @"algorithm";
static NSString *algorithmBehavior = @"algorithmHS512Behaviour";

SHARED_EXAMPLES_BEGIN(JWTAlgorithmHS512SpecExamples)

__block JWTAlgorithmHSBase *algorithm;

sharedExamplesFor(algorithmBehavior, ^(NSDictionary *data) {
    beforeEach(^{
        algorithm = data[dataAlgorithmKey];
    });
    
    it(@"name is HS512", ^{
        [[algorithm.name should] equal:@"HS512"];
    });
    
    it(@"HMAC encodes the payload using SHA512", ^{
        NSData *encodedPayload = [algorithm encodePayload:@"payload" withSecret:@"secret"];
        [[[encodedPayload base64String] should] equal:@"KR3aqiPK+jqq4cl1U5H0vvNbvby5JzmlYYpciW9lINKw0o0tKYfayXR54xIUpR2Wz86voo5GpPlhtjxGNSoYng=="];
    });
    
    it(@"HMAC encodes the payload data using SHA512", ^{
        NSData *payloadData = [NSData dataWithBase64String:[@"payload" base64String]];
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        
        NSData *encodedPayload = [algorithm encodePayloadData:payloadData withSecret:secretData];
        [[[encodedPayload base64String] should] equal:@"KR3aqiPK+jqq4cl1U5H0vvNbvby5JzmlYYpciW9lINKw0o0tKYfayXR54xIUpR2Wz86voo5GpPlhtjxGNSoYng=="];
    });
    
    it(@"should verify JWT with valid signature and secret", ^{
        NSString *secret = @"secret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"SerC5MWQIs3fRH6ZD7gKKbq51TsyydXTvl23WpD9sA085SzQ7pK6M0TnYjFITNUkwuniGG5Is2OKJCEIHPn1Kg";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beTrue];
    });
    
    it(@"should fail to verify JWT with invalid secret", ^{
        NSString *secret = @"notTheSecret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"SerC5MWQIs3fRH6ZD7gKKbq51TsyydXTvl23WpD9sA085SzQ7pK6M0TnYjFITNUkwuniGG5Is2OKJCEIHPn1Kg";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should fail to verify JWT with invalid signature", ^{
        NSString *secret = @"secret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should verify JWT with valid signature and secret data", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"SerC5MWQIs3fRH6ZD7gKKbq51TsyydXTvl23WpD9sA085SzQ7pK6M0TnYjFITNUkwuniGG5Is2OKJCEIHPn1Kg";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beTrue];
    });
    
    it(@"should fail to verify JWT with invalid secret data", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"notTheSecret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"SerC5MWQIs3fRH6ZD7gKKbq51TsyydXTvl23WpD9sA085SzQ7pK6M0TnYjFITNUkwuniGG5Is2OKJCEIHPn1Kg";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
    
    it(@"should fail to verify JWT with invalid signature data", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
});

SHARED_EXAMPLES_END

SPEC_BEGIN(JWTAlgorithmHS512Spec)
context(@"HSBased", ^{
    itBehavesLike(algorithmBehavior, @{dataAlgorithmKey: [JWTAlgorithmHSBase algorithm512]});
});
SPEC_END
