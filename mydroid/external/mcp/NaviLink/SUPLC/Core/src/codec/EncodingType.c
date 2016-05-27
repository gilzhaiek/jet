/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Encodingtype.h"

int
EncodingType_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	/* Replace with underlying type checker */
	td->check_constraints = asn_DEF_ENUMERATED.check_constraints;
	return td->check_constraints(td, sptr, ctfailcb, app_key);
}

/*
 * This type is implemented using ENUMERATED,
 * so here we adjust the DEF accordingly.
 */
static void
EncodingType_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
	td->free_struct    = asn_DEF_ENUMERATED.free_struct;
	td->print_struct   = asn_DEF_ENUMERATED.print_struct;
	td->uper_decoder   = asn_DEF_ENUMERATED.uper_decoder;
	td->uper_encoder   = asn_DEF_ENUMERATED.uper_encoder;
	if(!td->per_constraints)
		td->per_constraints = asn_DEF_ENUMERATED.per_constraints;
	td->elements       = asn_DEF_ENUMERATED.elements;
	td->elements_count = asn_DEF_ENUMERATED.elements_count;
     /* td->specifics      = asn_DEF_ENUMERATED.specifics;	// Defined explicitly */
}

void
EncodingType_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	EncodingType_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
EncodingType_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	EncodingType_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

asn_dec_rval_t
EncodingType_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	EncodingType_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
EncodingType_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	EncodingType_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static asn_INTEGER_enum_map_t asn_MAP_EncodingType_value2enum_1[] = {
	{ 0,	4,	"ucs2" },
	{ 1,	10,	"gsmDefault" },
	{ 2,	4,	"utf8" }
	/* This list is extensible */
};
static unsigned int asn_MAP_EncodingType_enum2value_1[] = {
	1,	/* gsmDefault(1) */
	0,	/* ucs2(0) */
	2	/* utf8(2) */
	/* This list is extensible */
};
static asn_INTEGER_specifics_t asn_SPC_EncodingType_specs_1 = {
	asn_MAP_EncodingType_value2enum_1,	/* "tag" => N; sorted by tag */
	asn_MAP_EncodingType_enum2value_1,	/* N => "tag"; sorted by N */
	3,	/* Number of elements in the maps */
	4,	/* Extensions before this member */
	1	/* Strict enumeration */
};

static asn_per_constraints_t asn_PER_EncodingType_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  2,  2,  0,  2 }	/* (0..2,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_EncodingType = {
	"EncodingType",
	EncodingType_free,
	EncodingType_print,
	EncodingType_constraint,
	EncodingType_decode_uper,
	EncodingType_encode_uper,
	&asn_PER_EncodingType_constr_1,
	0, 0,	/* Defined elsewhere */
	&asn_SPC_EncodingType_specs_1	/* Additional specs */
};

