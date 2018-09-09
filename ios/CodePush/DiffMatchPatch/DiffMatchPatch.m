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

#import "DiffMatchPatch.h"

#import "NSString+JavaSubstring.h"
#import "NSString+UriCompatibility.h"
#import "NSMutableDictionary+DMPExtensions.h"
#import "DiffMatchPatchCFUtilities.h"


#if !defined(MAX_OF_CONST_AND_DIFF)
  // Determines the maximum of two expressions:
  // The first is a constant (first parameter) while the second expression is
  // the difference between the second and third parameter.  The way this is
  // calculated prevents integer overflow in the result of the difference.
  #define MAX_OF_CONST_AND_DIFF(A,B,C)  ((B) <= (C) ? (A) : (B) - (C) + (A))
#endif


// JavaScript-style splice function
void splice(NSMutableArray *input, NSUInteger start, NSUInteger count, NSArray *objects);

/* NSMutableArray * */ void splice(NSMutableArray *input, NSUInteger start, NSUInteger count, NSArray *objects) {
  NSRange deletionRange = NSMakeRange(start, count);
  if (objects == nil) {
    [input removeObjectsInRange:deletionRange];
  } else {
    [input replaceObjectsInRange:deletionRange withObjectsFromArray:objects];
  }
}

@implementation Diff

@synthesize operation;
@synthesize text;

/**
 * Constructor.  Initializes the diff with the provided values.
 * @param anOperation One of DIFF_INSERT, DIFF_DELETE or DIFF_EQUAL.
 * @param aText The text being applied.
 */
+ (id)diffWithOperation:(Operation)anOperation
                andText:(NSString *)aText;
{
  return [[[self alloc] initWithOperation:anOperation andText:aText] autorelease];
}

- (id)initWithOperation:(Operation)anOperation
                andText:(NSString *)aText;
{
  self = [super init];
  if (self) {
    self.operation = anOperation;
    self.text = aText;
  }
  return self;

}

- (void)dealloc
{
  self.text = nil;

  [super dealloc];
}

- (id)copyWithZone:(NSZone *)zone
{
  id newDiff = [[[self class] allocWithZone:zone]
                initWithOperation:self.operation
                andText:self.text];

  return newDiff;
}


/**
 * Display a human-readable version of this Diff.
 * @return text version.
 */
- (NSString *)description
{
  NSString *prettyText = [self.text stringByReplacingOccurrencesOfString:@"\n" withString:@"\u00b6"];
  NSString *operationName = nil;
  switch (self.operation) {
    case DIFF_DELETE:
      operationName = @"DIFF_DELETE";
      break;
    case DIFF_INSERT:
      operationName = @"DIFF_INSERT";
      break;
    case DIFF_EQUAL:
      operationName = @"DIFF_EQUAL";
      break;
    default:
      break;
  }

  return [NSString stringWithFormat:@"Diff(%@,\"%@\")", operationName, prettyText];
}

/**
 * Is this Diff equivalent to another Diff?
 * @param obj Another Diff to compare against.
 * @return YES or NO.
 */
- (BOOL)isEqual:(id)obj
{
  // If parameter is nil return NO.
  if (obj == nil) {
    return NO;
  }

  // If parameter cannot be cast to Diff return NO.
  if (![obj isKindOfClass:[Diff class]]) {
    return NO;
  }

  // Return YES if the fields match.
  Diff *p = (Diff *)obj;
  return p.operation == self.operation && [p.text isEqualToString:self.text];
}

- (BOOL)isEqualToDiff:(Diff *)obj
{
  // If parameter is nil return NO.
  if (obj == nil) {
    return NO;
  }

  // Return YES if the fields match.
  return obj.operation == self.operation && [obj.text isEqualToString:self.text];
}

- (NSUInteger)hash
{
  return ([text hash] ^ (NSUInteger)operation);
}

@end


@implementation Patch

@synthesize diffs;
@synthesize start1;
@synthesize start2;
@synthesize length1;
@synthesize length2;

- (id)init
{
  self = [super init];

  if (self) {
    self.diffs = [NSMutableArray array];
  }

  return self;
}

- (void)dealloc
{
  self.diffs = nil;

  [super dealloc];
}

- (id)copyWithZone:(NSZone *)zone
{
  Patch *newPatch = [[[self class] allocWithZone:zone] init];

  newPatch.diffs = [[NSMutableArray alloc] initWithArray:self.diffs copyItems:YES];
  newPatch.start1 = self.start1;
  newPatch.start2 = self.start2;
  newPatch.length1 = self.length1;
  newPatch.length2 = self.length2;

  return newPatch;
}


/**
 * Emulate GNU diff's format.
 * Header: @@ -382,8 +481,9 @@
 * Indices are printed as 1-based, not 0-based.
 * @return The GNU diff NSString.
 */
- (NSString *)description
{
  NSString *coords1;
  NSString *coords2;

  if (self.length1 == 0) {
    coords1 = [NSString stringWithFormat:@"%lu,0",
               (unsigned long)self.start1];
  } else if (self.length1 == 1) {
    coords1 = [NSString stringWithFormat:@"%lu",
               (unsigned long)self.start1 + 1];
  } else {
    coords1 = [NSString stringWithFormat:@"%lu,%lu",
               (unsigned long)self.start1 + 1, (unsigned long)self.length1];
  }
  if (self.length2 == 0) {
    coords2 = [NSString stringWithFormat:@"%lu,0",
               (unsigned long)self.start2];
  } else if (self.length2 == 1) {
    coords2 = [NSString stringWithFormat:@"%lu",
               (unsigned long)self.start2 + 1];
  } else {
    coords2 = [NSString stringWithFormat:@"%lu,%lu",
               (unsigned long)self.start2 + 1, (unsigned long)self.length2];
  }

  NSMutableString *text = [NSMutableString stringWithFormat:@"@@ -%@ +%@ @@\n",
                           coords1, coords2];
  // Escape the body of the patch with %xx notation.
  for (Diff *aDiff in self.diffs) {
    switch (aDiff.operation) {
      case DIFF_INSERT:
        [text appendString:@"+"];
        break;
      case DIFF_DELETE:
        [text appendString:@"-"];
        break;
      case DIFF_EQUAL:
        [text appendString:@" "];
        break;
    }

    [text appendString:[aDiff.text diff_stringByAddingPercentEscapesForEncodeUriCompatibility]];
    [text appendString:@"\n"];
  }

  return text;
}

@end


@implementation DiffMatchPatch

@synthesize Diff_Timeout;
@synthesize Diff_EditCost;
@synthesize Match_Threshold;
@synthesize Match_Distance;
@synthesize Patch_DeleteThreshold;
@synthesize Patch_Margin;

- (id)init
{
  self = [super init];

  if (self) {
    Diff_Timeout = 1.0f;
    Diff_EditCost = 4;
    Match_Threshold = 0.5f;
    Match_Distance = 1000;
    Patch_DeleteThreshold = 0.5f;
    Patch_Margin = 4;

    Match_MaxBits = 32;
  }

  return self;
}

- (void)dealloc
{
  [super dealloc];
}


#pragma mark Diff Functions
//  DIFF FUNCTIONS


/**
 * Find the differences between two texts.
 * Run a faster, slightly less optimal diff.
 * This method allows the 'checklines' of diff_main() to be optional.
 * Most of the time checklines is wanted, so default to YES.
 * @param text1 Old NSString to be diffed.
 * @param text2 New NSString to be diffed.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1
                            andNewString:(NSString *)text2;
{
  return [self diff_mainOfOldString:text1 andNewString:text2 checkLines:YES];
}

/**
 * Find the differences between two texts.
 * @param text1 Old string to be diffed.
 * @param text2 New string to be diffed.
 * @param checklines Speedup flag.  If NO, then don't run a
 *     line-level diff first to identify the changed areas.
 *     If YES, then run a faster slightly less optimal diff.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1
                            andNewString:(NSString *)text2
                              checkLines:(BOOL)checklines;
{
  // Set a deadline by which time the diff must be complete.
  NSTimeInterval deadline;
  if (Diff_Timeout <= 0) {
    deadline = [[NSDate distantFuture] timeIntervalSinceReferenceDate];
  } else {
    deadline = [[NSDate dateWithTimeIntervalSinceNow:Diff_Timeout] timeIntervalSinceReferenceDate];
  }
  return [self diff_mainOfOldString:text1 andNewString:text2 checkLines:YES deadline:deadline];
}

/**
 * Find the differences between two texts.  Simplifies the problem by
 * stripping any common prefix or suffix off the texts before diffing.
 * @param text1 Old NSString to be diffed.
 * @param text2 New NSString to be diffed.
 * @param checklines Speedup flag.  If NO, then don't run a
 *     line-level diff first to identify the changed areas.
 *     If YES, then run a faster slightly less optimal diff
 * @param deadline Time when the diff should be complete by.  Used
 *     internally for recursive calls.  Users should set DiffTimeout
 *     instead.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1
                            andNewString:(NSString *)text2
                              checkLines:(BOOL)checklines
                                deadline:(NSTimeInterval)deadline;
{
  // Check for null inputs.
  if (text1 == nil || text2 == nil) {
    NSLog(@"Null inputs. (diff_main)");
    return nil;
  }

  // Check for equality (speedup).
  NSMutableArray *diffs;
  if ([text1 isEqualToString:text2]) {
    diffs = [NSMutableArray array];
    if (text1.length != 0) {
      [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:text1]];
    }
    return diffs;
  }

  // Trim off common prefix (speedup).
  NSUInteger commonlength = (NSUInteger)diff_commonPrefix((CFStringRef)text1, (CFStringRef)text2);
  NSString *commonprefix = [text1 substringWithRange:NSMakeRange(0, commonlength)];
  text1 = [text1 substringFromIndex:commonlength];
  text2 = [text2 substringFromIndex:commonlength];

  // Trim off common suffix (speedup).
  commonlength = (NSUInteger)diff_commonSuffix((CFStringRef)text1, (CFStringRef)text2);
  NSString *commonsuffix = [text1 substringFromIndex:text1.length - commonlength];
  text1 = [text1 substringWithRange:NSMakeRange(0, text1.length - commonlength)];
  text2 = [text2 substringWithRange:NSMakeRange(0, text2.length - commonlength)];

  // Compute the diff on the middle block.
  diffs = [self diff_computeFromOldString:text1 andNewString:text2 checkLines:checklines deadline:deadline];

  // Restore the prefix and suffix.
  if (commonprefix.length != 0) {
    [diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL andText:commonprefix] atIndex:0];
  }
  if (commonsuffix.length != 0) {
    [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:commonsuffix]];
  }

  [self diff_cleanupMerge:diffs];
  return diffs;
}

/**
 * Determine the common prefix of two strings.
 * @param text1 First string.
 * @param text2 Second string.
 * @return The number of characters common to the start of each string.
 */
- (NSUInteger)diff_commonPrefixOfFirstString:(NSString *)text1
                             andSecondString:(NSString *)text2;
{
  return (NSUInteger)diff_commonPrefix((CFStringRef)text1, (CFStringRef)text2);
}

/**
 * Determine the common suffix of two strings.
 * @param text1 First string.
 * @param text2 Second string.
 * @return The number of characters common to the end of each string.
 */
- (NSUInteger)diff_commonSuffixOfFirstString:(NSString *)text1
                             andSecondString:(NSString *)text2;
{
  return (NSUInteger)diff_commonSuffix((CFStringRef)text1, (CFStringRef)text2);
}

/**
 * Determine if the suffix of one CFStringRef is the prefix of another.
 * @param text1 First NSString.
 * @param text2 Second NSString.
 * @return The number of characters common to the end of the first
 *     CFStringRef and the start of the second CFStringRef.
 */
