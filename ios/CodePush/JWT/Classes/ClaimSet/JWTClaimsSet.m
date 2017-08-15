//
//  JWTClaimsSet.m
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import "JWTClaimsSet.h"

@implementation JWTClaimsSet

- (id)copyWithZone:(NSZone *)zone {
    JWTClaimsSet *newClaimsSet = [[self.class alloc] init];
    
    newClaimsSet.issuer = self.issuer;
    newClaimsSet.subject = self.subject;
    newClaimsSet.audience = self.audience;
    newClaimsSet.expirationDate = self.expirationDate;
    newClaimsSet.notBeforeDate = self.notBeforeDate;
    newClaimsSet.issuedAt = self.issuedAt;
    newClaimsSet.identifier = self.identifier;
    newClaimsSet.type = self.type;
    
    return newClaimsSet;
}

@end
