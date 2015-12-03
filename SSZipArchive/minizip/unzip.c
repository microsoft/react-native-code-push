/* unzip.c -- IO for uncompress .zip files using zlib
   Version 1.1, February 14h, 2010
   part of the MiniZip project

   Copyright (C) 1998-2010 Gilles Vollant
     http://www.winimage.com/zLibDll/minizip.html
   Modifications of Unzip for Zip64
     Copyright (C) 2007-2008 Even Rouault
   Modifications for Zip64 support on both zip and unzip
     Copyright (C) 2009-2010 Mathias Svensson
     http://result42.com
   Modifications for AES, PKWARE disk spanning
     Copyright (C) 2010-2014 Nathan Moinvaziri

   This program is distributed under the terms of the same license as zlib.
   See the accompanying LICENSE file for the full text of the license.
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*#ifndef NOUNCRYPT
 #  define NOUNCRYPT
 #endif*/

#include "zlib.h"
#include "unzip.h"

#include "Common.h"

#ifdef STDC
#  include <stddef.h>
#  include <string.h>
#  include <stdlib.h>
#endif
#ifdef NO_ERRNO_H
extern int errno;
#else
#  include <errno.h>
#endif

#ifdef HAVE_AES
#  define AES_METHOD          (99)
#  define AES_PWVERIFYSIZE    (2)
#  define AES_MAXSALTLENGTH   (16)
#  define AES_AUTHCODESIZE    (10)
#  define AES_HEADERSIZE      (11)
#  define AES_KEYSIZE(mode)   (64 + (mode * 64))

#  include "aes.h"
#  include "fileenc.h"
#endif
#ifndef NOUNCRYPT
#  include "crypt.h"
#endif

#ifndef local
#  define local static
#endif
/* compile with -Dlocal if your debugger can't find static symbols */

#define DISKHEADERMAGIC          (0x08074b50)
#define LOCALHEADERMAGIC         (0x04034b50)
#define CENTRALHEADERMAGIC       (0x02014b50)
#define ENDHEADERMAGIC           (0x06054b50)
#define ZIP64ENDHEADERMAGIC      (0x06064b50)
#define ZIP64ENDLOCHEADERMAGIC   (0x07064b50)

#define SIZECENTRALDIRITEM       (0x2e)
#define SIZECENTRALHEADERLOCATOR (0x14) /* 20 */
#define SIZEZIPLOCALHEADER       (0x1e)

#ifndef BUFREADCOMMENT
#  define BUFREADCOMMENT (0x400)
#endif

#ifndef UNZ_BUFSIZE
#  define UNZ_BUFSIZE (64 * 1024)
#endif
#ifndef UNZ_MAXFILENAMEINZIP
#  define UNZ_MAXFILENAMEINZIP (256)
#endif

#ifndef ALLOC
#  define ALLOC(size) (malloc(size))
#endif
#ifndef TRYFREE
#  define TRYFREE(p) {if (p) free(p); }
#endif

const char unz_copyright[] =
    " unzip 1.01 Copyright 1998-2004 Gilles Vollant - http://www.winimage.com/zLibDll";

/* unz_file_info_interntal contain internal info about a file in zipfile*/
typedef struct unz_file_info64_internal_s {
    ZPOS64_T offset_curfile;            /* relative offset of local header 8 bytes */
    ZPOS64_T byte_before_the_zipfile;   /* byte before the zipfile, (>0 for sfx) */
#ifdef HAVE_AES
    uLong aes_encryption_mode;
    uLong aes_compression_method;
    uLong aes_version;
#endif
} unz_file_info64_internal;

/* file_in_zip_read_info_s contain internal information about a file in zipfile */
typedef struct {
    Bytef *read_buffer;                 /* internal buffer for compressed data */
    z_stream stream;                    /* zLib stream structure for inflate */

#ifdef HAVE_BZIP2
    bz_stream bstream;                  /* bzLib stream structure for bziped */
#endif
#ifdef HAVE_AES
    fcrypt_ctx aes_ctx;
#endif

    ZPOS64_T pos_in_zipfile;            /* position in byte on the zipfile, for fseek */
    uLong stream_initialised;           /* flag set if stream structure is initialised */

    ZPOS64_T offset_local_extrafield;   /* offset of the local extra field */
    uInt size_local_extrafield;         /* size of the local extra field */
    ZPOS64_T pos_local_extrafield;      /* position in the local extra field in read */
    ZPOS64_T total_out_64;

    uLong crc32;                        /* crc32 of all data uncompressed */
    uLong crc32_wait;                   /* crc32 we must obtain after decompress all */
    ZPOS64_T rest_read_compressed;      /* number of byte to be decompressed */
    ZPOS64_T rest_read_uncompressed;    /* number of byte to be obtained after decomp */

    zlib_filefunc64_32_def z_filefunc;

    voidpf filestream;                  /* io structore of the zipfile */
    uLong compression_method;           /* compression method (0==store) */
    ZPOS64_T byte_before_the_zipfile;   /* byte before the zipfile, (>0 for sfx) */
    int raw;
} file_in_zip64_read_info_s;

/* unz64_s contain internal information about the zipfile */
typedef struct {
    zlib_filefunc64_32_def z_filefunc;
    voidpf filestream;                  /* io structure of the current zipfile */
    voidpf filestream_with_CD;          /* io structure of the disk with the central directory */
    unz_global_info64 gi;               /* public global information */
    ZPOS64_T byte_before_the_zipfile;   /* byte before the zipfile, (>0 for sfx)*/
    ZPOS64_T num_file;                  /* number of the current file in the zipfile*/
    ZPOS64_T pos_in_central_dir;        /* pos of the current file in the central dir*/
    ZPOS64_T current_file_ok;           /* flag about the usability of the current file*/
    ZPOS64_T central_pos;               /* position of the beginning of the central dir*/
    uLong number_disk;                  /* number of the current disk, used for spanning ZIP*/
    ZPOS64_T size_central_dir;          /* size of the central directory  */
    ZPOS64_T offset_central_dir;        /* offset of start of central directory with
                                           respect to the starting disk number */

    unz_file_info64 cur_file_info;      /* public info about the current file in zip*/
    unz_file_info64_internal cur_file_info_internal;
    /* private info about it*/
    file_in_zip64_read_info_s *pfile_in_zip_read;
    /* structure about the current file if we are decompressing it */
    int isZip64;                        /* is the current file zip64 */
#ifndef NOUNCRYPT
    unsigned long keys[3];              /* keys defining the pseudo-random sequence */
    const unsigned long *pcrc_32_tab;
#endif
} unz64_s;

/* Translate date/time from Dos format to tm_unz (readable more easily) */
local void unz64local_DosDateToTmuDate(ZPOS64_T ulDosDate, tm_unz *ptm)
{
    ZPOS64_T uDate = (ZPOS64_T)(ulDosDate >> 16);

    ptm->tm_mday = (uInt)(uDate & 0x1f);
    ptm->tm_mon = (uInt)((((uDate) & 0x1E0) / 0x20) - 1);
    ptm->tm_year = (uInt)(((uDate & 0x0FE00) / 0x0200) + 1980);
    ptm->tm_hour = (uInt)((ulDosDate & 0xF800) / 0x800);
    ptm->tm_min = (uInt)((ulDosDate & 0x7E0) / 0x20);
    ptm->tm_sec = (uInt)(2 * (ulDosDate & 0x1f));

#define unz64local_in_range(min, max, value) ((min) <= (value) && (value) <= (max))
    if (!unz64local_in_range(0, 11, ptm->tm_mon) ||
        !unz64local_in_range(1, 31, ptm->tm_mday) ||
        !unz64local_in_range(0, 23, ptm->tm_hour) ||
        !unz64local_in_range(0, 59, ptm->tm_min) ||
        !unz64local_in_range(0, 59, ptm->tm_sec))
        /* Invalid date stored, so don't return it. */
        memset(ptm, 0, sizeof(tm_unz));
#undef unz64local_in_range
}

/* Read a byte from a gz_stream; Return EOF for end of file. */
local int unz64local_getByte(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream, int *pi)
{
    unsigned char c;
    int err = (int)ZREAD64(*pzlib_filefunc_def, filestream, &c, 1);
    if (err == 1) {
        *pi = (int)c;
        return UNZ_OK;
    }
    if (ZERROR64(*pzlib_filefunc_def, filestream))
        return UNZ_ERRNO;
    return UNZ_EOF;
}

local int unz64local_getShort OF((const zlib_filefunc64_32_def * pzlib_filefunc_def, voidpf filestream, uLong * pX));
local int unz64local_getShort(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream, uLong *pX)
{
    uLong x;
    int i = 0;
    int err;

    err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x = (uLong)i;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((uLong)i) << 8;

    if (err == UNZ_OK)
        *pX = x;
    else
        *pX = 0;
    return err;
}

