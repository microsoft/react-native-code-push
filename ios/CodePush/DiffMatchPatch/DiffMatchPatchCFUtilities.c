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

#include <CoreFoundation/CoreFoundation.h>

#include "DiffMatchPatchCFUtilities.h"

#include "MinMaxMacros.h"
#include <regex.h>
#include <limits.h>
#include <assert.h>

CFStringRef diff_CFStringCreateSubstring(CFStringRef text, CFIndex start_index, CFIndex length);
CFRange diff_RightSubstringRange(CFIndex text_length, CFIndex new_length);
CFStringRef diff_CFStringCreateRightSubstring(CFStringRef text, CFIndex text_length, CFIndex new_length);
CFRange diff_LeftSubstringRange(CFIndex new_length);
CFStringRef diff_CFStringCreateLeftSubstring(CFStringRef text, CFIndex new_length);
CFStringRef diff_CFStringCreateSubstringWithStartIndex(CFStringRef text, CFIndex start_index);
CFStringRef diff_CFStringCreateJavaSubstring(CFStringRef s, CFIndex begin, CFIndex end);
CFStringRef diff_CFStringCreateByCombiningTwoStrings(CFStringRef best_common_part1, CFStringRef best_common_part2);
Boolean diff_regExMatch(CFStringRef text, const regex_t *re);

CFArrayRef diff_halfMatchICreate(CFStringRef longtext, CFStringRef shorttext, CFIndex i);

// Utility functions
CFStringRef diff_CFStringCreateFromUnichar(UniChar ch) {
  CFStringRef c = CFStringCreateWithCharacters(kCFAllocatorDefault, &ch, 1);
  CFMakeCollectable(c);
  return c;
}

CFStringRef diff_CFStringCreateSubstring(CFStringRef text, CFIndex start_index, CFIndex length) {
  CFRange substringRange;
  substringRange.length = length;
  substringRange.location = start_index;

  CFStringRef substring = CFStringCreateWithSubstring(kCFAllocatorDefault, text, substringRange);
  CFMakeCollectable(substring);

  return substring;
}

CFRange diff_RightSubstringRange(CFIndex text_length, CFIndex new_length) {
  CFRange substringRange;
  substringRange.length = new_length;
  substringRange.location = text_length - new_length;
  return substringRange;
}

CFStringRef diff_CFStringCreateRightSubstring(CFStringRef text, CFIndex text_length, CFIndex new_length) {
  return diff_CFStringCreateSubstring(text, text_length - new_length, new_length);
}

CFRange diff_LeftSubstringRange(CFIndex new_length) {
  CFRange substringRange;
  substringRange.length = new_length;
  substringRange.location = 0;
  return substringRange;
}

CFStringRef diff_CFStringCreateLeftSubstring(CFStringRef text, CFIndex new_length) {
  return diff_CFStringCreateSubstring(text, 0, new_length);
}

CFStringRef diff_CFStringCreateSubstringWithStartIndex(CFStringRef text, CFIndex start_index) {
  return diff_CFStringCreateSubstring(text, start_index, (CFStringGetLength(text) - start_index));
}

CFStringRef diff_CFStringCreateJavaSubstring(CFStringRef s, CFIndex begin, CFIndex end) {
  return diff_CFStringCreateSubstring(s, begin, end - begin);
}

CFStringRef diff_CFStringCreateByCombiningTwoStrings(CFStringRef best_common_part1, CFStringRef best_common_part2) {
  CFIndex best_common_length;
  CFMutableStringRef best_common_mutable;
  best_common_length = CFStringGetLength(best_common_part1) + CFStringGetLength(best_common_part2);
  best_common_mutable = CFStringCreateMutableCopy(kCFAllocatorDefault, best_common_length, best_common_part1);
  CFMakeCollectable(best_common_mutable);
  CFStringAppend(best_common_mutable, best_common_part2);
  return best_common_mutable;
}

