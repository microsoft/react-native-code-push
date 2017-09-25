//
//  JWTAlgorithmNoneSpec.m
//  JWT
//
//  Created by Lobanov Dmitry on 16.10.15.
//  Copyright © 2015 Karma. All rights reserved.
//

#import <Kiwi/Kiwi.h>
#import <Base64/MF_Base64Additions.h>

#import "JWTAlgorithmNone.h"

static NSString *dataAlgorithmKey = @"algorithm";
static NSString *algorithmBehavior = @"algorithmNoneBehaviour";

SHARED_EXAMPLES_BEGIN(JWTAlgorithmNoneSpecExamples)

sharedExamplesFor(algorithmBehavior, ^(NSDictionary *data) {
    __block JWTAlgorithmNone *algorithm;
    
    beforeEach(^{
        algorithm = data[dataAlgorithmKey];
    });
    
    it(@"name is none", ^{
        [[algorithm.name should] equal:@"none"];
    });
    
    it(@"should not encode payload and return emptry signature instead", ^{
        NSData *encodedPayload = [algorithm encodePayload:@"payload" withSecret:@"secret"];
        [[[encodedPayload base64Encoding] should] equal:@""];
    });
    
    it(@"should not encode payload data and return emptry signature instead", ^{
        NSData *payloadData = [NSData dataWithBase64String:[@"payload" base64String]];
        NSData *secretData = [NSData dataWithBase64String:[@"secret" base64String]];
        
        NSData *encodedPayload = [algorithm encodePayloadData:payloadData withSecret:secretData];
        [[[encodedPayload base64Encoding] should] equal:@""];
    });
    
    it(@"should not verify JWT with a secret provided", ^{
        NSString *secret = @"secret";
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should not verify JWT with a signature provided", ^{
        NSString *secret = nil;
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = @"signed";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beFalse];
    });
    
    it(@"should verify JWT with no signature and no secret provided", ^{
        NSString *secret = nil;
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKey:secret]) should] beTrue];
    });
    
    it(@"should not verify JWT with a secret data provided", ^{
        NSString *secret = @"secret";
        NSData *secretData = [NSData dataWithBase64String:[secret base64String]];
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
    
    it(@"should not verify JWT with a signature data provided", ^{
        NSString *secret = nil;
        NSData *secretData = [NSData dataWithBase64String:[secret base64String]];
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = @"signed";
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beFalse];
    });
    
    it(@"should verify JWT with no signature and no secret data provided", ^{
        NSString *secret = nil;
        NSData *secretData = [NSData dataWithBase64String:[secret base64String]];
        NSString *signingInput = @"eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
        NSString *signature = nil;
        
        [[theValue([algorithm verifySignedInput:signingInput withSignature:signature verificationKeyData:secretData]) should] beTrue];
    });
});

SHARED_EXAMPLES_END

SPEC_BEGIN(JWTAlgorithmNoneSpec)

context(@"Clean", ^{
    itBehavesLike(algorithmBehavior, @{dataAlgorithmKey: [[JWTAlgorithmNone alloc] init]});
});

SPEC_END