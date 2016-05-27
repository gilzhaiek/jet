/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/UnsignedInteger.h"

/*
 * NativeInteger basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_UnsignedInteger = {
	"UINTEGER",
	UnsignedInteger_free,
	UnsignedInteger_print,
	asn_generic_no_constraint,
	UnsignedInteger_decode_uper,	/* Unaligned PER decoder */
	UnsignedInteger_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

asn_dec_rval_t UnsignedInteger_decode_uper(asn_codec_ctx_t *opt_codec_ctx,
										 asn_TYPE_descriptor_t *td,
										 asn_per_constraints_t *constraints, 
										 void **sptr, 
										 asn_per_data_t *pd) 
{
	asn_dec_rval_t rval;
	unsigned long *native = (unsigned long*)*sptr;
	UINTEGER_t tmpint;
	void *tmpintptr = &tmpint;

	(void)opt_codec_ctx;
	ASN_DEBUG("Decoding UnsignedInteger %s (UPER)", td->name);

	if(!native) 
	{
		native = (unsigned long *)(*sptr = CALLOC(1, sizeof(*native)));
		if(!native) _ASN_DECODE_FAILED;
	}

	memset(&tmpint, 0, sizeof tmpint);
	//I should correct this function before use it!!!!
	rval = UINTEGER_decode_uper(opt_codec_ctx, td, constraints, &tmpintptr, pd);
	if(rval.code == RC_OK) 
	{
		if(asn_UINTEGER2ulong(&tmpint, native)) rval.code = RC_FAIL;
		else ASN_DEBUG("UnsignedInteger %s got value %ld", td->name, *native);
	}
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_UINTEGER, &tmpint);

	return rval;
}

asn_enc_rval_t UnsignedInteger_encode_uper(asn_TYPE_descriptor_t *td,
										 asn_per_constraints_t *constraints, 
										 void *sptr, 
										 asn_per_outp_t *po) 
{
	asn_enc_rval_t er;
	unsigned long native;
	UINTEGER_t tmpint;

	if(!sptr) _ASN_ENCODE_FAILED;

	native = *(unsigned long *)sptr;

	ASN_DEBUG("Encoding UnsignedInteger %s %ld (UPER)", td->name, native);

	memset(&tmpint, 0, sizeof(tmpint));
	if(asn_ulong2UINTEGER(&tmpint, native)) _ASN_ENCODE_FAILED;

	er = UINTEGER_encode_uper(td, constraints, &tmpint, po);
	ASN_STRUCT_FREE_CONTENTS_ONLY(asn_DEF_UINTEGER, &tmpint);
	return er;
}

/*
 * INTEGER specific human-readable output.
 */
int UnsignedInteger_print(asn_TYPE_descriptor_t *td, 
						const void *sptr, 
						int ilevel, 
						asn_app_consume_bytes_f *cb, 
						void *app_key) 
{
	const unsigned long *native = (const unsigned long*)sptr;
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

void UnsignedInteger_free(asn_TYPE_descriptor_t *td, 
						  void *ptr, 
						  int contents_only) 
{

	if(!td || !ptr) return;

	ASN_DEBUG("Freeing %s as UINTEGER (%d, %p, Native)", td->name, contents_only, ptr);

	if(!contents_only) 
	{
		FREEMEM(ptr);
	}
}

