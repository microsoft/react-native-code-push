//
//  JWTAlgorithmNone.h
//  JWT
//
//  Created by Lobanov Dmitry on 16.10.15.
//  Copyright © 2015 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JWTAlgorithm.h"
extern NSString *const JWTAlgorithmNameNone;

@interface JWTAlgorithmNone : NSObject <JWTAlgorithm>

@end