Boolean diff_regExMatch(CFStringRef text, const regex_t *re) {
  //TODO(jan): Using regex.h is far from optimal. Find an alternative.
  Boolean isMatch;
  const char *bytes;
  char *localBuffer = NULL;
  char *textCString = NULL;
  // We are only interested in line endings anyway so ASCII is fine.
  CFStringEncoding encoding = kCFStringEncodingASCII;

  bytes = CFStringGetCStringPtr(text, encoding);

  if (bytes == NULL) {
    Boolean success;
    CFIndex length;
    CFIndex usedBufferLength;
    CFIndex textLength = CFStringGetLength(text);
    CFRange rangeToProcess = CFRangeMake(0, textLength);

    success = (CFStringGetBytes(text, rangeToProcess, encoding, '?', false, NULL, LONG_MAX, &usedBufferLength) > 0);
    if (success) {
      length = usedBufferLength + 1;

      localBuffer = calloc(length, sizeof(char));
      success = (CFStringGetBytes(text, rangeToProcess, encoding, '?', false, (UInt8 *)localBuffer, length, NULL) > 0);

      if (success) {
        textCString = localBuffer;
      }
    }
  } else {
    textCString = (char *)bytes;
  }

  if (textCString != NULL) {
    isMatch = (regexec(re, textCString, 0, NULL, 0) == 0);
  } else {
    isMatch = false;
    //assert(0);
  }

  if (localBuffer != NULL) {
    free(localBuffer);
  }

  return isMatch;
}


/**
 * Determine the common prefix of two strings.
 * @param text1 First string.
 * @param text2 Second string.
 * @return The number of characters common to the start of each string.
 */
CFIndex diff_commonPrefix(CFStringRef text1, CFStringRef text2) {
  // Performance analysis: https://neil.fraser.name/news/2007/10/09/
  CFIndex text1_length = CFStringGetLength(text1);
  CFIndex text2_length = CFStringGetLength(text2);

  CFStringInlineBuffer text1_inlineBuffer, text2_inlineBuffer;
  CFStringInitInlineBuffer(text1, &text1_inlineBuffer, CFRangeMake(0, text1_length));
  CFStringInitInlineBuffer(text2, &text2_inlineBuffer, CFRangeMake(0, text2_length));

  UniChar char1, char2;
  CFIndex n = MIN(text1_length, text2_length);

  for (CFIndex i = 0; i < n; i++) {
    char1 = CFStringGetCharacterFromInlineBuffer(&text1_inlineBuffer, i);
    char2 = CFStringGetCharacterFromInlineBuffer(&text2_inlineBuffer, i);

    if (char1 != char2) {
      return i;
    }
  }

  return n;
}

/**
 * Determine the common suffix of two strings.
 * @param text1 First string.
 * @param text2 Second string.
 * @return The number of characters common to the end of each string.
 */
CFIndex diff_commonSuffix(CFStringRef text1, CFStringRef text2) {
  // Performance analysis: https://neil.fraser.name/news/2007/10/09/
  CFIndex text1_length = CFStringGetLength(text1);
  CFIndex text2_length = CFStringGetLength(text2);

  CFStringInlineBuffer text1_inlineBuffer, text2_inlineBuffer;
  CFStringInitInlineBuffer(text1, &text1_inlineBuffer, CFRangeMake(0, text1_length));
  CFStringInitInlineBuffer(text2, &text2_inlineBuffer, CFRangeMake(0, text2_length));

  UniChar char1, char2;
  CFIndex n = MIN(text1_length, text2_length);

  for (CFIndex i = 1; i <= n; i++) {
    char1 = CFStringGetCharacterFromInlineBuffer(&text1_inlineBuffer, (text1_length - i));
    char2 = CFStringGetCharacterFromInlineBuffer(&text2_inlineBuffer, (text2_length - i));

    if (char1 != char2) {
      return i - 1;
    }
  }
  return n;
}

/**
 * Determine if the suffix of one CFStringRef is the prefix of another.
 * @param text1 First CFStringRef.
 * @param text2 Second CFStringRef.
 * @return The number of characters common to the end of the first
 *     CFStringRef and the start of the second CFStringRef.
 */