local int unz64local_getLong OF((const zlib_filefunc64_32_def * pzlib_filefunc_def, voidpf filestream, uLong * pX));
local int unz64local_getLong(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream, uLong *pX)
{
    uLong x;
    int i = 0;
    int err;

    err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x = (uLong)i;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((uLong)i) << 8;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((uLong)i) << 16;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x += ((uLong)i) << 24;

    if (err == UNZ_OK)
        *pX = x;
    else
        *pX = 0;
    return err;
}

local int unz64local_getLong64 OF((const zlib_filefunc64_32_def * pzlib_filefunc_def, voidpf filestream, ZPOS64_T * pX));
local int unz64local_getLong64(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream, ZPOS64_T *pX)
{
    ZPOS64_T x;
    int i = 0;
    int err;

    err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x = (ZPOS64_T)i;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 8;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 16;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 24;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 32;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 40;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 48;
    if (err == UNZ_OK)
        err = unz64local_getByte(pzlib_filefunc_def, filestream, &i);
    x |= ((ZPOS64_T)i) << 56;

    if (err == UNZ_OK)
        *pX = x;
    else
        *pX = 0;
    return err;
}

/* Locate the Central directory of a zip file (at the end, just before the global comment) */
local ZPOS64_T unz64local_SearchCentralDir OF((const zlib_filefunc64_32_def * pzlib_filefunc_def, voidpf filestream));
local ZPOS64_T unz64local_SearchCentralDir(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream)
{
    unsigned char *buf;
    ZPOS64_T file_size;
    ZPOS64_T back_read = 4;
    ZPOS64_T max_back = 0xffff; /* maximum size of global comment */
    ZPOS64_T pos_found = 0;
    uLong read_size;
    ZPOS64_T read_pos;
    int i;

    buf = (unsigned char *)ALLOC(BUFREADCOMMENT + 4);
    if (buf == NULL)
        return 0;

    if (ZSEEK64(*pzlib_filefunc_def, filestream, 0, ZLIB_FILEFUNC_SEEK_END) != 0) {
        TRYFREE(buf);
        return 0;
    }

    file_size = ZTELL64(*pzlib_filefunc_def, filestream);

    if (max_back > file_size)
        max_back = file_size;

    while (back_read < max_back) {
        if (back_read + BUFREADCOMMENT > max_back)
            back_read = max_back;
        else
            back_read += BUFREADCOMMENT;

        read_pos = file_size - back_read;
        read_size = ((BUFREADCOMMENT + 4) < (file_size - read_pos)) ?
                    (BUFREADCOMMENT + 4) : (uLong)(file_size - read_pos);

        if (ZSEEK64(*pzlib_filefunc_def, filestream, read_pos, ZLIB_FILEFUNC_SEEK_SET) != 0)
            break;
        if (ZREAD64(*pzlib_filefunc_def, filestream, buf, read_size) != read_size)
            break;

        for (i = (int)read_size - 3; (i--) > 0; )
            if (((*(buf + i)) == (ENDHEADERMAGIC & 0xff)) &&
                ((*(buf + i + 1)) == (ENDHEADERMAGIC >> 8 & 0xff)) &&
                ((*(buf + i + 2)) == (ENDHEADERMAGIC >> 16 & 0xff)) &&
                ((*(buf + i + 3)) == (ENDHEADERMAGIC >> 24 & 0xff))) {
                pos_found = read_pos + i;
                break;
            }

        if (pos_found != 0)
            break;
    }
    TRYFREE(buf);
    return pos_found;
}

/* Locate the Central directory 64 of a zipfile (at the end, just before the global comment) */
local ZPOS64_T unz64local_SearchCentralDir64 OF((const zlib_filefunc64_32_def * pzlib_filefunc_def, voidpf filestream,
                                                 const ZPOS64_T endcentraloffset));
local ZPOS64_T unz64local_SearchCentralDir64(const zlib_filefunc64_32_def *pzlib_filefunc_def, voidpf filestream,
                                             const ZPOS64_T endcentraloffset)
{
    ZPOS64_T offset;
    uLong uL;

    /* Zip64 end of central directory locator */
    if (ZSEEK64(*pzlib_filefunc_def, filestream, endcentraloffset - SIZECENTRALHEADERLOCATOR, ZLIB_FILEFUNC_SEEK_SET) != 0)
        return 0;

    /* read locator signature */
    if (unz64local_getLong(pzlib_filefunc_def, filestream, &uL) != UNZ_OK)
        return 0;
    if (uL != ZIP64ENDLOCHEADERMAGIC)
        return 0;
    /* number of the disk with the start of the zip64 end of  central directory */
    if (unz64local_getLong(pzlib_filefunc_def, filestream, &uL) != UNZ_OK)
        return 0;
    /* relative offset of the zip64 end of central directory record */
    if (unz64local_getLong64(pzlib_filefunc_def, filestream, &offset) != UNZ_OK)
        return 0;
    /* total number of disks */
    if (unz64local_getLong(pzlib_filefunc_def, filestream, &uL) != UNZ_OK)
        return 0;
    /* Goto end of central directory record */
    if (ZSEEK64(*pzlib_filefunc_def, filestream, offset, ZLIB_FILEFUNC_SEEK_SET) != 0)
        return 0;
    /* the signature */
    if (unz64local_getLong(pzlib_filefunc_def, filestream, &uL) != UNZ_OK)
        return 0;
    if (uL != ZIP64ENDHEADERMAGIC)
        return 0;

    return offset;
}

