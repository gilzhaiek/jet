/*-
 * Copyright (c) 2003, 2004, 2006 Lev Walkin <vlm@lionet.info>.
 * All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/constr_SEQUENCE_OF.h"
#include "per_codec/asn_SEQUENCE_OF.h"

asn_enc_rval_t
SEQUENCE_OF_encode_uper(asn_TYPE_descriptor_t *td,
	asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {
	asn_anonymous_sequence_ *list;
	asn_per_constraint_t *ct;
	asn_enc_rval_t er;
	asn_TYPE_member_t *elm = td->elements;
	int seq;

	if(!sptr) _ASN_ENCODE_FAILED;
	list = _A_SEQUENCE_FROM_VOID(sptr);

	er.encoded = 0;

	ASN_DEBUG("Encoding %s as SEQUENCE OF (%d)", td->name, list->count);

	if(constraints) ct = &constraints->size;
	else if(td->per_constraints) ct = &td->per_constraints->size;
	else ct = 0;

	/* If extensible constraint, check if size is in root */
	if(ct) {
		int not_in_root = (list->count < ct->lower_bound
				|| list->count > ct->upper_bound);
		ASN_DEBUG("lb %ld ub %ld %s",
			ct->lower_bound, ct->upper_bound,
			ct->flags & APC_EXTENSIBLE ? "ext" : "fix");
		if(ct->flags & APC_EXTENSIBLE) {
			/* Declare whether size is in extension root */
			if(per_put_few_bits(po, not_in_root, 1))
				_ASN_ENCODE_FAILED;
			if(not_in_root) ct = 0;
		} else if(not_in_root && ct->effective_bits >= 0)
			_ASN_ENCODE_FAILED;
	}

	if(ct && ct->effective_bits >= 0) {
		/* X.691, #19.5: No length determinant */
		if(per_put_few_bits(po, list->count - ct->lower_bound,
				ct->effective_bits))
			_ASN_ENCODE_FAILED;
	}

	for(seq = -1; seq < list->count;) {
		ssize_t mayEncode;
		if(seq < 0) seq = 0;
		if(ct && ct->effective_bits >= 0) {
			mayEncode = list->count;
		} else {
			mayEncode = uper_put_length(po, list->count - seq);
			if(mayEncode < 0) _ASN_ENCODE_FAILED;
		}

		while(mayEncode--) {
			void *memb_ptr = list->array[seq++];
			if(!memb_ptr) _ASN_ENCODE_FAILED;
			er = elm->type->uper_encoder(elm->type,
				elm->per_constraints, memb_ptr, po);
			if(er.encoded == -1)
				_ASN_ENCODE_FAILED;
		}
	}

	_ASN_ENCODED_OK(er);
}

