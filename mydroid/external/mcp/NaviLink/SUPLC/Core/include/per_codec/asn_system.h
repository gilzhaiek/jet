/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


#ifndef	_ASN_SYSTEM_H_
#define	_ASN_SYSTEM_H_

#ifdef	HAVE_CONFIG_H
#include "config.h"
#endif

#if defined(_WIN32) || defined(WINCE)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#endif
#include <stdio.h>	/* For snprintf(3) */
#include <stdlib.h>	/* For *alloc(3) */
#include <string.h>	/* For memcpy(3) */
//#include <sys/types.h>	/* For size_t */
#include <stdarg.h>	/* For va_start */
#include <stddef.h>	/* for offsetof and ptrdiff_t */

#ifndef LINUX
//typedef int ssize_t;
#endif

#ifdef WINCE
static int errno;
#endif
#define E_INVAL 0xFF
#define E_RANGE 0xF0
#define E_PERM  0x0F

// static int strerror(int a)
// {
// 	return 0;
// }

//#define bsearch b_search
#ifndef LINUX
static void *b_search(const void *key, const void *base,
			   size_t nmemb, size_t size,
			   int (/*_cdecl*/ *compar)(const void *, const void *))
#else
static void *b_search(const void *key, const void *base,
			   size_t nmemb, size_t size,
			   int (/*_cdecl*/ *compar)(const void *, const void *))
#endif   
{
	size_t odd_mask, bytes;
	const char *center, *high, *low;
	int comp;

	odd_mask = ((size ^ (size - 1)) >> 1) + 1;
	low = (const char*)base;
	bytes = nmemb == 0 ? size : size + 1;
	center = low + nmemb * size;
	comp = 0;
	while (bytes != size) {
		if (comp > 0) {
			low = center;
		} else {
			high = center;
		}
		bytes = high - low;
		center = low + ((bytes & odd_mask ? bytes - size : bytes) >> 1);
		comp = compar(key, center);
		if (comp == 0) {
			return (void *)center;
		}
	}
	return 0;
}

#if	defined(_WIN32) || defined(WINCE)

#include <malloc.h>
#include "stdint.h"
#define	 snprintf	_snprintf
#define	 vsnprintf	_vsnprintf

#ifdef _MSC_VER			/* MSVS.Net */
#ifndef __cplusplus
#ifndef inline
#define inline __inline
#endif
#endif
#define	ssize_t		SSIZE_T
typedef	char		int8_t;
typedef	short		int16_t;
typedef	int		int32_t;
typedef	unsigned char	uint8_t;
typedef	unsigned short	uint16_t;
typedef	unsigned int	uint32_t;
// #define WIN32_LEAN_AND_MEAN
// #include <windows.h>
#include <float.h>
#define isnan _isnan
#define finite _finite
#define copysign _copysign
#define	ilogb	_logb
#endif	/* _MSC_VER */

#else	/* !defined(WIN32) | definde(WINCE) */

#if defined(__vxworks)
#include <types/vxTypes.h>
#else	/* !defined(__vxworks) */

#include "inttypes.h"	/* C99 specifies this file */
/*
 * 1. Earlier FreeBSD version didn't have <stdint.h>,
 * but <inttypes.h> was present.
 * 2. Sun Solaris requires <alloca.h> for alloca(3),
 * but does not have <stdint.h>.
 */
#if	(!defined(__FreeBSD__) || !defined(_SYS_INTTYPES_H_))
#if	defined(sun)
#include <alloca.h>	/* For alloca(3) */
#include <ieeefp.h>	/* for finite(3) */
#elif	defined(__hpux)
#ifdef	__GNUC__
#include <alloca.h>	/* For alloca(3) */
#else	/* !__GNUC__ */
#define inline
#endif	/* __GNUC__ */
#else
#include "stdint.h"	/* SUSv2+ and C99 specify this file, for uintXX_t */
#endif	/* defined(sun) */
#endif

#endif	/* defined(__vxworks) */

#endif	/* defined(WIN32) | definde(WINCE) */

#if	__GNUC__ >= 3
#ifndef	GCC_PRINTFLIKE
#define	GCC_PRINTFLIKE(fmt,var)	__attribute__((format(printf,fmt,var)))
#endif
#else
#ifndef	GCC_PRINTFLIKE
#define	GCC_PRINTFLIKE(fmt,var)	/* nothing */
#endif
#endif

#ifndef	offsetof	/* If not defined by <stddef.h> */
#define	offsetof(s, m)	((ptrdiff_t)&(((s *)0)->m) - (ptrdiff_t)((s *)0))
#endif	/* offsetof */

#ifndef	MIN		/* Suitable for comparing primitive types (integers) */
#if defined(__GNUC__)
#define	MIN(a,b)	({ __typeof a _a = a; __typeof b _b = b;	\
	((_a)<(_b)?(_a):(_b)); })
#else	/* !__GNUC__ */
#define	MIN(a,b)	((a)<(b)?(a):(b))	/* Unsafe variant */
#endif /* __GNUC__ */
#endif	/* MIN */

#endif	/* _ASN_SYSTEM_H_ */
