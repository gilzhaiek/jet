/*-
 * Copyright (c) 2003, 2005 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/asn_codecs_prim.h"
#include "per_codec/BOOLEAN.h"

asn_TYPE_descriptor_t asn_DEF_BOOLEAN = {
	"BOOLEAN",
	BOOLEAN_free,
	BOOLEAN_print,
	asn_generic_no_constraint,
	BOOLEAN_decode_uper,	/* Unaligned PER decoder */
	BOOLEAN_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};


int
BOOLEAN_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	const BOOLEAN_t *st = (const BOOLEAN_t *)sptr;
	const char *buf;
	size_t buflen;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(st) {
		if(*st) {
			buf = "TRUE";
			buflen = 4;
		} else {
			buf = "FALSE";
			buflen = 5;
		}
	} else {
		buf = "<absent>";
		buflen = 8;
	}

	return (cb(buf, buflen, app_key) < 0) ? -1 : 0;
}

void
BOOLEAN_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {
	if(td && ptr && !contents_only) {
		FREEMEM(ptr);
	}
}

asn_dec_rval_t
BOOLEAN_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_dec_rval_t rv;
	BOOLEAN_t *st = (BOOLEAN_t *)*sptr;

	(void)opt_codec_ctx;
	(void)constraints;

	if(!st) {
		st = (BOOLEAN_t *)(*sptr = MALLOC(sizeof(*st)));
		if(!st) _ASN_DECODE_FAILED;
	}

	/*
	 * Extract a single bit
	 */
	switch(per_get_few_bits(pd, 1)) {
	case 1: *st = 1; break;
	case 0: *st = 0; break;
	case -1: default: _ASN_DECODE_FAILED;
	}

	ASN_DEBUG("%s decoded as %s", td->name, *st ? "TRUE" : "FALSE");

	rv.code = RC_OK;
	rv.consumed = 1;
	return rv;
}


asn_enc_rval_t
BOOLEAN_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	const BOOLEAN_t *st = (const BOOLEAN_t *)sptr;
	asn_enc_rval_t er;

	(void)constraints;

	if(!st) _ASN_ENCODE_FAILED;

	per_put_few_bits(po, *st ? 1 : 0, 1);

	_ASN_ENCODED_OK(er);
}
