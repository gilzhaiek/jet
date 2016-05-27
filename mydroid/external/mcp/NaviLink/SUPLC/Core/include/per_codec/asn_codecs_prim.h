/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


#ifndef	ASN_CODECS_PRIM_H
#define	ASN_CODECS_PRIM_H

#include "asn_application.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct ASN__PRIMITIVE_TYPE_s {
	uint8_t *buf;	/* Buffer with consecutive primitive encoding bytes */
	int size;	/* Size of the buffer */
} ASN__PRIMITIVE_TYPE_t;	/* Do not use this type directly! */

asn_struct_free_f ASN__PRIMITIVE_TYPE_free;


#ifdef __cplusplus
}
#endif

#endif	/* ASN_CODECS_PRIM_H */
