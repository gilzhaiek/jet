/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Postechnology.h"

static asn_TYPE_member_t asn_MBR_PosTechnology_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, agpsSETassisted),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"agpsSETassisted"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, agpsSETBased),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"agpsSETBased"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, autonomousGPS),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"autonomousGPS"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, aFLT),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"aFLT"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, eCID),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"eCID"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, eOTD),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"eOTD"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosTechnology, oTDOA),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"oTDOA"
		},
};
static asn_SEQUENCE_specifics_t asn_SPC_PosTechnology_specs_1 = {
	sizeof(struct PosTechnology),
	offsetof(struct PosTechnology, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_PosTechnology = {
	"PosTechnology",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_PosTechnology_1,
	7,	/* Elements count */
	&asn_SPC_PosTechnology_specs_1	/* Additional specs */
};