- (NSUInteger)diff_commonOverlapOfFirstString:(NSString *)text1
                              andSecondString:(NSString *)text2;
{
  return (NSUInteger)diff_commonOverlap((CFStringRef)text1, (CFStringRef)text2);
}

/**
 * Do the two texts share a substring which is at least half the length of
 * the longer text?
 * This speedup can produce non-minimal diffs.
 * @param text1 First NSString.
 * @param text2 Second NSString.
 * @return Five element String array, containing the prefix of text1, the
 *     suffix of text1, the prefix of text2, the suffix of text2 and the
 *     common middle.   Or NULL if there was no match.
 */
- (NSArray *)diff_halfMatchOfFirstString:(NSString *)text1
                         andSecondString:(NSString *)text2;
{
  return [(NSArray *)diff_halfMatchCreate((CFStringRef)text1, (CFStringRef)text2, Diff_Timeout) autorelease];
}

/**
 * Does a substring of shorttext exist within longtext such that the
 * substring is at least half the length of longtext?
 * @param longtext Longer NSString.
 * @param shorttext Shorter NSString.
 * @param index Start index of quarter length substring within longtext.
 * @return Five element NSArray, containing the prefix of longtext, the
 *     suffix of longtext, the prefix of shorttext, the suffix of shorttext
 *     and the common middle.  Or nil if there was no match.
 */
- (NSArray *)diff_halfMatchIOfLongString:(NSString *)longtext
                          andShortString:(NSString *)shorttext
                                   index:(NSInteger)index;
{
  return [((NSArray *)diff_halfMatchICreate((CFStringRef)longtext, (CFStringRef)shorttext, (CFIndex)index)) autorelease];
}

/**
 * Find the differences between two texts.  Assumes that the texts do not
 * have any common prefix or suffix.
 * @param text1 Old NSString to be diffed.
 * @param text2 New NSString to be diffed.
 * @param checklines Speedup flag.  If NO, then don't run a
 *     line-level diff first to identify the changed areas.
 *     If YES, then run a faster slightly less optimal diff.
 * @param deadline Time the diff should be complete by.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_computeFromOldString:(NSString *)text1
                                 andNewString:(NSString *)text2
                                   checkLines:(BOOL)checklines
                                     deadline:(NSTimeInterval)deadline;
{
  NSMutableArray *diffs = [NSMutableArray array];

  if (text1.length == 0) {
    // Just add some text (speedup).
    [diffs addObject:[Diff diffWithOperation:DIFF_INSERT andText:text2]];
    return diffs;
  }

  if (text2.length == 0) {
    // Just delete some text (speedup).
    [diffs addObject:[Diff diffWithOperation:DIFF_DELETE andText:text1]];
    return diffs;
  }

  NSString *longtext = text1.length > text2.length ? text1 : text2;
  NSString *shorttext = text1.length > text2.length ? text2 : text1;
  NSUInteger i = [longtext rangeOfString:shorttext].location;
  if (i != NSNotFound) {
    // Shorter text is inside the longer text (speedup).
    Operation op = (text1.length > text2.length) ? DIFF_DELETE : DIFF_INSERT;
    [diffs addObject:[Diff diffWithOperation:op andText:[longtext substringWithRange:NSMakeRange(0, i)]]];
    [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:shorttext]];
    [diffs addObject:[Diff diffWithOperation:op andText:[longtext substringFromIndex:(i + shorttext.length)]]];
    return diffs;
  }

  if (shorttext.length == 1) {
    // Single character string.
    // After the previous speedup, the character can't be an equality.
    [diffs addObject:[Diff diffWithOperation:DIFF_DELETE andText:text1]];
    [diffs addObject:[Diff diffWithOperation:DIFF_INSERT andText:text2]];
    return diffs;
  }

  // Check to see if the problem can be split in two.
  NSArray *hm = [(NSArray *)diff_halfMatchCreate((CFStringRef)text1, (CFStringRef)text2, Diff_Timeout) autorelease];
  if (hm != nil) {
    NSAutoreleasePool *splitPool = [NSAutoreleasePool new];
    // A half-match was found, sort out the return data.
    NSString *text1_a = [hm objectAtIndex:0];
    NSString *text1_b = [hm objectAtIndex:1];
    NSString *text2_a = [hm objectAtIndex:2];
    NSString *text2_b = [hm objectAtIndex:3];
    NSString *mid_common = [hm objectAtIndex:4];
    // Send both pairs off for separate processing.
    NSMutableArray *diffs_a = [self diff_mainOfOldString:text1_a andNewString:text2_a checkLines:checklines deadline:deadline];
    NSMutableArray *diffs_b = [self diff_mainOfOldString:text1_b andNewString:text2_b checkLines:checklines deadline:deadline];
    // Merge the results.
    diffs = [diffs_a retain];
    [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:mid_common]];
    [diffs addObjectsFromArray:diffs_b];
    [splitPool drain];
    return [diffs autorelease];
  }

  if (checklines && text1.length > 100 && text2.length > 100) {
    return [self diff_lineModeFromOldString:text1 andNewString:text2 deadline:deadline];
  }

  NSAutoreleasePool *bisectPool = [NSAutoreleasePool new];
  diffs = [self diff_bisectOfOldString:text1 andNewString:text2 deadline:deadline];
  [diffs retain];
  [bisectPool drain];

  return [diffs autorelease];
}

/**
 * Do a quick line-level diff on both strings, then rediff the parts for
 * greater accuracy.
 * This speedup can produce non-minimal diffs.
 * @param text1 Old NSString to be diffed.
 * @param text2 New NSString to be diffed.
 * @param deadline Time when the diff should be complete by.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_lineModeFromOldString:(NSString *)text1
                                  andNewString:(NSString *)text2
                                      deadline:(NSTimeInterval)deadline;
{
  // Scan the text on a line-by-line basis first.
  NSArray *a = [self diff_linesToCharsForFirstString:text1 andSecondString:text2];
  text1 = (NSString *)[a objectAtIndex:0];
  text2 = (NSString *)[a objectAtIndex:1];
  NSMutableArray *linearray = (NSMutableArray *)[a objectAtIndex:2];

  NSAutoreleasePool *recursePool = [NSAutoreleasePool new];
  NSMutableArray *diffs = [self diff_mainOfOldString:text1 andNewString:text2 checkLines:NO deadline:deadline];
  [diffs retain];
  [recursePool drain];

  [diffs autorelease];

  // Convert the diff back to original text.
  [self diff_chars:diffs toLines:linearray];
  // Eliminate freak matches (e.g. blank lines)
  [self diff_cleanupSemantic:diffs];

  // Rediff any Replacement blocks, this time character-by-character.
  // Add a dummy entry at the end.
  [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:@""]];
  NSUInteger thisPointer = 0;
  NSUInteger count_delete = 0;
  NSUInteger count_insert = 0;
  NSString *text_delete = @"";
  NSString *text_insert = @"";
  while (thisPointer < diffs.count) {
    switch (((Diff *)[diffs objectAtIndex:thisPointer]).operation) {
      case DIFF_INSERT:
        count_insert++;
        text_insert = [text_insert stringByAppendingString:((Diff *)[diffs objectAtIndex:thisPointer]).text];
        break;
      case DIFF_DELETE:
        count_delete++;
        text_delete = [text_delete stringByAppendingString:((Diff *)[diffs objectAtIndex:thisPointer]).text];
        break;
      case DIFF_EQUAL:
        // Upon reaching an equality, check for prior redundancies.
        if (count_delete >= 1 && count_insert >= 1) {
          // Delete the offending records and add the merged ones.
          NSMutableArray *subDiff = [self diff_mainOfOldString:text_delete andNewString:text_insert checkLines:NO deadline:deadline];
          [diffs removeObjectsInRange:NSMakeRange(thisPointer - count_delete - count_insert,
                                                  count_delete + count_insert)];
          thisPointer = thisPointer - count_delete - count_insert;
          NSUInteger insertionIndex = thisPointer;
          for (Diff *thisDiff in subDiff) {
            [diffs insertObject:thisDiff atIndex:insertionIndex];
            insertionIndex++;
          }
          thisPointer = thisPointer + subDiff.count;
        }
        count_insert = 0;
        count_delete = 0;
        text_delete = @"";
        text_insert = @"";
        break;
    }
    thisPointer++;
  }
  [diffs removeLastObject];  // Remove the dummy entry at the end.

  return diffs;
}

/**
 * Find the 'middle snake' of a diff, split the problem in two
 * and return the recursively constructed diff.
 * See Myers 1986 paper: An O(ND) Difference Algorithm and Its Variations.
 * @param _text1 Old string to be diffed.
 * @param _text2 New string to be diffed.
 * @param deadline Time at which to bail if not yet complete.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_bisectOfOldString:(NSString *)_text1
                              andNewString:(NSString *)_text2
                                  deadline:(NSTimeInterval)deadline;
{
#define text1CharacterAtIndex(A)  text1_chars[(A)]
#define text2CharacterAtIndex(A)  text2_chars[(A)]
#define freeTextBuffers()  if (text1_buffer != NULL) free(text1_buffer);\
                           if (text2_buffer != NULL) free(text2_buffer);

  CFStringRef text1 = (CFStringRef)_text1;
  CFStringRef text2 = (CFStringRef)_text2;

  // Cache the text lengths to prevent multiple calls.
  CFIndex text1_length = CFStringGetLength(text1);
  CFIndex text2_length = CFStringGetLength(text2);
  CFIndex max_d = (text1_length + text2_length + 1) / 2;
  CFIndex v_offset = max_d;
  CFIndex v_length = 2 * max_d;
  CFIndex v1[v_length];
  CFIndex v2[v_length];
  for (CFIndex x = 0; x < v_length; x++) {
    v1[x] = -1;
    v2[x] = -1;
  }
  v1[v_offset + 1] = 0;
  v2[v_offset + 1] = 0;
  CFIndex delta = text1_length - text2_length;

  // Prepare access to chars arrays for text1 (massive speedup).
  const UniChar *text1_chars;
  UniChar *text1_buffer = NULL;
  diff_CFStringPrepareUniCharBuffer(text1, &text1_chars, &text1_buffer, CFRangeMake(0, text1_length));

  // Prepare access to chars arrays for text 2 (massive speedup).
  const UniChar *text2_chars;
  UniChar *text2_buffer = NULL;
  diff_CFStringPrepareUniCharBuffer(text2, &text2_chars, &text2_buffer, CFRangeMake(0, text2_length));

  // If the total number of characters is odd, then the front path will
  // collide with the reverse path.
  BOOL front = (delta % 2 != 0);
  // Offsets for start and end of k loop.
  // Prevents mapping of space beyond the grid.
  CFIndex k1start = 0;
  CFIndex k1end = 0;
  CFIndex k2start = 0;
  CFIndex k2end = 0;
  NSMutableArray *diffs;
  for (CFIndex d = 0; d < max_d; d++) {
    // Bail out if deadline is reached.
    if ([NSDate timeIntervalSinceReferenceDate] > deadline) {
      break;
    }

    // Walk the front path one step.
    for (CFIndex k1 = -d + k1start; k1 <= d - k1end; k1 += 2) {
      CFIndex k1_offset = v_offset + k1;
      CFIndex x1;
      if (k1 == -d || (k1 != d && v1[k1_offset - 1] < v1[k1_offset + 1])) {
        x1 = v1[k1_offset + 1];
      } else {
        x1 = v1[k1_offset - 1] + 1;
      }
      CFIndex y1 = x1 - k1;
      while (x1 < text1_length && y1 < text2_length
           && text1CharacterAtIndex(x1) == text2CharacterAtIndex(y1)) {
        x1++;
        y1++;
      }
      v1[k1_offset] = x1;
      if (x1 > text1_length) {
        // Ran off the right of the graph.
        k1end += 2;
      } else if (y1 > text2_length) {
        // Ran off the bottom of the graph.
        k1start += 2;
      } else if (front) {
        CFIndex k2_offset = v_offset + delta - k1;
        if (k2_offset >= 0 && k2_offset < v_length && v2[k2_offset] != -1) {
          // Mirror x2 onto top-left coordinate system.
          CFIndex x2 = text1_length - v2[k2_offset];
          if (x1 >= x2) {
            freeTextBuffers();

            // Overlap detected.
            return [self diff_bisectSplitOfOldString:_text1
                                        andNewString:_text2
                                                   x:x1
                                                   y:y1
                                            deadline:deadline];
          }
        }
      }
    }

    // Walk the reverse path one step.
    for (CFIndex k2 = -d + k2start; k2 <= d - k2end; k2 += 2) {
      CFIndex k2_offset = v_offset + k2;
      CFIndex x2;
      if (k2 == -d || (k2 != d && v2[k2_offset - 1] < v2[k2_offset + 1])) {
        x2 = v2[k2_offset + 1];
      } else {
        x2 = v2[k2_offset - 1] + 1;
      }
      CFIndex y2 = x2 - k2;
      while (x2 < text1_length && y2 < text2_length
           && text1CharacterAtIndex(text1_length - x2 - 1)
           == text2CharacterAtIndex(text2_length - y2 - 1)) {
        x2++;
        y2++;
      }
      v2[k2_offset] = x2;
      if (x2 > text1_length) {
        // Ran off the left of the graph.
        k2end += 2;
      } else if (y2 > text2_length) {
        // Ran off the top of the graph.
        k2start += 2;
      } else if (!front) {
        CFIndex k1_offset = v_offset + delta - k2;
        if (k1_offset >= 0 && k1_offset < v_length && v1[k1_offset] != -1) {
          CFIndex x1 = v1[k1_offset];
          CFIndex y1 = v_offset + x1 - k1_offset;
          // Mirror x2 onto top-left coordinate system.
          x2 = text1_length - x2;
          if (x1 >= x2) {
            // Overlap detected.
            freeTextBuffers();

            return [self diff_bisectSplitOfOldString:_text1
                                        andNewString:_text2
                                                   x:x1
                                                   y:y1
                                            deadline:deadline];
          }
        }
      }
    }
  }

  freeTextBuffers();

  // Diff took too long and hit the deadline or
  // number of diffs equals number of characters, no commonality at all.
  diffs = [NSMutableArray array];
  [diffs addObject:[Diff diffWithOperation:DIFF_DELETE andText:_text1]];
  [diffs addObject:[Diff diffWithOperation:DIFF_INSERT andText:_text2]];
  return diffs;

#undef freeTextBuffers
#undef text1CharacterAtIndex
#undef text2CharacterAtIndex
}

/**
 * Given the location of the 'middle snake', split the diff in two parts
 * and recurse.
 * @param text1 Old string to be diffed.
 * @param text2 New string to be diffed.
 * @param x Index of split point in text1.
 * @param y Index of split point in text2.
 * @param deadline Time at which to bail if not yet complete.
 * @return NSMutableArray of Diff objects.
 */
