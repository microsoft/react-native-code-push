//
// Created by Marcelo Schroeder on 12/03/2016.
// Copyright (c) 2016 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JWTAlgorithm.h"

@protocol JWTRSAlgorithm <JWTAlgorithm>

@required

@property(nonatomic, readwrite, copy) NSString *privateKeyCertificatePassphrase;

@end