local unzFile unzOpenInternal(const void *path, zlib_filefunc64_32_def *pzlib_filefunc64_32_def)
{
    unz64_s us;
    unz64_s *s;
    ZPOS64_T central_pos;
    uLong uL;
    voidpf filestream = NULL;
    ZPOS64_T number_entry_CD;
    int err = UNZ_OK;

    if (unz_copyright[0] != ' ')
        return NULL;

    us.filestream = NULL;
    us.filestream_with_CD = NULL;
    us.z_filefunc.zseek32_file = NULL;
    us.z_filefunc.ztell32_file = NULL;
    if (pzlib_filefunc64_32_def == NULL)
        fill_fopen64_filefunc(&us.z_filefunc.zfile_func64);
    else
        us.z_filefunc = *pzlib_filefunc64_32_def;

    us.filestream = ZOPEN64(us.z_filefunc, path, ZLIB_FILEFUNC_MODE_READ | ZLIB_FILEFUNC_MODE_EXISTING);

    if (us.filestream == NULL)
        return NULL;

    us.filestream_with_CD = us.filestream;
    us.isZip64 = 0;

    /* Use unz64local_SearchCentralDir first. Only based on the result
       is it necessary to locate the unz64local_SearchCentralDir64 */
    central_pos = unz64local_SearchCentralDir(&us.z_filefunc, us.filestream);
    if (central_pos) {
        if (ZSEEK64(us.z_filefunc, us.filestream, central_pos, ZLIB_FILEFUNC_SEEK_SET) != 0)
            err = UNZ_ERRNO;

        /* the signature, already checked */
        if (unz64local_getLong(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        /* number of this disk */
        if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        us.number_disk = uL;
        /* number of the disk with the start of the central directory */
        if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        us.gi.number_disk_with_CD = uL;
        /* total number of entries in the central directory on this disk */
        if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        us.gi.number_entry = uL;
        /* total number of entries in the central directory */
        if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        number_entry_CD = uL;
        if (number_entry_CD != us.gi.number_entry)
            err = UNZ_BADZIPFILE;
        /* size of the central directory */
        if (unz64local_getLong(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        us.size_central_dir = uL;
        /* offset of start of central directory with respect to the starting disk number */
        if (unz64local_getLong(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
            err = UNZ_ERRNO;
        us.offset_central_dir = uL;
        /* zipfile comment length */
        if (unz64local_getShort(&us.z_filefunc, us.filestream, &us.gi.size_comment) != UNZ_OK)
            err = UNZ_ERRNO;

        if ((err == UNZ_OK) &&
            ((us.gi.number_entry == 0xffff) || (us.size_central_dir == 0xffff) || (us.offset_central_dir == 0xffffffff))) {
            /* Format should be Zip64, as the central directory or file size is too large */
            central_pos = unz64local_SearchCentralDir64(&us.z_filefunc, us.filestream, central_pos);
            if (central_pos) {
                ZPOS64_T uL64;

                us.isZip64 = 1;

                if (ZSEEK64(us.z_filefunc, us.filestream, central_pos, ZLIB_FILEFUNC_SEEK_SET) != 0)
                    err = UNZ_ERRNO;

                /* the signature, already checked */
                if (unz64local_getLong(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* size of zip64 end of central directory record */
                if (unz64local_getLong64(&us.z_filefunc, us.filestream, &uL64) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* version made by */
                if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* version needed to extract */
                if (unz64local_getShort(&us.z_filefunc, us.filestream, &uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* number of this disk */
                if (unz64local_getLong(&us.z_filefunc, us.filestream, &us.number_disk) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* number of the disk with the start of the central directory */
                if (unz64local_getLong(&us.z_filefunc, us.filestream, &us.gi.number_disk_with_CD) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* total number of entries in the central directory on this disk */
                if (unz64local_getLong64(&us.z_filefunc, us.filestream, &us.gi.number_entry) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* total number of entries in the central directory */
                if (unz64local_getLong64(&us.z_filefunc, us.filestream, &number_entry_CD) != UNZ_OK)
                    err = UNZ_ERRNO;
                if (number_entry_CD != us.gi.number_entry)
                    err = UNZ_BADZIPFILE;
                /* size of the central directory */
                if (unz64local_getLong64(&us.z_filefunc, us.filestream, &us.size_central_dir) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* offset of start of central directory with respect to the starting disk number */
                if (unz64local_getLong64(&us.z_filefunc, us.filestream, &us.offset_central_dir) != UNZ_OK)
                    err = UNZ_ERRNO;
            } else
                err = UNZ_BADZIPFILE;
        }
    } else
        err = UNZ_ERRNO;

    if ((err == UNZ_OK) && (central_pos < us.offset_central_dir + us.size_central_dir))
        err = UNZ_BADZIPFILE;

    if (err != UNZ_OK) {
        ZCLOSE64(us.z_filefunc, us.filestream);
        return NULL;
    }

    if (us.gi.number_disk_with_CD == 0) {
        /* If there is only one disk open another stream so we don't have to seek between the CD
           and the file headers constantly */
        filestream = ZOPEN64(us.z_filefunc, path, ZLIB_FILEFUNC_MODE_READ | ZLIB_FILEFUNC_MODE_EXISTING);
        if (filestream != NULL)
            us.filestream = filestream;
    }

    /* Hack for zip files that have no respect for zip64
       if ((central_pos > 0xffffffff) && (us.offset_central_dir < 0xffffffff))
        us.offset_central_dir = central_pos - us.size_central_dir;*/

    us.byte_before_the_zipfile = central_pos - (us.offset_central_dir + us.size_central_dir);
    us.central_pos = central_pos;
    us.pfile_in_zip_read = NULL;

    s = (unz64_s *)ALLOC(sizeof(unz64_s));
    if (s != NULL) {
        *s = us;
        unzGoToFirstFile((unzFile)s);
    }
    return (unzFile)s;
}

extern unzFile ZEXPORT unzOpen2(const char *path, zlib_filefunc_def *pzlib_filefunc32_def)
{
    if (pzlib_filefunc32_def != NULL) {
        zlib_filefunc64_32_def zlib_filefunc64_32_def_fill;
        fill_zlib_filefunc64_32_def_from_filefunc32(&zlib_filefunc64_32_def_fill, pzlib_filefunc32_def);
        return unzOpenInternal(path, &zlib_filefunc64_32_def_fill);
    }
    return unzOpenInternal(path, NULL);
}

extern unzFile ZEXPORT unzOpen2_64(const void *path, zlib_filefunc64_def *pzlib_filefunc_def)
{
    if (pzlib_filefunc_def != NULL) {
        zlib_filefunc64_32_def zlib_filefunc64_32_def_fill;
        zlib_filefunc64_32_def_fill.zfile_func64 = *pzlib_filefunc_def;
        zlib_filefunc64_32_def_fill.ztell32_file = NULL;
        zlib_filefunc64_32_def_fill.zseek32_file = NULL;
        return unzOpenInternal(path, &zlib_filefunc64_32_def_fill);
    }
    return unzOpenInternal(path, NULL);
}

extern unzFile ZEXPORT unzOpen(const char *path)
{
    return unzOpenInternal(path, NULL);
}

extern unzFile ZEXPORT unzOpen64(const void *path)
{
    return unzOpenInternal(path, NULL);
}

extern int ZEXPORT unzClose(unzFile file)
{
    unz64_s *s;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;

    if (s->pfile_in_zip_read != NULL)
        unzCloseCurrentFile(file);

    if ((s->filestream != NULL) && (s->filestream != s->filestream_with_CD))
        ZCLOSE64(s->z_filefunc, s->filestream);
    if (s->filestream_with_CD != NULL)
        ZCLOSE64(s->z_filefunc, s->filestream_with_CD);

    s->filestream = NULL;
    s->filestream_with_CD = NULL;
    TRYFREE(s);
    return UNZ_OK;
}

/* Goto to the next available disk for spanned archives */
local int unzGoToNextDisk OF((unzFile file));
local int unzGoToNextDisk(unzFile file)
{
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    uLong number_disk_next = 0;

    s = (unz64_s *)file;
    if (s == NULL)
        return UNZ_PARAMERROR;
    pfile_in_zip_read_info = s->pfile_in_zip_read;
    number_disk_next = s->number_disk;

    if ((pfile_in_zip_read_info != NULL) && (pfile_in_zip_read_info->rest_read_uncompressed > 0))
        /* We are currently reading a file and we need the next sequential disk */
        number_disk_next += 1;
    else
        /* Goto the disk for the current file */
        number_disk_next = s->cur_file_info.disk_num_start;

    if (number_disk_next != s->number_disk) {
        /* Switch disks */
        if ((s->filestream != NULL) && (s->filestream != s->filestream_with_CD))
            ZCLOSE64(s->z_filefunc, s->filestream);

        if (number_disk_next == s->gi.number_disk_with_CD) {
            s->filestream = s->filestream_with_CD;
        } else {
            s->filestream = ZOPENDISK64(s->z_filefunc, s->filestream_with_CD, (unsigned int)number_disk_next,
                                        ZLIB_FILEFUNC_MODE_READ | ZLIB_FILEFUNC_MODE_EXISTING);
        }

        if (s->filestream == NULL)
            return UNZ_ERRNO;

        s->number_disk = number_disk_next;
    }

    return UNZ_OK;
}

extern int ZEXPORT unzGetGlobalInfo(unzFile file, unz_global_info *pglobal_info32)
{
    unz64_s *s;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    /* to do : check if number_entry is not truncated */
    pglobal_info32->number_entry = (uLong)s->gi.number_entry;
    pglobal_info32->size_comment = s->gi.size_comment;
    pglobal_info32->number_disk_with_CD = s->gi.number_disk_with_CD;
    return UNZ_OK;
}

extern int ZEXPORT unzGetGlobalInfo64(unzFile file, unz_global_info64 *pglobal_info)
{
    unz64_s *s;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    *pglobal_info = s->gi;
    return UNZ_OK;
}

extern int ZEXPORT unzGetGlobalComment(unzFile file, char *comment, uLong comment_size)
{
    unz64_s *s;
    uLong bytes_to_read = comment_size;
    if (file == NULL)
        return (int)UNZ_PARAMERROR;
    s = (unz64_s *)file;

    if (bytes_to_read > s->gi.size_comment)
        bytes_to_read = s->gi.size_comment;

    if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, s->central_pos + 22, ZLIB_FILEFUNC_SEEK_SET) != 0)
        return UNZ_ERRNO;

    if (bytes_to_read > 0) {
        *comment = 0;
        if (ZREAD64(s->z_filefunc, s->filestream_with_CD, comment, bytes_to_read) != bytes_to_read)
            return UNZ_ERRNO;
    }

    if ((comment != NULL) && (comment_size > s->gi.size_comment))
        *(comment + s->gi.size_comment) = 0;
    return (int)bytes_to_read;
}

/* Get Info about the current file in the zipfile, with internal only info */
local int unz64local_GetCurrentFileInfoInternal(unzFile file, unz_file_info64 *pfile_info,
                                                unz_file_info64_internal *pfile_info_internal, char *filename, uLong filename_size, void *extrafield,
                                                uLong extrafield_size, char *comment, uLong comment_size)
{
    unz64_s *s;
    unz_file_info64 file_info;
    unz_file_info64_internal file_info_internal;
    ZPOS64_T bytes_to_read;
    int err = UNZ_OK;
    uLong uMagic;
    long lSeek = 0;
    ZPOS64_T current_pos = 0;
    uLong acc = 0;
    uLong uL;
    ZPOS64_T uL64;

    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;

    if (ZSEEK64(s->z_filefunc, s->filestream_with_CD,
                s->pos_in_central_dir + s->byte_before_the_zipfile, ZLIB_FILEFUNC_SEEK_SET) != 0)
        err = UNZ_ERRNO;

    /* Check the magic */
    if (err == UNZ_OK) {
        if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &uMagic) != UNZ_OK)
            err = UNZ_ERRNO;
        else if (uMagic != CENTRALHEADERMAGIC)
            err = UNZ_BADZIPFILE;
    }

    /* Read central directory header */
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.version) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.version_needed) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.flag) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.compression_method) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &file_info.dosDate) != UNZ_OK)
        err = UNZ_ERRNO;
    unz64local_DosDateToTmuDate(file_info.dosDate, &file_info.tmu_date);
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &file_info.crc) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &uL) != UNZ_OK)
        err = UNZ_ERRNO;
    file_info.compressed_size = uL;
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &uL) != UNZ_OK)
        err = UNZ_ERRNO;
    file_info.uncompressed_size = uL;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.size_filename) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.size_file_extra) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.size_file_comment) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.disk_num_start) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &file_info.internal_fa) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &file_info.external_fa) != UNZ_OK)
        err = UNZ_ERRNO;
    /* Relative offset of local header */
    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &uL) != UNZ_OK)
        err = UNZ_ERRNO;

    file_info.size_file_extra_internal = 0;
    file_info.disk_offset = uL;
    file_info_internal.offset_curfile = uL;