- (NSMutableArray *)diff_bisectSplitOfOldString:(NSString *)text1
                                   andNewString:(NSString *)text2
                                              x:(NSUInteger)x
                                              y:(NSUInteger)y
                                       deadline:(NSTimeInterval)deadline;
{
  NSString *text1a = [text1 substringToIndex:x];
  NSString *text2a = [text2 substringToIndex:y];
  NSString *text1b = [text1 substringFromIndex:x];
  NSString *text2b = [text2 substringFromIndex:y];

  // Compute both diffs serially.
  NSMutableArray *diffs = [self diff_mainOfOldString:text1a
                                        andNewString:text2a
                                          checkLines:NO
                                            deadline:deadline];
  NSMutableArray *diffsb = [self diff_mainOfOldString:text1b
                                         andNewString:text2b
                                           checkLines:NO
                                             deadline:deadline];

  [diffs addObjectsFromArray: diffsb];
  return diffs;
}

/**
 * Split two texts into a list of strings.  Reduce the texts to a string of
 * hashes where each Unicode character represents one line.
 * @param text1 First NSString.
 * @param text2 Second NSString.
 * @return Three element NSArray, containing the encoded text1, the
 *     encoded text2 and the NSMutableArray of unique strings. The zeroth element
 *     of the NSArray of unique strings is intentionally blank.
 */
- (NSArray *)diff_linesToCharsForFirstString:(NSString *)text1
                             andSecondString:(NSString *)text2;
{
  NSMutableArray *lineArray = [NSMutableArray array]; // NSString objects
  NSMutableDictionary *lineHash = [NSMutableDictionary dictionary]; // keys: NSString, values:NSNumber
  // e.g. [lineArray objectAtIndex:4] == "Hello\n"
  // e.g. [lineHash objectForKey:"Hello\n"] == 4

  // "\x00" is a valid character, but various debuggers don't like it.
  // So we'll insert a junk entry to avoid generating a nil character.
  [lineArray addObject:@""];

  // Allocate 2/3rds of the space for text1, the rest for text2.
  NSString *chars1 = (NSString *)diff_linesToCharsMungeCFStringCreate((CFStringRef)text1,
                                                                      (CFMutableArrayRef)lineArray,
                                                                      (CFMutableDictionaryRef)lineHash,
                                                                      40000);
  NSString *chars2 = (NSString *)diff_linesToCharsMungeCFStringCreate((CFStringRef)text2,
                                                                      (CFMutableArrayRef)lineArray,
                                                                      (CFMutableDictionaryRef)lineHash,
                                                                      65535);

  NSArray *result = [NSArray arrayWithObjects:chars1, chars2, lineArray, nil];

  [chars1 release];
  [chars2 release];

  return result;
}

/**
 * Rehydrate the text in a diff from an NSString of line hashes to real lines
 * of text.
 * @param diffs NSArray of Diff objects.
 * @param lineArray NSMutableArray of unique strings.
 */
- (void)diff_chars:(NSArray *)diffs toLines:(NSMutableArray *)lineArray;
{
  NSMutableString *text;
  NSUInteger lineHash;
  for (Diff *diff in diffs) {
    text = [NSMutableString string];
    for (NSUInteger j = 0; j < [diff.text length]; j++) {
      lineHash = (NSUInteger)[diff.text characterAtIndex:j];
      [text appendString:[lineArray objectAtIndex:lineHash]];
    }
    diff.text = text;
  }
}

/**
 * Reorder and merge like edit sections.  Merge equalities.
 * Any edit section can move as long as it doesn't cross an equality.
 * @param diffs NSMutableArray of Diff objects.
 */
- (void)diff_cleanupMerge:(NSMutableArray *)diffs;
{
#define prevDiff ((Diff *)[diffs objectAtIndex:(thisPointer - 1)])
#define thisDiff ((Diff *)[diffs objectAtIndex:thisPointer])
#define nextDiff ((Diff *)[diffs objectAtIndex:(thisPointer + 1)])

  if (diffs.count == 0) {
    return;
  }

  // Add a dummy entry at the end.
  [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:@""]];
  NSUInteger thisPointer = 0;
  NSUInteger count_delete = 0;
  NSUInteger count_insert = 0;
  NSString *text_delete = @"";
  NSString *text_insert = @"";
  NSUInteger commonlength;
  while (thisPointer < diffs.count) {
    switch (thisDiff.operation) {
      case DIFF_INSERT:
        count_insert++;
        text_insert = [text_insert stringByAppendingString:thisDiff.text];
        thisPointer++;
        break;
      case DIFF_DELETE:
        count_delete++;
        text_delete = [text_delete stringByAppendingString:thisDiff.text];
        thisPointer++;
        break;
      case DIFF_EQUAL:
        // Upon reaching an equality, check for prior redundancies.
        if (count_delete + count_insert > 1) {
          if (count_delete != 0 && count_insert != 0) {
            // Factor out any common prefixes.
            commonlength = (NSUInteger)diff_commonPrefix((CFStringRef)text_insert, (CFStringRef)text_delete);
            if (commonlength != 0) {
              if ((thisPointer - count_delete - count_insert) > 0 &&
                  ((Diff *)[diffs objectAtIndex:(thisPointer - count_delete - count_insert - 1)]).operation
                  == DIFF_EQUAL) {
                ((Diff *)[diffs objectAtIndex:(thisPointer - count_delete - count_insert - 1)]).text
                    = [((Diff *)[diffs objectAtIndex:(thisPointer - count_delete - count_insert - 1)]).text
                       stringByAppendingString:[text_insert substringWithRange:NSMakeRange(0, commonlength)]];
              } else {
                [diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL
                                                    andText:[text_insert substringWithRange:NSMakeRange(0, commonlength)]]
                            atIndex:0];
                thisPointer++;
              }
              text_insert = [text_insert substringFromIndex:commonlength];
              text_delete = [text_delete substringFromIndex:commonlength];
            }
            // Factor out any common suffixes.
            commonlength = (NSUInteger)diff_commonSuffix((CFStringRef)text_insert, (CFStringRef)text_delete);
            if (commonlength != 0) {
              thisDiff.text = [[text_insert substringFromIndex:(text_insert.length
                  - commonlength)] stringByAppendingString:thisDiff.text];
              text_insert = [text_insert substringWithRange:NSMakeRange(0,
                  text_insert.length - commonlength)];
              text_delete = [text_delete substringWithRange:NSMakeRange(0,
                  text_delete.length - commonlength)];
            }
          }
          // Delete the offending records and add the merged ones.
          thisPointer -= count_delete + count_insert;

          splice(diffs, thisPointer, count_delete + count_insert, nil);
          if ([text_delete length] != 0) {
            splice(diffs, thisPointer, 0,
                   [NSMutableArray arrayWithObject:[Diff diffWithOperation:DIFF_DELETE andText:text_delete]]);
            thisPointer++;
          }
          if ([text_insert length] != 0) {
            splice(diffs, thisPointer, 0,
                   [NSMutableArray arrayWithObject:[Diff diffWithOperation:DIFF_INSERT andText:text_insert]]);
            thisPointer++;
          }
          thisPointer++;
        } else if (thisPointer != 0 && prevDiff.operation == DIFF_EQUAL) {
          // Merge this equality with the previous one.
          prevDiff.text = [prevDiff.text stringByAppendingString:thisDiff.text];
          [diffs removeObjectAtIndex:thisPointer];
        } else {
          thisPointer++;
        }
        count_insert = 0;
        count_delete = 0;
        text_delete = @"";
        text_insert = @"";
        break;
    }
  }
  if (((Diff *)diffs.lastObject).text.length == 0) {
    [diffs removeLastObject];  // Remove the dummy entry at the end.
  }

  // Second pass: look for single edits surrounded on both sides by
  // equalities which can be shifted sideways to eliminate an equality.
  // e.g: A<ins>BA</ins>C -> <ins>AB</ins>AC
  BOOL changes = NO;
  thisPointer = 1;
  // Intentionally ignore the first and last element (don't need checking).
  while (thisPointer < (diffs.count - 1)) {
    if (prevDiff.operation == DIFF_EQUAL &&
      nextDiff.operation == DIFF_EQUAL) {
      // This is a single edit surrounded by equalities.
      if ([prevDiff.text length] == 0) {
        splice(diffs, thisPointer - 1, 1, nil);
        changes = YES;
      } else if ([thisDiff.text hasSuffix:prevDiff.text]) {
        // Shift the edit over the previous equality.
        thisDiff.text = [prevDiff.text stringByAppendingString:
            [thisDiff.text substringWithRange:NSMakeRange(0, thisDiff.text.length - prevDiff.text.length)]];
        nextDiff.text = [prevDiff.text stringByAppendingString:nextDiff.text];
        splice(diffs, thisPointer - 1, 1, nil);
        changes = YES;
      } else if ([thisDiff.text hasPrefix:nextDiff.text]) {
        // Shift the edit over the next equality.
        prevDiff.text = [prevDiff.text stringByAppendingString:nextDiff.text];
        thisDiff.text = [[thisDiff.text substringFromIndex:nextDiff.text.length] stringByAppendingString:nextDiff.text];
        splice(diffs, thisPointer + 1, 1, nil);
        changes = YES;
      }
    }
    thisPointer++;
  }
  // If shifts were made, the diff needs reordering and another shift sweep.
  if (changes) {
    [self diff_cleanupMerge:diffs];
  }

