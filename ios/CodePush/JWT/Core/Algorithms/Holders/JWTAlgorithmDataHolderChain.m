//
//  JWTAlgorithmDataHolderChain.m
//  JWT
//
//  Created by Lobanov Dmitry on 02.10.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTAlgorithmDataHolderChain.h"

@interface JWTAlgorithmDataHolderChain()

@property (strong, nonatomic, readwrite) NSArray *holders;

@end

@implementation JWTAlgorithmDataHolderChain

- (NSArray *)holders {
    if (!_holders) {
        _holders = @[];
    }
    return _holders;
}

#pragma mark - Initialization
- (instancetype)initWithHolders:(NSArray *)holders {
    self = [super init];
    if (holders) {
        // check that holders conform to protocol
        NSArray *checkedHolders = [holders filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id  _Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
            return [evaluatedObject conformsToProtocol:@protocol(JWTAlgorithmDataHolderProtocol)];
        }]];
        self.holders = checkedHolders;
    }
    return self;
}

- (instancetype)initWithHolder:(id<JWTAlgorithmDataHolderProtocol>)holder {
    if (holder) {
        return [self initWithHolders:@[holder]];
    }
    return nil;
}

#pragma mark - Appending
- (instancetype)chainByAppendingChain:(JWTAlgorithmDataHolderChain *)chain {
    NSArray *holders = self.holders;
    if (chain) {
        holders = [holders arrayByAddingObjectsFromArray:chain.holders];
    }
    return [[self.class alloc] initWithHolders:holders];
}

- (instancetype)chainByAppendingHolders:(NSArray *)holders {
    // create new chain with holders
    JWTAlgorithmDataHolderChain *chain = nil;
    if (holders) {
        chain = [[self.class alloc] initWithHolders:holders];
    }
    return [self chainByAppendingChain:chain];
}

- (instancetype)chainByAppendingHolder:(id<JWTAlgorithmDataHolderProtocol>)holder {
    return [self chainByAppendingHolders:holder ? @[holder] : nil];
}

#pragma mark - Create
+ (instancetype)chainWithHolders:(NSArray *)holders {
    return [[self new] chainByAppendingHolders:holders];
}

+ (instancetype)chainWithHolder:(id<JWTAlgorithmDataHolderProtocol>)holder {
    return [[self new] chainByAppendingHolder:holder];
}

#pragma mark - Debug
- (NSString *)debugDescription {
    return [NSString stringWithFormat:@"%@ holders: %@", self.class, [self.holders valueForKey:@"debugDescription"]];
}
@end

@implementation JWTAlgorithmDataHolderChain (Convenient)
- (id<JWTAlgorithmDataHolderProtocol>)firstHolderByAlgorithm:(id<JWTAlgorithm>)algorithm {
    NSInteger index = [self.holders indexOfObjectPassingTest:^BOOL(id <JWTAlgorithmDataHolderProtocol> _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        return [[[obj internalAlgorithm] name] isEqualToString:[algorithm name]];
    }];
    if (index != NSNotFound) {
        return self.holders[index];
    }

    return nil;
}
- (id<JWTAlgorithmDataHolderProtocol>)firstHolderBySecretData:(NSData *)secretData {
    NSInteger index = [self.holders indexOfObjectPassingTest:^BOOL(id <JWTAlgorithmDataHolderProtocol> _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        return [[obj internalSecretData] isEqualToData:secretData];
    }];
    if (index != NSNotFound) {
        return self.holders[index];
    }

    return nil;
}
- (NSArray *)singleAlgorithm:(id<JWTAlgorithm>)algorithm withManySecretData:(NSArray *)secretsData {
    NSArray *holders = @[];

    id holder = [self firstHolderByAlgorithm:algorithm];

    if (!holder) {
        return holders;
    }

    for (NSData *secretData in secretsData) {
        id<JWTAlgorithmDataHolderProtocol> newHolder = [holder copy];
        [newHolder setInternalSecretData:secretData];
        holders = [holders arrayByAddingObject:newHolder];
    }
    return holders;
}

- (NSArray *)singleSecretData:(NSData *)secretData withManyAlgorithms:(NSArray *)algorithms {
    NSArray *holders = @[];

    id holder = [self firstHolderBySecretData:secretData];

    if (!holder) {
        return holders;
    }

    for (id<JWTAlgorithm>algorithm in algorithms) {
        id<JWTAlgorithmDataHolderProtocol> newHolder = [holder copy];
        [newHolder setInternalAlgorithm:algorithm];
        [holders arrayByAddingObject:newHolder];
    }
    return holders;
}

- (instancetype)chainByPopulatingAlgorithm:(id<JWTAlgorithm>)algorithm withManySecretData:(NSArray *)secretsData {
    return [[self.class alloc] initWithHolders:[self singleAlgorithm:algorithm withManySecretData:secretsData]];
}

- (instancetype)chainByPopulatingSecretData:(NSData *)secretData withManyAlgorithms:(NSArray *)algorithms {
    return [[self.class alloc] initWithHolders:[self singleSecretData:secretData withManyAlgorithms:algorithms]];
}
@end