#ifdef HAVE_AES
    file_info_internal.aes_compression_method = 0;
    file_info_internal.aes_encryption_mode = 0;
    file_info_internal.aes_version = 0;
#endif

    lSeek += file_info.size_filename;

    if ((err == UNZ_OK) && (filename != NULL)) {
        if (file_info.size_filename < filename_size) {
            *(filename + file_info.size_filename) = 0;
            bytes_to_read = file_info.size_filename;
        } else
            bytes_to_read = filename_size;

        if ((file_info.size_filename > 0) && (filename_size > 0))
            if (ZREAD64(s->z_filefunc, s->filestream_with_CD, filename, (uLong)bytes_to_read) != bytes_to_read)
                err = UNZ_ERRNO;
        lSeek -= (uLong)bytes_to_read;
    }

    /* Read extrafield */
    if ((err == UNZ_OK) && (extrafield != NULL)) {
        if (file_info.size_file_extra < extrafield_size)
            bytes_to_read = file_info.size_file_extra;
        else
            bytes_to_read = extrafield_size;

        if (lSeek != 0) {
            if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, lSeek, ZLIB_FILEFUNC_SEEK_CUR) == 0)
                lSeek = 0;
            else
                err = UNZ_ERRNO;
        }

        if ((file_info.size_file_extra > 0) && (extrafield_size > 0))
            if (ZREAD64(s->z_filefunc, s->filestream_with_CD, extrafield, (uLong)bytes_to_read) != bytes_to_read)
                err = UNZ_ERRNO;
        lSeek += file_info.size_file_extra - (uLong)bytes_to_read;
    } else
        lSeek += file_info.size_file_extra;

    if ((err == UNZ_OK) && (file_info.size_file_extra != 0)) {
        if (lSeek != 0) {
            if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, lSeek, ZLIB_FILEFUNC_SEEK_CUR) == 0)
                lSeek = 0;
            else
                err = UNZ_ERRNO;
        }

        /* We are going to parse the extra field so we need to move back */
        current_pos = ZTELL64(s->z_filefunc, s->filestream_with_CD);
        if (current_pos < file_info.size_file_extra)
            err = UNZ_ERRNO;
        current_pos -= file_info.size_file_extra;
        if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, current_pos, ZLIB_FILEFUNC_SEEK_SET) != 0)
            err = UNZ_ERRNO;

        while ((err != UNZ_ERRNO) && (acc < file_info.size_file_extra)) {
            uLong headerid;
            uLong datasize;

            if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &headerid) != UNZ_OK)
                err = UNZ_ERRNO;
            if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &datasize) != UNZ_OK)
                err = UNZ_ERRNO;

            /* ZIP64 extra fields */
            if (headerid == 0x0001) {
                /* Subtract size of ZIP64 field, since ZIP64 is handled internally */
                file_info.size_file_extra_internal += 2 + 2 + datasize;

                if (file_info.uncompressed_size == 0xffffffff) {
                    if (unz64local_getLong64(&s->z_filefunc, s->filestream_with_CD, &file_info.uncompressed_size) != UNZ_OK)
                        err = UNZ_ERRNO;
                }
                if (file_info.compressed_size == 0xffffffff) {
                    if (unz64local_getLong64(&s->z_filefunc, s->filestream_with_CD, &file_info.compressed_size) != UNZ_OK)
                        err = UNZ_ERRNO;
                }
                if (file_info_internal.offset_curfile == 0xffffffff) {
                    /* Relative Header offset */
                    if (unz64local_getLong64(&s->z_filefunc, s->filestream_with_CD, &uL64) != UNZ_OK)
                        err = UNZ_ERRNO;
                    file_info_internal.offset_curfile = uL64;
                    file_info.disk_offset = uL64;
                }
                if (file_info.disk_num_start == 0xffffffff) {
                    /* Disk Start Number */
                    if (unz64local_getLong(&s->z_filefunc, s->filestream_with_CD, &file_info.disk_num_start) != UNZ_OK)
                        err = UNZ_ERRNO;
                }
            }
#ifdef HAVE_AES
            /* AES header */
            else if (headerid == 0x9901) {
                /* Subtract size of AES field, since AES is handled internally */
                file_info.size_file_extra_internal += 2 + 2 + datasize;

                /* Verify version info */
                if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                /* Support AE-1 and AE-2 */
                if (uL != 1 && uL != 2)
                    err = UNZ_ERRNO;
                file_info_internal.aes_version = uL;
                if (unz64local_getByte(&s->z_filefunc, s->filestream_with_CD, (int *)&uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                if ((char)uL != 'A')
                    err = UNZ_ERRNO;
                if (unz64local_getByte(&s->z_filefunc, s->filestream_with_CD, (int *)&uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                if ((char)uL != 'E')
                    err = UNZ_ERRNO;
                /* Get AES encryption strength and actual compression method */
                if (unz64local_getByte(&s->z_filefunc, s->filestream_with_CD, (int *)&uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                file_info_internal.aes_encryption_mode = uL;
                if (unz64local_getShort(&s->z_filefunc, s->filestream_with_CD, &uL) != UNZ_OK)
                    err = UNZ_ERRNO;
                file_info_internal.aes_compression_method = uL;
            }
#endif
            else {
                if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, datasize, ZLIB_FILEFUNC_SEEK_CUR) != 0)
                    err = UNZ_ERRNO;
            }

            acc += 2 + 2 + datasize;
        }
    }

    if (file_info.disk_num_start == s->gi.number_disk_with_CD)
        file_info_internal.byte_before_the_zipfile = s->byte_before_the_zipfile;
    else
        file_info_internal.byte_before_the_zipfile = 0;

    if ((err == UNZ_OK) && (comment != NULL)) {
        if (file_info.size_file_comment < comment_size) {
            *(comment + file_info.size_file_comment) = 0;
            bytes_to_read = file_info.size_file_comment;
        } else
            bytes_to_read = comment_size;

        if (lSeek != 0) {
            if (ZSEEK64(s->z_filefunc, s->filestream_with_CD, lSeek, ZLIB_FILEFUNC_SEEK_CUR) != 0)
                err = UNZ_ERRNO;
        }

        if ((file_info.size_file_comment > 0) && (comment_size > 0))
            if (ZREAD64(s->z_filefunc, s->filestream_with_CD, comment, (uLong)bytes_to_read) != bytes_to_read)
                err = UNZ_ERRNO;
        lSeek += file_info.size_file_comment - (uLong)bytes_to_read;
    } else
        lSeek += file_info.size_file_comment;

    if ((err == UNZ_OK) && (pfile_info != NULL))
        *pfile_info = file_info;

    if ((err == UNZ_OK) && (pfile_info_internal != NULL))
        *pfile_info_internal = file_info_internal;

    return err;
}

extern int ZEXPORT unzGetCurrentFileInfo(unzFile file, unz_file_info *pfile_info, char *filename,
                                         uLong filename_size, void *extrafield, uLong extrafield_size, char *comment, uLong comment_size)
{
    unz_file_info64 file_info64;
    int err;

    err = unz64local_GetCurrentFileInfoInternal(file, &file_info64, NULL, filename, filename_size,
                                                extrafield, extrafield_size, comment, comment_size);

    if ((err == UNZ_OK) && (pfile_info != NULL)) {
        pfile_info->version = file_info64.version;
        pfile_info->version_needed = file_info64.version_needed;
        pfile_info->flag = file_info64.flag;
        pfile_info->compression_method = file_info64.compression_method;
        pfile_info->dosDate = file_info64.dosDate;
        pfile_info->crc = file_info64.crc;

        pfile_info->size_filename = file_info64.size_filename;
        pfile_info->size_file_extra = file_info64.size_file_extra - file_info64.size_file_extra_internal;
        pfile_info->size_file_comment = file_info64.size_file_comment;

        pfile_info->disk_num_start = file_info64.disk_num_start;
        pfile_info->internal_fa = file_info64.internal_fa;
        pfile_info->external_fa = file_info64.external_fa;

        pfile_info->tmu_date = file_info64.tmu_date,

        pfile_info->compressed_size = (uLong)file_info64.compressed_size;
        pfile_info->uncompressed_size = (uLong)file_info64.uncompressed_size;

    }
    return err;
}

extern int ZEXPORT unzGetCurrentFileInfo64(unzFile file, unz_file_info64 *pfile_info, char *filename,
                                           uLong filename_size, void *extrafield, uLong extrafield_size, char *comment, uLong comment_size)
{
    return unz64local_GetCurrentFileInfoInternal(file, pfile_info, NULL, filename, filename_size,
                                                 extrafield, extrafield_size, comment, comment_size);
}

/* Read the local header of the current zipfile. Check the coherency of the local header and info in the
   end of central directory about this file store in *piSizeVar the size of extra info in local header
   (filename and size of extra field data) */
local int unz64local_CheckCurrentFileCoherencyHeader(unz64_s *s, uInt *piSizeVar, ZPOS64_T *poffset_local_extrafield,
                                                     uInt *psize_local_extrafield)
{
    uLong uMagic, uL, uFlags;
    uLong size_filename;
    uLong size_extra_field;
    int err = UNZ_OK;
    int compression_method = 0;

    *piSizeVar = 0;
    *poffset_local_extrafield = 0;
    *psize_local_extrafield = 0;

    err = unzGoToNextDisk((unzFile)s);
    if (err != UNZ_OK)
        return err;

    if (ZSEEK64(s->z_filefunc, s->filestream, s->cur_file_info_internal.offset_curfile +
                s->cur_file_info_internal.byte_before_the_zipfile, ZLIB_FILEFUNC_SEEK_SET) != 0)
        return UNZ_ERRNO;

    if (err == UNZ_OK) {
        if (unz64local_getLong(&s->z_filefunc, s->filestream, &uMagic) != UNZ_OK)
            err = UNZ_ERRNO;
        else if (uMagic != LOCALHEADERMAGIC)
            err = UNZ_BADZIPFILE;
    }

    if (unz64local_getShort(&s->z_filefunc, s->filestream, &uL) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream, &uFlags) != UNZ_OK)
        err = UNZ_ERRNO;
    if (unz64local_getShort(&s->z_filefunc, s->filestream, &uL) != UNZ_OK)
        err = UNZ_ERRNO;
    else if ((err == UNZ_OK) && (uL != s->cur_file_info.compression_method))
        err = UNZ_BADZIPFILE;

    compression_method = (int)s->cur_file_info.compression_method;
#ifdef HAVE_AES
    if (compression_method == AES_METHOD)
        compression_method = (int)s->cur_file_info_internal.aes_compression_method;
#endif

    if ((err == UNZ_OK) && (compression_method != 0) &&
#ifdef HAVE_BZIP2
        (compression_method != Z_BZIP2ED) &&
#endif
        (compression_method != Z_DEFLATED))
        err = UNZ_BADZIPFILE;

    if (unz64local_getLong(&s->z_filefunc, s->filestream, &uL) != UNZ_OK) /* date/time */
        err = UNZ_ERRNO;
    if (unz64local_getLong(&s->z_filefunc, s->filestream, &uL) != UNZ_OK) /* crc */
        err = UNZ_ERRNO;
    else if ((err == UNZ_OK) && (uL != s->cur_file_info.crc) && ((uFlags & 8) == 0))
        err = UNZ_BADZIPFILE;
    if (unz64local_getLong(&s->z_filefunc, s->filestream, &uL) != UNZ_OK) /* size compr */
        err = UNZ_ERRNO;
    else if ((uL != 0xffffffff) && (err == UNZ_OK) && (uL != s->cur_file_info.compressed_size) && ((uFlags & 8) == 0))
        err = UNZ_BADZIPFILE;
    if (unz64local_getLong(&s->z_filefunc, s->filestream, &uL) != UNZ_OK) /* size uncompr */
        err = UNZ_ERRNO;
    else if ((uL != 0xffffffff) && (err == UNZ_OK) && (uL != s->cur_file_info.uncompressed_size) && ((uFlags & 8) == 0))
        err = UNZ_BADZIPFILE;
    if (unz64local_getShort(&s->z_filefunc, s->filestream, &size_filename) != UNZ_OK)
        err = UNZ_ERRNO;
    else if ((err == UNZ_OK) && (size_filename != s->cur_file_info.size_filename))
        err = UNZ_BADZIPFILE;

    *piSizeVar += (uInt)size_filename;

    if (unz64local_getShort(&s->z_filefunc, s->filestream, &size_extra_field) != UNZ_OK)
        err = UNZ_ERRNO;
    *poffset_local_extrafield = s->cur_file_info_internal.offset_curfile + SIZEZIPLOCALHEADER + size_filename;
    *psize_local_extrafield = (uInt)size_extra_field;

    *piSizeVar += (uInt)size_extra_field;

    return err;
}

