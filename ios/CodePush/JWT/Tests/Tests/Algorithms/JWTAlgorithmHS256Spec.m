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
static NSString *algorithmBehavior = @"algorithmHS256Behaviour";

SHARED_EXAMPLES_BEGIN(JWTAlgorithmHS256SpecExamples)

sharedExamplesFor(algorithmBehavior, ^(NSDictionary *data) {
    __block JWTAlgorithmHSBase *algorithm;
    
    beforeEach(^{
        algorithm = data[dataAlgorithmKey];
    });
    
    it(@"name is HS256", ^{
        [[algorithm.name should] equal:@"HS256"];
    });
    
    it(@"HMAC encodes the payload canonically", ^{
        NSString *payload = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *secret = @"secret";
        NSString *signature = @"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        
        [[theValue([algorithm verifySignedInput:payload withSignature:signature verificationKey:secret]) should] beTrue];
    });
    
    it(@"HMAC encodes the payload using SHA256", ^{
        NSData *encodedPayload = [algorithm encodePayload:@"payload" withSecret:@"secret"];
        [[[encodedPayload base64String] should] equal:@"uC/LeRrOxXhZuYm0MKgmSIzi5Hn9+SMmvQoug3WkK6Q="];
    });
    
    it(@"HMAC encodes the payload data using SHA256", ^{
        NSData *payloadData = [NSData dataWithBase64String:[@"payload" base64String]];
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        
        NSData *encodedPayload = [algorithm encodePayloadData:payloadData withSecret:secretData];
        [[[encodedPayload base64String] should] equal:@"uC/LeRrOxXhZuYm0MKgmSIzi5Hn9+SMmvQoug3WkK6Q="];
    });
    
    it(@"should verify JWT with valid signature and secret", ^{
        NSString *secret = @"secret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beTrue];
    });
    
    it(@"should fail to verify JWT with invalid secret", ^{
        NSString *secret = @"notTheSecret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should fail to verify JWT with invalid signature", ^{
        NSString *secret = @"secret";
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should verify JWT with valid signature and secret Data", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beTrue];
    });
    
    it(@"should fail to verify JWT with invalid secret", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"notTheSecret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = @"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
    
    it(@"should fail to verify JWT with invalid signature", ^{
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        NSString *signingInput = @"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
});

SHARED_EXAMPLES_END

SPEC_BEGIN(JWTAlgorithmHS256Spec)

context(@"HSBased", ^{
    itBehavesLike(algorithmBehavior, @{dataAlgorithmKey: [JWTAlgorithmHSBase algorithm256]});
});

SPEC_END
