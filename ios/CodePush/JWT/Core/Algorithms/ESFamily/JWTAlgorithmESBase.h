//
//  JWTAlgorithmESBase.h
//  Pods
//
//  Created by Lobanov Dmitry on 12.02.17.
//
//

#import <Foundation/Foundation.h>
#import "JWTRSAlgorithm.h"
extern NSString *const JWTAlgorithmNameES256;
extern NSString *const JWTAlgorithmNameES384;
extern NSString *const JWTAlgorithmNameES512;
@interface JWTAlgorithmESBase : NSObject @end

@interface JWTAlgorithmESBase (JWTAsymmetricKeysAlgorithm) <JWTAsymmetricKeysAlgorithm> @end

@interface JWTAlgorithmESBase (Create)

+ (instancetype)algorithm256;
+ (instancetype)algorithm384;
+ (instancetype)algorithm512;

@end
