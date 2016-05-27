/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#ifndef	_NativeEnumerated_H_
#define	_NativeEnumerated_H_

#include "NativeInteger.h"

#ifdef __cplusplus
extern "C" {
#endif

extern asn_TYPE_descriptor_t asn_DEF_NativeEnumerated;

per_type_decoder_f NativeEnumerated_decode_uper;
per_type_encoder_f NativeEnumerated_encode_uper;

#ifdef __cplusplus
}
#endif

#endif	/* _NativeEnumerated_H_ */
