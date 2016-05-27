/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/asn_codecs_prim.h"
#include "per_codec/NULL.h"
#include "per_codec/BOOLEAN.h"	/* Implemented in terms of BOOLEAN type */

/*
 * NULL basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_NULL = {
	"NULL",
	BOOLEAN_free,
	NULL_print,
	asn_generic_no_constraint,
	NULL_decode_uper,	/* Unaligned PER decoder */
	NULL_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

int
NULL_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(sptr) {
		return (cb("<present>", 9, app_key) < 0) ? -1 : 0;
	} else {
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
	}
}

asn_dec_rval_t
NULL_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_dec_rval_t rv;

	(void)opt_codec_ctx;
	(void)td;
	(void)constraints;
	(void)pd;

	if(!*sptr) {
		*sptr = MALLOC(sizeof(NULL_t));
		if(*sptr) {
			*(NULL_t *)*sptr = 0;
		} else {
			_ASN_DECODE_FAILED;
		}
	}

	/*
	 * NULL type does not have content octets.
	 */

	rv.code = RC_OK;
	rv.consumed = 0;
	return rv;
}

asn_enc_rval_t
NULL_encode_uper(asn_TYPE_descriptor_t *td, asn_per_constraints_t *constraints,
		void *sptr, asn_per_outp_t *po) {
	asn_enc_rval_t er;

	(void)td;
	(void)constraints;
	(void)sptr;
	(void)po;

	er.encoded = 0;
	_ASN_ENCODED_OK(er);
}
