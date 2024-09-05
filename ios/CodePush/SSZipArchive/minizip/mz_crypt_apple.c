/* mz_crypt_apple.c -- Crypto/hash functions for Apple
   part of the minizip-ng project

   Copyright (C) Nathan Moinvaziri
     https://github.com/zlib-ng/minizip-ng

   This program is distributed under the terms of the same license as zlib.
   See the accompanying LICENSE file for the full text of the license.
*/

#include "mz.h"

#include <CoreFoundation/CoreFoundation.h>
#include <CommonCrypto/CommonCryptor.h>
#include <CommonCrypto/CommonDigest.h>
#include <CommonCrypto/CommonHMAC.h>
#include <Security/Security.h>
#include <Security/SecPolicy.h>

/***************************************************************************/

int32_t mz_crypt_rand(uint8_t *buf, int32_t size) {
    if (SecRandomCopyBytes(kSecRandomDefault, size, buf) != errSecSuccess)
        return 0;
    return size;
}

/***************************************************************************/

typedef struct mz_crypt_sha_s {
    union {
        CC_SHA1_CTX   ctx1;
        CC_SHA256_CTX ctx256;
        CC_SHA512_CTX ctx512;
    };
    int32_t           error;
    int32_t           initialized;
    uint16_t          algorithm;
} mz_crypt_sha;

/***************************************************************************/

static const uint8_t mz_crypt_sha_digest_size[] = {
    MZ_HASH_SHA1_SIZE,                     0, MZ_HASH_SHA224_SIZE,
    MZ_HASH_SHA256_SIZE, MZ_HASH_SHA384_SIZE, MZ_HASH_SHA512_SIZE
};

/***************************************************************************/

void mz_crypt_sha_reset(void *handle) {
    mz_crypt_sha *sha = (mz_crypt_sha *)handle;

    sha->error = 0;
    sha->initialized = 0;
}

int32_t mz_crypt_sha_begin(void *handle) {
    mz_crypt_sha *sha = (mz_crypt_sha *)handle;

    if (!sha)
        return MZ_PARAM_ERROR;

    mz_crypt_sha_reset(handle);

    switch (sha->algorithm) {
    case MZ_HASH_SHA1:
        sha->error = CC_SHA1_Init(&sha->ctx1);
        break;
    case MZ_HASH_SHA224:
        sha->error = CC_SHA224_Init(&sha->ctx256);
        break;
    case MZ_HASH_SHA256:
        sha->error = CC_SHA256_Init(&sha->ctx256);
        break;
    case MZ_HASH_SHA384:
        sha->error = CC_SHA384_Init(&sha->ctx512);
        break;
    case MZ_HASH_SHA512:
        sha->error = CC_SHA512_Init(&sha->ctx512);
        break;
    default:
        return MZ_PARAM_ERROR;
    }

    if (!sha->error)
        return MZ_HASH_ERROR;

    sha->initialized = 1;
    return MZ_OK;
}

int32_t mz_crypt_sha_update(void *handle, const void *buf, int32_t size) {
    mz_crypt_sha *sha = (mz_crypt_sha *)handle;

    if (!sha || !buf || !sha->initialized)
        return MZ_PARAM_ERROR;

    switch (sha->algorithm) {
    case MZ_HASH_SHA1:
        sha->error = CC_SHA1_Update(&sha->ctx1, buf, size);
        break;
    case MZ_HASH_SHA224:
        sha->error = CC_SHA224_Update(&sha->ctx256, buf, size);
        break;
    case MZ_HASH_SHA256:
        sha->error = CC_SHA256_Update(&sha->ctx256, buf, size);
        break;
    case MZ_HASH_SHA384:
        sha->error = CC_SHA384_Update(&sha->ctx512, buf, size);
        break;
    case MZ_HASH_SHA512:
        sha->error = CC_SHA512_Update(&sha->ctx512, buf, size);
        break;
    }

    if (!sha->error)
        return MZ_HASH_ERROR;

    return size;
}

