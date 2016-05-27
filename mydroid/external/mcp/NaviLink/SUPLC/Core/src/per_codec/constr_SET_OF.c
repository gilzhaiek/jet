/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/constr_SET_OF.h"
#include "per_codec/asn_SET_OF.h"

/*
 * Number of bytes left for this structure.
 * (ctx->left) indicates the number of bytes _transferred_ for the structure.
 * (size) contains the number of bytes in the buffer passed.
 */
#define	LEFT	((size<(size_t)ctx->left)?size:(size_t)ctx->left)

/*
 * If the subprocessor function returns with an indication that it wants
 * more data, it may well be a fatal decoding problem, because the
 * size is constrained by the <TLV>'s L, even if the buffer size allows
 * reading more data.
 * For example, consider the buffer containing the following TLVs:
 * <T:5><L:1><V> <T:6>...
 * The TLV length clearly indicates that one byte is expected in V, but
 * if the V processor returns with "want more data" even if the buffer
 * contains way more data than the V processor have seen.
 */
#define	SIZE_VIOLATION	(ctx->left >= 0 && (size_t)ctx->left <= size)

/*
 * This macro "eats" the part of the buffer which is definitely "consumed",
 * i.e. was correctly converted into local representation or rightfully skipped.
 */
#undef	ADVANCE
#define	ADVANCE(num_bytes)	do {		\
		size_t num = num_bytes;		\
		ptr = ((const char *)ptr) + num;\
		size -= num;			\
		if(ctx->left >= 0)		\
			ctx->left -= num;	\
		consumed_myself += num;		\
	} while(0)

/*
 * Switch to the next phase of parsing.
 */
#undef	NEXT_PHASE
#undef	PHASE_OUT
#define	NEXT_PHASE(ctx)	do {			\
		ctx->phase++;			\
		ctx->step = 0;			\
	} while(0)
#define	PHASE_OUT(ctx)	do { ctx->phase = 10; } while(0)

/*
 * Return a standardized complex structure.
 */
#undef	RETURN
#define	RETURN(_code)	do {			\
		rval.code = _code;		\
		rval.consumed = consumed_myself;\
		return rval;			\
	} while(0)

/*
 * The decoder of the SET OF type.
 */

/*
 * Internally visible buffer holding a single encoded element.
 */
struct _el_buffer {
	uint8_t *buf;
	size_t length;
	size_t size;
};
/* Append bytes to the above structure */
static int _el_addbytes(const void *buffer, size_t size, void *el_buf_ptr) {
	struct _el_buffer *el_buf = (struct _el_buffer *)el_buf_ptr;

	if(el_buf->length + size > el_buf->size)
		return -1;

	memcpy(el_buf->buf + el_buf->length, buffer, size);

	el_buf->length += size;
	return 0;
}
static int _el_buf_cmp(const void *ap, const void *bp) {
	const struct _el_buffer *a = (const struct _el_buffer *)ap;
	const struct _el_buffer *b = (const struct _el_buffer *)bp;
	int ret;
	size_t common_len;

	if(a->length < b->length)
		common_len = a->length;
	else
		common_len = b->length;

	ret = memcmp(a->buf, b->buf, common_len);
	if(ret == 0) {
		if(a->length < b->length)
			ret = -1;
		else if(a->length > b->length)
			ret = 1;
	}

	return ret;
}

int
SET_OF_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	asn_TYPE_member_t *elm = td->elements;
	const asn_anonymous_set_ *list = _A_CSET_FROM_VOID(sptr);
	int ret;
	int i;

	if(!sptr) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/* Dump preamble */
	if(cb(td->name, strlen(td->name), app_key) < 0
	|| cb(" ::= {", 6, app_key) < 0)
		return -1;

	for(i = 0; i < list->count; i++) {
		const void *memb_ptr = list->array[i];
		if(!memb_ptr) continue;

		_i_INDENT(1);

		ret = elm->type->print_struct(elm->type, memb_ptr,
			ilevel + 1, cb, app_key);
		if(ret) return ret;
	}

	ilevel--;
	_i_INDENT(1);

	return (cb("}", 1, app_key) < 0) ? -1 : 0;
}

