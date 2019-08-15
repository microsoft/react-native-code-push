//
//  JWTClaimsSetSerializer.h
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "JWTClaimsSet.h"

@interface JWTClaimsSetSerializer : NSObject

+ (NSArray *)claimsSetKeys;
+ (NSDictionary *)dictionaryWithClaimsSet:(JWTClaimsSet *)theClaimsSet;
+ (JWTClaimsSet *)claimsSetWithDictionary:(NSDictionary *)theDictionary;

@end
