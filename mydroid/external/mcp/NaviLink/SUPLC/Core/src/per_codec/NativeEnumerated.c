/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/NativeEnumerated.h"

asn_TYPE_descriptor_t asn_DEF_NativeEnumerated = {
	"ENUMERATED",			/* The ASN.1 type is still ENUMERATED */
	NativeInteger_free,
	NativeInteger_print,
	asn_generic_no_constraint,
	NativeEnumerated_decode_uper,
	NativeEnumerated_encode_uper,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

asn_dec_rval_t
NativeEnumerated_decode_uper(asn_codec_ctx_t *opt_codec_ctx,
	asn_TYPE_descriptor_t *td, asn_per_constraints_t *constraints,
	void **sptr, asn_per_data_t *pd) {
	asn_INTEGER_specifics_t *specs = (asn_INTEGER_specifics_t *)td->specifics;
	asn_dec_rval_t rval = { RC_OK, 0 };
	long *native = (long *)*sptr;
	asn_per_constraint_t *ct;
	long value;

	(void)opt_codec_ctx;

	if(constraints) ct = &constraints->value;
	else if(td->per_constraints) ct = &td->per_constraints->value;
	else _ASN_DECODE_FAILED;	/* Mandatory! */
	if(!specs) _ASN_DECODE_FAILED;

	if(!native) {
		native = (long *)(*sptr = CALLOC(1, sizeof(*native)));
		if(!native) _ASN_DECODE_FAILED;
	}

	ASN_DEBUG("Decoding %s as NativeEnumerated", td->name);

	if(ct->flags & APC_EXTENSIBLE) {
		int inext = per_get_few_bits(pd, 1);
		if(inext < 0) _ASN_DECODE_STARVED;
		if(inext) ct = 0;
	}

	if(ct && ct->range_bits >= 0) {
		value = per_get_few_bits(pd, ct->range_bits);
		if(value < 0) _ASN_DECODE_STARVED;
		if(value >= (specs->extension
			? specs->extension - 1 : specs->map_count))
			_ASN_DECODE_EINVAL;
	} else {
		if(!specs->extension)
			_ASN_DECODE_FAILED;
		/*
		 * X.691, #10.6: normally small non-negative whole number;
		 */
		value = uper_get_nsnnwn(pd);
		if(value < 0) _ASN_DECODE_STARVED;
		value += specs->extension - 1;
		if(value >= specs->map_count)
			_ASN_DECODE_FAILED;
	}

	*native = specs->value2enum[value].nat_value;
	ASN_DEBUG("Decoded %s = %ld", td->name, *native);

	return rval;
}

static int
NativeEnumerated__compar_value2enum(const void *ap, const void *bp) {
	const asn_INTEGER_enum_map_t *a = ap;
	const asn_INTEGER_enum_map_t *b = bp;
	if(a->nat_value == b->nat_value)
		return 0;
	if(a->nat_value < b->nat_value)
		return -1;
	return 1;
}

asn_enc_rval_t
NativeEnumerated_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	asn_INTEGER_specifics_t *specs = (asn_INTEGER_specifics_t *)td->specifics;
	asn_enc_rval_t er;
	long native, value;
	asn_per_constraint_t *ct;
	int inext = 0;
	asn_INTEGER_enum_map_t key;
	asn_INTEGER_enum_map_t *kf;

	if(!sptr) _ASN_ENCODE_FAILED;
	if(!specs) _ASN_ENCODE_FAILED;

	if(constraints) ct = &constraints->value;
	else if(td->per_constraints) ct = &td->per_constraints->value;
	else _ASN_ENCODE_FAILED;	/* Mandatory! */

	ASN_DEBUG("Encoding %s as NativeEnumerated", td->name);

	er.encoded = 0;

	native = *(long *)sptr;
	if(native < 0) _ASN_ENCODE_FAILED;

	key.nat_value = native;
	kf = b_search(&key, specs->value2enum, specs->map_count,
		sizeof(key), NativeEnumerated__compar_value2enum);
	if(!kf) {
		ASN_DEBUG("No element corresponds to %ld", native);
		_ASN_ENCODE_FAILED;
	}
	value = kf - specs->value2enum;

	if(ct->range_bits >= 0) {
		int cmpWith = specs->extension
				? specs->extension - 1 : specs->map_count;
		if(value >= cmpWith)
			inext = 1;
	}
   
	if(ct->flags & APC_EXTENSIBLE) {
		/*if(per_put_few_bits(po, inext, 0)) _ASN_ENCODE_FAILED;
		ct = 0;*///rsuvorov change  
        	if(per_put_few_bits(po, inext, 1))//per_put_few_bits(po, inext, 0)
			_ASN_ENCODE_FAILED;

            if(inext==1) ct = 0; //rsuvorov extension addon
	} else if(inext) {
		_ASN_ENCODE_FAILED;
	}

	if(ct && ct->range_bits >= 0) {
		if(per_put_few_bits(po, value, ct->range_bits))
			_ASN_ENCODE_FAILED;
		_ASN_ENCODED_OK(er);
	}

	if(!specs->extension)
		_ASN_ENCODE_FAILED;

	/*
	 * X.691, #10.6: normally small non-negative whole number;
	 */
	if(uper_put_nsnnwn(po, value - (specs->extension - 1)))
		_ASN_ENCODE_FAILED;

	_ASN_ENCODED_OK(er);
}

