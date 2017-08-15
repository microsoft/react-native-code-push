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
            return [evaluatedObject conformsToProtocol:@protocol(JWTAlgorithmDataHolder)];
        }]];
        self.holders = checkedHolders;
    }
    return self;
}

- (instancetype)initWithHolder:(id<JWTAlgorithmDataHolder>)holder {
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

- (instancetype)chainByAppendingHolder:(id<JWTAlgorithmDataHolder>)holder {
    return [self chainByAppendingHolders:holder ? @[holder] : nil];
}

@end

@implementation JWTAlgorithmDataHolderChain (Convenient)
- (id<JWTAlgorithmDataHolder>)firstHolderByAlgorithm:(id<JWTAlgorithm>)algorithm {
    NSInteger index = [self.holders indexOfObjectPassingTest:^BOOL(id <JWTAlgorithmDataHolder> _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        return [[[obj currentAlgorithm] name] isEqualToString:[algorithm name]];
    }];
    if (index != NSNotFound) {
        return self.holders[index];
    }

    return nil;
}
- (id<JWTAlgorithmDataHolder>)firstHolderBySecretData:(NSData *)secretData {
    NSInteger index = [self.holders indexOfObjectPassingTest:^BOOL(id <JWTAlgorithmDataHolder> _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        return [[obj currentSecretData] isEqualToData:secretData];
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
        id<JWTAlgorithmDataHolder> newHolder = [holder copy];
        [newHolder setCurrentSecretData:secretData];
        [holders arrayByAddingObject:newHolder];
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
        id<JWTAlgorithmDataHolder> newHolder = [holder copy];
        [newHolder setCurrentAlgorithm:algorithm];
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