/*
   Open for reading data the current file in the zipfile.
   If there is no error and the file is opened, the return value is UNZ_OK.
 */
extern int ZEXPORT unzOpenCurrentFile3(unzFile file, int *method, int *level, int raw, const char *password)
{
    int err = UNZ_OK;
    int compression_method;
    uInt iSizeVar;
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    ZPOS64_T offset_local_extrafield;
    uInt size_local_extrafield;
#ifndef NOUNCRYPT
    char source[12];
#else
    if (password != NULL)
        return UNZ_PARAMERROR;
#endif
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    if (!s->current_file_ok)
        return UNZ_PARAMERROR;

    if (s->pfile_in_zip_read != NULL)
        unzCloseCurrentFile(file);

    if (unz64local_CheckCurrentFileCoherencyHeader(s, &iSizeVar, &offset_local_extrafield, &size_local_extrafield) != UNZ_OK)
        return UNZ_BADZIPFILE;

    pfile_in_zip_read_info = (file_in_zip64_read_info_s *)ALLOC(sizeof(file_in_zip64_read_info_s));
    if (pfile_in_zip_read_info == NULL)
        return UNZ_INTERNALERROR;

    pfile_in_zip_read_info->read_buffer = (Bytef *)ALLOC(UNZ_BUFSIZE);
    pfile_in_zip_read_info->offset_local_extrafield = offset_local_extrafield;
    pfile_in_zip_read_info->size_local_extrafield = size_local_extrafield;
    pfile_in_zip_read_info->pos_local_extrafield = 0;
    pfile_in_zip_read_info->raw = raw;

    if (pfile_in_zip_read_info->read_buffer == NULL) {
        TRYFREE(pfile_in_zip_read_info);
        return UNZ_INTERNALERROR;
    }

    pfile_in_zip_read_info->stream_initialised = 0;

    compression_method = (int)s->cur_file_info.compression_method;
#ifdef HAVE_AES
    if (compression_method == AES_METHOD)
        compression_method = (int)s->cur_file_info_internal.aes_compression_method;
#endif

    if (method != NULL)
        *method = compression_method;

    if (level != NULL) {
        *level = 6;
        switch (s->cur_file_info.flag & 0x06) {
        case 6: *level = 1; break;
        case 4: *level = 2; break;
        case 2: *level = 9; break;
        }
    }

    if ((compression_method != 0) &&
#ifdef HAVE_BZIP2
        (compression_method != Z_BZIP2ED) &&
#endif
        (compression_method != Z_DEFLATED))
        err = UNZ_BADZIPFILE;

    pfile_in_zip_read_info->crc32_wait = s->cur_file_info.crc;
    pfile_in_zip_read_info->crc32 = 0;
    pfile_in_zip_read_info->total_out_64 = 0;
    pfile_in_zip_read_info->compression_method = compression_method;
    pfile_in_zip_read_info->filestream = s->filestream;
    pfile_in_zip_read_info->z_filefunc = s->z_filefunc;
    if (s->number_disk == s->gi.number_disk_with_CD)
        pfile_in_zip_read_info->byte_before_the_zipfile = s->byte_before_the_zipfile;
    else
        pfile_in_zip_read_info->byte_before_the_zipfile = 0;
    pfile_in_zip_read_info->stream.total_out = 0;
    pfile_in_zip_read_info->stream.total_in = 0;
    pfile_in_zip_read_info->stream.next_in = NULL;

    if (!raw) {
        if (compression_method == Z_BZIP2ED) {
#ifdef HAVE_BZIP2
            pfile_in_zip_read_info->bstream.bzalloc = (void *(*)(void *, int, int)) 0;
            pfile_in_zip_read_info->bstream.bzfree = (free_func)0;
            pfile_in_zip_read_info->bstream.opaque = (voidpf)0;
            pfile_in_zip_read_info->bstream.state = (voidpf)0;

            pfile_in_zip_read_info->stream.zalloc = (alloc_func)0;
            pfile_in_zip_read_info->stream.zfree = (free_func)0;
            pfile_in_zip_read_info->stream.opaque = (voidpf)0;
            pfile_in_zip_read_info->stream.next_in = (voidpf)0;
            pfile_in_zip_read_info->stream.avail_in = 0;

            err = BZ2_bzDecompressInit(&pfile_in_zip_read_info->bstream, 0, 0);
            if (err == Z_OK)
                pfile_in_zip_read_info->stream_initialised = Z_BZIP2ED;
            else {
                TRYFREE(pfile_in_zip_read_info);
                return err;
            }
#else
            pfile_in_zip_read_info->raw = 1;
#endif
        } else if (compression_method == Z_DEFLATED) {
            pfile_in_zip_read_info->stream.zalloc = (alloc_func)0;
            pfile_in_zip_read_info->stream.zfree = (free_func)0;
            pfile_in_zip_read_info->stream.opaque = (voidpf)s;
            pfile_in_zip_read_info->stream.next_in = 0;
            pfile_in_zip_read_info->stream.avail_in = 0;

            err = inflateInit2(&pfile_in_zip_read_info->stream, -MAX_WBITS);
            if (err == Z_OK)
                pfile_in_zip_read_info->stream_initialised = Z_DEFLATED;
            else {
                TRYFREE(pfile_in_zip_read_info);
                return err;
            }
            /* windowBits is passed < 0 to tell that there is no zlib header.
             * Note that in this case inflate *requires* an extra "dummy" byte
             * after the compressed stream in order to complete decompression and
             * return Z_STREAM_END.
             * In unzip, i don't wait absolutely Z_STREAM_END because I known the
             * size of both compressed and uncompressed data
             */
        }
    }

    pfile_in_zip_read_info->rest_read_compressed = s->cur_file_info.compressed_size;
    pfile_in_zip_read_info->rest_read_uncompressed = s->cur_file_info.uncompressed_size;
    pfile_in_zip_read_info->pos_in_zipfile = s->cur_file_info_internal.offset_curfile + SIZEZIPLOCALHEADER + iSizeVar;
    pfile_in_zip_read_info->stream.avail_in = (uInt)0;

    s->pfile_in_zip_read = pfile_in_zip_read_info;

#ifndef NOUNCRYPT
    if ((password != NULL) && ((s->cur_file_info.flag & 1) != 0)) {
        if (ZSEEK64(s->z_filefunc, s->filestream,
                    s->pfile_in_zip_read->pos_in_zipfile + s->pfile_in_zip_read->byte_before_the_zipfile,
                    ZLIB_FILEFUNC_SEEK_SET) != 0)
            return UNZ_INTERNALERROR;
#ifdef HAVE_AES
        if (s->cur_file_info.compression_method == AES_METHOD) {
            unsigned char passverify[AES_PWVERIFYSIZE];
            unsigned char saltvalue[AES_MAXSALTLENGTH];
            uInt saltlength;

            if ((s->cur_file_info_internal.aes_encryption_mode < 1) ||
                (s->cur_file_info_internal.aes_encryption_mode > 3))
                return UNZ_INTERNALERROR;

            saltlength = SALT_LENGTH(s->cur_file_info_internal.aes_encryption_mode);

            if (ZREAD64(s->z_filefunc, s->filestream, saltvalue, saltlength) != saltlength)
                return UNZ_INTERNALERROR;
            if (ZREAD64(s->z_filefunc, s->filestream, passverify, AES_PWVERIFYSIZE) != AES_PWVERIFYSIZE)
                return UNZ_INTERNALERROR;

            fcrypt_init((int)s->cur_file_info_internal.aes_encryption_mode, (unsigned char *)password, (unsigned int)strlen(password), saltvalue,
                        passverify, &s->pfile_in_zip_read->aes_ctx);

            pfile_in_zip_read_info->rest_read_compressed -= saltlength + AES_PWVERIFYSIZE;
            pfile_in_zip_read_info->rest_read_compressed -= AES_AUTHCODESIZE;

            s->pfile_in_zip_read->pos_in_zipfile += saltlength + AES_PWVERIFYSIZE;
        } else
#endif
        {
            int i;
            s->pcrc_32_tab = (const unsigned long *)get_crc_table();
            init_keys(password, s->keys, s->pcrc_32_tab);

            if (ZREAD64(s->z_filefunc, s->filestream, source, 12) < 12)
                return UNZ_INTERNALERROR;

            for (i = 0; i < 12; i++)
                zdecode(s->keys, s->pcrc_32_tab, source[i]);

            pfile_in_zip_read_info->rest_read_compressed -= 12;

            s->pfile_in_zip_read->pos_in_zipfile += 12;
        }
    }
#endif

    return UNZ_OK;
}

