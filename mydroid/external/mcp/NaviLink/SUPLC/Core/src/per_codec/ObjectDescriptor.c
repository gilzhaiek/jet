/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/ObjectDescriptor.h"

/*
 * ObjectDescriptor basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_ObjectDescriptor = {
	"ObjectDescriptor",
	OCTET_STRING_free,
	OCTET_STRING_print_utf8,   /* Treat as ASCII subset (it's not) */
	asn_generic_unknown_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

