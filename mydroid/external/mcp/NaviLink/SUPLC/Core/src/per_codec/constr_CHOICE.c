/*
 * Copyright (c) 2003, 2004, 2005, 2006 Lev Walkin <vlm@lionet.info>.
 * All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/constr_CHOICE.h"

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
 * See the definitions.
 */
static int _fetch_present_idx(const void *struct_ptr, int off, int size);
static void _set_present_idx(void *sptr, int offset, int size, int pres);


/*
 * The decoder of the CHOICE type.
 */
int
CHOICE_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	asn_CHOICE_specifics_t *specs = (asn_CHOICE_specifics_t *)td->specifics;
	int present;

	if(!sptr) {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	/*
	 * Figure out which CHOICE element is encoded.
	 */
	present = _fetch_present_idx(sptr, specs->pres_offset,specs->pres_size);
	if(present > 0 && present <= td->elements_count) {
		asn_TYPE_member_t *elm = &td->elements[present-1];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) {
				if(elm->optional)
					return 0;
				_ASN_CTFAIL(app_key, td,
					"%s: mandatory CHOICE element %s absent (%s:%d)",
					td->name, elm->name, __FILE__, __LINE__);
				return -1;
			}
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		if(elm->memb_constraints) {
			return elm->memb_constraints(elm->type, memb_ptr,
				ctfailcb, app_key);
		} else {
			int ret = elm->type->check_constraints(elm->type,
					memb_ptr, ctfailcb, app_key);
			/*
			 * Cannot inherit it eralier:
			 * need to make sure we get the updated version.
			 */
			elm->memb_constraints = elm->type->check_constraints;
			return ret;
		}
	} else {
		_ASN_CTFAIL(app_key, td,
			"%s: no CHOICE element given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

asn_dec_rval_t
CHOICE_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_CHOICE_specifics_t *specs = (asn_CHOICE_specifics_t *)td->specifics;
	asn_dec_rval_t rv;
	asn_per_constraint_t *ct;
	asn_TYPE_member_t *elm;	/* CHOICE's element */
	void *memb_ptr;
	void **memb_ptr2;
	void *st = *sptr;
	int value;

	if(_ASN_STACK_OVERFLOW_CHECK(opt_codec_ctx))
		_ASN_DECODE_FAILED;

	/*
	 * Create the target structure if it is not present already.
	 */
	if(!st) {
		st = *sptr = CALLOC(1, specs->struct_size);
		if(!st) _ASN_DECODE_FAILED;
	}

	if(constraints) ct = &constraints->value;
	else if(td->per_constraints) ct = &td->per_constraints->value;
	else ct = 0;

	if(ct && ct->flags & APC_EXTENSIBLE) {
		value = per_get_few_bits(pd, 1);
		if(value < 0) _ASN_DECODE_STARVED;
		if(value) ct = 0;	/* Not restricted */
	}

	if(ct && ct->range_bits >= 0) {
		value = per_get_few_bits(pd, ct->range_bits);
		if(value < 0) _ASN_DECODE_STARVED;
		ASN_DEBUG("CHOICE %s got index %d in range %d",
			td->name, value, ct->range_bits);
		if(value > ct->upper_bound)
			_ASN_DECODE_EINVAL;
	} else {
		if(specs->ext_start == -1)
			_ASN_DECODE_FAILED;
		value = uper_get_nsnnwn(pd);
		if(value < 0) _ASN_DECODE_STARVED;
		value += specs->ext_start;
		if(value >= td->elements_count)
			_ASN_DECODE_FAILED;
		ASN_DEBUG("NOT IMPLEMENTED YET");
		_ASN_DECODE_FAILED;
	}

	/* Adjust if canonical order is different from natural order */
	if(specs->canonical_order)
		value = specs->canonical_order[value];

	/* Set presence to be able to free it later */
	_set_present_idx(st, specs->pres_offset, specs->pres_size, value + 1);

	elm = &td->elements[value];
	if(elm->flags & ATF_POINTER) {
		/* Member is a pointer to another structure */
		memb_ptr2 = (void **)((char *)st + elm->memb_offset);
	} else {
		memb_ptr = (char *)st + elm->memb_offset;
		memb_ptr2 = &memb_ptr;
	}
	ASN_DEBUG("Discovered CHOICE %s encodes %s", td->name, elm->name);

	rv = elm->type->uper_decoder(opt_codec_ctx, elm->type,
			elm->per_constraints, memb_ptr2, pd);
	if(rv.code != RC_OK)
	{
		ASN_DEBUG("Failed to decode %s in %s (CHOICE)",elm->name, td->name);
	}
	return rv;
}
   
asn_enc_rval_t
CHOICE_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	asn_CHOICE_specifics_t *specs = (asn_CHOICE_specifics_t *)td->specifics;
	asn_TYPE_member_t *elm;	/* CHOICE's element */
	asn_per_constraint_t *ct;
	void *memb_ptr;
	int present;

	if(!sptr) _ASN_ENCODE_FAILED;

	ASN_DEBUG("Encoding %s as CHOICE", td->name);

	if(constraints) ct = &constraints->value;
	else if(td->per_constraints) ct = &td->per_constraints->value;
	else ct = 0;

	present = _fetch_present_idx(sptr,
		specs->pres_offset, specs->pres_size);

	/*
	 * If the structure was not initialized properly, it cannot be encoded:
	 * can't deduce what to encode in the choice type.
	 */
	if(present <= 0 || present > td->elements_count)
		_ASN_ENCODE_FAILED;
	else
		present--;

	/* Adjust if canonical order is different from natural order */
	if(specs->canonical_order)
		present = specs->canonical_order[present];

	ASN_DEBUG("Encoding %s CHOICE element %d", td->name, present);

	if(ct && ct->range_bits >= 0) {
		if(present < ct->lower_bound
		|| present > ct->upper_bound) {
			if(ct->flags & APC_EXTENSIBLE) {
				if(per_put_few_bits(po, 1, 1))
					_ASN_ENCODE_FAILED;
			} else {
				_ASN_ENCODE_FAILED;
			}
			ct = 0;
		}
	}
	if(ct && ct->flags & APC_EXTENSIBLE)
		if(per_put_few_bits(po, 0, 1))
			_ASN_ENCODE_FAILED;

	if(ct && ct->range_bits >= 0) {
		if(per_put_few_bits(po, present, ct->range_bits))
			_ASN_ENCODE_FAILED;
	} else {
		if(specs->ext_start == -1)
			_ASN_ENCODE_FAILED;
		if(uper_put_nsnnwn(po, present - specs->ext_start))
			_ASN_ENCODE_FAILED;
		ASN_DEBUG("NOT IMPLEMENTED YET");
		_ASN_ENCODE_FAILED;
	}

	elm = &td->elements[present];
	if(elm->flags & ATF_POINTER) {
		/* Member is a pointer to another structure */
		memb_ptr = *(void **)((char *)sptr + elm->memb_offset);
		if(!memb_ptr) _ASN_ENCODE_FAILED;
	} else {
		memb_ptr = (char *)sptr + elm->memb_offset;
	}

	return elm->type->uper_encoder(elm->type, elm->per_constraints,
			memb_ptr, po);
}
   

