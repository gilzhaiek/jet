/*-
 * Copyright (c) 2004 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
/*
 * This type differs from the standard INTEGER in that it is modelled using
 * the fixed machine type (long, int, short), so it can hold only values of
 * limited length. There is no type (i.e., NativeInteger_t, any integer type
 * will do).
 * This type may be used when integer range is limited by subtype constraints.
 */
#ifndef	_NativeInteger64_H_
#define	_NativeInteger64_H_

#include "asn_application.h"
#include "INTEGER64.h"

#ifdef __cplusplus
extern "C" {
#endif

extern asn_TYPE_descriptor_t asn_DEF_NativeInteger64;

asn_struct_free_f  Native64Integer_free;
asn_struct_print_f Native64Integer_print;
per_type_decoder_f Native64Integer_decode_uper;
per_type_encoder_f Native64Integer_encode_uper;

#ifdef __cplusplus
}
#endif

#endif	/* _NativeInteger64_H_ */