CFIndex diff_commonOverlap(CFStringRef text1, CFStringRef text2) {
  CFIndex common_overlap = 0;

  // Cache the text lengths to prevent multiple calls.
  CFIndex text1_length = CFStringGetLength(text1);
  CFIndex text2_length = CFStringGetLength(text2);

  // Eliminate the nil case.
  if (text1_length == 0 || text2_length == 0) {
    return 0;
  }

  // Truncate the longer CFStringRef.
  CFStringRef text1_trunc;
  CFStringRef text2_trunc;
  CFIndex text1_trunc_length;
  if (text1_length > text2_length) {
    text1_trunc_length = text2_length;
    text1_trunc = diff_CFStringCreateRightSubstring(text1, text1_length, text1_trunc_length);

    text2_trunc = CFRetain(text2);
  } else if (text1_length < text2_length) {
    text1_trunc_length = text1_length;
    text1_trunc = CFRetain(text1);

    CFIndex text2_trunc_length = text1_length;
    text2_trunc = diff_CFStringCreateLeftSubstring(text2, text2_trunc_length);
  } else {
    text1_trunc_length = text1_length;
    text1_trunc = CFRetain(text1);

    text2_trunc = CFRetain(text2);
  }

  CFIndex text_length = MIN(text1_length, text2_length);
  // Quick check for the worst case.
  if (text1_trunc == text2_trunc) {
    common_overlap = text_length;
  } else {
    // Start by looking for a single character match
    // and increase length until no match is found.
    // Performance analysis: https://neil.fraser.name/news/2010/11/04/
    CFIndex best = 0;
    CFIndex length = 1;
    while (true) {
      CFStringRef pattern = diff_CFStringCreateRightSubstring(text1_trunc, text1_trunc_length, length);
      CFRange foundRange = CFStringFind(text2_trunc, pattern, 0);
      CFRelease(pattern);

      CFIndex found =  foundRange.location;
      if (found == kCFNotFound) {
        common_overlap = best;
        break;
      }
      length += found;

      CFStringRef text1_sub = diff_CFStringCreateRightSubstring(text1_trunc, text1_trunc_length, length);
      CFStringRef text2_sub = diff_CFStringCreateLeftSubstring(text2_trunc, length);

      if (found == 0 || (CFStringCompare(text1_sub, text2_sub, 0) == kCFCompareEqualTo)) {
        best = length;
        length++;
      }

      CFRelease(text1_sub);
      CFRelease(text2_sub);
    }
  }

  CFRelease(text1_trunc);
  CFRelease(text2_trunc);
  return common_overlap;
}

/**
 * Do the two texts share a Substring which is at least half the length of
 * the longer text?
 * This speedup can produce non-minimal diffs.
 * @param text1 First CFStringRef.
 * @param text2 Second CFStringRef.
 * @param diffTimeout Time limit for diff.
 * @return Five element String array, containing the prefix of text1, the
 *     suffix of text1, the prefix of text2, the suffix of text2 and the
 *     common middle.   Or NULL if there was no match.
 */
