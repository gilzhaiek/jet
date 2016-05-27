/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Frequencyinfofdd.h"

static asn_TYPE_member_t asn_MBR_FrequencyInfoFDD_1[] = {
	{ ATF_POINTER, 1, offsetof(struct FrequencyInfoFDD, uarfcn_UL),
		&asn_DEF_UARFCN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"uarfcn-UL"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct FrequencyInfoFDD, uarfcn_DL),
		&asn_DEF_UARFCN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"uarfcn-DL"
		},
};
static int asn_MAP_FrequencyInfoFDD_oms_1[] = { 0 };

static asn_SEQUENCE_specifics_t asn_SPC_FrequencyInfoFDD_specs_1 = {
	sizeof(struct FrequencyInfoFDD),
	offsetof(struct FrequencyInfoFDD, _asn_ctx),
	asn_MAP_FrequencyInfoFDD_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_FrequencyInfoFDD = {
	"FrequencyInfoFDD",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_FrequencyInfoFDD_1,
	2,	/* Elements count */
	&asn_SPC_FrequencyInfoFDD_specs_1	/* Additional specs */
};

