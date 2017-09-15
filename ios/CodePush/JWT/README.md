[![JWT](http://jwt.io/assets/logo.svg)](https://jwt.io/)

[![Build Status](https://travis-ci.org/yourkarma/JWT.svg?branch=master)](https://travis-ci.org/yourkarma/JWT)
[![Pod Version](http://img.shields.io/cocoapods/v/JWT.svg?style=flat)](http://cocoadocs.org/docsets/JWT)
[![Pod Platform](http://img.shields.io/cocoapods/p/JWT.svg?style=flat)](http://cocoadocs.org/docsets/JWT)
[![Reference Status](https://www.versioneye.com/objective-c/jwt/reference_badge.svg?style=flat)](https://www.versioneye.com/objective-c/jwt/references)

# JWT

A [JSON Web Token][] implementation in Objective-C.

[JSON Web Token]: http://self-issued.info/docs/draft-ietf-oauth-json-web-token.html

# What's new in master and bleeding edge.
Nothing here.

# What's new in Version 3.0

* Fluent style expanded.
* Coding result types added.
* Algorithms and data holders.
* Algorithms and data holders chain.
* Keys loaded from Pem files.

## Introduction to Algorithms data holders and chain.
You have algorithm, secret data and unknown jwt token.
Let's try to decode it.

```objective-c
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
```

Maybe you would like to try different secrets.

```objective-c
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

JWTAlgorithmDataHolderChain *expandedChain = [chain chainByPopulatingAlgorithm:firstHolder.currentAlgorithm withManySecretData:manySecretsData];

// now we have expanded chain with many secrets and one algorithm.
NSLog(@"expanded chain has: %@", expandedChain.debugDescription);
```

## Decode and encode with chain.

```objective-c
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
```

## Keys loaded from Pem files.

You have a key in pem file. And you want to use it directly for sign/verify.
Suppose, that "public_rsa.pem" and "private_rsa.pem" are public and private keys in pem format.
```objective-c
// Load keys
- (NSString *)pemKeyStringFromFileWithName:(NSString *)string inBundle:(NSBundle *)bundle {
    NSURL *fileURL = [bundle URLForResource:name withExtension:@"pem"];
    NSError *error = nil;
    NSString *fileContent = [NSString stringWithContentsOfURL:fileURL encoding:NSUTF8StringEncoding error:&error];
    if (error) {
        NSLog(@"%@ error: %@", self.debugDescription, error);
        return nil;
    }
}

// Sign and verify
- (void)signAndVerifyWithPrivateKeyPemString:(NSString *)privateKey publicKeyPemString:(NSString *)publicKey privateKeyPassphrase:(NSString *)passphrase {
    NSString *algorithmName = @"RS256";

    id <JWTAlgorithmDataHolderProtocol> signDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor privateKeyWithPEMBase64].type).privateKeyCertificatePassphrase(passphrase).algorithmName(algorithmName).secret(privateKey);

    id <JWTAlgorithmDataHolderProtocol> verifyDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor publicKeyWithPEMBase64].type).algorithmName(algorithmName).secret(publicKey);

    // sign
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
```

# Experiments in Version 2.0
## Whitelists possible algorithms.
When you need to decode jwt by several algorithms you could specify their names in whitelist.
Later this feature possible will migrate to options.
For example, someone returns result or error.
### Limitations
Restricted to pair (algorithm or none) due to limitations of unique `secret`.

```objective-c
NSString *jwtResultOrError = /*...*/;
NSString *secret = @"secret";
JWTBuilder *builder = [JWT decodeMessage:jwtResultOrError].secret(@"secret").whitelist(@[@"HS256", @"none"]);
NSDictionary *decoded = builder.decode;
if (builder.jwtError) {
    // oh!
}
else {
    NSDictionary *payload = decoded[@"payload"];
    NSDictionary *header = decoded[@"header"];
    NSArray *tries = decoded[@"tries"]; // will be evolded into something appropriate later.
}
```

# What's new in Version 2.0

* Old plain style deprecated.
* Use modern fluent style instead.

```objective-c
NSDictionary *payload = @{@"foo" : @"bar"};
NSString *secret = @"secret";
id<JWTAlgorithm> algorithm = [JWTAlgorithmFactory algorithmByName:@"HS256"];
// Deprecated
[JWT encodePayload:payload withSecret:secret algorithm:algorithm];

// Modern
[JWTBuilder encodePayload:payload].secret(secret).algorithm(algorithm).encode;
```

# Installation

Add the following to your [CocoaPods][] Podfile:

    pod "JWT"

[CocoaPods]: http://cocoapods.org

Install via Cartfile:

    github "yourkarma/JWT" "master"

and `import JWT`

# Documentation
# Usage

## JWTBuilder

To encode & decode JWTs, use fluent style with the `JWTBuilder` interface

```objective-c
+ (JWTBuilder *)encodePayload:(NSDictionary *)payload;
+ (JWTBuilder *)encodeClaimsSet:(JWTClaimsSet *)claimsSet;
+ (JWTBuilder *)decodeMessage:(NSString *)message;
```

As you can see, JWTBuilder has interface from both decoding and encoding.

Note: some attributes are encode-only or decode-only.

    #pragma mark - Encode only
    *payload;
    *headers;
    *algorithm;

    #pragma mark - Decode only
    *message
    *options // as forcedOption from jwt decode functions interface.
    *whitelist  //optional array of algorithm names to whitelist for decoding

You can inspect JWTBuilder by `jwt`-prefixed attributes.

You can set JWTBuilder attributes by fluent style (block interface).

You can encode arbitrary payloads like so:

```objective-c
NSDictionary *payload = @{@"foo" : @"bar"};
NSString *secret = @"secret";
id<JWTAlgorithm> algorithm = [JWTAlgorithmFactory algorithmByName:@"HS256"];

[JWTBuilder encodePayload:payload].secret(@"secret").algorithm(algorithm).encode;
```

If you're using reserved claim names you can encode your claim set like so (all properties are optional):

```objective-c
JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
claimsSet.issuer = @"Facebook";
claimsSet.subject = @"Token";
claimsSet.audience = @"http://yourkarma.com";
claimsSet.expirationDate = [NSDate distantFuture];
claimsSet.notBeforeDate = [NSDate distantPast];
claimsSet.issuedAt = [NSDate date];
claimsSet.identifier = @"thisisunique";
claimsSet.type = @"test";

NSString *secret = @"secret";
id<JWTAlgorithm> algorithm = [JWTAlgorithmFactory algorithmByName:@"HS256"];

[JWTBuilder encodeClaimsSet:claimsSet].secret(secret).algorithm(algorithm).encode;
```

You can decode a JWT like so:

```objective-c
NSString *jwtToken = @"header.payload.signature";
NSString *secret = @"secret";
NSString *algorithmName = @"HS256"; //Must specify an algorithm to use

NSDictionary *payload = [JWTBuilder decodeMessage:jwtToken].secret(secret).algorithmName(algorithmName).decode;
```

If you want to check claims while decoding, you could use next sample of code (all properties are optional):

```objective-c
// Trusted Claims Set
JWTClaimsSet *trustedClaimsSet = [[JWTClaimsSet alloc] init];
trustedClaimsSet.issuer = @"Facebook";
trustedClaimsSet.subject = @"Token";
trustedClaimsSet.audience = @"http://yourkarma.com";
trustedClaimsSet.expirationDate = [NSDate date];
trustedClaimsSet.notBeforeDate = [NSDate date];
trustedClaimsSet.issuedAt = [NSDate date];
trustedClaimsSet.identifier = @"thisisunique";
trustedClaimsSet.type = @"test";

NSString *message = @"encodedJwt";
NSString *secret = @"secret";
NSString *algorithmName = @"chosenAlgorithm"

JWTBuilder *builder = [JWTBuilder decodeMessage:jwt].secret(secret).algorithmName(algorithmName).claimsSet(trustedClaimsSet);
NSDictionary *payload = builder.decode;

if (builder.jwtError == nil) {
    // do your work here
}
else {
    // handle error
}
```

If you want to enforce a whitelist of valid algorithms:

```objective-c
NSArray *whitelist = @[@"HS256", @"HS512"];
NSString *jwtToken = @"header.payload.signature";
NSString *secret = @"secret";
NSString *algorithmName = @"HS256";

//Returns nil
NSDictionary *payload = [JWTBuilder decodeMessage:jwtToken].secret(secret).algorithmName(algorithmName).whitelist(@[]).decode;

//Returns the decoded payload
NSDictionary *payload = [JWTBuilder decodeMessage:jwtToken].secret(secret).algorithmName(algorithmName).whitelist(whitelist).decode;
```

### Encode / Decode Example

```objective-c
// suppose, that you create ClaimsSet
JWTClaimsSet *claimsSet = [[JWTClaimsSet alloc] init];
// fill it
claimsSet.issuer = @"Facebook";
claimsSet.subject = @"Token";
claimsSet.audience = @"http://yourkarma.com";

// encode it
NSString *secret = @"secret";
NSString *algorithmName = @"HS384";
NSDictionary *headers = @{@"custom":@"value"};
id<JWTAlgorithm> algorithm = [JWTAlgorithmFactory algorithmByName:algorithmName];

JWTBuilder *encodeBuilder = [JWT encodeClaimsSet:claimsSet];
NSString *encodedResult = encodeBuilder.secret(secret).algorithm(algorithm).headers(headers).encode;

if (encodeBuilder.jwtError == nil) {
    // handle encoded result
    NSLog(@"encoded result: %@", encodedResult);
}
else {
    // handle error
    NSLog(@"encode failed, error: %@", encodeBuilder.jwtError);
}

// decode it
// you can set any property that you want, all properties are optional
JWTClaimsSet *trustedClaimsSet = [claimsSet copy];

// decode forced ? try YES
BOOL decodeForced = NO;
NSNumber *options = @(decodeForced);
NSString *yourJwt = encodedResult; // from previous example
NSString *yourSecret = secret; // from previous example
NSString *yourAlgorithm = algorithmName; // from previous example
JWTBuilder *decodeBuilder = [JWT decodeMessage:yourJwt];
NSDictionary *decodedResult = decodeBuilder.message(yourJwt).secret(yourSecret).algorithmName(yourAlgorithm).claimsSet(trustedClaimsSet).options(options).decode;
if (decodeBuilder.jwtError == nil) {
    // handle decoded result
    NSLog(@"decoded result: %@", decodedResult);
}
else {
    // handle error
    NSLog(@"decode failed, error: %@", decodeBuilder.jwtError);
}
```

#### NSData
You can also encode/decode using a secret that is represented as an NSData object

```objective-c
//Encode
NSData *secretData = "<your data>";
NSString *algorithmName = @"HS384";
NSDictionary *headers = @{@"custom":@"value"};
id<JWTAlgorithm> algorithm = [JWTAlgorithmFactory algorithmByName:algorithmName];

JWTBuilder *encodeBuilder = [JWT encodeClaimsSet:claimsSet];
NSString *encodedResult = encodeBuilder.secretData(secretData).algorithm(algorithm).headers(headers).encode;

//Decode
NSString *jwtToken = @"header.payload.signature";
NSData *secretData = "<your data>"
NSString *algorithmName = @"HS256"; //Must specify an algorithm to use

NSDictionary *payload = [JWTBuilder decodeMessage:jwtToken].secretData(secretData).algorithmName(algorithmName).decode;
```

# Algorithms

The following algorithms are supported:

* RS256
* HS512 - HMAC using SHA-512.
* HS256 / HS384 / HS512
* None

## RS256 usage.
For example, you have your file with privateKey: `file.p12`.
And you have a secret passphrase for that file: `secret`.

```objective-c
// Encode
NSDictionary *payload = @{@"payload" : @"hidden_information"};
NSString *algorithmName = @"RS256";

NSString *filePath = [[NSBundle mainBundle] pathForResource:@"secret_key" ofType:@"p12"];
NSData *privateKeySecretData = [NSData dataWithContentsOfFile:filePath];

NSString *passphraseForPrivateKey = @"secret";

JWTBuilder *builder = [JWTBuilder encodePayload:payload].secretData(privateKeySecretData).privateKeyCertificatePassphrase(passphraseForPrivateKey).algorithmName(algorithmName);
NSString *token = builder.encode;

// check error
if (builder.jwtError == nil) {
    // handle result
}
else {
    // error occurred.
}

// Decode
// Suppose, that you get token from previous example. You need a valid public key for a private key in previous example.
// Private key stored in @"secret_key.p12". So, you need public key for that private key.
NSString *publicKey = @"..."; // load public key. Or use it as raw string.

algorithmName = @"RS256";

JWTBuilder *decodeBuilder = [JWTBuilder decodeMessage:token].secret(publicKey).algorithmName(algorithmName);
NSDictionary *envelopedPayload = decodeBuilder.decode;

// check error
if (decodeBuilder.jwtError == nil) {
    // handle result
}
else {
    // error occurred.
}
```


Additional algorithms can be added by implementing the `JWTAlgorithm` protocol.

## Before pull request

Please, read [Contribution notes](https://github.com/yourkarma/JWT/blob/master/.github/CONTRIBUTING.md) before make pull request.