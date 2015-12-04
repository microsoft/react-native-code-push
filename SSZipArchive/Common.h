#ifndef SSZipCommon
#define SSZipCommon

/* tm_unz contain date/time info */
typedef struct tm_unz_s
{
    unsigned int tm_sec;            /* seconds after the minute - [0,59] */
    unsigned int tm_min;            /* minutes after the hour - [0,59] */
    unsigned int tm_hour;           /* hours since midnight - [0,23] */
    unsigned int tm_mday;           /* day of the month - [1,31] */
    unsigned int tm_mon;            /* months since January - [0,11] */
    unsigned int tm_year;           /* years - [1980..2044] */
} tm_unz;

typedef struct unz_file_info_s
{
    unsigned long version;              /* version made by                 2 bytes */
    unsigned long version_needed;       /* version needed to extract       2 bytes */
    unsigned long flag;                 /* general purpose bit flag        2 bytes */
    unsigned long compression_method;   /* compression method              2 bytes */
    unsigned long dosDate;              /* last mod file date in Dos fmt   4 bytes */
    unsigned long crc;                  /* crc-32                          4 bytes */
    unsigned long compressed_size;      /* compressed size                 4 bytes */
    unsigned long uncompressed_size;    /* uncompressed size               4 bytes */
    unsigned long size_filename;        /* filename length                 2 bytes */
    unsigned long size_file_extra;      /* extra field length              2 bytes */
    unsigned long size_file_comment;    /* file comment length             2 bytes */

    unsigned long disk_num_start;       /* disk number start               2 bytes */
    unsigned long internal_fa;          /* internal file attributes        2 bytes */
    unsigned long external_fa;          /* external file attributes        4 bytes */

    tm_unz tmu_date;
} unz_file_info;

/* unz_file_info contain information about a file in the zipfile */
typedef struct unz_file_info64_s
{
    unsigned long version;              /* version made by                 2 bytes */
    unsigned long version_needed;       /* version needed to extract       2 bytes */
    unsigned long flag;                 /* general purpose bit flag        2 bytes */
    unsigned long compression_method;   /* compression method              2 bytes */
    unsigned long dosDate;              /* last mod file date in Dos fmt   4 bytes */
    unsigned long crc;                  /* crc-32                          4 bytes */
    unsigned long long compressed_size;   /* compressed size                 8 bytes */
    unsigned long long uncompressed_size; /* uncompressed size               8 bytes */
    unsigned long size_filename;        /* filename length                 2 bytes */
    unsigned long size_file_extra;      /* extra field length              2 bytes */
    unsigned long size_file_comment;    /* file comment length             2 bytes */
    
    unsigned long disk_num_start;       /* disk number start               2 bytes */
    unsigned long internal_fa;          /* internal file attributes        2 bytes */
    unsigned long external_fa;          /* external file attributes        4 bytes */
    
    tm_unz tmu_date;
    unsigned long long disk_offset;
    unsigned long size_file_extra_internal;
} unz_file_info64;

typedef struct unz_global_info_s
{
    unsigned long number_entry;         /* total number of entries in
                                         the central dir on this disk */
    
    unsigned long number_disk_with_CD;  /* number the the disk with central dir, used for spanning ZIP*/

    
    unsigned long size_comment;         /* size of the global comment of the zipfile */
} unz_global_info;

typedef struct unz_global_info64
{
    unsigned long long number_entry;         /* total number of entries in
                                 the central dir on this disk */
    
     unsigned long number_disk_with_CD;  /* number the the disk with central dir, used for spanning ZIP*/
    
    unsigned long size_comment;         /* size of the global comment of the zipfile */
} unz_global_info64;

#endif