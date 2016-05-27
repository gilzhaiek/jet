/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Statuscode.h"

int
StatusCode_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
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
StatusCode_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
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
StatusCode_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	StatusCode_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
StatusCode_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	StatusCode_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

asn_dec_rval_t
StatusCode_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	StatusCode_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
StatusCode_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	StatusCode_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static asn_INTEGER_enum_map_t asn_MAP_StatusCode_value2enum_1[] = {
	{ 0,	11,	"unspecified" },
	{ 1,	13,	"systemFailure" },
	{ 2,	17,	"unexpectedMessage" },
	{ 3,	13,	"protocolError" },
	{ 4,	11,	"dataMissing" },
	{ 5,	19,	"unexpectedDataValue" },
	{ 6,	16,	"posMethodFailure" },
	{ 7,	17,	"posMethodMismatch" },
	{ 8,	19,	"posProtocolMismatch" },
	{ 9,	21,	"targetSETnotReachable" },
	{ 10,	19,	"versionNotSupported" },
	{ 11,	16,	"resourceShortage" },
	{ 12,	16,	"invalidSessionId" },
	{ 13,	24,	"nonProxyModeNotSupported" },
	{ 14,	21,	"proxyModeNotSupported" },
	{ 15,	23,	"positioningNotPermitted" },
	{ 16,	14,	"authNetFailure" },
	{ 17,	19,	"authSuplinitFailure" },
	{ 100,	19,	"consentDeniedByUser" },
	{ 101,	20,	"consentGrantedByUser" }
	/* This list is extensible */
};
static unsigned int asn_MAP_StatusCode_enum2value_1[] = {
	16,	/* authNetFailure(16) */
	17,	/* authSuplinitFailure(17) */
	18,	/* consentDeniedByUser(100) */
	19,	/* consentGrantedByUser(101) */
	4,	/* dataMissing(4) */
	12,	/* invalidSessionId(12) */
	13,	/* nonProxyModeNotSupported(13) */
	6,	/* posMethodFailure(6) */
	7,	/* posMethodMismatch(7) */
	8,	/* posProtocolMismatch(8) */
	15,	/* positioningNotPermitted(15) */
	3,	/* protocolError(3) */
	14,	/* proxyModeNotSupported(14) */
	11,	/* resourceShortage(11) */
	1,	/* systemFailure(1) */
	9,	/* targetSETnotReachable(9) */
	5,	/* unexpectedDataValue(5) */
	2,	/* unexpectedMessage(2) */
	0,	/* unspecified(0) */
	10	/* versionNotSupported(10) */
	/* This list is extensible */
};
static asn_INTEGER_specifics_t asn_SPC_StatusCode_specs_1 = {
	asn_MAP_StatusCode_value2enum_1,	/* "tag" => N; sorted by tag */
	asn_MAP_StatusCode_enum2value_1,	/* N => "tag"; sorted by N */
	20,	/* Number of elements in the maps */
	21,	/* Extensions before this member */
	1	/* Strict enumeration */
};

static asn_per_constraints_t asn_PER_StatusCode_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  5,  5,  0,  19 }	/* (0..19,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_StatusCode = {
	"StatusCode",
	StatusCode_free,
	StatusCode_print,
	StatusCode_constraint,
	StatusCode_decode_uper,
	StatusCode_encode_uper,
	&asn_PER_StatusCode_constr_1,
	0, 0,	/* Defined elsewhere */
	&asn_SPC_StatusCode_specs_1	/* Additional specs */
};

