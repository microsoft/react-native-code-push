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

/*
 * Functions for diff, match and patch.
 * Computes the difference between two texts to create a patch.
 * Applies the patch onto another text, allowing for errors.
 */

/*
 * The data structure representing a diff is an NSMutableArray of Diff objects:
 * {Diff(Operation.DIFF_DELETE, "Hello"),
 *  Diff(Operation.DIFF_INSERT, "Goodbye"),
 *  Diff(Operation.DIFF_EQUAL, " world.")}
 * which means: delete "Hello", add "Goodbye" and keep " world."
 */

typedef enum {
  DIFF_DELETE = 1,
  DIFF_INSERT = 2,
  DIFF_EQUAL = 3
} Operation;


/*
 * Class representing one diff operation.
 */
@interface Diff : NSObject <NSCopying> {
  Operation operation;  // One of: DIFF_INSERT, DIFF_DELETE or DIFF_EQUAL.
  NSString *text;      // The text associated with this diff operation.
}

@property (nonatomic, assign) Operation operation;
@property (nonatomic, copy) NSString *text;

+ (id)diffWithOperation:(Operation)anOperation andText:(NSString *)aText;

- (id)initWithOperation:(Operation)anOperation andText:(NSString *)aText;

@end

/*
 * Class representing one patch operation.
 */
@interface Patch : NSObject <NSCopying> {
  NSMutableArray *diffs;
  NSUInteger start1;
  NSUInteger start2;
  NSUInteger length1;
  NSUInteger length2;
}

@property (nonatomic, retain) NSMutableArray *diffs;
@property (nonatomic, assign) NSUInteger start1;
@property (nonatomic, assign) NSUInteger start2;
@property (nonatomic, assign) NSUInteger length1;
@property (nonatomic, assign) NSUInteger length2;

@end


/*
 * Class containing the diff, match and patch methods.
 * Also Contains the behaviour settings.
 */
@interface DiffMatchPatch : NSObject {
  // Number of seconds to map a diff before giving up (0 for infinity).
  NSTimeInterval Diff_Timeout;

  // Cost of an empty edit operation in terms of edit characters.
  NSUInteger Diff_EditCost;

  // At what point is no match declared (0.0 = perfection, 1.0 = very loose).
  double Match_Threshold;

  // How far to search for a match (0 = exact location, 1000+ = broad match).
  // A match this many characters away from the expected location will add
  // 1.0 to the score (0.0 is a perfect match).
  NSInteger Match_Distance;

  // When deleting a large block of text (over ~64 characters), how close
  // do the contents have to be to match the expected contents. (0.0 =
  // perfection, 1.0 = very loose).  Note that Match_Threshold controls
  // how closely the end points of a delete need to match.
  float Patch_DeleteThreshold;

  // Chunk size for context length.
  uint16_t Patch_Margin;

  // The number of bits in an int.
  NSUInteger Match_MaxBits;
}

@property (nonatomic, assign) NSTimeInterval Diff_Timeout;
@property (nonatomic, assign) NSUInteger Diff_EditCost;
@property (nonatomic, assign) double Match_Threshold;
@property (nonatomic, assign) NSInteger Match_Distance;
@property (nonatomic, assign) float Patch_DeleteThreshold;
@property (nonatomic, assign) uint16_t Patch_Margin;

- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1 andNewString:(NSString *)text2;
- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1 andNewString:(NSString *)text2 checkLines:(BOOL)checklines;
- (NSUInteger)diff_commonPrefixOfFirstString:(NSString *)text1 andSecondString:(NSString *)text2;
- (NSUInteger)diff_commonSuffixOfFirstString:(NSString *)text1 andSecondString:(NSString *)text2;
- (void)diff_cleanupSemantic:(NSMutableArray *)diffs;
- (void)diff_cleanupSemanticLossless:(NSMutableArray *)diffs;
- (void)diff_cleanupEfficiency:(NSMutableArray *)diffs;
- (void)diff_cleanupMerge:(NSMutableArray *)diffs;
- (NSUInteger)diff_xIndexIn:(NSMutableArray *)diffs location:(NSUInteger) loc;
- (NSString *)diff_prettyHtml:(NSMutableArray *)diffs;
- (NSString *)diff_text1:(NSMutableArray *)diffs;
- (NSString *)diff_text2:(NSMutableArray *)diffs;
- (NSUInteger)diff_levenshtein:(NSMutableArray *)diffs;
- (NSString *)diff_toDelta:(NSMutableArray *)diffs;
- (NSMutableArray *)diff_fromDeltaWithText:(NSString *)text1 andDelta:(NSString *)delta error:(NSError **)error;

- (NSUInteger)match_mainForText:(NSString *)text pattern:(NSString *)pattern near:(NSUInteger)loc;
- (NSMutableDictionary *)match_alphabet:(NSString *)pattern;

- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1 andNewString:(NSString *)text2;
- (NSMutableArray *)patch_makeFromDiffs:(NSMutableArray *)diffs;
- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1 newString:(NSString *)text2 diffs:(NSMutableArray *)diffs;
- (NSMutableArray *)patch_makeFromOldString:(NSString *)text1 andDiffs:(NSMutableArray *)diffs;
- (NSMutableArray *)patch_deepCopy:(NSArray *)patches; // Copy rule applies!
- (NSArray *)patch_apply:(NSArray *)sourcePatches toString:(NSString *)text;
- (NSString *)patch_addPadding:(NSMutableArray *)patches;
- (void)patch_splitMax:(NSMutableArray *)patches;
- (NSString *)patch_toText:(NSMutableArray *)patches;
- (NSMutableArray *)patch_fromText:(NSString *)textline error:(NSError **)error;

@end


@interface DiffMatchPatch (PrivateMethods)

- (NSMutableArray *)diff_mainOfOldString:(NSString *)text1 andNewString:(NSString *)text2 checkLines:(BOOL)checklines deadline:(NSTimeInterval)deadline;
- (NSMutableArray *)diff_computeFromOldString:(NSString *)text1 andNewString:(NSString *)text2 checkLines:(BOOL)checklines deadline:(NSTimeInterval)deadline;
- (NSMutableArray *)diff_lineModeFromOldString:(NSString *)text1 andNewString:(NSString *)text2 deadline:(NSTimeInterval)deadline;
- (NSArray *)diff_linesToCharsForFirstString:(NSString *)text1 andSecondString:(NSString *)text1;
- (void)diff_chars:(NSArray *)diffs toLines:(NSMutableArray *)lineArray;
- (NSMutableArray *)diff_bisectOfOldString:(NSString *)text1 andNewString:(NSString *)text2 deadline:(NSTimeInterval)deadline;
- (NSMutableArray *)diff_bisectSplitOfOldString:(NSString *)text1 andNewString:(NSString *)text2 x:(NSUInteger)x y:(NSUInteger)y deadline:(NSTimeInterval)deadline;
- (NSUInteger)diff_commonOverlapOfFirstString:(NSString *)text1 andSecondString:(NSString *)text2;
- (NSArray *)diff_halfMatchOfFirstString:(NSString *)text1 andSecondString:(NSString *)text2;
- (NSArray *)diff_halfMatchIOfLongString:(NSString *)longtext andShortString:(NSString *)shorttext;
- (NSInteger)diff_cleanupSemanticScoreOfFirstString:(NSString *)one andSecondString:(NSString *)two;

- (NSUInteger)match_bitapOfText:(NSString *)text andPattern:(NSString *)pattern near:(NSUInteger)loc;
- (double)match_bitapScoreForErrorCount:(NSUInteger)e location:(NSUInteger)x near:(NSUInteger)loc pattern:(NSString *)pattern;

- (void)patch_addContextToPatch:(Patch *)patch sourceText:(NSString *)text;

@end
