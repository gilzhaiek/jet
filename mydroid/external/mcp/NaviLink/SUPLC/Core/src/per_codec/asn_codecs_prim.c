/*-
 * Copyright (c) 2003, 2004 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/asn_codecs_prim.h"


/*
 * Decode an always-primitive type.
 */
void
ASN__PRIMITIVE_TYPE_free(asn_TYPE_descriptor_t *td, void *sptr,
		int contents_only) {
	ASN__PRIMITIVE_TYPE_t *st = (ASN__PRIMITIVE_TYPE_t *)sptr;

	if(!td || !sptr)
		return;

	ASN_DEBUG("Freeing %s as a primitive type", td->name);

	if(st->buf)
		FREEMEM(st->buf);

	if(!contents_only)
		FREEMEM(st);
}


/*
 * Local internal type passed around as an argument.
 */
struct xdp_arg_s {
	asn_TYPE_descriptor_t *type_descriptor;
	void *struct_key;
	int decoded_something;
	int want_more;
};

