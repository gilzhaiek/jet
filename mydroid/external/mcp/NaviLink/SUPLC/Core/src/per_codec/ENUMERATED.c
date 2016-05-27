/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/ENUMERATED.h"
#include "per_codec/NativeEnumerated.h"
#include "per_codec/asn_codecs_prim.h"	/* Encoder and decoder of a primitive type */

asn_TYPE_descriptor_t asn_DEF_ENUMERATED = {
	"ENUMERATED",
	ASN__PRIMITIVE_TYPE_free,
	INTEGER_print,			/* Implemented in terms of INTEGER */
	asn_generic_no_constraint,
	ENUMERATED_decode_uper,	/* Unaligned PER decoder */
	ENUMERATED_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

asn_dec_rval_t
ENUMERATED_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_dec_rval_t rval;
	ENUMERATED_t *st = (ENUMERATED_t *)*sptr;
	long value;
	void *vptr = &value;

	if(!st) {
		st = (ENUMERATED_t *)(*sptr = CALLOC(1, sizeof(*st)));
		if(!st) _ASN_DECODE_FAILED;
	}

	rval = NativeEnumerated_decode_uper(opt_codec_ctx, td, constraints,
			(void **)&vptr, pd);
	if(rval.code == RC_OK)
		if(asn_long2INTEGER(st, value))
			rval.code = RC_FAIL;
	return rval;
}

asn_enc_rval_t
ENUMERATED_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	ENUMERATED_t *st = (ENUMERATED_t *)sptr;
	long value;

	if(asn_INTEGER2long(st, &value))
		_ASN_ENCODE_FAILED;

	return NativeEnumerated_encode_uper(td, constraints, &value, po);
}