int32_t mz_crypt_sha_end(void *handle, uint8_t *digest, int32_t digest_size) {
    mz_crypt_sha *sha = (mz_crypt_sha *)handle;

    if (!sha || !digest || !sha->initialized)
        return MZ_PARAM_ERROR;
    if (digest_size < mz_crypt_sha_digest_size[sha->algorithm - MZ_HASH_SHA1])
        return MZ_PARAM_ERROR;

    switch (sha->algorithm) {
    case MZ_HASH_SHA1:
        sha->error = CC_SHA1_Final(digest, &sha->ctx1);
        break;
    case MZ_HASH_SHA224:
        sha->error = CC_SHA224_Final(digest, &sha->ctx256);
        break;
    case MZ_HASH_SHA256:
        sha->error = CC_SHA256_Final(digest, &sha->ctx256);
        break;
    case MZ_HASH_SHA384:
        sha->error = CC_SHA384_Final(digest, &sha->ctx512);
        break;
    case MZ_HASH_SHA512:
        sha->error = CC_SHA512_Final(digest, &sha->ctx512);
        break;
    }

    if (!sha->error)
        return MZ_HASH_ERROR;

    return MZ_OK;
}

void mz_crypt_sha_set_algorithm(void *handle, uint16_t algorithm) {
    mz_crypt_sha *sha = (mz_crypt_sha *)handle;
    if (MZ_HASH_SHA1 <= algorithm && algorithm <= MZ_HASH_SHA512)
        sha->algorithm = algorithm;
}

void *mz_crypt_sha_create(void **handle) {
    mz_crypt_sha *sha = NULL;

    sha = (mz_crypt_sha *)calloc(1, sizeof(mz_crypt_sha));
    if (sha) {
        memset(sha, 0, sizeof(mz_crypt_sha));
        sha->algorithm = MZ_HASH_SHA256;
    }
    if (handle)
        *handle = sha;

    return sha;
}

void mz_crypt_sha_delete(void **handle) {
    mz_crypt_sha *sha = NULL;
    if (!handle)
        return;
    sha = (mz_crypt_sha *)*handle;
    if (sha) {
        mz_crypt_sha_reset(*handle);
        free(sha);
    }
    *handle = NULL;
}

/***************************************************************************/

typedef struct mz_crypt_aes_s {
    CCCryptorRef crypt;
    int32_t      mode;
    int32_t      error;
} mz_crypt_aes;

/***************************************************************************/

void mz_crypt_aes_reset(void *handle) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;

    if (aes->crypt)
        CCCryptorRelease(aes->crypt);
    aes->crypt = NULL;
}

int32_t mz_crypt_aes_encrypt(void *handle, uint8_t *buf, int32_t size) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;
    size_t data_moved = 0;

    if (!aes || !buf)
        return MZ_PARAM_ERROR;
    if (size != MZ_AES_BLOCK_SIZE)
        return MZ_PARAM_ERROR;

    aes->error = CCCryptorUpdate(aes->crypt, buf, size, buf, size, &data_moved);

    if (aes->error != kCCSuccess)
        return MZ_HASH_ERROR;

    return size;
}

int32_t mz_crypt_aes_decrypt(void *handle, uint8_t *buf, int32_t size) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;
    size_t data_moved = 0;

    if (!aes || !buf)
        return MZ_PARAM_ERROR;
    if (size != MZ_AES_BLOCK_SIZE)
        return MZ_PARAM_ERROR;

    aes->error = CCCryptorUpdate(aes->crypt, buf, size, buf, size, &data_moved);

    if (aes->error != kCCSuccess)
        return MZ_HASH_ERROR;

    return size;
}

int32_t mz_crypt_aes_set_encrypt_key(void *handle, const void *key, int32_t key_length) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;

    if (!aes || !key || !key_length)
        return MZ_PARAM_ERROR;

    mz_crypt_aes_reset(handle);

    aes->error = CCCryptorCreate(kCCEncrypt, kCCAlgorithmAES, kCCOptionECBMode,
        key, key_length, NULL, &aes->crypt);

    if (aes->error != kCCSuccess)
        return MZ_HASH_ERROR;

    return MZ_OK;
}

int32_t mz_crypt_aes_set_decrypt_key(void *handle, const void *key, int32_t key_length) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;

    if (!aes || !key || !key_length)
        return MZ_PARAM_ERROR;

    mz_crypt_aes_reset(handle);

    aes->error = CCCryptorCreate(kCCDecrypt, kCCAlgorithmAES, kCCOptionECBMode,
        key, key_length, NULL, &aes->crypt);

    if (aes->error != kCCSuccess)
        return MZ_HASH_ERROR;

    return MZ_OK;
}