extern int ZEXPORT unzOpenCurrentFile(unzFile file)
{
    return unzOpenCurrentFile3(file, NULL, NULL, 0, NULL);
}

extern int ZEXPORT unzOpenCurrentFilePassword(unzFile file, const char *password)
{
    return unzOpenCurrentFile3(file, NULL, NULL, 0, password);
}

extern int ZEXPORT unzOpenCurrentFile2(unzFile file, int *method, int *level, int raw)
{
    return unzOpenCurrentFile3(file, method, level, raw, NULL);
}

/* Read bytes from the current file.
   buf contain buffer where data must be copied
   len the size of buf.

   return the number of byte copied if some bytes are copied
   return 0 if the end of file was reached
   return <0 with error code if there is an error (UNZ_ERRNO for IO error, or zLib error for uncompress error) */
extern int ZEXPORT unzReadCurrentFile(unzFile file, voidp buf, unsigned len)
{
    int err = UNZ_OK;
    uInt read = 0;
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;

    if (pfile_in_zip_read_info == NULL)
        return UNZ_PARAMERROR;
    if (pfile_in_zip_read_info->read_buffer == NULL)
        return UNZ_END_OF_LIST_OF_FILE;
    if (len == 0)
        return 0;

    pfile_in_zip_read_info->stream.next_out = (Bytef *)buf;
    pfile_in_zip_read_info->stream.avail_out = (uInt)len;

    if (pfile_in_zip_read_info->raw) {
        if (len > pfile_in_zip_read_info->rest_read_compressed + pfile_in_zip_read_info->stream.avail_in)
            pfile_in_zip_read_info->stream.avail_out = (uInt)pfile_in_zip_read_info->rest_read_compressed +
                                                       pfile_in_zip_read_info->stream.avail_in;
    } else {
        
        // NOTE:
        // This bit of code seems to try to set the amount of space in the output buffer based on the
        // value stored in the headers stored in the .zip file. However, if those values are incorrect
        // it may result in a loss of data when uncompresssing that file. The compressed data is still
        // legit and will deflate without knowing the uncompressed code so this tidbit is unnecessary and
        // may cause issues for some .zip files.
        //
        // It's removed in here to fix those issues.
        //
        // See: https://github.com/ZipArchive/ziparchive/issues/16
        //
        
        /*
        
         
         FIXME: Upgrading to minizip 1.1 caused issues here, Uncommented the code that was commented before. 11/24/2015
         */
        
        if (len > pfile_in_zip_read_info->rest_read_uncompressed)
            pfile_in_zip_read_info->stream.avail_out = (uInt)pfile_in_zip_read_info->rest_read_uncompressed;
        
         
    
    }

    while (pfile_in_zip_read_info->stream.avail_out > 0) {
        if (pfile_in_zip_read_info->stream.avail_in == 0) {
            uInt bytes_to_read = UNZ_BUFSIZE;
            uInt bytes_not_read = 0;
            uInt bytes_read = 0;
            uInt total_bytes_read = 0;

            if (pfile_in_zip_read_info->stream.next_in != NULL)
                bytes_not_read = (uInt)(pfile_in_zip_read_info->read_buffer + UNZ_BUFSIZE -
                                        pfile_in_zip_read_info->stream.next_in);
            bytes_to_read -= bytes_not_read;
            if (bytes_not_read > 0)
                memcpy(pfile_in_zip_read_info->read_buffer, pfile_in_zip_read_info->stream.next_in, bytes_not_read);
            if (pfile_in_zip_read_info->rest_read_compressed < bytes_to_read)
                bytes_to_read = (uInt)pfile_in_zip_read_info->rest_read_compressed;

            while (total_bytes_read != bytes_to_read) {
                if (ZSEEK64(pfile_in_zip_read_info->z_filefunc, pfile_in_zip_read_info->filestream,
                            pfile_in_zip_read_info->pos_in_zipfile + pfile_in_zip_read_info->byte_before_the_zipfile,
                            ZLIB_FILEFUNC_SEEK_SET) != 0)
                    return UNZ_ERRNO;

                bytes_read = (int)ZREAD64(pfile_in_zip_read_info->z_filefunc, pfile_in_zip_read_info->filestream,
                                          pfile_in_zip_read_info->read_buffer + bytes_not_read + total_bytes_read,
                                          bytes_to_read - total_bytes_read);

                total_bytes_read += bytes_read;
                pfile_in_zip_read_info->pos_in_zipfile += bytes_read;

                if (bytes_read == 0) {
                    if (ZERROR64(pfile_in_zip_read_info->z_filefunc, pfile_in_zip_read_info->filestream))
                        return UNZ_ERRNO;

                    err = unzGoToNextDisk(file);
                    if (err != UNZ_OK)
                        return err;

                    pfile_in_zip_read_info->pos_in_zipfile = 0;
                    pfile_in_zip_read_info->filestream = s->filestream;
                }
            }

#ifndef NOUNCRYPT
            if ((s->cur_file_info.flag & 1) != 0) {
#ifdef HAVE_AES
                if (s->cur_file_info.compression_method == AES_METHOD) {
                    fcrypt_decrypt(pfile_in_zip_read_info->read_buffer, bytes_to_read, &s->pfile_in_zip_read->aes_ctx);
                } else
#endif
                {
                    uInt i;
                    for (i = 0; i < total_bytes_read; i++)
                        pfile_in_zip_read_info->read_buffer[i] =
                            zdecode(s->keys, s->pcrc_32_tab, pfile_in_zip_read_info->read_buffer[i]);
                }
            }
#endif

            pfile_in_zip_read_info->rest_read_compressed -= total_bytes_read;
            pfile_in_zip_read_info->stream.next_in = (Bytef *)pfile_in_zip_read_info->read_buffer;
            pfile_in_zip_read_info->stream.avail_in = (uInt)bytes_not_read + total_bytes_read;
        }

        if ((pfile_in_zip_read_info->compression_method == 0) || (pfile_in_zip_read_info->raw)) {
            uInt copy, i;

            if ((pfile_in_zip_read_info->stream.avail_in == 0) &&
                (pfile_in_zip_read_info->rest_read_compressed == 0))
                return (read == 0) ? UNZ_EOF : read;

            if (pfile_in_zip_read_info->stream.avail_out < pfile_in_zip_read_info->stream.avail_in)
                copy = pfile_in_zip_read_info->stream.avail_out;
            else
                copy = pfile_in_zip_read_info->stream.avail_in;

            for (i = 0; i < copy; i++)
                *(pfile_in_zip_read_info->stream.next_out + i) =
                    *(pfile_in_zip_read_info->stream.next_in + i);

            pfile_in_zip_read_info->total_out_64 = pfile_in_zip_read_info->total_out_64 + copy;
            pfile_in_zip_read_info->rest_read_uncompressed -= copy;
            pfile_in_zip_read_info->crc32 = crc32(pfile_in_zip_read_info->crc32,
                                                  pfile_in_zip_read_info->stream.next_out, copy);

            pfile_in_zip_read_info->stream.avail_in -= copy;
            pfile_in_zip_read_info->stream.avail_out -= copy;
            pfile_in_zip_read_info->stream.next_out += copy;
            pfile_in_zip_read_info->stream.next_in += copy;
            pfile_in_zip_read_info->stream.total_out += copy;
            read += copy;
        } else if (pfile_in_zip_read_info->compression_method == Z_BZIP2ED) {
#ifdef HAVE_BZIP2
            uLong total_out_before, total_out_after;
            const Bytef *buf_before;
            uLong out_bytes;

            pfile_in_zip_read_info->bstream.next_in = (char *)pfile_in_zip_read_info->stream.next_in;
            pfile_in_zip_read_info->bstream.avail_in = pfile_in_zip_read_info->stream.avail_in;
            pfile_in_zip_read_info->bstream.total_in_lo32 = pfile_in_zip_read_info->stream.total_in;
            pfile_in_zip_read_info->bstream.total_in_hi32 = 0;
            pfile_in_zip_read_info->bstream.next_out = (char *)pfile_in_zip_read_info->stream.next_out;
            pfile_in_zip_read_info->bstream.avail_out = pfile_in_zip_read_info->stream.avail_out;
            pfile_in_zip_read_info->bstream.total_out_lo32 = pfile_in_zip_read_info->stream.total_out;
            pfile_in_zip_read_info->bstream.total_out_hi32 = 0;

            total_out_before = pfile_in_zip_read_info->bstream.total_out_lo32;
            buf_before = (const Bytef *)pfile_in_zip_read_info->bstream.next_out;

            err = BZ2_bzDecompress(&pfile_in_zip_read_info->bstream);

            total_out_after = pfile_in_zip_read_info->bstream.total_out_lo32;
            out_bytes = total_out_after - total_out_before;

            pfile_in_zip_read_info->total_out_64 = pfile_in_zip_read_info->total_out_64 + out_bytes;
            pfile_in_zip_read_info->rest_read_uncompressed -= out_bytes;
            pfile_in_zip_read_info->crc32 = crc32(pfile_in_zip_read_info->crc32, buf_before, (uInt)(out_bytes));

            read += (uInt)(total_out_after - total_out_before);

            pfile_in_zip_read_info->stream.next_in = (Bytef *)pfile_in_zip_read_info->bstream.next_in;
            pfile_in_zip_read_info->stream.avail_in = pfile_in_zip_read_info->bstream.avail_in;
            pfile_in_zip_read_info->stream.total_in = pfile_in_zip_read_info->bstream.total_in_lo32;
            pfile_in_zip_read_info->stream.next_out = (Bytef *)pfile_in_zip_read_info->bstream.next_out;
            pfile_in_zip_read_info->stream.avail_out = pfile_in_zip_read_info->bstream.avail_out;
            pfile_in_zip_read_info->stream.total_out = pfile_in_zip_read_info->bstream.total_out_lo32;

            if (err == BZ_STREAM_END)
                return (read == 0) ? UNZ_EOF : read;
            if (err != BZ_OK)
                break;
#endif
        } else {
            ZPOS64_T total_out_before, total_out_after;
            const Bytef *buf_before;
            ZPOS64_T out_bytes;
            int flush = Z_SYNC_FLUSH;

            total_out_before = pfile_in_zip_read_info->stream.total_out;
            buf_before = pfile_in_zip_read_info->stream.next_out;

            /*
               if ((pfile_in_zip_read_info->rest_read_uncompressed ==
                     pfile_in_zip_read_info->stream.avail_out) &&
                (pfile_in_zip_read_info->rest_read_compressed == 0))
                flush = Z_FINISH;
             */
            err = inflate(&pfile_in_zip_read_info->stream, flush);

            if ((err >= 0) && (pfile_in_zip_read_info->stream.msg != NULL))
                err = Z_DATA_ERROR;

            total_out_after = pfile_in_zip_read_info->stream.total_out;
            out_bytes = total_out_after - total_out_before;

            pfile_in_zip_read_info->total_out_64 += out_bytes;
            pfile_in_zip_read_info->rest_read_uncompressed -= out_bytes;
            pfile_in_zip_read_info->crc32 =
                crc32(pfile_in_zip_read_info->crc32, buf_before, (uInt)(out_bytes));

            read += (uInt)(total_out_after - total_out_before);

            if (err == Z_STREAM_END)
                return (read == 0) ? UNZ_EOF : read;
            if (err != Z_OK)
                break;
        }
    }

    if (err == Z_OK)
        return read;
    return err;
}

