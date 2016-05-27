/*-
 * Copyright (c) 2003 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/GeneralString.h"

/*
 * GeneralString basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_GeneralString = {
	"GeneralString",
	OCTET_STRING_free,
	OCTET_STRING_print,         /* non-ascii string */
	asn_generic_unknown_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

