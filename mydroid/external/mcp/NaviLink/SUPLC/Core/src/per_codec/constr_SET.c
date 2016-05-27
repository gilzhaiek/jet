/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#if defined(WINXP) | defined(WINCE)
#include <winsock2.h>	/* for ntohl() */
#else
//#include <netinet/in.h>	/* for ntohl() */
#include <sys/endian.h>	/* for ntohl() */
#endif


#include "per_codec/asn_internal.h"
#include "per_codec/constr_SET.h"


/* Check that all the mandatory members are present */
static int _SET_is_populated(asn_TYPE_descriptor_t *td, void *st);

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
#define	NEXT_PHASE(ctx)	do {			\
		ctx->phase++;			\
		ctx->step = 0;			\
	} while(0)

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
 * The decoder of the SET type.
 */
static int
_SET_is_populated(asn_TYPE_descriptor_t *td, void *st) {
	asn_SET_specifics_t *specs = (asn_SET_specifics_t *)td->specifics;
	int edx;

	/*
	 * Check that all mandatory elements are present.
	 */
	for(edx = 0; edx < td->elements_count;
		edx += (8 * sizeof(specs->_mandatory_elements[0]))) {
		unsigned int midx, pres, must;

		midx = edx/(8 * sizeof(specs->_mandatory_elements[0]));
		pres = ((unsigned int *)((char *)st+specs->pres_offset))[midx];
		must = ntohl(specs->_mandatory_elements[midx]);

		if((pres & must) == must) {
			/*
			 * Yes, everything seems to be in place.
			 */
		} else {
			ASN_DEBUG("One or more mandatory elements "
				"of a SET %s %d (%08x.%08x)=%08x "
				"are not present",
				td->name,
				midx,
				pres,
				must,
				(~(pres & must) & must)
			);
			return 0;
		}
	}

	return 1;
}


int
SET_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	int edx;
	int ret;

	if(!sptr) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/* Dump preamble */
	if(cb(td->name, strlen(td->name), app_key) < 0
	|| cb(" ::= {", 6, app_key) < 0)
		return -1;

	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) {
				if(elm->optional) continue;
				/* Print <absent> line */
				/* Fall through */
			}
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		_i_INDENT(1);

		/* Print the member's name and stuff */
		if(cb(elm->name, strlen(elm->name), app_key) < 0
		|| cb(": ", 2, app_key) < 0)
			return -1;

		/* Print the member itself */
		ret = elm->type->print_struct(elm->type, memb_ptr, ilevel + 1,
			cb, app_key);
		if(ret) return ret;
	}

	ilevel--;
	_i_INDENT(1);

	return (cb("}", 1, app_key) < 0) ? -1 : 0;
}

void
SET_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {
	int edx;

	if(!td || !ptr)
		return;

	ASN_DEBUG("Freeing %s as SET", td->name);

	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		void *memb_ptr;
		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(void **)((char *)ptr + elm->memb_offset);
			if(memb_ptr)
				ASN_STRUCT_FREE(*elm->type, memb_ptr);
		} else {
			memb_ptr = (void *)((char *)ptr + elm->memb_offset);
			ASN_STRUCT_FREE_CONTENTS_ONLY(*elm->type, memb_ptr);
		}
	}

	if(!contents_only) {
		FREEMEM(ptr);
	}
}

int
SET_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	int edx;

	if(!sptr) {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	/*
	 * Iterate over structure members and check their validity.
	 */
	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) {
				if(elm->optional)
					continue;
				_ASN_CTFAIL(app_key, td,
				"%s: mandatory element %s absent (%s:%d)",
				td->name, elm->name, __FILE__, __LINE__);
				return -1;
			}
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		if(elm->memb_constraints) {
			int ret = elm->memb_constraints(elm->type, memb_ptr,
				ctfailcb, app_key);
			if(ret) return ret;
		} else {
			int ret = elm->type->check_constraints(elm->type,
				memb_ptr, ctfailcb, app_key);
			if(ret) return ret;
			/*
			 * Cannot inherit it earlier:
			 * need to make sure we get the updated version.
			 */
			elm->memb_constraints = elm->type->check_constraints;
		}
	}

	return 0;
}
