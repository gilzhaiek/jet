/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


#ifndef	_CONSTR_SET_H_
#define	_CONSTR_SET_H_

#include "asn_application.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct asn_SET_specifics_s {
	/*
	 * Target structure description.
	 */
	int struct_size;	/* Size of the target structure. */
	int ctx_offset;		/* Offset of the asn_struct_ctx_t member */
	int pres_offset;	/* Offset of _presence_map member */

	/*
	 * Extensions-related stuff.
	 */
	int extensible;				/* Whether SET is extensible */
	unsigned int *_mandatory_elements;	/* Bitmask of mandatory ones */
} asn_SET_specifics_t;

/*
 * A set specialized functions dealing with the SET type.
 */
asn_struct_free_f SET_free;
asn_struct_print_f SET_print;
asn_constr_check_f SET_constraint;
per_type_decoder_f SET_decode_uper;
per_type_encoder_f SET_encode_uper;

/***********************
 * Some handy helpers. *
 ***********************/

/*
 * Figure out whether the SET member indicated by PR_x has already been decoded.
 * It is very simple bitfield test, despite its visual complexity.
 */
#define	ASN_SET_ISPRESENT(set_ptr, PR_x)				\
	ASN_SET_ISPRESENT2(&((set_ptr)->_presence_map), PR_x)
#define	ASN_SET_ISPRESENT2(map_ptr, PR_x)				\
	(((unsigned int *)(map_ptr))					\
		[(PR_x)	/ (8 * sizeof(unsigned int))]			\
		& (1 << ((8 * sizeof(unsigned int)) - 1			\
		- ((PR_x) % (8 * sizeof(unsigned int))))))

#define	ASN_SET_MKPRESENT(map_ptr, PR_x)				\
	(((unsigned int *)(map_ptr))					\
		[(PR_x)	/ (8 * sizeof(unsigned int))]			\
		|= (1 << ((8 * sizeof(unsigned int)) - 1		\
		- ((PR_x) % (8 * sizeof(unsigned int))))))

#ifdef __cplusplus
}
#endif

#endif	/* _CONSTR_SET_H_ */