void mz_crypt_aes_set_mode(void *handle, int32_t mode) {
    mz_crypt_aes *aes = (mz_crypt_aes *)handle;
    aes->mode = mode;
}

void *mz_crypt_aes_create(void **handle) {
    mz_crypt_aes *aes = NULL;

    aes = (mz_crypt_aes *)calloc(1, sizeof(mz_crypt_aes));
    if (handle)
        *handle = aes;

    return aes;
}

void mz_crypt_aes_delete(void **handle) {
    mz_crypt_aes *aes = NULL;
    if (!handle)
        return;
    aes = (mz_crypt_aes *)*handle;
    if (aes) {
        mz_crypt_aes_reset(*handle);
        free(aes);
    }
    *handle = NULL;
}

/***************************************************************************/

typedef struct mz_crypt_hmac_s {
    CCHmacContext   ctx;
    int32_t         initialized;
    int32_t         error;
    uint16_t        algorithm;
} mz_crypt_hmac;

/***************************************************************************/

static void mz_crypt_hmac_free(void *handle) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;
    memset(&hmac->ctx, 0, sizeof(hmac->ctx));
}

void mz_crypt_hmac_reset(void *handle) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;
    mz_crypt_hmac_free(handle);
    hmac->error = 0;
}

int32_t mz_crypt_hmac_init(void *handle, const void *key, int32_t key_length) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;
    CCHmacAlgorithm algorithm = 0;

    if (!hmac || !key)
        return MZ_PARAM_ERROR;

    mz_crypt_hmac_reset(handle);

    if (hmac->algorithm == MZ_HASH_SHA1)
        algorithm = kCCHmacAlgSHA1;
    else if (hmac->algorithm == MZ_HASH_SHA256)
        algorithm = kCCHmacAlgSHA256;
    else
        return MZ_PARAM_ERROR;

    CCHmacInit(&hmac->ctx, algorithm, key, key_length);
    return MZ_OK;
}

int32_t mz_crypt_hmac_update(void *handle, const void *buf, int32_t size) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;

    if (!hmac || !buf)
        return MZ_PARAM_ERROR;

    CCHmacUpdate(&hmac->ctx, buf, size);
    return MZ_OK;
}

int32_t mz_crypt_hmac_end(void *handle, uint8_t *digest, int32_t digest_size) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;

    if (!hmac || !digest)
        return MZ_PARAM_ERROR;

    if (hmac->algorithm == MZ_HASH_SHA1) {
        if (digest_size < MZ_HASH_SHA1_SIZE)
            return MZ_BUF_ERROR;
        CCHmacFinal(&hmac->ctx, digest);
    } else {
        if (digest_size < MZ_HASH_SHA256_SIZE)
            return MZ_BUF_ERROR;
        CCHmacFinal(&hmac->ctx, digest);
    }

    return MZ_OK;
}

void mz_crypt_hmac_set_algorithm(void *handle, uint16_t algorithm) {
    mz_crypt_hmac *hmac = (mz_crypt_hmac *)handle;
    hmac->algorithm = algorithm;
}

int32_t mz_crypt_hmac_copy(void *src_handle, void *target_handle) {
    mz_crypt_hmac *source = (mz_crypt_hmac *)src_handle;
    mz_crypt_hmac *target = (mz_crypt_hmac *)target_handle;

    if (!source || !target)
        return MZ_PARAM_ERROR;

    memcpy(&target->ctx, &source->ctx, sizeof(CCHmacContext));
    return MZ_OK;
}

void *mz_crypt_hmac_create(void **handle) {
    mz_crypt_hmac *hmac = NULL;

    hmac = (mz_crypt_hmac *)calloc(1, sizeof(mz_crypt_hmac));
    if (hmac)
        hmac->algorithm = MZ_HASH_SHA256;
    if (handle)
        *handle = hmac;

    return hmac;
}

void mz_crypt_hmac_delete(void **handle) {
    mz_crypt_hmac *hmac = NULL;
    if (!handle)
        return;
    hmac = (mz_crypt_hmac *)*handle;
    if (hmac) {
        mz_crypt_hmac_free(*handle);
        free(hmac);
    }
    *handle = NULL;
}

/***************************************************************************/

