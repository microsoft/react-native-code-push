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

#ifndef _DIFFMATCHPATCHCFUTILITIES_H
#define _DIFFMATCHPATCHCFUTILITIES_H

CFStringRef diff_CFStringCreateFromUnichar(UniChar ch);
CFStringRef diff_CFStringCreateJavaSubstring(CFStringRef s, CFIndex begin, CFIndex end);

CFIndex diff_commonPrefix(CFStringRef text1, CFStringRef text2);
CFIndex diff_commonSuffix(CFStringRef text1, CFStringRef text2);
CFIndex diff_commonOverlap(CFStringRef text1, CFStringRef text2);
CFArrayRef diff_halfMatchCreate(CFStringRef text1, CFStringRef text2, const float diffTimeout);
CFArrayRef diff_halfMatchICreate(CFStringRef longtext, CFStringRef shorttext, CFIndex i);

CFStringRef diff_linesToCharsMungeCFStringCreate(CFStringRef text, CFMutableArrayRef lineArray, CFMutableDictionaryRef lineHash, CFIndex maxLines);

CFIndex diff_cleanupSemanticScore(CFStringRef one, CFStringRef two);

CF_INLINE void diff_CFStringPrepareUniCharBuffer(CFStringRef string, const UniChar **string_chars, UniChar **string_buffer, CFRange string_range) {
  *string_chars = CFStringGetCharactersPtr(string);
  if (*string_chars == NULL) {
    // Fallback in case CFStringGetCharactersPtr() didn’t work.
    *string_buffer = malloc(string_range.length * sizeof(UniChar));
    CFStringGetCharacters(string, string_range, *string_buffer);
    *string_chars = *string_buffer;
  }
}

#endif //ifndef _DIFFMATCHPATCHCFUTILITIES_H
