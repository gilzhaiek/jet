/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/RELATIVE-OID.h"
#include "per_codec/asn_codecs_prim.h"	/* Encoder and decoder of a primitive type */
#include <limits.h>	/* for CHAR_BIT */
#include <errno.h>

/*
 * RELATIVE-OID basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_RELATIVE_OID = {
	"RELATIVE-OID",
	ASN__PRIMITIVE_TYPE_free,
	RELATIVE_OID_print,
	asn_generic_no_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

static ssize_t
RELATIVE_OID__dump_body(const RELATIVE_OID_t *st, asn_app_consume_bytes_f *cb, void *app_key) {
	ssize_t wrote = 0;
	ssize_t ret;
	int startn;
	int i;

	for(i = 0, startn = 0; i < st->size; i++) {
		uint8_t b = st->buf[i];
		if((b & 0x80))			/* Continuation expected */
			continue;
		if(startn) {
			/* Separate arcs */
			if(cb(".", 1, app_key) < 0)
				return -1;
			wrote++;
		}

		ret = OBJECT_IDENTIFIER__dump_arc(&st->buf[startn],
			i - startn + 1, 0, cb, app_key);
		if(ret < 0) return -1;
		wrote += ret;

		startn = i + 1;
	}

	return wrote;
}

int
RELATIVE_OID_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	const RELATIVE_OID_t *st = (const RELATIVE_OID_t *)sptr;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(!st || !st->buf)
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/* Dump preamble */
	if(cb("{ ", 2, app_key) < 0)
		return -1;

	if(RELATIVE_OID__dump_body(st, cb, app_key) < 0)
		return -1;

	return (cb(" }", 2, app_key) < 0) ? -1 : 0;
}


int
RELATIVE_OID_get_arcs(RELATIVE_OID_t *roid,
	void *arcs, unsigned int arc_type_size, unsigned int arc_slots) {
	void *arcs_end = (char *)arcs + (arc_slots * arc_type_size);
	int num_arcs = 0;
	int startn = 0;
	int i;

	if(!roid || !roid->buf) {
		errno = EINVAL;
		return -1;
	}

	for(i = 0; i < roid->size; i++) {
		uint8_t b = roid->buf[i];
		if((b & 0x80))			/* Continuation expected */
			continue;

		if(arcs < arcs_end) {
			if(OBJECT_IDENTIFIER_get_single_arc(
				&roid->buf[startn],
					i - startn + 1, 0,
					arcs, arc_type_size))
				return -1;
			arcs = ((char *)arcs) + arc_type_size;
			num_arcs++;
		}

		startn = i + 1;
	}

	return num_arcs;
}

int
RELATIVE_OID_set_arcs(RELATIVE_OID_t *roid, void *arcs, unsigned int arc_type_size, unsigned int arcs_slots) {
	uint8_t *buf;
	uint8_t *bp;
	unsigned int size;
	unsigned int i;

	if(roid == NULL || arcs == NULL || arc_type_size < 1) {
		errno = EINVAL;
		return -1;
	}

	/*
	 * Roughly estimate the maximum size necessary to encode these arcs.
	 */
	size = ((arc_type_size * CHAR_BIT + 6) / 7) * arcs_slots;
	bp = buf = (uint8_t *)MALLOC(size + 1);
	if(!buf) {
		/* ENOMEM */
		return -1;
	}

	/*
	 * Encode the arcs.
	 */
	for(i = 0; i < arcs_slots; i++, arcs = ((char *)arcs) + arc_type_size) {
		bp += OBJECT_IDENTIFIER_set_single_arc(bp,
			arcs, arc_type_size, 0);
	}

	assert((unsigned)(bp - buf) <= size);

	/*
	 * Replace buffer.
	 */
	roid->size = (int)(bp - buf);
	bp = roid->buf;
	roid->buf = buf;
	if(bp) FREEMEM(bp);

	return 0;
}

