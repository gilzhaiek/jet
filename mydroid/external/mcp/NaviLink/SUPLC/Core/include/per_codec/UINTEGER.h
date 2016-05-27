/*-
 * Copyright (c) 2003, 2005 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#ifndef	_UINTEGER_H_
#define	_UINTEGER_H_

#include "asn_application.h"
#include "asn_codecs_prim.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef ASN__PRIMITIVE_TYPE_t UINTEGER_t;

extern asn_TYPE_descriptor_t asn_DEF_UINTEGER;

/* Map with <tag> to integer value association */
typedef struct asn_UINTEGER_enum_map_s 
{
	unsigned long		 nat_value;	/* associated native integer value */
	size_t		 enum_len;	/* strlen("tag") */
	const char	*enum_name;	/* "tag" */
} asn_UINTEGER_enum_map_t;

/* This type describes an enumeration for INTEGER and ENUMERATED types */
typedef struct asn_UINTEGER_specifics_s {
	asn_UINTEGER_enum_map_t *value2enum;	/* N -> "tag"; sorted by N */
	unsigned int *enum2value;		/* "tag" => N; sorted by tag */
	int map_count;				/* Elements in either map */
	int extension;				/* This map is extensible */
	int strict_enumeration;			/* Enumeration set is fixed */
} asn_UINTEGER_specifics_t;

asn_struct_print_f UINTEGER_print;
per_type_decoder_f UINTEGER_decode_uper;
per_type_encoder_f UINTEGER_encode_uper;

/***********************************
 * Some handy conversion routines. *
 ***********************************/

/*
 * Returns 0 if it was possible to convert, -1 otherwise.
 * -1/EINVAL: Mandatory argument missing
 * -1/ERANGE: Value encoded is out of range for long representation
 * -1/ENOMEM: Memory allocation failed (in asn_long2INTEGER()).
 */
int asn_UINTEGER2ulong(const UINTEGER_t *i,unsigned long *l);
int asn_ulong2UINTEGER(UINTEGER_t *i,unsigned long l);
//int asn_longlong2UINTEGER(UINTEGER_t *st, long long value);

/*
 * Convert the integer value into the corresponding enumeration map entry.
 */
const asn_UINTEGER_enum_map_t *UINTEGER_map_value2enum(asn_UINTEGER_specifics_t *specs, unsigned long value);

#ifdef __cplusplus
}
#endif

#endif	/* _INTEGER_H_ */
