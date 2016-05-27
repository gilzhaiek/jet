/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Formatindicator.h"

int
FormatIndicator_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
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
FormatIndicator_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
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
FormatIndicator_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	FormatIndicator_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
FormatIndicator_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	FormatIndicator_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}


asn_dec_rval_t
FormatIndicator_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	FormatIndicator_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
FormatIndicator_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	FormatIndicator_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static asn_INTEGER_enum_map_t asn_MAP_FormatIndicator_value2enum_1[] = {
	{ 0,	11,	"logicalName" },
	{ 1,	13,	"e-mailAddress" },
	{ 2,	6,	"msisdn" },
	{ 3,	3,	"url" },
	{ 4,	6,	"sipUrl" },
	{ 5,	3,	"min" },
	{ 6,	3,	"mdn" },
	{ 7,	17,	"imsPublicIdentity" }
	/* This list is extensible */
};
static unsigned int asn_MAP_FormatIndicator_enum2value_1[] = {
	1,	/* e-mailAddress(1) */
	7,	/* imsPublicIdentity(7) */
	0,	/* logicalName(0) */
	6,	/* mdn(6) */
	5,	/* min(5) */
	2,	/* msisdn(2) */
	4,	/* sipUrl(4) */
	3	/* url(3) */
	/* This list is extensible */
};
static asn_INTEGER_specifics_t asn_SPC_FormatIndicator_specs_1 = {
	asn_MAP_FormatIndicator_value2enum_1,	/* "tag" => N; sorted by tag */
	asn_MAP_FormatIndicator_enum2value_1,	/* N => "tag"; sorted by N */
	8,	/* Number of elements in the maps */
	9,	/* Extensions before this member */
	1	/* Strict enumeration */
};

static asn_per_constraints_t asn_PER_FormatIndicator_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  3,  3,  0,  7 }	/* (0..7,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_FormatIndicator = {
	"FormatIndicator",
	FormatIndicator_free,
	FormatIndicator_print,
	FormatIndicator_constraint,
	FormatIndicator_decode_uper,
	FormatIndicator_encode_uper,
	&asn_PER_FormatIndicator_constr_1,
	0, 0,	/* Defined elsewhere */
	&asn_SPC_FormatIndicator_specs_1	/* Additional specs */
};

