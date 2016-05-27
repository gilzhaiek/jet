/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/NativeReal.h"
#include "per_codec/REAL.h"

/*
 * NativeReal basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_NativeReal = {
	"REAL",
	NativeReal_free,
	NativeReal_print,
	asn_generic_no_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

/*
 * Decode REAL type.
 */
/*
 * REAL specific human-readable output.
 */
int
NativeReal_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	const double *Dbl = (const double *)sptr;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(!Dbl) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	return (REAL__dump(*Dbl, 0, cb, app_key) < 0) ? -1 : 0;
}

void
NativeReal_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {

	if(!td || !ptr)
		return;

	ASN_DEBUG("Freeing %s as REAL (%d, %p, Native)",
		td->name, contents_only, ptr);

	if(!contents_only) {
		FREEMEM(ptr);
	}
}

