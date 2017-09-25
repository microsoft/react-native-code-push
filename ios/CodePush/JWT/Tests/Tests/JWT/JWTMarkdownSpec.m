//
//  JWTMarkdownSpec.m
//  JWT Tests
//
//  Created by Lobanov Dmitry on 02.12.16.
//
//

#import <Kiwi/Kiwi.h>

#import "JWT.h"
#import "JWTCryptoKeyExtractor.h"

SPEC_BEGIN(JWTMarkdownSpec)
describe(@"markdown examples", ^{
    context(@"VersionThree", ^{
        it(@"API should work well with Pem keys loading", ^{
            NSString *privatePemFilename = @"rs256-private";
            NSString *publicPemFilename = @"rs256-public";
            NSString *passphrase = @"password";
            NSString *(^loadKey)(NSString *, NSBundle *) = ^NSString *(NSString *name, NSBundle *bundle){
                 NSURL *fileURL = [bundle URLForResource:name withExtension:@"pem"];
                 NSError *error = nil;
                 NSString *fileContent = [NSString stringWithContentsOfURL:fileURL encoding:NSUTF8StringEncoding error:&error];
                 if (error) {
                     NSLog(@"%@ error: %@", self.debugDescription, error);
                     return nil;
                 }
                return fileContent;
            };
            NSBundle *bundle = [NSBundle bundleForClass:self.class];
            NSString *publicPemKey = loadKey(publicPemFilename, bundle);
            NSString *privatePemKey = loadKey(privatePemFilename, bundle);
            
            // sign and verify
            {
                NSString *algorithmName = @"RS256";
                id <JWTAlgorithmDataHolderProtocol> signDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor privateKeyWithPEMBase64].type).privateKeyCertificatePassphrase(passphrase).algorithmName(algorithmName).secret(privatePemKey);
                
                id <JWTAlgorithmDataHolderProtocol> verifyDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor publicKeyWithPEMBase64].type).algorithmName(algorithmName).secret(publicPemKey);
                
                NSDictionary *payloadDictionary = @{@"hello": @"world"};
                
                JWTCodingBuilder *signBuilder = [JWTEncodingBuilder encodePayload:payloadDictionary].addHolder(signDataHolder);
                JWTCodingResultType *signResult = signBuilder.result;
                NSString *token = nil;
                if (signResult.successResult) {
                    // success
                    NSLog(@"%@ success: %@", self.debugDescription, signResult.successResult.encoded);
                    token = signResult.successResult.encoded;
                }
                else {
                    // error
                    NSLog(@"%@ error: %@", self.debugDescription, signResult.errorResult.error);
                }
                
                // verify
                if (token == nil) {
                    NSLog(@"something wrong");
                }
                
                JWTCodingBuilder *verifyBuilder = [JWTDecodingBuilder decodeMessage:token].addHolder(verifyDataHolder);
                JWTCodingResultType *verifyResult = verifyBuilder.result;
                if (verifyResult.successResult) {
                    // success
                    NSLog(@"%@ success: %@", self.debugDescription, verifyResult.successResult.payload);
                    token = verifyResult.successResult.encoded;
                }
                else {
                    // error
                    NSLog(@"%@ error: %@", self.debugDescription, verifyResult.errorResult.error);
                }
            }
        });
        it(@"API should show work with chains moderate", ^{
            JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
            // fill it
            claimsSet.issuer = @"Facebook";
            claimsSet.subject = @"Token";
            claimsSet.audience = @"https://jwt.io";

            // possible that your algorithm has several secrets.
            // you don't know which secret to use.
            // but you want to decode it.
            NSString *firstSecret = @"first";
            NSArray *manySecrets = @[@"second", @"third", @"forty two"];
            // translate to data
            NSArray *manySecretsData = @[];
            for (NSString *secret in manySecrets) {
                NSData *secretData = [JWTBase64Coder dataWithBase64UrlEncodedString:secret];
                if (secret) {
                    manySecretsData = [manySecretsData arrayByAddingObject:secretData];
                }
            }

            NSString *algorithmName = JWTAlgorithmNameHS384;

            id <JWTAlgorithmDataHolderProtocol> firstHolder = [JWTAlgorithmHSFamilyDataHolder new].algorithmName(algorithmName).secret(firstSecret);

            // lets create chain
            JWTAlgorithmDataHolderChain *chain = [JWTAlgorithmDataHolderChain chainWithHolder:firstHolder];

            // and lets populate chain with secrets.
            NSLog(@"chain has: %@", chain.debugDescription);

            JWTAlgorithmDataHolderChain *expandedChain = [chain chainByPopulatingAlgorithm:firstHolder.internalAlgorithm withManySecretData:manySecretsData];

            // now we have expanded chain with many secrets and one algorithm.
            NSLog(@"expanded chain has: %@", expandedChain.debugDescription);

        });
        it(@"API should show work with chains simple", ^{
            JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
            // fill it
            claimsSet.issuer = @"Facebook";
            claimsSet.subject = @"Token";
            claimsSet.audience = @"https://jwt.io";

            // create token
            NSString *token = @"...";

            // possible that algorithm could return error.
            // you could try use algorithm and data chain.

            NSString *firstSecret = @"first";
            NSString *firstAlgorithmName = JWTAlgorithmNameHS384;

            id <JWTAlgorithmDataHolderProtocol> firstHolder = [JWTAlgorithmHSFamilyDataHolder new].algorithmName(firstAlgorithmName).secret(firstSecret);

            id <JWTAlgorithmDataHolderProtocol> errorHolder = [JWTAlgorithmNoneDataHolder new];

            // chain together.
            JWTAlgorithmDataHolderChain *chain = [[JWTAlgorithmDataHolderChain alloc] initWithHolders:@[firstHolder, errorHolder]];

            // or add them in builder
            [JWTDecodingBuilder decodeMessage:token].addHolder(firstHolder).addHolder(errorHolder);

            // or add them as chain
            [JWTDecodingBuilder decodeMessage:token].chain(chain);
        });
        it(@"API should api work correctly", ^{
            JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
            // fill it
            claimsSet.issuer = @"Facebook";
            claimsSet.subject = @"Token";
            claimsSet.audience = @"https://jwt.io";

            // encode it
            NSString *secret = @"secret";
            NSString *algorithmName = @"HS384";
            NSDictionary *headers = @{@"custom":@"value"};

            id<JWTAlgorithmDataHolderProtocol>holder = [JWTAlgorithmHSFamilyDataHolder new].algorithmName(algorithmName).secret(secret);

            JWTCodingResultType *result = [JWTEncodingBuilder encodeClaimsSet:claimsSet].headers(headers).addHolder(holder).result;

            NSString *encodedToken = result.successResult.encoded;
            if (result.successResult) {
                // handle encoded result
                NSLog(@"encoded result: %@", result.successResult.encoded);
            }
            else {
                // handle error
                NSLog(@"encode failed, error: %@", result.errorResult.error);
            }

            // decode it
            // you can set any property that you want, all properties are optional
            JWTClaimsSet *trustedClaimsSet = [claimsSet copy];

            NSNumber *options = @(JWTCodingDecodingOptionsNone);
            NSString *yourJwt = encodedToken; // from previous example
            JWTCodingResultType *decodedResult = [JWTDecodingBuilder decodeMessage:yourJwt].claimsSet(claimsSet).addHolder(holder).options(options).and.result;

            if (decodedResult.successResult) {
                // handle decoded result
                NSLog(@"decoded result: %@", decodedResult.successResult.headerAndPayloadDictionary);
                NSLog(@"headers: %@", decodedResult.successResult.headers);
                NSLog(@"payload: %@", decodedResult.successResult.payload);
            }
            else {
                // handle error
                NSLog(@"decode failed, error: %@", decodedResult.errorResult.error);
            }
        });
    });
    it(@"RS coding should work correctly", ^{
        // Test example.
        // Encode
        NSDictionary *payload = @{@"payload" : @"hidden_information"};
        NSString *algorithmName = @"RS256";
        NSString *fileName = @"Test certificate and private key 1";

        NSString *filePath = [[NSBundle bundleForClass:[self class]] pathForResource:fileName ofType:@"p12"];
        NSData *privateKeySecretData = [NSData dataWithContentsOfFile:filePath];

        NSString *passphraseForPrivateKey = @"password";

        JWTBuilder *builder = [JWTBuilder encodePayload:payload].secretData(privateKeySecretData).privateKeyCertificatePassphrase(passphraseForPrivateKey).algorithmName(algorithmName);
        NSString *token = builder.encode;

        // check error
        if (builder.jwtError) {
            // error occurred.
            NSLog(@"%@ error occured while encoding: %@", self, builder.jwtError);
        }
        else {
            NSLog(@"%@ token: %@", self, token);
        }
        // Decode
        // Suppose, that you get token from previous example. You need a valid public key for a private key in previous example.
        // Private key stored in @"secret_key.p12". So, you need public key for that private key.

        NSString *publicKey = @"..."; // load public key. Or use it as raw string.

        // test example:
        publicKey = @"MIICnTCCAYUCBEReYeAwDQYJKoZIhvcNAQEFBQAwEzERMA8GA1UEAxMIand0LTIwNDgwHhcNMTQwMTI0MTMwOTE2WhcNMzQwMjIzMjAwMDAwWjATMREwDwYDVQQDEwhqd3QtMjA0ODCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKhWb9KXmv45+TKOKhFJkrboZbpbKPJ9Yp12xKLXf8060KfStEStIX+7dCuAYylYWoqiGpuLVVUL5JmHgXmK9TJpzv9Dfe3TAc/+35r8r9IYB2gXUOZkebty05R6PLY0RO/hs2ZhrOozHMo+x216Gwz0CWaajcuiY5Yg1V8VvJ1iQ3rcRgZapk49RNX69kQrGS63gzj0gyHnRtbqc/Ua2kobCA83nnznCom3AGinnlSN65AFPP5jmri0l79+4ZZNIerErSW96mUF8jlJFZI1yJIbzbv73tL+y4i0+BvzsWBs6TkHAp4pinaI8zT+hrVQ2jD4fkJEiRN9lAqLPUd8CNkCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAnqBw3UHOSSHtU7yMi1+HE+9119tMh7X/fCpcpOnjYmhW8uy9SiPBZBl1z6vQYkMPcURnDMGHdA31kPKICZ6GLWGkBLY3BfIQi064e8vWHW7zX6+2Wi1zFWdJlmgQzBhbr8pYh9xjZe6FjPwbSEuS0uE8dWSWHJLdWsA4xNX9k3pr601R2vPVFCDKs3K1a8P/Xi59kYmKMjaX6vYT879ygWt43yhtGTF48y85+eqLdFRFANTbBFSzdRlPQUYa5d9PZGxeBTcg7UBkK/G+d6D5sd78T2ymwlLYrNi+cSDYD6S4hwZaLeEK6h7p/OoG02RBNuT4VqFRu5DJ6Po+C6JhqQ==";

        algorithmName = @"RS256";

        JWTBuilder *decodeBuilder = [JWTBuilder decodeMessage:token].secret(publicKey).algorithmName(algorithmName);
        NSDictionary *envelopedPayload = decodeBuilder.decode;

        // check error
        if (decodeBuilder.jwtError) {
            // error occurred.
            NSLog(@"%@ error occured while decoding %@", self,decodeBuilder.jwtError);
        }
        else {
            NSLog(@"%@ envelopedPayload: %@ ", self, envelopedPayload);
        }
    });
    it(@"fluent example should work correctly", ^{
        // suppose, that you create ClaimsSet
        JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
        // fill it
        claimsSet.issuer = @"Facebook";
        claimsSet.subject = @"Token";
        claimsSet.audience = @"https://jwt.io";

        // encode it
        NSString *secret = @"secret";
        NSString *algorithmName = @"HS384";
        NSDictionary *headers = @{@"custom":@"value"};
        JWTBuilder *encodeBuilder = [JWT encodeClaimsSet:claimsSet];
        NSString *encodedResult = encodeBuilder.secret(secret).algorithmName(algorithmName).headers(headers).encode;

        if (encodeBuilder.jwtError) {
            // handle error
            NSLog(@"encode failed, error: %@", encodeBuilder.jwtError);
        }
        else {
            // handle encoded result
            NSLog(@"encoded result: %@", encodedResult);
        }

        // decode it
        // you can set any property that you want, all properties are optional
        JWTClaimsSet *trustedClaimsSet = [claimsSet copy];

        // decode forced ? try YES
        BOOL decodeForced = NO;
        NSNumber *options = @(decodeForced);
        NSString *yourJwt = encodedResult; // from previous example
        NSString *yourSecret = secret; // from previous example
        JWTBuilder *decodeBuilder = [JWT decodeMessage:yourJwt];
        NSDictionary *decodedResult = decodeBuilder.message(yourJwt).secret(yourSecret).claimsSet(trustedClaimsSet).options(options).decode;
        if (decodeBuilder.jwtError) {
            // handle error
            NSLog(@"decode failed, error: %@", decodeBuilder.jwtError);
        }
        else {
            // handle decoded result
            NSLog(@"decoded result: %@", decodedResult);
        }
    });
});
SPEC_END
