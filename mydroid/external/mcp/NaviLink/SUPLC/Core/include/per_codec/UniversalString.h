/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#ifndef	_UniversalString_H_
#define	_UniversalString_H_

#include "OCTET_STRING.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef OCTET_STRING_t UniversalString_t;  /* Implemented via OCTET STRING */

extern asn_TYPE_descriptor_t asn_DEF_UniversalString;

asn_struct_print_f UniversalString_print;	/* Human-readable output */

#ifdef __cplusplus
}
#endif

#endif	/* _UniversalString_H_ */