extern ZPOS64_T ZEXPORT unzGetCurrentFileZStreamPos64(unzFile file)
{
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    s = (unz64_s *)file;
    if (file == NULL)
        return 0; /* UNZ_PARAMERROR */
    pfile_in_zip_read_info = s->pfile_in_zip_read;
    if (pfile_in_zip_read_info == NULL)
        return 0; /* UNZ_PARAMERROR */
    return pfile_in_zip_read_info->pos_in_zipfile + pfile_in_zip_read_info->byte_before_the_zipfile;
}

extern int ZEXPORT unzGetLocalExtrafield(unzFile file, voidp buf, unsigned len)
{
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    uInt read_now;
    ZPOS64_T size_to_read;

    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;

    if (pfile_in_zip_read_info == NULL)
        return UNZ_PARAMERROR;

    size_to_read = pfile_in_zip_read_info->size_local_extrafield - pfile_in_zip_read_info->pos_local_extrafield;

    if (buf == NULL)
        return (int)size_to_read;

    if (len > size_to_read)
        read_now = (uInt)size_to_read;
    else
        read_now = (uInt)len;

    if (read_now == 0)
        return 0;

    if (ZSEEK64(pfile_in_zip_read_info->z_filefunc, pfile_in_zip_read_info->filestream,
                pfile_in_zip_read_info->offset_local_extrafield + pfile_in_zip_read_info->pos_local_extrafield,
                ZLIB_FILEFUNC_SEEK_SET) != 0)
        return UNZ_ERRNO;

    if (ZREAD64(pfile_in_zip_read_info->z_filefunc, pfile_in_zip_read_info->filestream, buf, read_now) != read_now)
        return UNZ_ERRNO;

    return (int)read_now;
}

extern int ZEXPORT unzCloseCurrentFile(unzFile file)
{
    int err = UNZ_OK;

    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;

    if (pfile_in_zip_read_info == NULL)
        return UNZ_PARAMERROR;

#ifdef HAVE_AES
    if (s->cur_file_info.compression_method == AES_METHOD) {
        unsigned char authcode[AES_AUTHCODESIZE];
        unsigned char rauthcode[AES_AUTHCODESIZE];

        if (ZREAD64(s->z_filefunc, s->filestream, authcode, AES_AUTHCODESIZE) != AES_AUTHCODESIZE)
            return UNZ_ERRNO;

        if (fcrypt_end(rauthcode, &s->pfile_in_zip_read->aes_ctx) != AES_AUTHCODESIZE)
            err = UNZ_CRCERROR;
        if (memcmp(authcode, rauthcode, AES_AUTHCODESIZE) != 0)
            err = UNZ_CRCERROR;
    }
    /* AES zip version AE-1 will expect a valid crc as well */
    if ((s->cur_file_info.compression_method != AES_METHOD) ||
        (s->cur_file_info_internal.aes_version == 0x0001))
#endif
    {
        if ((pfile_in_zip_read_info->rest_read_uncompressed == 0) &&
            (!pfile_in_zip_read_info->raw)) {
            if (pfile_in_zip_read_info->crc32 != pfile_in_zip_read_info->crc32_wait)
                err = UNZ_CRCERROR;
        }
    }

    TRYFREE(pfile_in_zip_read_info->read_buffer);
    pfile_in_zip_read_info->read_buffer = NULL;
    if (pfile_in_zip_read_info->stream_initialised == Z_DEFLATED)
        inflateEnd(&pfile_in_zip_read_info->stream);
#ifdef HAVE_BZIP2
    else if (pfile_in_zip_read_info->stream_initialised == Z_BZIP2ED)
        BZ2_bzDecompressEnd(&pfile_in_zip_read_info->bstream);
#endif

    pfile_in_zip_read_info->stream_initialised = 0;
    TRYFREE(pfile_in_zip_read_info);

    s->pfile_in_zip_read = NULL;

    return err;
}

