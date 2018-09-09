/*
 * Diff Match and Patch
 * Copyright 2018 The diff-match-patch Authors.
 * https://github.com/google/diff-match-patch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: fraser@google.com (Neil Fraser)
 * ObjC port: jan@geheimwerk.de (Jan Weiß)
 */

#import <Foundation/Foundation.h>


@interface NSMutableDictionary (DMPExtensions)

- (id)diff_objectForIntegerKey:(NSInteger)keyInteger;
- (id)diff_objectForUnsignedIntegerKey:(NSUInteger)keyUInteger;
- (id)diff_objectForUnicharKey:(unichar)aUnicharKey;

- (NSInteger)diff_integerForKey:(id)aKey;
- (NSUInteger)diff_unsignedIntegerForKey:(id)aKey;
- (NSInteger)diff_integerForIntegerKey:(NSInteger)keyInteger;
- (NSUInteger)diff_unsignedIntegerForUnicharKey:(unichar)aUnicharKey;

- (BOOL)diff_containsObjectForKey:(id)aKey;
- (BOOL)diff_containsObjectForUnicharKey:(unichar)aUnicharKey;

- (void)diff_setIntegerValue:(NSInteger)anInteger forKey:(id)aKey;
- (void)diff_setIntegerValue:(NSInteger)anInteger forIntegerKey:(NSInteger)keyInteger;

- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forKey:(id)aKey;
- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forUnsignedIntegerKey:(NSUInteger)keyUInteger;
- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forUnicharKey:(unichar)aUnicharKey;

@end
