/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Altitudeinfo.h"

static int
altitudeDirection_2_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
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
altitudeDirection_2_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
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

static void
altitudeDirection_2_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	altitudeDirection_2_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

static int
altitudeDirection_2_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	altitudeDirection_2_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

static asn_dec_rval_t
altitudeDirection_2_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	altitudeDirection_2_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

static asn_enc_rval_t
altitudeDirection_2_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	altitudeDirection_2_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}

static int
memb_altitude_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 32767)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static int
memb_altUncertainty_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 127)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_INTEGER_enum_map_t asn_MAP_altitudeDirection_value2enum_2[] = {
	{ 0,	6,	"height" },
	{ 1,	5,	"depth" }
};
static unsigned int asn_MAP_altitudeDirection_enum2value_2[] = {
	1,	/* depth(1) */
	0	/* height(0) */
};
static asn_INTEGER_specifics_t asn_SPC_altitudeDirection_specs_2 = {
	asn_MAP_altitudeDirection_value2enum_2,	/* "tag" => N; sorted by tag */
	asn_MAP_altitudeDirection_enum2value_2,	/* N => "tag"; sorted by N */
	2,	/* Number of elements in the maps */
	0,	/* Enumeration is not extensible */
	1	/* Strict enumeration */
};

static asn_per_constraints_t asn_PER_altitudeDirection_constr_2 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_altitudeDirection_2 = {
	"altitudeDirection",
	altitudeDirection_2_free,
	altitudeDirection_2_print,
	altitudeDirection_2_constraint,
	altitudeDirection_2_decode_uper,
	altitudeDirection_2_encode_uper,
	&asn_PER_altitudeDirection_constr_2,
	0, 0,	/* Defined elsewhere */
	&asn_SPC_altitudeDirection_specs_2	/* Additional specs */
};

static asn_per_constraints_t asn_PER_memb_altitudeDirection_constr_2 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_altitude_constr_5 = {
	{ APC_CONSTRAINED,	 15,  15,  0,  32767 }	/* (0..32767) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_altUncertainty_constr_6 = {
	{ APC_CONSTRAINED,	 7,  7,  0,  127 }	/* (0..127) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_AltitudeInfo_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct AltitudeInfo, altitudeDirection),
		&asn_DEF_altitudeDirection_2,
		0,	/* Defer constraints checking to the member type */
		&asn_PER_memb_altitudeDirection_constr_2,
		0,
		"altitudeDirection"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct AltitudeInfo, altitude),
		&asn_DEF_NativeInteger,
		memb_altitude_constraint_1,
		&asn_PER_memb_altitude_constr_5,
		0,
		"altitude"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct AltitudeInfo, altUncertainty),
		&asn_DEF_NativeInteger,
		memb_altUncertainty_constraint_1,
		&asn_PER_memb_altUncertainty_constr_6,
		0,
		"altUncertainty"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_AltitudeInfo_specs_1 = {
	sizeof(struct AltitudeInfo),
	offsetof(struct AltitudeInfo, _asn_ctx),
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_AltitudeInfo = {
	"AltitudeInfo",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_AltitudeInfo_1,
	3,	/* Elements count */
	&asn_SPC_AltitudeInfo_specs_1	/* Additional specs */
};

