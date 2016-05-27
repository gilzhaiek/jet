/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


#ifndef	_NativeInteger_H_
#define	_NativeInteger_H_

//#include "asn_application.h"
#include "INTEGER.h"

#ifdef __cplusplus
extern "C" {
#endif

extern asn_TYPE_descriptor_t asn_DEF_NativeInteger;

asn_struct_free_f  NativeInteger_free;
asn_struct_print_f NativeInteger_print;
per_type_decoder_f NativeInteger_decode_uper;
per_type_encoder_f NativeInteger_encode_uper;

#ifdef __cplusplus
}
#endif

#endif	/* _NativeInteger_H_ */
