//
//  JWTDeprecations.h
//  JWT
//
//  Created by Lobanov Dmitry on 31.08.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#ifndef JWTDeprecations_h
#define JWTDeprecations_h

#define STR(str) #str
#define JWTVersion_2_1_0 2.1
#define JWTVersion_2_2_0 2.2
#define JWTVersion_3_0_0 3.0
#define __first_deprecated_in_release_version(version) __deprecated_msg("first deprecated in release version: " STR(version))
#define __deprecated_and_will_be_removed_in_release_version(version) __deprecated_msg("deprecated. will be removed in release version: "STR(version))
#define __available_in_release_version(version) __deprecated_msg("will be introduced in release version: " STR(version))

#define __jwt_technical_debt(debt) __deprecated_msg("Don't forget to inspect it later." STR(debt))

#endif /* JWTDeprecations_h */
