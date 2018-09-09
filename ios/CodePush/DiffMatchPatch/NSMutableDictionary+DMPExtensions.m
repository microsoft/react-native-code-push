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

#import "NSMutableDictionary+DMPExtensions.h"

#import "NSString+UnicharUtilities.h"


@implementation NSMutableDictionary (DMPExtensions)

- (id)diff_objectForIntegerKey:(NSInteger)keyInteger;
{
  return [self objectForKey:[NSNumber numberWithInteger:keyInteger]];
}

- (id)diff_objectForUnsignedIntegerKey:(NSUInteger)keyUInteger;
{
  return [self objectForKey:[NSNumber numberWithUnsignedInteger:keyUInteger]];
}

- (id)diff_objectForUnicharKey:(unichar)aUnicharKey;
{
  return [self objectForKey:[NSString diff_stringFromUnichar:aUnicharKey]];
}


- (NSInteger)diff_integerForKey:(id)aKey;
{
  return [((NSNumber *)[self objectForKey:aKey]) integerValue];
}

- (NSUInteger)diff_unsignedIntegerForKey:(id)aKey;
{
  return [((NSNumber *)[self objectForKey:aKey]) unsignedIntegerValue];
}

- (NSInteger)diff_integerForIntegerKey:(NSInteger)keyInteger;
{
  return [((NSNumber *)[self objectForKey:[NSNumber numberWithInteger:keyInteger]]) integerValue];
}

- (NSUInteger)diff_unsignedIntegerForUnicharKey:(unichar)aUnicharKey;
{
  return [((NSNumber *)[self diff_objectForUnicharKey:aUnicharKey]) unsignedIntegerValue];
}


- (BOOL)diff_containsObjectForKey:(id)aKey;
{
  return ([self objectForKey:aKey] != nil);
}

- (BOOL)containsObjectForIntegerKey:(NSInteger)keyInteger;
{
  return ([self objectForKey:[NSNumber numberWithInteger:keyInteger]] != nil);
}

- (BOOL)diff_containsObjectForUnicharKey:(unichar)aUnicharKey;
{
  return ([self diff_objectForUnicharKey:aUnicharKey] != nil);
}


- (void)diff_setIntegerValue:(NSInteger)anInteger forKey:(id)aKey;
{
  [self setObject:[NSNumber numberWithInteger:anInteger] forKey:aKey];
}

- (void)diff_setIntegerValue:(NSInteger)anInteger forIntegerKey:(NSInteger)keyInteger;
{
  [self setObject:[NSNumber numberWithInteger:anInteger] forKey:[NSNumber numberWithInteger:keyInteger]];
}


- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forKey:(id)aKey;
{
  [self setObject:[NSNumber numberWithUnsignedInteger:anUInteger] forKey:aKey];
}

- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forUnsignedIntegerKey:(NSUInteger)keyUInteger;
{
  [self setObject:[NSNumber numberWithUnsignedInteger:anUInteger] forKey:[NSNumber numberWithUnsignedInteger:keyUInteger]];
}

- (void)diff_setUnsignedIntegerValue:(NSUInteger)anUInteger forUnicharKey:(unichar)aUnicharKey;
{
  [self setObject:[NSNumber numberWithUnsignedInteger:anUInteger] forKey:[NSString diff_stringFromUnichar:aUnicharKey]];
}

@end