int
CHOICE_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	asn_CHOICE_specifics_t *specs = (asn_CHOICE_specifics_t *)td->specifics;
	int present;

	if(!sptr) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/*
	 * Figure out which CHOICE element is encoded.
	 */
	present = _fetch_present_idx(sptr, specs->pres_offset,specs->pres_size);

	/*
	 * Print that element.
	 */
	if(present > 0 && present <= td->elements_count) {
		asn_TYPE_member_t *elm = &td->elements[present-1];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		/* Print member's name and stuff */
		if(0) {
			if(cb(elm->name, strlen(elm->name), app_key) < 0
			|| cb(": ", 2, app_key) < 0)
				return -1;
		}

		return elm->type->print_struct(elm->type, memb_ptr, ilevel,
			cb, app_key);
	} else {
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
	}
}

void
CHOICE_free(asn_TYPE_descriptor_t *td, void *ptr, int contents_only) {
	asn_CHOICE_specifics_t *specs = (asn_CHOICE_specifics_t *)td->specifics;
	int present;

	if(!td || !ptr)
		return;

	ASN_DEBUG("Freeing %s as CHOICE", td->name);

	/*
	 * Figure out which CHOICE element is encoded.
	 */
	present = _fetch_present_idx(ptr, specs->pres_offset, specs->pres_size);

	/*
	 * Free that element.
	 */
	if(present > 0 && present <= td->elements_count) {
		asn_TYPE_member_t *elm = &td->elements[present-1];
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


/*
 * The following functions functions offer protection against -fshort-enums,
 * compatible with little- and big-endian machines.
 * If assertion is triggered, either disable -fshort-enums, or add an entry
 * here with the ->pres_size of your target stracture.
 * Unless the target structure is packed, the ".present" member
 * is guaranteed to be aligned properly. ASN.1 compiler itself does not
 * produce packed code.
 */
static int
_fetch_present_idx(const void *struct_ptr, int pres_offset, int pres_size) {
	const void *present_ptr;
	int present;

	present_ptr = ((const char *)struct_ptr) + pres_offset;

	switch(pres_size) {
	case sizeof(int):	present =   *(const int *)present_ptr; break;
	case sizeof(short):	present = *(const short *)present_ptr; break;
	case sizeof(char):	present =  *(const char *)present_ptr; break;
	default:
		/* ANSI C mandates enum to be equivalent to integer */
		assert(pres_size != sizeof(int));
		return 0;	/* If not aborted, pass back safe value */
	}

	return present;
}

static void
_set_present_idx(void *struct_ptr, int pres_offset, int pres_size, int present) {
	void *present_ptr;
	present_ptr = ((char *)struct_ptr) + pres_offset;

	switch(pres_size) {
	case sizeof(int):	*(int *)present_ptr   = present; break;
	case sizeof(short):	*(short *)present_ptr = present; break;
	case sizeof(char):	*(char *)present_ptr  = present; break;
	default:
		/* ANSI C mandates enum to be equivalent to integer */
		assert(pres_size != sizeof(int));
	}
}
