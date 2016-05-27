/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/ISO646String.h"

/*
 * ISO646String basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_ISO646String = {
	"ISO646String",
	OCTET_STRING_free,
	OCTET_STRING_print_utf8,	/* ASCII subset */
	VisibleString_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

