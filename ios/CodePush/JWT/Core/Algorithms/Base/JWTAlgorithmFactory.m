//
//  JWTAlgorithmFactory.m
//  JWT
//
//  Created by Lobanov Dmitry on 07.10.15.
//  Copyright © 2015 Karma. All rights reserved.
//

#import "JWTAlgorithmFactory.h"
#import "JWTAlgorithmHSBase.h"
#import "JWTAlgorithmRSBase.h"
#import "JWTAlgorithmNone.h"

// not implemented.
NSString *const JWTAlgorithmNameES256 = @"ES256";
NSString *const JWTAlgorithmNameES384 = @"ES384";
NSString *const JWTAlgorithmNameES512 = @"ES512";

@implementation JWTAlgorithmFactory

+ (NSArray *)algorithms {
    return @[
            [JWTAlgorithmNone new],
            [JWTAlgorithmHSBase algorithm256],
            [JWTAlgorithmHSBase algorithm384],
            [JWTAlgorithmHSBase algorithm512],
            [JWTAlgorithmRSBase algorithm256],
            [JWTAlgorithmRSBase algorithm384],
            [JWTAlgorithmRSBase algorithm512]
            ];

}

+ (id<JWTAlgorithm>)algorithmByName:(NSString *)name {
    id<JWTAlgorithm> algorithm = nil;
    
    NSString *algName = [name copy];
    
    NSUInteger index = [[self algorithms] indexOfObjectPassingTest:^BOOL(id<JWTAlgorithm> obj, NSUInteger idx, BOOL *stop) {
        // lowercase comparison
        return [obj.name.lowercaseString isEqualToString:algName.lowercaseString];
    }];
    
    if (index != NSNotFound) {
        algorithm = [self algorithms][index];
    }
    
    return algorithm;
}

@end