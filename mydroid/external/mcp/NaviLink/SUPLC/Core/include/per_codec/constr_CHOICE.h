/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */



#ifndef	_CONSTR_CHOICE_H_
#define	_CONSTR_CHOICE_H_

#include "asn_application.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct asn_CHOICE_specifics_s {
	/*
	 * Target structure description.
	 */
	int struct_size;	/* Size of the target structure. */
	int ctx_offset;		/* Offset of the asn_codec_ctx_t member */
	int pres_offset;	/* Identifier of the present member */
	int pres_size;		/* Size of the identifier (enum) */


	/* Canonical ordering of CHOICE elements, for PER */
	int *canonical_order;

	/*
	 * Extensions-related stuff.
	 */
	int ext_start;		/* First member of extensions, or -1 */
} asn_CHOICE_specifics_t;

/*
 * A set specialized functions dealing with the CHOICE type.
 */
asn_struct_free_f CHOICE_free;
asn_struct_print_f CHOICE_print;
asn_constr_check_f CHOICE_constraint;
per_type_decoder_f CHOICE_decode_uper;
per_type_encoder_f CHOICE_encode_uper;

#ifdef __cplusplus
}
#endif

#endif	/* _CONSTR_CHOICE_H_ */