#undef prevDiff
#undef thisDiff
#undef nextDiff
}


/**
 * Look for single edits surrounded on both sides by equalities
 * which can be shifted sideways to align the edit to a word boundary.
 * e.g: The c<ins>at c</ins>ame. -> The <ins>cat </ins>came.
 * @param diffs NSMutableArray of Diff objects.
 */
- (void)diff_cleanupSemanticLossless:(NSMutableArray *)diffs;
{
#define prevDiff ((Diff *)[diffs objectAtIndex:(thisPointer - 1)])
#define thisDiff ((Diff *)[diffs objectAtIndex:thisPointer])
#define nextDiff ((Diff *)[diffs objectAtIndex:(thisPointer + 1)])

  if (diffs.count == 0) {
    return;
  }

  NSUInteger thisPointer = 1;
  // Intentionally ignore the first and last element (don't need checking).
  while (thisPointer < (diffs.count - 1)) {
    if (prevDiff.operation == DIFF_EQUAL && nextDiff.operation == DIFF_EQUAL) {
      // This is a single edit surrounded by equalities.
      NSString *equality1 = prevDiff.text;
      NSString *edit = thisDiff.text;
      NSString *equality2 = nextDiff.text;

      // First, shift the edit as far left as possible.
      NSUInteger commonOffset = (NSUInteger)diff_commonSuffix((CFStringRef)equality1, (CFStringRef)edit);

      if (commonOffset > 0) {
        NSString *commonString = [edit substringFromIndex:(edit.length - commonOffset)];
        equality1 = [equality1 substringWithRange:NSMakeRange(0, (equality1.length - commonOffset))];
        edit = [commonString stringByAppendingString:[edit substringWithRange:NSMakeRange(0, (edit.length - commonOffset))]];
        equality2 = [commonString stringByAppendingString:equality2];
      }

      // Second, step right character by character,
      // looking for the best fit.
      NSString *bestEquality1 = equality1;
      NSString *bestEdit = edit;
      NSString *bestEquality2 = equality2;
      CFIndex bestScore = diff_cleanupSemanticScore((CFStringRef)equality1, (CFStringRef)edit) +
      diff_cleanupSemanticScore((CFStringRef)edit, (CFStringRef)equality2);
      while (edit.length != 0 && equality2.length != 0
           && [edit characterAtIndex:0] == [equality2 characterAtIndex:0]) {
        equality1 = [equality1 stringByAppendingString:[edit substringWithRange:NSMakeRange(0, 1)]];
        edit = [[edit substringFromIndex:1] stringByAppendingString:[equality2 substringWithRange:NSMakeRange(0, 1)]];
        equality2 = [equality2 substringFromIndex:1];
        CFIndex score = diff_cleanupSemanticScore((CFStringRef)equality1, (CFStringRef)edit) +
        diff_cleanupSemanticScore((CFStringRef)edit, (CFStringRef)equality2);
        // The >= encourages trailing rather than leading whitespace on edits.
        if (score >= bestScore) {
          bestScore = score;
          bestEquality1 = equality1;
          bestEdit = edit;
          bestEquality2 = equality2;
        }
      }

      if (prevDiff.text != bestEquality1) {
        // We have an improvement, save it back to the diff.
        if (bestEquality1.length != 0) {
          prevDiff.text = bestEquality1;
        } else {
          [diffs removeObjectAtIndex:thisPointer - 1];
          thisPointer--;
        }
        thisDiff.text = bestEdit;
        if (bestEquality2.length != 0) {
          nextDiff.text = bestEquality2;
        } else {
          [diffs removeObjectAtIndex:thisPointer + 1];
          thisPointer--;
        }
      }
    }
    thisPointer++;
  }

#undef prevDiff
#undef thisDiff
#undef nextDiff
}

/**
 * Given two strings, comAdde a score representing whether the internal
 * boundary falls on logical boundaries.
 * Scores range from 5 (best) to 0 (worst).
 * @param one First string.
 * @param two Second string.
 * @return The score.
 */
- (NSInteger)diff_cleanupSemanticScoreOfFirstString:(NSString *)one
                                    andSecondString:(NSString *)two;
{
  return diff_cleanupSemanticScore((CFStringRef)one, (CFStringRef)two);
}

/**
 * Reduce the number of edits by eliminating operationally trivial
 * equalities.
 * @param diffs NSMutableArray of Diff objects.
 */
- (void)diff_cleanupEfficiency:(NSMutableArray *)diffs;
{
#define thisDiff ((Diff *)[diffs objectAtIndex:thisPointer])
#define equalitiesLastItem ((NSNumber *)equalities.lastObject)
#define equalitiesLastValue ((NSNumber *)equalities.lastObject).integerValue
  if (diffs.count == 0) {
    return;
  }

  BOOL changes = NO;
  // Stack of indices where equalities are found.
  NSMutableArray *equalities = [NSMutableArray array];
  // Always equal to equalities.lastObject.text
  NSString *lastEquality = nil;
  NSInteger thisPointer = 0;  // Index of current position.
  // Is there an insertion operation before the last equality.
  BOOL pre_ins = NO;
  // Is there a deletion operation before the last equality.
  BOOL pre_del = NO;
  // Is there an insertion operation after the last equality.
  BOOL post_ins = NO;
  // Is there a deletion operation after the last equality.
  BOOL post_del = NO;

  NSUInteger indexToChange;
  Diff *diffToChange;

  while (thisPointer < (NSInteger)diffs.count) {
    if (thisDiff.operation == DIFF_EQUAL) {  // Equality found.
      if (thisDiff.text.length < Diff_EditCost && (post_ins || post_del)) {
        // Candidate found.
        [equalities addObject:[NSNumber numberWithInteger:thisPointer]];
        pre_ins = post_ins;
        pre_del = post_del;
        lastEquality = thisDiff.text;
      } else {
        // Not a candidate, and can never become one.
        [equalities removeAllObjects];
        lastEquality = nil;
      }
      post_ins = post_del = NO;
    } else {  // An insertion or deletion.
      if (thisDiff.operation == DIFF_DELETE) {
        post_del = YES;
      } else {
        post_ins = YES;
      }
      /*
       * Five types to be split:
       * <ins>A</ins><del>B</del>XY<ins>C</ins><del>D</del>
       * <ins>A</ins>X<ins>C</ins><del>D</del>
       * <ins>A</ins><del>B</del>X<ins>C</ins>
       * <ins>A</del>X<ins>C</ins><del>D</del>
       * <ins>A</ins><del>B</del>X<del>C</del>
       */
      if (lastEquality != nil
          && ((pre_ins && pre_del && post_ins && post_del)
          || ((lastEquality.length < Diff_EditCost / 2)
          && ((pre_ins ? 1 : 0) + (pre_del ? 1 : 0) + (post_ins ? 1 : 0)
          + (post_del ? 1 : 0)) == 3))) {
        // Duplicate record.
        [diffs insertObject:[Diff diffWithOperation:DIFF_DELETE andText:lastEquality]
                    atIndex:equalitiesLastValue];
        // Change second copy to insert.
        // Hash values for objects must not change while in a collection
        indexToChange = equalitiesLastValue + 1;
        diffToChange = [[diffs objectAtIndex:indexToChange] retain];
        [diffs replaceObjectAtIndex:indexToChange withObject:[NSNull null]];
        diffToChange.operation = DIFF_INSERT;
        [diffs replaceObjectAtIndex:indexToChange withObject:diffToChange];
        [diffToChange release];

        [equalities removeLastObject];   // Throw away the equality we just deleted.
        lastEquality = nil;
        if (pre_ins && pre_del) {
          // No changes made which could affect previous entry, keep going.
          post_ins = post_del = YES;
          [equalities removeAllObjects];
        } else {
          if (equalities.count > 0) {
            [equalities removeLastObject];
          }

          thisPointer = equalities.count > 0 ? equalitiesLastValue : -1;
          post_ins = post_del = NO;
        }
        changes = YES;
      }
    }
    thisPointer++;
  }

  if (changes) {
    [self diff_cleanupMerge:diffs];
  }

#undef thisDiff
#undef equalitiesLastItem
#undef equalitiesLastValue
}

/**
 * Convert a Diff list into a pretty HTML report.
 * @param diffs NSMutableArray of Diff objects.
 * @return HTML representation.
 */
- (NSString *)diff_prettyHtml:(NSMutableArray *)diffs;
{
  NSMutableString *html = [NSMutableString string];
  for (Diff *aDiff in diffs) {
    NSMutableString *text = [[aDiff.text mutableCopy] autorelease];
    [text replaceOccurrencesOfString:@"&" withString:@"&amp;" options:NSLiteralSearch range:NSMakeRange(0, text.length)];
    [text replaceOccurrencesOfString:@"<" withString:@"&lt;" options:NSLiteralSearch range:NSMakeRange(0, text.length)];
    [text replaceOccurrencesOfString:@">" withString:@"&gt;" options:NSLiteralSearch range:NSMakeRange(0, text.length)];
    [text replaceOccurrencesOfString:@"\n" withString:@"&para;<br>" options:NSLiteralSearch range:NSMakeRange(0, text.length)];

    switch (aDiff.operation) {
      case DIFF_INSERT:
        [html appendFormat:@"<ins style=\"background:#e6ffe6;\">%@</ins>", text];
        break;
      case DIFF_DELETE:
        [html appendFormat:@"<del style=\"background:#ffe6e6;\">%@</del>", text];
        break;
      case DIFF_EQUAL:
        [html appendFormat:@"<span>%@</span>", text];
        break;
    }
  }
  return html;
}

/**
 * Compute and return the source text (all equalities and deletions).
 * @param diffs NSMutableArray of Diff objects.
 * @return Source text.
 */
- (NSString *)diff_text1:(NSMutableArray *)diffs;
{
  NSMutableString *text = [NSMutableString string];
  for (Diff *aDiff in diffs) {
    if (aDiff.operation != DIFF_INSERT) {
      [text appendString:aDiff.text];
    }
  }
  return text;
}

/**
 * Compute and return the destination text (all equalities and insertions).
 * @param diffs NSMutableArray of Diff objects.
 * @return Destination text.
 */
- (NSString *)diff_text2:(NSMutableArray *)diffs;
{
  NSMutableString *text = [NSMutableString string];
  for (Diff *aDiff in diffs) {
    if (aDiff.operation != DIFF_DELETE) {
      [text appendString:aDiff.text];
    }
  }
  return text;
}

/**
 * Crush the diff into an encoded NSString which describes the operations
 * required to transform text1 into text2.
 * E.g. =3\t-2\t+ing  -> Keep 3 chars, delete 2 chars, insert 'ing'.
 * Operations are tab-separated.  Inserted text is escaped using %xx
 * notation.
 * @param diffs NSMutableArray of Diff objects.
 * @return Delta text.
 */