CFArrayRef diff_halfMatchCreate(CFStringRef text1, CFStringRef text2, const float diffTimeout) {
  if (diffTimeout <= 0) {
    // Don't risk returning a non-optimal diff if we have unlimited time.
    return NULL;
  }
  CFStringRef longtext = CFStringGetLength(text1) > CFStringGetLength(text2) ? text1 : text2;
  CFStringRef shorttext = CFStringGetLength(text1) > CFStringGetLength(text2) ? text2 : text1;
  if (CFStringGetLength(longtext) < 4 || CFStringGetLength(shorttext) * 2 < CFStringGetLength(longtext)) {
    return NULL;  // Pointless.
  }

  // First check if the second quarter is the seed for a half-match.
  CFArrayRef hm1 = diff_halfMatchICreate(longtext, shorttext,
                       (CFStringGetLength(longtext) + 3) / 4);
  // Check again based on the third quarter.
  CFArrayRef hm2 = diff_halfMatchICreate(longtext, shorttext,
                       (CFStringGetLength(longtext) + 1) / 2);
  CFArrayRef hm;
  if (hm1 == NULL && hm2 == NULL) {
    return NULL;
  } else if (hm2 == NULL) {
    hm = CFRetain(hm1);
  } else if (hm1 == NULL) {
    hm = CFRetain(hm2);
  } else {
    // Both matched.  Select the longest.
    hm = CFStringGetLength(CFArrayGetValueAtIndex(hm1, 4)) > CFStringGetLength(CFArrayGetValueAtIndex(hm2, 4)) ? CFRetain(hm1) : CFRetain(hm2);
  }

  if (hm1 != NULL) {
    CFRelease(hm1);
  }
  if (hm2 != NULL) {
    CFRelease(hm2);
  }

  // A half-match was found, sort out the return data.
  if (CFStringGetLength(text1) > CFStringGetLength(text2)) {
    return hm;
    //return new CFStringRef[]{hm[0], hm[1], hm[2], hm[3], hm[4]};
  } else {
    //    { hm[0], hm[1], hm[2], hm[3], hm[4] }
    // => { hm[2], hm[3], hm[0], hm[1], hm[4] }

    CFMutableArrayRef hm_mutable = CFArrayCreateMutableCopy(kCFAllocatorDefault, CFArrayGetCount(hm), hm);
    CFMakeCollectable(hm_mutable);

    CFRelease(hm);

    CFArrayExchangeValuesAtIndices(hm_mutable, 0, 2);
    CFArrayExchangeValuesAtIndices(hm_mutable, 1, 3);
    return hm_mutable;
  }
}

/**
 * Does a Substring of shorttext exist within longtext such that the
 * Substring is at least half the length of longtext?
 * @param longtext Longer CFStringRef.
 * @param shorttext Shorter CFStringRef.
 * @param i Start index of quarter length Substring within longtext.
 * @return Five element CFStringRef array, containing the prefix of longtext, the
 *     suffix of longtext, the prefix of shorttext, the suffix of shorttext
 *     and the common middle.   Or NULL if there was no match.
 */
CFArrayRef diff_halfMatchICreate(CFStringRef longtext, CFStringRef shorttext, CFIndex i) {
  // Start with a 1/4 length Substring at position i as a seed.
  CFStringRef seed = diff_CFStringCreateSubstring(longtext, i, CFStringGetLength(longtext) / 4);
  CFIndex j = -1;
  CFStringRef best_common = CFSTR("");
  CFStringRef best_longtext_a = CFSTR(""), best_longtext_b = CFSTR("");
  CFStringRef best_shorttext_a = CFSTR(""), best_shorttext_b = CFSTR("");

  CFStringRef best_common_part1, best_common_part2;

  CFStringRef longtext_substring, shorttext_substring;
  CFIndex shorttext_length = CFStringGetLength(shorttext);
  CFRange resultRange;
  CFRange rangeToSearch;
  rangeToSearch.length = shorttext_length - (j + 1);
  rangeToSearch.location = j + 1;

  while (j < CFStringGetLength(shorttext)
       && (CFStringFindWithOptions(shorttext, seed, rangeToSearch, 0, &resultRange) == true)) {
    j = resultRange.location;
    rangeToSearch.length = shorttext_length - (j + 1);
    rangeToSearch.location = j + 1;

    longtext_substring = diff_CFStringCreateSubstringWithStartIndex(longtext, i);
    shorttext_substring = diff_CFStringCreateSubstringWithStartIndex(shorttext, j);

    CFIndex prefixLength = diff_commonPrefix(longtext_substring, shorttext_substring);

    CFRelease(longtext_substring);
    CFRelease(shorttext_substring);

    longtext_substring = diff_CFStringCreateLeftSubstring(longtext, i);
    shorttext_substring = diff_CFStringCreateLeftSubstring(shorttext, j);

    CFIndex suffixLength = diff_commonSuffix(longtext_substring, shorttext_substring);

    CFRelease(longtext_substring);
    CFRelease(shorttext_substring);

    if (CFStringGetLength(best_common) < suffixLength + prefixLength) {
      CFRelease(best_common);
      CFRelease(best_longtext_a);
      CFRelease(best_longtext_b);
      CFRelease(best_shorttext_a);
      CFRelease(best_shorttext_b);

      best_common_part1 = diff_CFStringCreateSubstring(shorttext, j - suffixLength, suffixLength);
      best_common_part2 = diff_CFStringCreateSubstring(shorttext, j, prefixLength);

      best_common = diff_CFStringCreateByCombiningTwoStrings(best_common_part1, best_common_part2);

      CFRelease(best_common_part1);
      CFRelease(best_common_part2);

      best_longtext_a = diff_CFStringCreateLeftSubstring(longtext, i - suffixLength);
      best_longtext_b = diff_CFStringCreateSubstringWithStartIndex(longtext, i + prefixLength);
      best_shorttext_a = diff_CFStringCreateLeftSubstring(shorttext, j - suffixLength);
      best_shorttext_b = diff_CFStringCreateSubstringWithStartIndex(shorttext, j + prefixLength);
    }
  }

  CFRelease(seed);

  CFArrayRef halfMatchIArray;
  if (CFStringGetLength(best_common) * 2 >= CFStringGetLength(longtext)) {
    const CFStringRef values[] = { best_longtext_a, best_longtext_b,
                     best_shorttext_a, best_shorttext_b, best_common };
    halfMatchIArray = CFArrayCreate(kCFAllocatorDefault, (const void **)values, (sizeof(values) / sizeof(values[0])), &kCFTypeArrayCallBacks);
    CFMakeCollectable(halfMatchIArray);
  } else {
    halfMatchIArray = NULL;
  }

  CFRelease(best_common);
  CFRelease(best_longtext_a);
  CFRelease(best_longtext_b);
  CFRelease(best_shorttext_a);
  CFRelease(best_shorttext_b);

  return halfMatchIArray;
}