#if defined(MZ_ZIP_SIGNING)
int32_t mz_crypt_sign(uint8_t *message, int32_t message_size, uint8_t *cert_data, int32_t cert_data_size,
    const char *cert_pwd, uint8_t **signature, int32_t *signature_size) {
    CFStringRef password_ref = NULL;
    CFDictionaryRef options_dict = NULL;
    CFDictionaryRef identity_trust = NULL;
    CFDataRef signature_out = NULL;
    CFDataRef pkcs12_data = NULL;
    CFArrayRef items = 0;
    SecIdentityRef identity = NULL;
    SecTrustRef trust = NULL;
    OSStatus status = noErr;
    const void *options_key[2] = {kSecImportExportPassphrase, kSecReturnRef};
    const void *options_values[2] = {0, kCFBooleanTrue};
    int32_t err = MZ_SIGN_ERROR;

    if (!message || !cert_data || !signature || !signature_size)
        return MZ_PARAM_ERROR;

    *signature = NULL;
    *signature_size = 0;

    password_ref = CFStringCreateWithCString(0, cert_pwd, kCFStringEncodingUTF8);
    options_values[0] = password_ref;

    options_dict = CFDictionaryCreate(0, options_key, options_values, 2, 0, 0);
    if (options_dict)
        pkcs12_data = CFDataCreate(0, cert_data, cert_data_size);
    if (pkcs12_data)
        status = SecPKCS12Import(pkcs12_data, options_dict, &items);
    if (status == noErr)
        identity_trust = CFArrayGetValueAtIndex(items, 0);
    if (identity_trust)
        identity = (SecIdentityRef)CFDictionaryGetValue(identity_trust, kSecImportItemIdentity);
    if (identity)
        trust = (SecTrustRef)CFDictionaryGetValue(identity_trust, kSecImportItemTrust);
    if (trust) {
        status = CMSEncodeContent(identity, NULL, NULL, FALSE, 0, message, message_size, &signature_out);

        if (status == errSecSuccess) {
            *signature_size = CFDataGetLength(signature_out);
            *signature = (uint8_t *)malloc(*signature_size);

            memcpy(*signature, CFDataGetBytePtr(signature_out), *signature_size);

            err = MZ_OK;
        }
    }

    if (signature_out)
        CFRelease(signature_out);
    if (items)
        CFRelease(items);
    if (pkcs12_data)
        CFRelease(pkcs12_data);
    if (options_dict)
        CFRelease(options_dict);
    if (password_ref)
        CFRelease(password_ref);

    return err;
}

int32_t mz_crypt_sign_verify(uint8_t *message, int32_t message_size, uint8_t *signature, int32_t signature_size) {
    CMSDecoderRef decoder = NULL;
    CMSSignerStatus signer_status = 0;
    CFDataRef message_out = NULL;
    SecPolicyRef trust_policy = NULL;
    OSStatus status = noErr;
    OSStatus verify_status = noErr;
    size_t signer_count = 0;
    size_t i = 0;
    int32_t err = MZ_SIGN_ERROR;

    if (!message || !signature)
        return MZ_PARAM_ERROR;

    status = CMSDecoderCreate(&decoder);
    if (status == errSecSuccess)
        status = CMSDecoderUpdateMessage(decoder, signature, signature_size);
    if (status == errSecSuccess)
        status = CMSDecoderFinalizeMessage(decoder);
    if (status == errSecSuccess)
        trust_policy = SecPolicyCreateBasicX509();

    if (status == errSecSuccess && trust_policy) {
        CMSDecoderGetNumSigners(decoder, &signer_count);
        if (signer_count > 0)
            err = MZ_OK;
        for (i = 0; i < signer_count; i += 1) {
            status = CMSDecoderCopySignerStatus(decoder, i, trust_policy, TRUE, &signer_status, NULL, &verify_status);
            if (status != errSecSuccess || verify_status != 0 || signer_status != kCMSSignerValid) {
                err = MZ_SIGN_ERROR;
                break;
            }
        }
    }

    if (err == MZ_OK) {
        status = CMSDecoderCopyContent(decoder, &message_out);
        if ((status != errSecSuccess) ||
            (CFDataGetLength(message_out) != message_size) ||
            (memcmp(message, CFDataGetBytePtr(message_out), message_size) != 0))
            err = MZ_SIGN_ERROR;
    }

    if (trust_policy)
        CFRelease(trust_policy);
    if (decoder)
        CFRelease(decoder);

    return err;
}

#endif
