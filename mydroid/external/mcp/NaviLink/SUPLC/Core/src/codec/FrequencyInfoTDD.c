/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Frequencyinfotdd.h"

static asn_TYPE_member_t asn_MBR_FrequencyInfoTDD_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct FrequencyInfoTDD, uarfcn_Nt),
		&asn_DEF_UARFCN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"uarfcn-Nt"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_FrequencyInfoTDD_specs_1 = {
	sizeof(struct FrequencyInfoTDD),
	offsetof(struct FrequencyInfoTDD, _asn_ctx),
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_FrequencyInfoTDD = {
	"FrequencyInfoTDD",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_FrequencyInfoTDD_1,
	1,	/* Elements count */
	&asn_SPC_FrequencyInfoTDD_specs_1	/* Additional specs */
};

