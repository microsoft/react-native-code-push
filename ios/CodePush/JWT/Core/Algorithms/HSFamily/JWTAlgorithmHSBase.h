//
//  JWTAlgorithmHSBase.h
//  JWT
//
//  Created by Lobanov Dmitry on 13.03.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JWTAlgorithm.h"
extern NSString *const JWTAlgorithmNameHS256;
extern NSString *const JWTAlgorithmNameHS384;
extern NSString *const JWTAlgorithmNameHS512;

@interface JWTAlgorithmHSBase : NSObject <JWTAlgorithm>

@property (assign, nonatomic, readonly) size_t ccSHANumberDigestLength;
@property (assign, nonatomic, readonly) uint32_t ccHmacAlgSHANumber;

@end

@interface JWTAlgorithmHSBase (Create)

+ (instancetype)algorithm256;
+ (instancetype)algorithm384;
+ (instancetype)algorithm512;

@end