extern int ZEXPORT unzGoToFirstFile2(unzFile file, unz_file_info64 *pfile_info, char *filename,
                                     uLong filename_size, void *extrafield, uLong extrafield_size, char *comment, uLong comment_size)
{
    int err = UNZ_OK;
    unz64_s *s;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    s->pos_in_central_dir = s->offset_central_dir;
    s->num_file = 0;
    err = unz64local_GetCurrentFileInfoInternal(file, &s->cur_file_info, &s->cur_file_info_internal,
                                                filename, filename_size, extrafield, extrafield_size, comment, comment_size);
    s->current_file_ok = (err == UNZ_OK);
    if ((err == UNZ_OK) && (pfile_info != NULL))
        memcpy(pfile_info, &s->cur_file_info, sizeof(unz_file_info64));
    return err;
}

extern int ZEXPORT unzGoToFirstFile(unzFile file)
{
    return unzGoToFirstFile2(file, NULL, NULL, 0, NULL, 0, NULL, 0);
}

extern int ZEXPORT unzGoToNextFile2(unzFile file, unz_file_info64 *pfile_info, char *filename,
                                    uLong filename_size, void *extrafield, uLong extrafield_size, char *comment, uLong comment_size)
{
    unz64_s *s;
    int err;

    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    if (!s->current_file_ok)
        return UNZ_END_OF_LIST_OF_FILE;
    if (s->gi.number_entry != 0xffff)    /* 2^16 files overflow hack */
        if (s->num_file + 1 == s->gi.number_entry)
            return UNZ_END_OF_LIST_OF_FILE;
    s->pos_in_central_dir += SIZECENTRALDIRITEM + s->cur_file_info.size_filename +
                             s->cur_file_info.size_file_extra + s->cur_file_info.size_file_comment;
    s->num_file++;
    err = unz64local_GetCurrentFileInfoInternal(file, &s->cur_file_info, &s->cur_file_info_internal,
                                                filename, filename_size, extrafield, extrafield_size, comment, comment_size);
    s->current_file_ok = (err == UNZ_OK);
    if ((err == UNZ_OK) && (pfile_info != NULL))
        memcpy(pfile_info, &s->cur_file_info, sizeof(unz_file_info64));
    return err;
}

extern int ZEXPORT unzGoToNextFile(unzFile file)
{
    return unzGoToNextFile2(file, NULL, NULL, 0, NULL, 0, NULL, 0);
}

extern int ZEXPORT unzLocateFile(unzFile file, const char *filename, unzFileNameComparer filename_compare_func)
{
    unz64_s *s;
    int err;
    unz_file_info64 cur_file_info_saved;
    unz_file_info64_internal cur_file_info_internal_saved;
    ZPOS64_T num_file_saved;
    ZPOS64_T pos_in_central_dir_saved;
    char current_filename[UNZ_MAXFILENAMEINZIP + 1];

    if (file == NULL)
        return UNZ_PARAMERROR;
    if (strlen(filename) >= UNZ_MAXFILENAMEINZIP)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    if (!s->current_file_ok)
        return UNZ_END_OF_LIST_OF_FILE;

    /* Save the current state */
    num_file_saved = s->num_file;
    pos_in_central_dir_saved = s->pos_in_central_dir;
    cur_file_info_saved = s->cur_file_info;
    cur_file_info_internal_saved = s->cur_file_info_internal;

    err = unzGoToFirstFile2(file, NULL, current_filename, sizeof(current_filename) - 1, NULL, 0, NULL, 0);

    while (err == UNZ_OK) {
        if (filename_compare_func != NULL)
            err = filename_compare_func(file, current_filename, filename);
        else
            err = strcmp(current_filename, filename);
        if (err == 0)
            return UNZ_OK;
        err = unzGoToNextFile2(file, NULL, current_filename, sizeof(current_filename) - 1, NULL, 0, NULL, 0);
    }

    /* We failed, so restore the state of the 'current file' to where we were. */
    s->num_file = num_file_saved;
    s->pos_in_central_dir = pos_in_central_dir_saved;
    s->cur_file_info = cur_file_info_saved;
    s->cur_file_info_internal = cur_file_info_internal_saved;
    return err;
}

extern int ZEXPORT unzGetFilePos(unzFile file, unz_file_pos *file_pos)
{
    unz64_file_pos file_pos64;
    int err = unzGetFilePos64(file, &file_pos64);
    if (err == UNZ_OK) {
        file_pos->pos_in_zip_directory = (uLong)file_pos64.pos_in_zip_directory;
        file_pos->num_of_file = (uLong)file_pos64.num_of_file;
    }
    return err;
}

extern int ZEXPORT unzGoToFilePos(unzFile file, unz_file_pos *file_pos)
{
    unz64_file_pos file_pos64;

    if (file_pos == NULL)
        return UNZ_PARAMERROR;
    file_pos64.pos_in_zip_directory = file_pos->pos_in_zip_directory;
    file_pos64.num_of_file = file_pos->num_of_file;
    return unzGoToFilePos64(file, &file_pos64);
}

extern int ZEXPORT unzGetFilePos64(unzFile file, unz64_file_pos *file_pos)
{
    unz64_s *s;

    if (file == NULL || file_pos == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    if (!s->current_file_ok)
        return UNZ_END_OF_LIST_OF_FILE;

    file_pos->pos_in_zip_directory = s->pos_in_central_dir;
    file_pos->num_of_file = s->num_file;

    return UNZ_OK;
}

extern int ZEXPORT unzGoToFilePos64(unzFile file, const unz64_file_pos *file_pos)
{
    unz64_s *s;
    int err;

    if (file == NULL || file_pos == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;

    /* jump to the right spot */
    s->pos_in_central_dir = file_pos->pos_in_zip_directory;
    s->num_file = file_pos->num_of_file;

    /* set the current file */
    err = unz64local_GetCurrentFileInfoInternal(file, &s->cur_file_info, &s->cur_file_info_internal, NULL, 0, NULL, 0, NULL, 0);
    /* return results */
    s->current_file_ok = (err == UNZ_OK);
    return err;
}

extern uLong ZEXPORT unzGetOffset(unzFile file)
{
    ZPOS64_T offset64;

    if (file == NULL)
        return 0; /* UNZ_PARAMERROR; */
    offset64 = unzGetOffset64(file);
    return (uLong)offset64;
}

extern ZPOS64_T ZEXPORT unzGetOffset64(unzFile file)
{
    unz64_s *s;

    if (file == NULL)
        return 0; /* UNZ_PARAMERROR; */
    s = (unz64_s *)file;
    if (!s->current_file_ok)
        return 0;
    if (s->gi.number_entry != 0 && s->gi.number_entry != 0xffff)
        if (s->num_file == s->gi.number_entry)
            return 0;
    return s->pos_in_central_dir;
}

extern int ZEXPORT unzSetOffset(unzFile file, uLong pos)
{
    return unzSetOffset64(file, pos);
}

extern int ZEXPORT unzSetOffset64(unzFile file, ZPOS64_T pos)
{
    unz64_s *s;
    int err;

    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;

    s->pos_in_central_dir = pos;
    s->num_file = s->gi.number_entry;      /* hack */
    err = unz64local_GetCurrentFileInfoInternal(file, &s->cur_file_info, &s->cur_file_info_internal, NULL, 0, NULL, 0, NULL, 0);
    s->current_file_ok = (err == UNZ_OK);
    return err;
}

extern z_off_t ZEXPORT unztell(unzFile file)
{
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;
    if (pfile_in_zip_read_info == NULL)
        return UNZ_PARAMERROR;
    return (z_off_t)pfile_in_zip_read_info->stream.total_out;
}

extern ZPOS64_T ZEXPORT unztell64(unzFile file)
{

    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    if (file == NULL)
        return (ZPOS64_T)-1;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;

    if (pfile_in_zip_read_info == NULL)
        return (ZPOS64_T)-1;

    return pfile_in_zip_read_info->total_out_64;
}

extern int ZEXPORT unzeof(unzFile file)
{
    unz64_s *s;
    file_in_zip64_read_info_s *pfile_in_zip_read_info;
    if (file == NULL)
        return UNZ_PARAMERROR;
    s = (unz64_s *)file;
    pfile_in_zip_read_info = s->pfile_in_zip_read;

    if (pfile_in_zip_read_info == NULL)
        return UNZ_PARAMERROR;

    if (pfile_in_zip_read_info->rest_read_uncompressed == 0)
        return 1;
    return 0;
}

