/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Posmethod.h"

int
PosMethod_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
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
PosMethod_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
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
PosMethod_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	PosMethod_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
PosMethod_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	PosMethod_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

asn_dec_rval_t
PosMethod_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	PosMethod_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
PosMethod_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	PosMethod_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static asn_INTEGER_enum_map_t asn_MAP_PosMethod_value2enum_1[] = {
	{ 0,	15,	"agpsSETassisted" },
	{ 1,	12,	"agpsSETbased" },
	{ 2,	19,	"agpsSETassistedpref" },
	{ 3,	16,	"agpsSETbasedpref" },
	{ 4,	13,	"autonomousGPS" },
	{ 5,	4,	"aFLT" },
	{ 6,	4,	"eCID" },
	{ 7,	4,	"eOTD" },
	{ 8,	5,	"oTDOA" },
	{ 9,	10,	"noPosition" }
	/* This list is extensible */
};
static unsigned int asn_MAP_PosMethod_enum2value_1[] = {
	5,	/* aFLT(5) */
	0,	/* agpsSETassisted(0) */
	2,	/* agpsSETassistedpref(2) */
	1,	/* agpsSETbased(1) */
	3,	/* agpsSETbasedpref(3) */
	4,	/* autonomousGPS(4) */
	6,	/* eCID(6) */
	7,	/* eOTD(7) */
	9,	/* noPosition(9) */
	8	/* oTDOA(8) */
	/* This list is extensible */
};
static asn_INTEGER_specifics_t asn_SPC_PosMethod_specs_1 = {
	asn_MAP_PosMethod_value2enum_1,	/* "tag" => N; sorted by tag */
	asn_MAP_PosMethod_enum2value_1,	/* N => "tag"; sorted by N */
	10,	/* Number of elements in the maps */
	11,	/* Extensions before this member */
	1	/* Strict enumeration *///17 
};
static asn_per_constraints_t asn_PER_PosMethod_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  4,  4,  0,  9 }	/* (0..9,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_PosMethod = {
	"PosMethod",
	PosMethod_free,
	PosMethod_print,
	PosMethod_constraint,
	PosMethod_decode_uper,
	PosMethod_encode_uper,
	&asn_PER_PosMethod_constr_1,
	0, 0,	/* Defined elsewhere */
	&asn_SPC_PosMethod_specs_1	/* Additional specs */
};