- (NSString *)diff_toDelta:(NSMutableArray *)diffs;
{
  NSMutableString *delta = [NSMutableString string];
  for (Diff *aDiff in diffs) {
    switch (aDiff.operation) {
      case DIFF_INSERT:
        [delta appendFormat:@"+%@\t", [[aDiff.text diff_stringByAddingPercentEscapesForEncodeUriCompatibility]
                                       stringByReplacingOccurrencesOfString:@"%20" withString:@" "]];
        break;
      case DIFF_DELETE:
        [delta appendFormat:@"-%" PRId32 "\t", (int32_t)aDiff.text.length];
        break;
      case DIFF_EQUAL:
        [delta appendFormat:@"=%" PRId32 "\t", (int32_t)aDiff.text.length];
        break;
    }
  }

  if (delta.length != 0) {
    // Strip off trailing tab character.
    return [delta substringWithRange:NSMakeRange(0, delta.length-1)];
  }
  return delta;
}

/**
 * Given the original text1, and an encoded NSString which describes the
 * operations required to transform text1 into text2, compute the full diff.
 * @param text1 Source NSString for the diff.
 * @param delta Delta text.
 * @param error NSError if invalid input.
 * @return NSMutableArray of Diff objects or nil if invalid.
 */
- (NSMutableArray *)diff_fromDeltaWithText:(NSString *)text1
                                  andDelta:(NSString *)delta
                                     error:(NSError **)error;
{
  NSMutableArray *diffs = [NSMutableArray array];
  NSUInteger thisPointer = 0;  // Cursor in text1
  NSArray *tokens = [delta componentsSeparatedByString:@"\t"];
  NSInteger n;
  NSDictionary *errorDetail = nil;
  for (NSString *token in tokens) {
    if (token.length == 0) {
      // Blank tokens are ok (from a trailing \t).
      continue;
    }
    // Each token begins with a one character parameter which specifies the
    // operation of this token (delete, insert, equality).
    NSString *param = [token substringFromIndex:1];
    switch ([token characterAtIndex:0]) {
      case '+':
        param = [param diff_stringByReplacingPercentEscapesForEncodeUriCompatibility];
        if (param == nil) {
          if (error != NULL) {
            errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
                [NSString stringWithFormat:NSLocalizedString(@"Invalid character in diff_fromDelta: %@", @"Error"), param],
                NSLocalizedDescriptionKey, nil];
            *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:99 userInfo:errorDetail];
          }
          return nil;
        }
        [diffs addObject:[Diff diffWithOperation:DIFF_INSERT andText:param]];
        break;
      case '-':
        // Fall through.
      case '=':
        n = [param integerValue];
        if (n == 0) {
          if (error != NULL) {
            errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
                [NSString stringWithFormat:NSLocalizedString(@"Invalid number in diff_fromDelta: %@", @"Error"), param],
                NSLocalizedDescriptionKey, nil];
            *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:100 userInfo:errorDetail];
          }
          return nil;
        } else if (n < 0) {
          if (error != NULL) {
            errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
                [NSString stringWithFormat:NSLocalizedString(@"Negative number in diff_fromDelta: %@", @"Error"), param],
                NSLocalizedDescriptionKey, nil];
            *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:101 userInfo:errorDetail];
          }
          return nil;
        }
        NSString *text;
        @try {
          text = [text1 substringWithRange:NSMakeRange(thisPointer, (NSUInteger)n)];
          thisPointer += (NSUInteger)n;
        }
        @catch (NSException *e) {
          if (error != NULL) {
            // CHANGME: Pass on the information contained in e
            errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
                [NSString stringWithFormat:NSLocalizedString(@"Delta length (%lu) larger than source text length (%lu).", @"Error"),
                 (unsigned long)thisPointer, (unsigned long)text1.length],
                NSLocalizedDescriptionKey, nil];
            *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:102 userInfo:errorDetail];
          }
          return nil;
        }
        if ([token characterAtIndex:0] == '=') {
          [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:text]];
        } else {
          [diffs addObject:[Diff diffWithOperation:DIFF_DELETE andText:text]];
        }
        break;
      default:
        // Anything else is an error.
        if (error != NULL) {
          errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
              [NSString stringWithFormat:NSLocalizedString(@"Invalid diff operation in diff_fromDelta: %C", @"Error"),
               [token characterAtIndex:0]],
              NSLocalizedDescriptionKey, nil];
          *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:102 userInfo:errorDetail];
        }
        return nil;
    }
  }
  if (thisPointer != text1.length) {
    if (error != NULL) {
      errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
          [NSString stringWithFormat:NSLocalizedString(@"Delta length (%lu) smaller than source text length (%lu).", @"Error"),
           (unsigned long)thisPointer, (unsigned long)text1.length],
          NSLocalizedDescriptionKey, nil];
      *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:103 userInfo:errorDetail];
    }
    return nil;
  }
  return diffs;
}

/**
 * loc is a location in text1, compute and return the equivalent location in
 * text2.
 * e.g. "The cat" vs "The big cat", 1->1, 5->8
 * @param diffs NSMutableArray of Diff objects.
 * @param loc Location within text1.
 * @return Location within text2.
 */
- (NSUInteger)diff_xIndexIn:(NSMutableArray *)diffs
                   location:(NSUInteger) loc;
{
  NSUInteger chars1 = 0;
  NSUInteger chars2 = 0;
  NSUInteger last_chars1 = 0;
  NSUInteger last_chars2 = 0;
  Diff *lastDiff = nil;
  for (Diff *aDiff in diffs) {
    if (aDiff.operation != DIFF_INSERT) {
      // Equality or deletion.
      chars1 += aDiff.text.length;
    }
    if (aDiff.operation != DIFF_DELETE) {
      // Equality or insertion.
      chars2 += aDiff.text.length;
    }
    if (chars1 > loc) {
      // Overshot the location.
      lastDiff = aDiff;
      break;
    }
    last_chars1 = chars1;
    last_chars2 = chars2;
  }
  if (lastDiff != nil && lastDiff.operation == DIFF_DELETE) {
    // The location was deleted.
    return last_chars2;
  }
  // Add the remaining character length.
  return last_chars2 + (loc - last_chars1);
}

/**
 * Compute the Levenshtein distance; the number of inserted, deleted or
 * substituted characters.
 * @param diffs NSMutableArray of Diff objects.
 * @return Number of changes.
 */
- (NSUInteger)diff_levenshtein:(NSMutableArray *)diffs;
{
  NSUInteger levenshtein = 0;
  NSUInteger insertions = 0;
  NSUInteger deletions = 0;
  for (Diff *aDiff in diffs) {
    switch (aDiff.operation) {
      case DIFF_INSERT:
        insertions += aDiff.text.length;
        break;
      case DIFF_DELETE:
        deletions += aDiff.text.length;
        break;
      case DIFF_EQUAL:
        // A deletion and an insertion is one substitution.
        levenshtein += MAX(insertions, deletions);
        insertions = 0;
        deletions = 0;
        break;
    }
  }
  levenshtein += MAX(insertions, deletions);
  return levenshtein;
}

/**
 * Reduce the number of edits by eliminating semantically trivial
 * equalities.
 * @param diffs NSMutableArray of Diff objects.
 */
- (void)diff_cleanupSemantic:(NSMutableArray *)diffs;
{
#define prevDiff ((Diff *)[diffs objectAtIndex:(thisPointer - 1)])
#define thisDiff ((Diff *)[diffs objectAtIndex:thisPointer])
#define nextDiff ((Diff *)[diffs objectAtIndex:(thisPointer + 1)])
#define equalitiesLastItem ((NSNumber *)equalities.lastObject)
#define equalitiesLastValue ((NSNumber *)equalities.lastObject).integerValue

  if (diffs == nil || diffs.count == 0) {
    return;
  }

  BOOL changes = NO;
  // Stack of indices where equalities are found.
  NSMutableArray *equalities = [NSMutableArray array];
  // Always equal to equalities.lastObject.text
  NSString *lastEquality = nil;
  NSUInteger thisPointer = 0;  // Index of current position.
  // Number of characters that changed prior to the equality.
  NSUInteger length_insertions1 = 0;
  NSUInteger length_deletions1 = 0;
  // Number of characters that changed after the equality.
  NSUInteger length_insertions2 = 0;
  NSUInteger length_deletions2 = 0;

  NSUInteger indexToChange;
  Diff *diffToChange;

  while (thisPointer < diffs.count) {
    if (thisDiff.operation == DIFF_EQUAL) {  // Equality found.
      [equalities addObject:[NSNumber numberWithInteger:thisPointer]];
      length_insertions1 = length_insertions2;
      length_deletions1 = length_deletions2;
      length_insertions2 = 0;
      length_deletions2 = 0;
      lastEquality = thisDiff.text;
    } else {  // an insertion or deletion
      if (thisDiff.operation == DIFF_INSERT) {
        length_insertions2 += thisDiff.text.length;
      } else {
        length_deletions2 += thisDiff.text.length;
      }
      // Eliminate an equality that is smaller or equal to the edits on both
      // sides of it.
      if (lastEquality != nil
          && (lastEquality.length <= MAX(length_insertions1, length_deletions1))
          && (lastEquality.length <= MAX(length_insertions2, length_deletions2))) {
        // Duplicate record.
        [diffs insertObject:[Diff diffWithOperation:DIFF_DELETE andText:lastEquality] atIndex:equalitiesLastValue];
        // Change second copy to insert.
        // Hash values for objects must not change while in a collection.
        indexToChange = equalitiesLastValue + 1;
        diffToChange = [[diffs objectAtIndex:indexToChange] retain];
        [diffs replaceObjectAtIndex:indexToChange withObject:[NSNull null]];
        diffToChange.operation = DIFF_INSERT;
        [diffs replaceObjectAtIndex:indexToChange withObject:diffToChange];
        [diffToChange release];

        // Throw away the equality we just deleted.
        [equalities removeLastObject];
        if (equalities.count > 0) {
          [equalities removeLastObject];
        }
        // Setting an unsigned value to -1 may seem weird to some,
        // but we will pass thru a ++ below:
        // => overflow => 0
        thisPointer = equalities.count > 0 ? equalitiesLastValue : -1;
        length_insertions1 = 0; // Reset the counters.
        length_deletions1 = 0;
        length_insertions2 = 0;
        length_deletions2 = 0;
        lastEquality = nil;
        changes = YES;
      }
    }
    thisPointer++;
  }

  // Normalize the diff.
  if (changes) {
    [self diff_cleanupMerge:diffs];
  }
  [self diff_cleanupSemanticLossless:diffs];

  // Find any overlaps between deletions and insertions.
  // e.g: <del>abcxxx</del><ins>xxxdef</ins>
  //   -> <del>abc</del>xxx<ins>def</ins>
  // e.g: <del>xxxabc</del><ins>defxxx</ins>
  //   -> <ins>def</ins>xxx<del>abc</del>
  // Only extract an overlap if it is as big as the edit ahead or behind it.
  thisPointer = 1;
  while (thisPointer < diffs.count) {
    if (prevDiff.operation == DIFF_DELETE && thisDiff.operation == DIFF_INSERT) {
      NSString *deletion = prevDiff.text;
      NSString *insertion = thisDiff.text;
      NSUInteger overlap_length1 = (NSUInteger)diff_commonOverlap((CFStringRef)deletion, (CFStringRef)insertion);
      NSUInteger overlap_length2 = (NSUInteger)diff_commonOverlap((CFStringRef)insertion, (CFStringRef)deletion);
      if (overlap_length1 >= overlap_length2) {
        if (overlap_length1 >= deletion.length / 2.0 ||
            overlap_length1 >= insertion.length / 2.0) {
          // Overlap found.
          // Insert an equality and trim the surrounding edits.
          [diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL
              andText:[insertion substringWithRange:NSMakeRange(0, overlap_length1)]]
              atIndex:thisPointer];
          prevDiff.text = [deletion substringWithRange:NSMakeRange(0, deletion.length - overlap_length1)];
          nextDiff.text = [insertion substringFromIndex:overlap_length1];
          thisPointer++;
        }
      } else {
        if (overlap_length2 >= deletion.length / 2.0 ||
            overlap_length2 >= insertion.length / 2.0) {
          // Reverse overlap found.
          // Insert an equality and swap and trim the surrounding edits.
          [diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL
              andText:[deletion substringWithRange:NSMakeRange(0, overlap_length2)]]
              atIndex:thisPointer];
          prevDiff.operation = DIFF_INSERT;
          prevDiff.text = [insertion substringWithRange:NSMakeRange(0, insertion.length - overlap_length2)];
          nextDiff.operation = DIFF_DELETE;
          nextDiff.text = [deletion substringFromIndex:overlap_length2];
          thisPointer++;
        }
      }
      thisPointer++;
    }
    thisPointer++;
  }

