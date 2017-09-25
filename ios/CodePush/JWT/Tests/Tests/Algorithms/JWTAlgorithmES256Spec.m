//
//  JWTAlgorithmES256Spec.m
//  Tests
//
//  Created by Lobanov Dmitry on 15.02.17.
//
//
#import <Kiwi/Kiwi.h>
#import "JWT.h"
#import "JWTAlgorithmRSBase.h"
#import "JWTCryptoKeyExtractor.h"
#import "JWTCryptoSecurity.h"
#import "JWTCryptoKey.h"
static NSString *algorithmBehavior = @"algorithmES256Behaviour";
static NSString *dataAlgorithmKey = @"dataAlgorithmKey";
SHARED_EXAMPLES_BEGIN(JWTAlgorithmES256Examples)
sharedExamplesFor(algorithmBehavior, ^(NSDictionary *data) {
    __block id<JWTAlgorithm> algorithm;
    __block NSString *valid_token = nil;
    __block NSString *algorithmName;
    __block NSDictionary *headerAndPayloadDictionary;
    __block NSDictionary *headerDictionary;
    __block NSDictionary *payloadDictionary;
    __block void (^assertDecodedDictionary)(NSDictionary *);
    __block NSString *privateKeyPemEncoded;
    __block NSString *publicKeyPemEncoded;
    beforeAll(^{
        algorithm = data[dataAlgorithmKey];
        algorithmName = JWTAlgorithmNameRS256;
        
        NSString *privateKeyPemString = [JWTCryptoSecurity keyFromPemFileWithName:@"ec256-private"];
        NSString *publicKeyPemString = [JWTCryptoSecurity keyFromPemFileWithName:@"ec256-public"];
        privateKeyPemEncoded = privateKeyPemString;
        publicKeyPemEncoded = publicKeyPemString;
        
        payloadDictionary = @{@"hello": @"world"};
        headerDictionary = @{@"alg":algorithmName, @"typ":@"JWT"};
        
        headerAndPayloadDictionary = @{JWTCodingResultHeaders : headerDictionary, JWTCodingResultPayload : payloadDictionary};
        
        assertDecodedDictionary = ^(NSDictionary *decodedDictionary) {
            [[(decodedDictionary) shouldNot] beNil];
            [[(decodedDictionary) should] equal:headerAndPayloadDictionary];
        };
        
        NSError *error = nil;
        JWTCryptoKeyBuilder *builderForEC = [JWTCryptoKeyBuilder new].keyTypeEC;
        id<JWTCryptoKeyProtocol> signKey = [[JWTCryptoKeyPrivate alloc] initWithPemEncoded:privateKeyPemString parameters:@{[JWTCryptoKey parametersKeyBuilder] : builderForEC} error:&error];
        id<JWTAlgorithmDataHolderProtocol> dataHolder = [JWTAlgorithmRSFamilyDataHolder new].signKey(signKey).algorithmName(algorithmName);
        JWTCodingBuilder *encodingBuilder = [JWTEncodingBuilder encodePayload:payloadDictionary].addHolder(dataHolder);
        JWTCodingResultType *encoded = encodingBuilder.result;
        if (encoded.successResult) {
            valid_token = encoded.successResult.encoded;
            NSLog(@"success result: %@", valid_token);
        }
        else {
            NSLog(@"%@ error: %@", self.debugDescription, encoded.errorResult.error);
        }
    });
    context(@"first", ^{
        it(@"Empty test", ^{
            // for beforeAll invocation
        });
        pending(@"Should decode well", ^{
            JWTCryptoKeyBuilder *builderForEC = [JWTCryptoKeyBuilder new].keyTypeEC;
            id<JWTCryptoKeyProtocol> verifyKey = [[JWTCryptoKeyPublic alloc] initWithPemEncoded:publicKeyPemEncoded parameters:@{[JWTCryptoKey parametersKeyBuilder] : builderForEC} error:nil];
            id<JWTAlgorithmDataHolderProtocol> dataHolder = [JWTAlgorithmRSFamilyDataHolder new].verifyKey(verifyKey).algorithmName(algorithmName);
            JWTCodingBuilder *decodingBuilder = [JWTDecodingBuilder decodeMessage:valid_token].addHolder(dataHolder);
            JWTCodingResultType *result = decodingBuilder.result;
            if (result.successResult) {
                assertDecodedDictionary(result.successResult.headerAndPayloadDictionary);
            }
            else {
                NSLog(@"%@ error: %@", self.debugDescription, result.errorResult.error);
            }
        });
    });
});
SHARED_EXAMPLES_END

SPEC_BEGIN(JWTAlgorithmES256Spec)

context(@"ESBased", ^{
    itBehavesLike(algorithmBehavior, @{dataAlgorithmKey: [JWTAlgorithmRSBase algorithm256]});
});

SPEC_END
