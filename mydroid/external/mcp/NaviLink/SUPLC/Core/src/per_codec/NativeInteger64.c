/*-
 * Copyright (c) 2004, 2005, 2006 Lev Walkin <vlm@lionet.info>.
 * All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
/*
 * Read the NativeInteger.h for the explanation wrt. differences between
 * INTEGER and NativeInteger.
 * Basically, both are decoders and encoders of ASN.1 INTEGER type, but this
 * implementation deals with the standard (machine-specific) representation
 * of them instead of using the platform-independent buffer.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/NativeInteger64.h"

/*
 * NativeInteger basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_NativeInteger64 = {
	"INTEGER64",
	Native64Integer_free,
	Native64Integer_print,
	asn_generic_no_constraint,
	Native64Integer_encode_uper,	/* Unaligned PER decoder */
	Native64Integer_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

asn_dec_rval_t Native64Integer_decode_uper(asn_codec_ctx_t *opt_codec_ctx,
										 asn_TYPE_descriptor_t *td,
										 asn_per_constraints_t *constraints, 
										 void **sptr, 
										 asn_per_data_t *pd) 
{
	asn_dec_rval_t rval;
	long long *native = (long long*)*sptr;
	INTEGER64_t tmpint;
	void *tmpintptr = &tmpint;

	(void)opt_codec_ctx;
	ASN_DEBUG("Decoding NativeInteger %s (UPER)", td->name);

	if(!native) 
	{
		native = (long long *)(*sptr = CALLOC(1, sizeof(*native)));
		if(!native) _ASN_DECODE_FAILED;
	}

	memset(&tmpint, 0, sizeof tmpint);
	//I should correct this function before use it!!!!
	rval = INTEGER64_decode_uper(opt_codec_ctx, td, constraints, &tmpintptr, pd);
	if(rval.code == RC_OK) 
	{
		if(asn_INTEGER2longlong(&tmpint, native)) rval.code = RC_FAIL;
		else ASN_DEBUG("NativeInteger %s got value %ld", td->name, *native);
	}
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_INTEGER64, &tmpint);

	return rval;
}

asn_enc_rval_t Native64Integer_encode_uper(asn_TYPE_descriptor_t *td,
										 asn_per_constraints_t *constraints, 
										 void *sptr, 
										 asn_per_outp_t *po) 
{
	asn_enc_rval_t er;
	long long native;
	INTEGER64_t tmpint;

	if(!sptr) _ASN_ENCODE_FAILED;

	native = *(long long *)sptr;

	ASN_DEBUG("Encoding Native64Integer %s %ld (UPER)", td->name, native);

	memset(&tmpint, 0, sizeof(tmpint));
	if(asn_longlong2INTEGER(&tmpint, native)) _ASN_ENCODE_FAILED;

	er = INTEGER64_encode_uper(td, constraints, &tmpint, po);
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_INTEGER64, &tmpint);
	return er;
}

/*
 * INTEGER specific human-readable output.
 */
int Native64Integer_print(asn_TYPE_descriptor_t *td, 
						const void *sptr, 
						int ilevel, 
						asn_app_consume_bytes_f *cb, 
						void *app_key) 
{
	const long long *native = (const long long*)sptr;
	char scratch[32];	/* Enough for 64-bit int */
	int ret;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(native) 
	{
		ret = snprintf(scratch, sizeof(scratch), "%ld", *native);
		assert(ret > 0 && (size_t)ret < sizeof(scratch));
		return (cb(scratch, ret, app_key) < 0) ? -1 : 0;
	} 
	else 
	{
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
	}
}

void Native64Integer_free(asn_TYPE_descriptor_t *td, 
						  void *ptr, 
						  int contents_only) 
{

	if(!td || !ptr) return;

	ASN_DEBUG("Freeing %s as INTEGER64 (%d, %p, Native)", td->name, contents_only, ptr);

	if(!contents_only) 
	{
		FREEMEM(ptr);
	}
}