#undef prevDiff
#undef thisDiff
#undef nextDiff
#undef equalitiesLastItem
#undef equalitiesLastValue
}

#pragma mark Match Functions
//  MATCH FUNCTIONS


/**
 * Locate the best instance of 'pattern' in 'text' near 'loc'.
 * Returns NSNotFound if no match found.
 * @param text The text to search.
 * @param pattern The pattern to search for.
 * @param loc The location to search around.
 * @return Best match index or NSNotFound.
 */
- (NSUInteger)match_mainForText:(NSString *)text
                        pattern:(NSString *)pattern
                           near:(NSUInteger)loc;
{
  // Check for null inputs.
  if (text == nil || pattern == nil) {
    NSLog(@"Null inputs. (match_main)");
    return NSNotFound;
  }
  if (text.length == 0) {
    NSLog(@"Empty text. (match_main)");
    return NSNotFound;
  }

  NSUInteger new_loc;
  new_loc = MIN(loc, text.length);
  new_loc = MAX((NSUInteger)0, new_loc);

  if ([text isEqualToString:pattern]) {
    // Shortcut (potentially not guaranteed by the algorithm)
    return 0;
  } else if (text.length == 0) {
    // Nothing to match.
    return NSNotFound;
  } else if (new_loc + pattern.length <= text.length
      && [[text substringWithRange:NSMakeRange(new_loc, pattern.length)] isEqualToString:pattern]) {
    // Perfect match at the perfect spot!   (Includes case of empty pattern)
    return new_loc;
  } else {
    // Do a fuzzy compare.
    return [self match_bitapOfText:text andPattern:pattern near:new_loc];
  }
}

/**
 * Locate the best instance of 'pattern' in 'text' near 'loc' using the
 * Bitap algorithm.   Returns NSNotFound if no match found.
 * @param text The text to search.
 * @param pattern The pattern to search for.
 * @param loc The location to search around.
 * @return Best match index or NSNotFound.
 */
- (NSUInteger)match_bitapOfText:(NSString *)text
                     andPattern:(NSString *)pattern
                           near:(NSUInteger)loc;
{
  NSAssert((Match_MaxBits == 0 || pattern.length <= Match_MaxBits),
           @"Pattern too long for this application.");

  // Initialise the alphabet.
  NSMutableDictionary *s = [self match_alphabet:pattern];

  // Highest score beyond which we give up.
  double score_threshold = Match_Threshold;
  // Is there a nearby exact match? (speedup)
  NSUInteger best_loc = [text rangeOfString:pattern options:NSLiteralSearch range:NSMakeRange(loc, text.length - loc)].location;
  if (best_loc != NSNotFound) {
    score_threshold = MIN([self match_bitapScoreForErrorCount:0 location:best_loc near:loc pattern:pattern], score_threshold);
    // What about in the other direction? (speedup)
    NSUInteger searchRangeLoc = MIN(loc + pattern.length, text.length);
    NSRange searchRange = NSMakeRange(0, searchRangeLoc);
    best_loc = [text rangeOfString:pattern options:(NSLiteralSearch | NSBackwardsSearch) range:searchRange].location;
    if (best_loc != NSNotFound) {
      score_threshold = MIN([self match_bitapScoreForErrorCount:0 location:best_loc near:loc pattern:pattern], score_threshold);
    }
  }

  // Initialise the bit arrays.
  NSUInteger matchmask = 1 << (pattern.length - 1);
  best_loc = NSNotFound;

  NSUInteger bin_min, bin_mid;
  NSUInteger bin_max = pattern.length + text.length;
  NSUInteger *rd = NULL;
  NSUInteger *last_rd = NULL;
  for (NSUInteger d = 0; d < pattern.length; d++) {
    // Scan for the best match; each iteration allows for one more error.
    // Run a binary search to determine how far from 'loc' we can stray at
    // this error level.
    bin_min = 0;
    bin_mid = bin_max;
    while (bin_min < bin_mid) {
      double score = [self match_bitapScoreForErrorCount:d location:(loc + bin_mid) near:loc pattern:pattern];
      if (score <= score_threshold) {
        bin_min = bin_mid;
      } else {
        bin_max = bin_mid;
      }
      bin_mid = (bin_max - bin_min) / 2 + bin_min;
    }
    // Use the result from this iteration as the maximum for the next.
    bin_max = bin_mid;
    NSUInteger start = MAX_OF_CONST_AND_DIFF(1, loc, bin_mid);
    NSUInteger finish = MIN(loc + bin_mid, text.length) + pattern.length;

    rd = (NSUInteger *)calloc((finish + 2), sizeof(NSUInteger));
    rd[finish + 1] = (1 << d) - 1;

    for (NSUInteger j = finish; j >= start; j--) {
      NSUInteger charMatch;
      if (text.length <= j - 1 || ![s diff_containsObjectForUnicharKey:[text characterAtIndex:(j - 1)]]) {
        // Out of range.
        charMatch = 0;
      } else {
        charMatch = [s diff_unsignedIntegerForUnicharKey:[text characterAtIndex:(j - 1)]];
      }
      if (d == 0) {
        // First pass: exact match.
        rd[j] = ((rd[j + 1] << 1) | 1) & charMatch;
      } else {
        // Subsequent passes: fuzzy match.
        rd[j] = (((rd[j + 1] << 1) | 1) & charMatch)
            | (((last_rd[j + 1] | last_rd[j]) << 1) | 1) | last_rd[j + 1];
      }
      if ((rd[j] & matchmask) != 0) {
        double score = [self match_bitapScoreForErrorCount:d location:(j - 1) near:loc pattern:pattern];
        // This match will almost certainly be better than any existing match.
        // But check anyway.
        if (score <= score_threshold) {
          // Told you so.
          score_threshold = score;
          best_loc = j - 1;
          if (best_loc > loc) {
            // When passing loc, don't exceed our current distance from loc.
            start = MAX_OF_CONST_AND_DIFF(1, 2 * loc, best_loc);
          } else {
            // Already passed loc, downhill from here on in.
            break;
          }
        }
      }
    }
    if ([self match_bitapScoreForErrorCount:(d + 1) location:loc near:loc pattern:pattern] > score_threshold) {
      // No hope for a (better) match at greater error levels.
      break;
    }

    if (last_rd != NULL) {
      free(last_rd);
    }
    last_rd = rd;
  }

  if (rd != NULL && last_rd != rd) {
    free(rd);
  }
  if (last_rd != NULL) {
    free(last_rd);
  }

  return best_loc;
}

/**
 * Compute and return the score for a match with e errors and x location.
 * @param e Number of errors in match.
 * @param x Location of match.
 * @param loc Expected location of match.
 * @param pattern Pattern being sought.
 * @return Overall score for match (0.0 = good, 1.0 = bad).
 */
- (double)match_bitapScoreForErrorCount:(NSUInteger)e
                               location:(NSUInteger)x
                                   near:(NSUInteger)loc
                                pattern:(NSString *)pattern;
{
  double score;

  double accuracy = (double)e / pattern.length;
  NSUInteger proximity = (NSUInteger)ABS((long long)loc - (long long)x);
  if (Match_Distance == 0) {
    // Dodge divide by zero error.
    return proximity == 0 ? accuracy : 1.0;
  }
  score = accuracy + (proximity / (double) Match_Distance);

  return score;
}

/**
 * Initialise the alphabet for the Bitap algorithm.
 * @param pattern The text to encode.
 * @return Hash of character locations
 *     (NSMutableDictionary: keys:NSString/unichar, values:NSNumber/NSUInteger).
 */
- (NSMutableDictionary *)match_alphabet:(NSString *)pattern;
{
  NSMutableDictionary *s = [NSMutableDictionary dictionary];
  CFStringRef str = (CFStringRef)pattern;
  CFStringInlineBuffer inlineBuffer;
  CFIndex length;
  CFIndex cnt;

  length = CFStringGetLength(str);
  CFStringInitInlineBuffer(str, &inlineBuffer, CFRangeMake(0, length));

  UniChar ch;
  CFStringRef c;
  for (cnt = 0; cnt < length; cnt++) {
    ch = CFStringGetCharacterFromInlineBuffer(&inlineBuffer, cnt);
    c = diff_CFStringCreateFromUnichar(ch);
    if (![s diff_containsObjectForKey:(NSString *)c]) {
      [s diff_setUnsignedIntegerValue:0 forKey:(NSString *)c];
    }
    CFRelease(c);
  }

  NSUInteger i = 0;
  for (cnt = 0; cnt < length; cnt++) {
    ch = CFStringGetCharacterFromInlineBuffer(&inlineBuffer, cnt);
    c = diff_CFStringCreateFromUnichar(ch);
    NSUInteger value = [s diff_unsignedIntegerForKey:(NSString *)c] | (1 << (pattern.length - i - 1));
    [s diff_setUnsignedIntegerValue:value forKey:(NSString *)c];
    i++;
    CFRelease(c);
  }
  return s;
}


#pragma mark Patch Functions
//  PATCH FUNCTIONS


/**
 * Increase the context until it is unique,
 * but don't let the pattern expand beyond Match_MaxBits.
 * @param patch The patch to grow.
 * @param text Source text.
 */
- (void)patch_addContextToPatch:(Patch *)patch
                     sourceText:(NSString *)text;
{
  if (text.length == 0) {
    return;
  }
  NSString *pattern = [text substringWithRange:NSMakeRange(patch.start2, patch.length1)];
  NSUInteger padding = 0;

  // Look for the first and last matches of pattern in text.  If two
  // different matches are found, increase the pattern length.
  while ([text rangeOfString:pattern options:NSLiteralSearch].location
      != [text rangeOfString:pattern options:(NSLiteralSearch | NSBackwardsSearch)].location
      && pattern.length < (Match_MaxBits - Patch_Margin - Patch_Margin)) {
    padding += Patch_Margin;
    pattern = [text diff_javaSubstringFromStart:MAX_OF_CONST_AND_DIFF(0, patch.start2, padding)
        toEnd:MIN(text.length, patch.start2 + patch.length1 + padding)];
  }
  // Add one chunk for good luck.
  padding += Patch_Margin;

  // Add the prefix.
  NSString *prefix = [text diff_javaSubstringFromStart:MAX_OF_CONST_AND_DIFF(0, patch.start2, padding)
      toEnd:patch.start2];
  if (prefix.length != 0) {
    [patch.diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL andText:prefix] atIndex:0];
  }
  // Add the suffix.
  NSString *suffix = [text diff_javaSubstringFromStart:(patch.start2 + patch.length1)
      toEnd:MIN(text.length, patch.start2 + patch.length1 + padding)];
  if (suffix.length != 0) {
    [patch.diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:suffix]];
  }

  // Roll back the start points.
  patch.start1 -= prefix.length;
  patch.start2 -= prefix.length;
  // Extend the lengths.
  patch.length1 += prefix.length + suffix.length;
  patch.length2 += prefix.length + suffix.length;
}

