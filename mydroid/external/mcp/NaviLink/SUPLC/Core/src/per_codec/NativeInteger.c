/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/NativeInteger.h"

asn_TYPE_descriptor_t asn_DEF_NativeInteger = {
	"INTEGER",			/* The ASN.1 type is still INTEGER */
	NativeInteger_free,
	NativeInteger_print,
	asn_generic_no_constraint,
	NativeInteger_decode_uper,	/* Unaligned PER decoder */
	NativeInteger_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

/*
 * Decode INTEGER type.
 */

/*
 * Decode the chunk of XML text encoding INTEGER.
 */

asn_dec_rval_t
NativeInteger_decode_uper(asn_codec_ctx_t *opt_codec_ctx,
	asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {

	asn_dec_rval_t rval;
	long *native = (long *)*sptr;
	INTEGER_t tmpint;
	void *tmpintptr = &tmpint;

	(void)opt_codec_ctx;
	ASN_DEBUG("Decoding NativeInteger %s (UPER)", td->name);

	if(!native) {
		native = (long *)(*sptr = CALLOC(1, sizeof(*native)));
		if(!native) _ASN_DECODE_FAILED;
	}

	memset(&tmpint, 0, sizeof tmpint);
	rval = INTEGER_decode_uper(opt_codec_ctx, td, constraints,
				   &tmpintptr, pd);
	if(rval.code == RC_OK) {
		if(asn_INTEGER2long(&tmpint, native))
			rval.code = RC_FAIL;
		else
			ASN_DEBUG("NativeInteger %s got value %ld",
				td->name, *native);
	}
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_INTEGER, &tmpint);

	return rval;
}

asn_enc_rval_t
NativeInteger_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	asn_enc_rval_t er;
	long native;
	INTEGER_t tmpint;

	if(!sptr) _ASN_ENCODE_FAILED;

	native = *(long *)sptr;

	ASN_DEBUG("Encoding NativeInteger %s %ld (UPER)", td->name, native);

	memset(&tmpint, 0, sizeof(tmpint));
	if(asn_long2INTEGER(&tmpint, native))
		_ASN_ENCODE_FAILED;
	er = INTEGER_encode_uper(td, constraints, &tmpint, po);
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_INTEGER, &tmpint);
	return er;
}

/*
 * INTEGER specific human-readable output.
 */
int
NativeInteger_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	const long *native = (const long *)sptr;
	char scratch[32];	/* Enough for 64-bit int */
	int ret;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(native) {
		ret = snprintf(scratch, sizeof(scratch), "%ld", *native);
		assert(ret > 0 && (size_t)ret < sizeof(scratch));
		return (cb(scratch, ret, app_key) < 0) ? -1 : 0;
	} else {
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
	}
}

void
NativeInteger_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {

	if(!td || !ptr)
		return;

	ASN_DEBUG("Freeing %s as INTEGER (%d, %p, Native)",
		td->name, contents_only, ptr);

	if(!contents_only) {
		FREEMEM(ptr);
	}
}

