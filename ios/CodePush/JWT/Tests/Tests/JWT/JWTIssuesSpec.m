//
//  JWTIssuesSpec.m
//  Tests
//
//  Created by Lobanov Dmitry on 22.08.17.
//
//

#import <Kiwi/Kiwi.h>

#import "JWT.h"
#import "JWTAlgorithmFactory.h"
#import "JWTClaimsSetSerializer.h"

SPEC_BEGIN(JWTIssuesSpec)
describe(@"Issue examples", ^{
    it(@"RS256 signature verification crashes application #141", ^{
        NSString *publicKey = @"MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM/l2OJO6C6eZWwV4h2FooxI2YZ/XNL+BP7kEB7wiudqi0vGRAheahe+vtazFE+J3V1iy+bwIqLXop571zgj6G0CAwEAAQ==";
        NSString *algorithmName = JWTAlgorithmNameRS256;
        NSString *token = @"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbVZlcnNpb24iOiIxLjAuMCIsImNvbnRlbnRIYXNoIjoiMDJlMzNkZjhlMjUwYzRmOTZlODUwM2VlZDRlMDJlN2MxMWQ1MWVmOGViYzI0MDkyM2NlOWQ0NDcwMWU5Y2YxOSIsImlhdCI6MTUwMzQwNzUxOH0.d1ETmvpXxPl747A8OzIGDy-Tke5D9NRVmnJtTBxx61wLQo7fH14u4XMWChMlkjG_9WAvWOAIVFtMQb9IZuA5QQ";
        id <JWTAlgorithmDataHolderProtocol> verifyDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor publicKeyWithPEMBase64].type).algorithmName(algorithmName).secret(publicKey);
        JWTCodingBuilder *verifyBuilder = [JWTDecodingBuilder decodeMessage:token].addHolder(verifyDataHolder);
        JWTCodingResultType *verifyResult = verifyBuilder.result;
        
        if (verifyResult.successResult) {
            // success
            NSLog(@"%@ success: %@", self.debugDescription, verifyResult.successResult.payload);
        }
        else {
            // error
            NSLog(@"%@ error: %@", self.debugDescription, verifyResult.errorResult.error);
        }
    });
    it(@"RS256 verification fails with valid public key #139", ^{
        NSString *publicKey = @"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlyT86d6Stui8i8ZpzzbFHE+WVx/77fU+rglC6CMAslBr3WZO0xibQJJtBCTEkw7r6LkLaEOTkvHdjE1/cUnAFw4M7iIy238gx5gRoELZ7g+nh9C6v8HuQJovabaOFed+wnayw8D0YV5+JG6HJ4ExOO/3TmAum1yacBAzYFHcxOO/glbJY0/41K1kU7d5bFK9gs7DsMyBOInXDdIiTO9XrmN8zY3zncnsgYiwlrVwm5lfJIBnE38gOWen7EnFossogJqrn84SPao9Kslr9064PJN74AWh1ricU/A1zYH0QAFHSI2WGlyoH9V9ZbOWm8gn1IqCypVyg1YCrwaqThjESQIDAQAB";
        NSString *algorithmName = JWTAlgorithmNameRS256;
        NSString* token = @"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC93d3cuY3JhY2tlZC5jb20iLCJhdWQiOiJjcmFja2VkIGFwcCIsInN1YiI6ImNyYWNrZWQgYXBwIiwiZXhwIjoxNTA0OTEzMTY2LCJ1aWQiOiI3Nzk3MiIsInNhbHQiOiI0NTQ3YjQ0ZTA2OTAyOTE3OTJiMjM0MjcwOWY5NTU4ZmQ2MTE3MGExIiwiYXBwX2FkIjpmYWxzZSwiYXBwX3ZvdGVfY29tbWVudCI6dHJ1ZX0.Bqllr9A2ULTlncb0EKLNjTn0qWKG_8NX6mwjf2S1GdS3JH9D7uGGVioxhHN24OZS5QCN9q6rcuYSQzMn-Vz4fAKtDOQws6LZLm7OFwe7uLYXlrK0w3GIxs6nRuGWGIzyxiwjOcy5Vs0HlKAZF7bE8aDUtW5WpbBz4JvgvKc2kmAc3IAMhxs8zRF0jaaiAye7Z7EMjtztmuW8eUosnwKPSa-P2zC-ElcAA67WJ7otYThlbqYDEnVhHrSxj3i3LdgRk0dwumf1zmIikCxFJBDClIroSat9J3hBYFTs6R8EL1YAUc387H_XXLMItLWeHPIMwmXx5wVzr6G_biWlaSyK1w";
        
        id <JWTAlgorithmDataHolderProtocol> verifyDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor publicKeyWithPEMBase64].type).algorithmName(algorithmName).secret(publicKey);
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
    });
    it(@"Crashing RS256 decoding #112", ^{
        NSString *publicKey = @" MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDdlatRjRjogo3WojgGHFHYLugdUWAY9iR3fy4arWNA1KoS8kVw33cJibXr8bvwUAUparCwlvdbH6dvEOfou0/gCFQsHUfQrSDv+MuSUMAe8jzKE4qW+jK+xQU9a03GUnKHkkle+Q0pX/g6jXZ7r1/xAK5Do2kQ+X5xK9cipRgEKwIDAQAB==";
        NSString *algorithmName = @"RS256";
        NSString *message = @"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJijNLis4EZsHeY559a4DFOd50_OqgHGuERTqYZyuhtF39yxJPAjUESwxk2J5k_4zM3O-vtd1Ghyo4IbqKKSy6J9mTniYJPenn5-HIirE";
        JWTBuilder *builder = [JWTBuilder decodeMessage:message].secret(publicKey).algorithmName(algorithmName);
        NSDictionary *dictionary = builder.decode;
        
        // should be invalid certificate error.
        if (builder.jwtError) {
            NSLog(@"%@ error occurred: %@", self, builder.jwtError);
        }
        else {
            NSLog(@"%@ payload! %@", self, dictionary);
        }
        [[dictionary should] beNil];
    });
});

SPEC_END