/**
 * Compute a list of patches to turn text1 into text2.
 * A set of diffs will be computed.
 * @param text1 Old text.
 * @param text2 New text.
 * @return NSMutableArray of Patch objects.
 */
- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1
                               andNewString:(NSString *)text2;
{
  // Check for null inputs.
  if (text1 == nil || text2 == nil) {
    NSLog(@"Null inputs. (patch_make)");
    return nil;
  }

  // No diffs provided, compute our own.
  NSMutableArray *diffs = [self diff_mainOfOldString:text1 andNewString:text2 checkLines:YES];
  if (diffs.count > 2) {
    [self diff_cleanupSemantic:diffs];
    [self diff_cleanupEfficiency:diffs];
  }

  return [self patch_makeFromOldString:text1 andDiffs:diffs];
}

/**
 * Compute a list of patches to turn text1 into text2.
 * text1 will be derived from the provided diffs.
 * @param diffs NSMutableArray of Diff objects for text1 to text2.
 * @return NSMutableArray of Patch objects.
 */
- (NSMutableArray *)patch_makeFromDiffs:(NSMutableArray *)diffs;
{
  // Check for nil inputs not needed since nil can't be passed in C#.
  // No origin NSString *provided, comAdde our own.
  NSString *text1 = [self diff_text1:diffs];
  return [self patch_makeFromOldString:text1 andDiffs:diffs];
}

/**
 * Compute a list of patches to turn text1 into text2.
 * text2 is ignored, diffs are the delta between text1 and text2.
 * @param text1 Old text
 * @param text2 New text
 * @param diffs NSMutableArray of Diff objects for text1 to text2.
 * @return NSMutableArray of Patch objects.
 * @deprecated Prefer -patch_makeFromOldString:diffs:.
 */
- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1
                                  newString:(NSString *)text2
                                      diffs:(NSMutableArray *)diffs __deprecated;
{
  // Check for null inputs.
  if (text1 == nil || text2 == nil) {
    NSLog(@"Null inputs. (patch_make)");
    return nil;
  }

  return [self patch_makeFromOldString:text1 andDiffs:diffs];
}

/**
 * Compute a list of patches to turn text1 into text2.
 * text2 is not provided, diffs are the delta between text1 and text2.
 * @param text1 Old text.
 * @param diffs NSMutableArray of Diff objects for text1 to text2.
 * @return NSMutableArray of Patch objects.
 */
- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1
                                   andDiffs:(NSMutableArray *)diffs;
{
  // Check for null inputs.
  if (text1 == nil) {
    NSLog(@"Null inputs. (patch_make)");
    return nil;
  }

  NSMutableArray *patches = [NSMutableArray array];
  if (diffs.count == 0) {
    return patches;   // Get rid of the nil case.
  }
  Patch *patch = [[Patch new] autorelease];
  NSUInteger char_count1 = 0;  // Number of characters into the text1 NSString.
  NSUInteger char_count2 = 0;  // Number of characters into the text2 NSString.
  // Start with text1 (prepatch_text) and apply the diffs until we arrive at
  // text2 (postpatch_text). We recreate the patches one by one to determine
  // context info.
  NSString *prepatch_text = [text1 retain];
  NSMutableString *postpatch_text = [text1 mutableCopy];
  for (Diff *aDiff in diffs) {
    if (patch.diffs.count == 0 && aDiff.operation != DIFF_EQUAL) {
      // A new patch starts here.
      patch.start1 = char_count1;
      patch.start2 = char_count2;
    }

    switch (aDiff.operation) {
      case DIFF_INSERT:
        [patch.diffs addObject:aDiff];
        patch.length2 += aDiff.text.length;
        [postpatch_text insertString:aDiff.text atIndex:char_count2];
        break;
      case DIFF_DELETE:
        patch.length1 += aDiff.text.length;
        [patch.diffs addObject:aDiff];
        [postpatch_text deleteCharactersInRange:NSMakeRange(char_count2, aDiff.text.length)];
        break;
      case DIFF_EQUAL:
        if (aDiff.text.length <= 2 * Patch_Margin
          && [patch.diffs count] != 0 && aDiff != diffs.lastObject) {
          // Small equality inside a patch.
          [patch.diffs addObject:aDiff];
          patch.length1 += aDiff.text.length;
          patch.length2 += aDiff.text.length;
        }

        if (aDiff.text.length >= 2 * Patch_Margin) {
          // Time for a new patch.
          if (patch.diffs.count != 0) {
            [self patch_addContextToPatch:patch sourceText:prepatch_text];
            [patches addObject:patch];
            patch = [[Patch new] autorelease];
            // Unlike Unidiff, our patch lists have a rolling context.
            // https://github.com/google/diff-match-patch/wiki/Unidiff
            // Update prepatch text & pos to reflect the application of the
            // just completed patch.
            [prepatch_text release];
            prepatch_text = [postpatch_text copy];
            char_count1 = char_count2;
          }
        }
        break;
    }

    // Update the current character count.
    if (aDiff.operation != DIFF_INSERT) {
      char_count1 += aDiff.text.length;
    }
    if (aDiff.operation != DIFF_DELETE) {
      char_count2 += aDiff.text.length;
    }
  }
  // Pick up the leftover patch if not empty.
  if (patch.diffs.count != 0) {
    [self patch_addContextToPatch:patch sourceText:prepatch_text];
    [patches addObject:patch];
  }

  [prepatch_text release];
  [postpatch_text release];

  return patches;
}

/**
 * Given an array of patches, return another array that is identical.
 * @param patches NSArray of Patch objects.
 * @return NSMutableArray of Patch objects.
 */
- (NSMutableArray *)patch_deepCopy:(NSArray *)patches;
{
  NSMutableArray *patchesCopy = [[NSMutableArray alloc] initWithArray:patches copyItems:YES];
  return patchesCopy;
}

/**
 * Merge a set of patches onto the text.  Return a patched text, as well
 * as an array of YES/NO values indicating which patches were applied.
 * @param sourcePatches NSMutableArray of Patch objects
 * @param text Old text.
 * @return Two element NSArray, containing the new text and an array of
 *      BOOL values.
 */
- (NSArray *)patch_apply:(NSArray *)sourcePatches
                toString:(NSString *)text;
{
  if (sourcePatches.count == 0) {
    return [NSArray arrayWithObjects:text, [NSMutableArray array], nil];
  }

  // Deep copy the patches so that no changes are made to originals.
  NSMutableArray *patches = [self patch_deepCopy:sourcePatches];

  NSMutableString *textMutable = [[text mutableCopy] autorelease];

  NSString *nullPadding = [self patch_addPadding:patches];
  [textMutable insertString:nullPadding atIndex:0];
  [textMutable appendString:nullPadding];
  [self patch_splitMax:patches];

  NSUInteger x = 0;
  // delta keeps track of the offset between the expected and actual
  // location of the previous patch.  If there are patches expected at
  // positions 10 and 20, but the first patch was found at 12, delta is 2
  // and the second patch has an effective expected position of 22.
  NSUInteger delta = 0;
  BOOL *results = (BOOL *)calloc(patches.count, sizeof(BOOL));
  for (Patch *aPatch in patches) {
    NSUInteger expected_loc = aPatch.start2 + delta;
    NSString *text1 = [self diff_text1:aPatch.diffs];
    NSUInteger start_loc;
    NSUInteger end_loc = NSNotFound;
    if (text1.length > Match_MaxBits) {
      // patch_splitMax will only provide an oversized pattern
      // in the case of a monster delete.
      start_loc = [self match_mainForText:textMutable
                                  pattern:[text1 substringWithRange:NSMakeRange(0, Match_MaxBits)]
                                     near:expected_loc];
      if (start_loc != NSNotFound) {
        end_loc = [self match_mainForText:textMutable
            pattern:[text1 substringFromIndex:text1.length - Match_MaxBits]
            near:(expected_loc + text1.length - Match_MaxBits)];
        if (end_loc == NSNotFound || start_loc >= end_loc) {
          // Can't find valid trailing context.   Drop this patch.
          start_loc = NSNotFound;
        }
      }
    } else {
      start_loc = [self match_mainForText:textMutable pattern:text1 near:expected_loc];
    }
    if (start_loc == NSNotFound) {
      // No match found.  :(
      results[x] = NO;
      // Subtract the delta for this failed patch from subsequent patches.
      delta -= aPatch.length2 - aPatch.length1;
    } else {
      // Found a match.   :)
      results[x] = YES;
      delta = start_loc - expected_loc;
      NSString *text2;
      if (end_loc == NSNotFound) {
        text2 = [textMutable diff_javaSubstringFromStart:start_loc
            toEnd:MIN(start_loc + text1.length, textMutable.length)];
      } else {
        text2 = [textMutable diff_javaSubstringFromStart:start_loc
            toEnd:MIN(end_loc + Match_MaxBits, textMutable.length)];
      }
      if (text1 == text2) {
        // Perfect match, just shove the Replacement text in.
        [textMutable replaceCharactersInRange:NSMakeRange(start_loc, text1.length) withString:[self diff_text2:aPatch.diffs]];
      } else {
        // Imperfect match.   Run a diff to get a framework of equivalent
        // indices.
        NSMutableArray *diffs = [self diff_mainOfOldString:text1 andNewString:text2 checkLines:NO];
        if (text1.length > Match_MaxBits
            && ([self diff_levenshtein:diffs] / (float)text1.length)
            > Patch_DeleteThreshold) {
          // The end points match, but the content is unacceptably bad.
          results[x] = NO;
        } else {
          [self diff_cleanupSemanticLossless:diffs];
          NSUInteger index1 = 0;
          for (Diff *aDiff in aPatch.diffs) {
            if (aDiff.operation != DIFF_EQUAL) {
              NSUInteger index2 = [self diff_xIndexIn:diffs location:index1];
              if (aDiff.operation == DIFF_INSERT) {
                // Insertion
                [textMutable insertString:aDiff.text atIndex:(start_loc + index2)];
              } else if (aDiff.operation == DIFF_DELETE) {
                // Deletion
                [textMutable deleteCharactersInRange:NSMakeRange(start_loc + index2,
                    ([self diff_xIndexIn:diffs
                    location:(index1 + aDiff.text.length)] - index2))];
              }
            }
            if (aDiff.operation != DIFF_DELETE) {
              index1 += aDiff.text.length;
            }
          }
        }
      }
    }
    x++;
  }

  NSMutableArray *resultsArray = [NSMutableArray arrayWithCapacity:patches.count];
  for (NSUInteger i = 0; i < patches.count; i++) {
    [resultsArray addObject:[NSNumber numberWithBool:(results[i])]];
  }

  if (results != NULL) {
    free(results);
  }

  // Strip the padding off.
  text = [textMutable substringWithRange:NSMakeRange(nullPadding.length,
      textMutable.length - 2 * nullPadding.length)];
  [patches release];
  return [NSArray arrayWithObjects:text, resultsArray, nil];
}

/**
 * Add some padding on text start and end so that edges can match something.
 * Intended to be called only from within patch_apply.
 * @param patches NSMutableArray of Patch objects.
 * @return The padding NSString added to each side.
 */
