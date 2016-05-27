/*-
 * Copyright (c) 2003, 2004 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#ifndef	_CONSTR_SEQUENCE_H_
#define	_CONSTR_SEQUENCE_H_

#include "asn_application.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct asn_SEQUENCE_specifics_s {
	/*
	 * Target structure description.
	 */
	int struct_size;	/* Size of the target structure. */
	int ctx_offset;		/* Offset of the asn_struct_ctx_t member */

	/*
	 * Optional members of the extensions root (roms) or additions (aoms).
	 * Meaningful for PER.
	 */
	int *oms;		/* Optional MemberS */
	int  roms_count;	/* Root optional members count */
	int  aoms_count;	/* Additions optional members count */

	/*
	 * Description of an extensions group.
	 */
	int ext_after;		/* Extensions start after this member */
	int ext_before;		/* Extensions stop before this member */
} asn_SEQUENCE_specifics_t;


/*
 * A set specialized functions dealing with the SEQUENCE type.
 */
asn_struct_free_f SEQUENCE_free;
asn_struct_print_f SEQUENCE_print;
asn_constr_check_f SEQUENCE_constraint;
per_type_decoder_f SEQUENCE_decode_uper;
per_type_encoder_f SEQUENCE_encode_uper;

#ifdef __cplusplus
}
#endif

#endif	/* _CONSTR_SEQUENCE_H_ */