void
SET_OF_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {
	if(td && ptr) {
		asn_TYPE_member_t *elm = td->elements;
		asn_anonymous_set_ *list = _A_SET_FROM_VOID(ptr);
		int i;

		/*
		 * Could not use set_of_empty() because of (*free)
		 * incompatibility.
		 */
		for(i = 0; i < list->count; i++) {
			void *memb_ptr = list->array[i];
			if(memb_ptr)
			ASN_STRUCT_FREE(*elm->type, memb_ptr);
		}
		list->count = 0;	/* No meaningful elements left */

		asn_set_empty(list);	/* Remove (list->array) */

		if(!contents_only) {
			FREEMEM(ptr);
		}
	}
}

int
SET_OF_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	asn_TYPE_member_t *elm = td->elements;
	asn_constr_check_f *constr;
	const asn_anonymous_set_ *list = _A_CSET_FROM_VOID(sptr);
	int i;

	if(!sptr) {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	constr = elm->memb_constraints;
	if(!constr) constr = elm->type->check_constraints;

	/*
	 * Iterate over the members of an array.
	 * Validate each in turn, until one fails.
	 */
	for(i = 0; i < list->count; i++) {
		const void *memb_ptr = list->array[i];
		int ret;

		if(!memb_ptr) continue;

		ret = constr(elm->type, memb_ptr, ctfailcb, app_key);
		if(ret) return ret;
	}

	/*
	 * Cannot inherit it eralier:
	 * need to make sure we get the updated version.
	 */
	if(!elm->memb_constraints)
		elm->memb_constraints = elm->type->check_constraints;

	return 0;
}

asn_dec_rval_t
SET_OF_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
        asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_dec_rval_t rv;
        asn_SET_OF_specifics_t *specs = (asn_SET_OF_specifics_t *)td->specifics;
	asn_TYPE_member_t *elm = td->elements;	/* Single one */
	void *st = *sptr;
	asn_anonymous_set_ *list;
	asn_per_constraint_t *ct;
	int repeat = 0;
	ssize_t nelems;

	if(_ASN_STACK_OVERFLOW_CHECK(opt_codec_ctx))
		_ASN_DECODE_FAILED;

	/*
	 * Create the target structure if it is not present already.
	 */
	if(!st) {
		st = *sptr = CALLOC(1, specs->struct_size);
		if(!st) _ASN_DECODE_FAILED;
	}                                                                       
	list = _A_SET_FROM_VOID(st);

	/* Figure out which constraints to use */
	if(constraints) ct = &constraints->size;
	else if(td->per_constraints) ct = &td->per_constraints->size;
	else ct = 0;

	if(ct && ct->flags & APC_EXTENSIBLE) {
		int value = per_get_few_bits(pd, 1);
		if(value < 0) _ASN_DECODE_STARVED;
		if(value) ct = 0;	/* Not restricted! */
	}

	if(ct && ct->effective_bits >= 0) {
		/* X.691, #19.5: No length determinant */
		nelems = per_get_few_bits(pd, ct->effective_bits);
		ASN_DEBUG("Preparing to fetch %ld+%ld elements from %s",
			(long)nelems, ct->lower_bound, td->name);
		if(nelems < 0)  _ASN_DECODE_STARVED;
		nelems += ct->lower_bound;
	} else {
		nelems = -1;
	}

	do {
		int i;
		if(nelems < 0) 
		{
			nelems = uper_get_length(pd, ct ? ct->effective_bits : -1, &repeat);
			ASN_DEBUG("Got to decode %d elements (eff %d)", (int)nelems, (int)ct ? ct->effective_bits : -1);
			if(nelems < 0) _ASN_DECODE_STARVED;
		}

		for(i = 0; i < nelems; i++) 
		{
			void *ptr = 0;
			ASN_DEBUG("SET OF %s decoding", elm->type->name);
			rv = elm->type->uper_decoder(opt_codec_ctx, elm->type, elm->per_constraints, &ptr, pd);
			ASN_DEBUG("%s SET OF %s decoded %d, %p", td->name, elm->type->name, rv.code, ptr);
			if(rv.code == RC_OK) 
			{
				if(ASN_SET_ADD(list, ptr) == 0) continue;
				ASN_DEBUG("Failed to add element into %s", td->name);
				/* Fall through */
				rv.code = RC_FAIL;
			} 
			else 
			{
				ASN_DEBUG("Failed decoding %s of %s (SET OF)", elm->type->name, td->name);
			}
			if(ptr) ASN_STRUCT_FREE(*elm->type, ptr);
			return rv;
		}

		nelems = -1;	/* Allow uper_get_length() */
	} while(repeat);

	ASN_DEBUG("Decoded %s as SET OF", td->name);

	rv.code = RC_OK;
	rv.consumed = 0;
	return rv;
}