- (NSString *)patch_addPadding:(NSMutableArray *)patches;
{
  uint16_t paddingLength = Patch_Margin;
  NSMutableString *nullPadding = [NSMutableString string];
  for (UniChar x = 1; x <= paddingLength; x++) {
    CFStringAppendCharacters((CFMutableStringRef)nullPadding, &x, 1);
  }

  // Bump all the patches forward.
  for (Patch *aPatch in patches) {
    aPatch.start1 += paddingLength;
    aPatch.start2 += paddingLength;
  }

  // Add some padding on start of first diff.
  Patch *patch = [patches objectAtIndex:0];
  NSMutableArray *diffs = patch.diffs;
  if (diffs.count == 0 || ((Diff *)[diffs objectAtIndex:0]).operation != DIFF_EQUAL) {
    // Add nullPadding equality.
    [diffs insertObject:[Diff diffWithOperation:DIFF_EQUAL andText:nullPadding] atIndex:0];
    patch.start1 -= paddingLength;  // Should be 0.
    patch.start2 -= paddingLength;  // Should be 0.
    patch.length1 += paddingLength;
    patch.length2 += paddingLength;
  } else if (paddingLength > ((Diff *)[diffs objectAtIndex:0]).text.length) {
    // Grow first equality.
    Diff *firstDiff = [diffs objectAtIndex:0];
    NSUInteger extraLength = paddingLength - firstDiff.text.length;
    firstDiff.text = [[nullPadding substringFromIndex:(firstDiff.text.length)]
              stringByAppendingString:firstDiff.text];
    patch.start1 -= extraLength;
    patch.start2 -= extraLength;
    patch.length1 += extraLength;
    patch.length2 += extraLength;
  }

  // Add some padding on end of last diff.
  patch = patches.lastObject;
  diffs = patch.diffs;
  if (diffs.count == 0 || ((Diff *)diffs.lastObject).operation != DIFF_EQUAL) {
    // Add nullPadding equality.
    [diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:nullPadding]];
    patch.length1 += paddingLength;
    patch.length2 += paddingLength;
  } else if (paddingLength > ((Diff *)diffs.lastObject).text.length) {
    // Grow last equality.
    Diff *lastDiff = diffs.lastObject;
    NSUInteger extraLength = paddingLength - lastDiff.text.length;
    lastDiff.text = [lastDiff.text stringByAppendingString:[nullPadding substringWithRange:NSMakeRange(0, extraLength)]];
    patch.length1 += extraLength;
    patch.length2 += extraLength;
  }

  return nullPadding;
}

/**
 * Look through the patches and break up any which are longer than the
 * maximum limit of the match algorithm.
 * Intended to be called only from within patch_apply.
 * @param patches NSMutableArray of Patch objects.
 */
- (void)patch_splitMax:(NSMutableArray *)patches;
{
  NSUInteger patch_size = Match_MaxBits;
  for (NSUInteger x = 0; x < patches.count; x++) {
    if (((Patch *)[patches objectAtIndex:x]).length1 <= patch_size) {
      continue;
    }
    Patch *bigpatch = [[patches objectAtIndex:x] retain];
    // Remove the big old patch.
    splice(patches, x--, 1, nil);
    NSUInteger start1 = bigpatch.start1;
    NSUInteger start2 = bigpatch.start2;
    NSString *precontext = @"";
    while (bigpatch.diffs.count != 0) {
      // Create one of several smaller patches.
      Patch *patch = [[Patch new] autorelease];
      BOOL empty = YES;
      patch.start1 = start1 - precontext.length;
      patch.start2 = start2 - precontext.length;
      if (precontext.length != 0) {
        patch.length1 = patch.length2 = precontext.length;
        [patch.diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:precontext]];
      }
      while (bigpatch.diffs.count != 0
          && patch.length1 < patch_size - self.Patch_Margin) {
        Operation diff_type = ((Diff *)[bigpatch.diffs objectAtIndex:0]).operation;
        NSString *diff_text = ((Diff *)[bigpatch.diffs objectAtIndex:0]).text;
        if (diff_type == DIFF_INSERT) {
          // Insertions are harmless.
          patch.length2 += diff_text.length;
          start2 += diff_text.length;
          [patch.diffs addObject:[bigpatch.diffs objectAtIndex:0]];
          [bigpatch.diffs removeObjectAtIndex:0];
          empty = NO;
        } else if (diff_type == DIFF_DELETE && patch.diffs.count == 1
              && ((Diff *)[patch.diffs objectAtIndex:0]).operation == DIFF_EQUAL
              && diff_text.length > 2 * patch_size) {
          // This is a large deletion.  Let it pass in one chunk.
          patch.length1 += diff_text.length;
          start1 += diff_text.length;
          empty = NO;
          [patch.diffs addObject:[Diff diffWithOperation:diff_type andText:diff_text]];
          [bigpatch.diffs removeObjectAtIndex:0];
        } else {
          // Deletion or equality.  Only take as much as we can stomach.
          diff_text = [diff_text substringWithRange:NSMakeRange(0,
              MIN(diff_text.length,
              (patch_size - patch.length1 - Patch_Margin)))];
          patch.length1 += diff_text.length;
          start1 += diff_text.length;
          if (diff_type == DIFF_EQUAL) {
            patch.length2 += diff_text.length;
            start2 += diff_text.length;
          } else {
            empty = NO;
          }
          [patch.diffs addObject:[Diff diffWithOperation:diff_type andText:diff_text]];
          if (diff_text == ((Diff *)[bigpatch.diffs objectAtIndex:0]).text) {
            [bigpatch.diffs removeObjectAtIndex:0];
          } else {
            Diff *firstDiff = [bigpatch.diffs objectAtIndex:0];
            firstDiff.text = [firstDiff.text substringFromIndex:diff_text.length];
          }
        }
      }
      // Compute the head context for the next patch.
      precontext = [self diff_text2:patch.diffs];
      precontext = [precontext substringFromIndex:MAX_OF_CONST_AND_DIFF(0, precontext.length, Patch_Margin)];

      NSString *postcontext = nil;
      // Append the end context for this patch.
      if ([self diff_text1:bigpatch.diffs].length > Patch_Margin) {
        postcontext = [[self diff_text1:bigpatch.diffs]
                 substringWithRange:NSMakeRange(0, Patch_Margin)];
      } else {
        postcontext = [self diff_text1:bigpatch.diffs];
      }

      if (postcontext.length != 0) {
        patch.length1 += postcontext.length;
        patch.length2 += postcontext.length;
        if (patch.diffs.count != 0
            && ((Diff *)[patch.diffs objectAtIndex:(patch.diffs.count - 1)]).operation
            == DIFF_EQUAL) {
          Diff *lastDiff = [patch.diffs lastObject];
          lastDiff.text = [lastDiff.text stringByAppendingString:postcontext];
        } else {
          [patch.diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:postcontext]];
        }
      }
      if (!empty) {
        splice(patches, ++x, 0, [NSMutableArray arrayWithObject:patch]);
      }
    }

    [bigpatch release];

  }
}

/**
 * Take a list of patches and return a textual representation.
 * @param patches NSMutableArray of Patch objects.
 * @return Text representation of patches.
 */
- (NSString *)patch_toText:(NSMutableArray *)patches;
{
  NSMutableString *text = [NSMutableString string];
  for (Patch *aPatch in patches) {
    [text appendString:[aPatch description]];
  }
  return text;
}

/**
 * Parse a textual representation of patches and return a NSMutableArray of
 * Patch objects.
 * @param textline Text representation of patches.
 * @param error NSError if invalid input.
 * @return NSMutableArray of Patch objects.
 */
- (NSMutableArray *)patch_fromText:(NSString *)textline
                             error:(NSError **)error;
{
  NSMutableArray *patches = [NSMutableArray array];
  if (textline.length == 0) {
    return patches;
  }
  NSArray *text = [textline componentsSeparatedByString:@"\n"];
  NSUInteger textPointer = 0;
  Patch *patch;
  //NSString *patchHeader = @"^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@$";
  NSString *patchHeaderStart = @"@@ -";
  NSString *patchHeaderMid = @"+";
  NSString *patchHeaderEnd = @"@@";
  NSString *optionalValueDelimiter = @",";
  BOOL scanSuccess, hasOptional;
  NSInteger scannedValue, optionalValue;
  NSDictionary *errorDetail = nil;

  unichar sign;
  NSString *line;
  while (textPointer < text.count) {
    NSString *thisLine = [text objectAtIndex:textPointer];
    NSScanner *theScanner = [NSScanner scannerWithString:thisLine];
    patch = [[Patch new] autorelease];

    scanSuccess = ([theScanner scanString:patchHeaderStart intoString:NULL]
        && [theScanner scanInteger:&scannedValue]);

    if (scanSuccess) {
      patch.start1 = scannedValue;

      hasOptional = [theScanner scanString:optionalValueDelimiter intoString:NULL];

      if (hasOptional) {
        // First set has an optional value.
        scanSuccess = [theScanner scanInteger:&optionalValue];
        if (scanSuccess) {
          if (optionalValue == 0) {
            patch.length1 = 0;
          } else {
            patch.start1--;
            patch.length1 = optionalValue;
          }
        }
      } else {
        patch.start1--;
        patch.length1 = 1;
      }

      if (scanSuccess) {
        scanSuccess = ([theScanner scanString:patchHeaderMid intoString:NULL]
            && [theScanner scanInteger:&scannedValue]);

        if (scanSuccess) {
          patch.start2 = scannedValue;

          hasOptional = [theScanner scanString:optionalValueDelimiter intoString:NULL];

          if (hasOptional) {
            // Second set has an optional value.
            scanSuccess = [theScanner scanInteger:&optionalValue];
            if (scanSuccess) {
              if (optionalValue == 0) {
                patch.length2 = 0;
              } else {
                patch.start2--;
                patch.length2 = optionalValue;
              }
            }
          } else {
            patch.start2--;
            patch.length2 = 1;
          }

          if (scanSuccess) {
            scanSuccess = ([theScanner scanString:patchHeaderEnd intoString:NULL]
                && [theScanner isAtEnd] == YES);
          }
        }
      }
    }

    if (!scanSuccess) {
      if (error != NULL) {
        errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
            [NSString stringWithFormat:NSLocalizedString(@"Invalid patch string: %@", @"Error"),
             [text objectAtIndex:textPointer]],
            NSLocalizedDescriptionKey, nil];
        *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:104 userInfo:errorDetail];
      }
      return nil;
    }

    [patches addObject:patch];

    textPointer++;

    while (textPointer < text.count) {
      @try {
        sign = [[text objectAtIndex:textPointer] characterAtIndex:0];
      }
      @catch (NSException *e) {
        // Blank line?  Whatever.
        textPointer++;
        continue;
      }
      line = [[[text objectAtIndex:textPointer] substringFromIndex:1]
              diff_stringByReplacingPercentEscapesForEncodeUriCompatibility];
      if (sign == '-') {
        // Deletion.
        [patch.diffs addObject:[Diff diffWithOperation:DIFF_DELETE andText:line]];
      } else if (sign == '+') {
        // Insertion.
        [patch.diffs addObject:[Diff diffWithOperation:DIFF_INSERT andText:line]];
      } else if (sign == ' ') {
        // Minor equality.
        [patch.diffs addObject:[Diff diffWithOperation:DIFF_EQUAL andText:line]];
      } else if (sign == '@') {
        // Start of next patch.
        break;
      } else {
        // WTF?
        if (error != NULL) {
          errorDetail = [NSDictionary dictionaryWithObjectsAndKeys:
              [NSString stringWithFormat:NSLocalizedString(@"Invalid patch mode '%C' in: %@", @"Error"), sign, line],
              NSLocalizedDescriptionKey, nil];
          *error = [NSError errorWithDomain:@"DiffMatchPatchErrorDomain" code:104 userInfo:errorDetail];
        }
        return nil;
      }
      textPointer++;
    }
  }
  return patches;
}

@end
