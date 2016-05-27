#include "common/endianess.h"

const int i = 1;
#define is_bigendian() ( (*(char*)&i) == 0 )

short RevShortFromShort (short s) {
	unsigned char c1, c2;

	if (is_bigendian()) {
		return s;
	} else {
		c1 = s & 255;
		c2 = (s >> 8) & 255;

		return (c1 << 8) + c2;
	}
}

short RevShortFromString (char *c) {
	short s;
	char *p = (char *)&s;

	if (is_bigendian()) {
		p[0] = c[0];
		p[1] = c[1];
	} else {
		p[0] = c[1];
		p[1] = c[0];
	}

	return s;
}
