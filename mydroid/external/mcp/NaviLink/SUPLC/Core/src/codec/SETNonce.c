/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Setnonce.h"

int
SETNonce_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	if(st->size > 0) {
		/* Size in bits */
		size = 8 * st->size - (st->bits_unused & 0x07);
	} else {
		size = 0;
	}
	
	if((size == 128)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

/*
 * This type is implemented using BIT_STRING,
 * so here we adjust the DEF accordingly.
 */
static void
SETNonce_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
	td->free_struct    = asn_DEF_BIT_STRING.free_struct;
	td->print_struct   = asn_DEF_BIT_STRING.print_struct;
	td->uper_decoder   = asn_DEF_BIT_STRING.uper_decoder;
	td->uper_encoder   = asn_DEF_BIT_STRING.uper_encoder;
	if(!td->per_constraints)
		td->per_constraints = asn_DEF_BIT_STRING.per_constraints;
	td->elements       = asn_DEF_BIT_STRING.elements;
	td->elements_count = asn_DEF_BIT_STRING.elements_count;
	td->specifics      = asn_DEF_BIT_STRING.specifics;
}

void
SETNonce_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	SETNonce_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
SETNonce_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	SETNonce_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

asn_dec_rval_t
SETNonce_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	SETNonce_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
SETNonce_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	SETNonce_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static asn_per_constraints_t asn_PER_SETNonce_constr_1 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  128,  128 }	/* (SIZE(128..128)) */
};
asn_TYPE_descriptor_t asn_DEF_SETNonce = {
	"SETNonce",
	SETNonce_free,
	SETNonce_print,
	SETNonce_constraint,
	SETNonce_decode_uper,
	SETNonce_encode_uper,
	&asn_PER_SETNonce_constr_1,
	0, 0,	/* No members */
	0	/* No specifics */
};