/**
 * Split a text into a list of strings.   Reduce the texts to a CFStringRef of
 * hashes where each Unicode character represents one line.
 * @param text CFString to encode.
 * @param lineArray CFMutableArray of unique strings.
 * @param lineHash Map of strings to indices.
 * @param maxLines Maximum length for lineArray.
 * @return Encoded CFStringRef.
 */
CFStringRef diff_linesToCharsMungeCFStringCreate(CFStringRef text, CFMutableArrayRef lineArray, CFMutableDictionaryRef lineHash, CFIndex maxLines) {
  #define lineStart lineStartRange.location
  #define lineEnd lineEndRange.location

  CFRange lineStartRange;
  CFRange lineEndRange;
  lineStart = 0;
  lineEnd = -1;
  CFStringRef line;
  CFMutableStringRef chars = CFStringCreateMutable(kCFAllocatorDefault, 0);

  CFIndex textLength = CFStringGetLength(text);
  CFIndex hash;
  CFNumberRef hashNumber;

  // Walk the text, pulling out a Substring for each line.
  // CFStringCreateArrayBySeparatingStrings(kCFAllocatorDefault, text, CFSTR("\n")) would temporarily double our memory footprint.
  // Modifying text would create many large strings.
  while (lineEnd < textLength - 1) {
    lineStartRange.length = textLength - lineStart;

    if (CFStringFindWithOptions(text, CFSTR("\n"), lineStartRange, 0, &lineEndRange) == false) {
      lineEnd = textLength - 1;
    } /* else {
      lineEnd = lineEndRange.location;
    }*/

    line = diff_CFStringCreateJavaSubstring(text, lineStart, lineEnd + 1);

    if (CFDictionaryContainsKey(lineHash, line)) {
      CFDictionaryGetValueIfPresent(lineHash, line, (const void **)&hashNumber);
      CFNumberGetValue(hashNumber, kCFNumberCFIndexType, &hash);
      const UniChar hashChar = (UniChar)hash;
      CFStringAppendCharacters(chars, &hashChar, 1);
    } else {
      if (CFArrayGetCount(lineArray) == maxLines) {
        // Bail out at 65535 because char 65536 == char 0.
        line = diff_CFStringCreateJavaSubstring(text, lineStart, textLength);
        lineEnd = textLength;
      }
      CFArrayAppendValue(lineArray, line);
      hash = CFArrayGetCount(lineArray) - 1;
      hashNumber = CFNumberCreate(kCFAllocatorDefault, kCFNumberCFIndexType, &hash);
      CFMakeCollectable(hashNumber);
      CFDictionaryAddValue(lineHash, line, hashNumber);
      CFRelease(hashNumber);
      const UniChar hashChar = (UniChar)hash;
      CFStringAppendCharacters(chars, &hashChar, 1);
    }
    lineStart = lineEnd + 1;

    CFRelease(line);
  }
  return chars;

  #undef lineStart
  #undef lineEnd
}

