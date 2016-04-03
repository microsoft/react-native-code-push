/*
 ---------------------------------------------------------------------------
 Copyright (c) 2002, Dr Brian Gladman, Worcester, UK.   All rights reserved.

 LICENSE TERMS

 The free distribution and use of this software in both source and binary
 form is allowed (with or without changes) provided that:

   1. distributions of this source code include the above copyright
      notice, this list of conditions and the following disclaimer;

   2. distributions in binary form include the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other associated materials;

   3. the copyright holder's name is not used to endorse products
      built using this software without specific written permission.

 ALTERNATIVELY, provided that this notice is retained in full, this product
 may be distributed under the terms of the GNU General Public License (GPL),
 in which case the provisions of the GPL apply INSTEAD OF those given above.

 DISCLAIMER

 This software is provided 'as is' with no explicit or implied warranties
 in respect of its properties, including, but not limited to, correctness
 and/or fitness for purpose.
 ---------------------------------------------------------------------------
 Issue Date: 26/08/2003

 This is an implementation of RFC2898, which specifies key derivation from
 a password and a salt value.
*/

#include <memory.h>
#include "hmac.h"

#if defined(__cplusplus)
extern "C"
{
#endif

void derive_key(const unsigned char pwd[],  /* the PASSWORD     */
               unsigned int pwd_len,        /* and its length   */
               const unsigned char salt[],  /* the SALT and its */
               unsigned int salt_len,       /* length           */
               unsigned int iter,   /* the number of iterations */
               unsigned char key[], /* space for the output key */
               unsigned int key_len)/* and its required length  */
{
    unsigned int    i, j, k, n_blk;
    unsigned char uu[HASH_OUTPUT_SIZE], ux[HASH_OUTPUT_SIZE];
    hmac_ctx c1[1], c2[1], c3[1];

    /* set HMAC context (c1) for password               */
    hmac_sha_begin(c1);
    hmac_sha_key(pwd, pwd_len, c1);

    /* set HMAC context (c2) for password and salt      */
    memcpy(c2, c1, sizeof(hmac_ctx));
    hmac_sha_data(salt, salt_len, c2);

    /* find the number of SHA blocks in the key         */
    n_blk = 1 + (key_len - 1) / HASH_OUTPUT_SIZE;

    for(i = 0; i < n_blk; ++i) /* for each block in key */
    {
        /* ux[] holds the running xor value             */
        memset(ux, 0, HASH_OUTPUT_SIZE);

        /* set HMAC context (c3) for password and salt  */
        memcpy(c3, c2, sizeof(hmac_ctx));

        /* enter additional data for 1st block into uu  */
        uu[0] = (unsigned char)((i + 1) >> 24);
        uu[1] = (unsigned char)((i + 1) >> 16);
        uu[2] = (unsigned char)((i + 1) >> 8);
        uu[3] = (unsigned char)(i + 1);

        /* this is the key mixing iteration         */
        for(j = 0, k = 4; j < iter; ++j)
        {
            /* add previous round data to HMAC      */
            hmac_sha_data(uu, k, c3);

            /* obtain HMAC for uu[]                 */
            hmac_sha_end(uu, HASH_OUTPUT_SIZE, c3);

            /* xor into the running xor block       */
            for(k = 0; k < HASH_OUTPUT_SIZE; ++k)
                ux[k] ^= uu[k];

            /* set HMAC context (c3) for password   */
            memcpy(c3, c1, sizeof(hmac_ctx));
        }

        /* compile key blocks into the key output   */
        j = 0; k = i * HASH_OUTPUT_SIZE;
        while(j < HASH_OUTPUT_SIZE && k < key_len)
            key[k++] = ux[j++];
    }
}