/**
 * Given two strings, compute a score representing whether the internal
 * boundary falls on logical boundaries.
 * Scores range from 6 (best) to 0 (worst).
 * @param one First CFStringRef.
 * @param two Second CFStringRef.
 * @return The score.
 */
CFIndex diff_cleanupSemanticScore(CFStringRef one, CFStringRef two) {
  static Boolean firstRun = true;
  static CFCharacterSetRef alphaNumericSet = NULL;
  static CFCharacterSetRef whiteSpaceSet = NULL;
  static CFCharacterSetRef controlSet = NULL;
  static regex_t blankLineEndRegEx;
  static regex_t blankLineStartRegEx;

  if (firstRun) {
    // Define some regex patterns for matching boundaries.
    alphaNumericSet = CFCharacterSetGetPredefined(kCFCharacterSetAlphaNumeric);
    whiteSpaceSet = CFCharacterSetGetPredefined(kCFCharacterSetWhitespaceAndNewline);
    controlSet = CFCharacterSetGetPredefined(kCFCharacterSetControl);
    int status;
    status = regcomp(&blankLineEndRegEx, "\n\r?\n$", REG_EXTENDED | REG_NOSUB);
    assert(status == 0);
    status = regcomp(&blankLineStartRegEx, "^\r?\n\r?\n", REG_EXTENDED | REG_NOSUB);
    assert(status == 0);
    firstRun = false;
  }

  if (CFStringGetLength(one) == 0 || CFStringGetLength(two) == 0) {
    // Edges are the best.
    return 6;
  }

  // Each port of this function behaves slightly differently due to
  // subtle differences in each language's definition of things like
  // 'whitespace'.  Since this function's purpose is largely cosmetic,
  // the choice has been made to use each language's native features
  // rather than force total conformity.
  UniChar char1 =
      CFStringGetCharacterAtIndex(one, (CFStringGetLength(one) - 1));
  UniChar char2 =
      CFStringGetCharacterAtIndex(two, 0);
  Boolean nonAlphaNumeric1 =
      !CFCharacterSetIsCharacterMember(alphaNumericSet, char1);
  Boolean nonAlphaNumeric2 =
      !CFCharacterSetIsCharacterMember(alphaNumericSet, char2);
  Boolean whitespace1 =
      nonAlphaNumeric1 && CFCharacterSetIsCharacterMember(whiteSpaceSet, char1);
  Boolean whitespace2 =
      nonAlphaNumeric2 && CFCharacterSetIsCharacterMember(whiteSpaceSet, char2);
  Boolean lineBreak1 =
      whitespace1 && CFCharacterSetIsCharacterMember(controlSet, char1);
  Boolean lineBreak2 =
      whitespace2 && CFCharacterSetIsCharacterMember(controlSet, char2);
  Boolean blankLine1 =
      lineBreak1 && diff_regExMatch(one, &blankLineEndRegEx);
  Boolean blankLine2 =
      lineBreak2 && diff_regExMatch(two, &blankLineStartRegEx);

  if (blankLine1 || blankLine2) {
    // Five points for blank lines.
    return 5;
  } else if (lineBreak1 || lineBreak2) {
    // Four points for line breaks.
    return 4;
  } else if (nonAlphaNumeric1 && !whitespace1 && whitespace2) {
    // Three points for end of sentences.
    return 3;
  } else if (whitespace1 || whitespace2) {
    // Two points for whitespace.
    return 2;
  } else if (nonAlphaNumeric1 || nonAlphaNumeric2) {
    // One point for non-alphanumeric.
    return 1;
  }
  return 0;
}
