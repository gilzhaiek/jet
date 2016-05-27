/*
 * Convert RGB565 image file to YCbCr
 *
 * 2011 Vladimir Barinov <batareich@gmail.com>
 * based on kopinfb rgb565->ycbcr conversion algorithm by
 *	Jimmy Huang <jimmy.huang@picoinstruments.com>
 *
 * This software may be used and distributed according to the terms
 * of the GNU General Public License, incorporated herein by reference.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define AVERAGE_RGB565(a, b)    (u16)( (a) == (b) ? (a)  : (((a) & 0xf7dfU) + ((b) & 0xf7dfU)) >> 1 )

#define SRC_WIDTH                       428
#define SRC_WIDTH_ALIGNED               (SRC_WIDTH + 4)
#define SRC_HEIGHT                      240

#define BACKBUF_SIZE            (SRC_WIDTH * SRC_HEIGHT * 2)
#define MIDDLEBUF_SIZE          (SRC_WIDTH_ALIGNED * SRC_HEIGHT * 2)

typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;

static void convert (u16* backBuf, u8* middleBufA)
{
	int w, h, r, g, b;
	u8  Yn = 0, Ynp1, Cb, Cr;
	u16 rgb565Pixel = 0, rgb565PrevPixel = 0;

	/* convert rgb565 in 'back' buffer to bt.656 YCrCb:422 in 'middle' buffer with conversion equations as:
		Y' = 16 +  (  65.738 * R + 129.057 * G +  25.064 * B) / 256
		Cb = 128 + ( -37.945 * R -  74.494 * G + 112.439 * B) / 256
		Cr = 128 + ( 112.439 * R -  94.154 * G -  18.285 * B) / 256
		Input RGB range = [0~255,0~255,0~255]
		Output Y'CbCr range = [16~235, 16~235,16~235]
		Note: the equations above gurrantee the output range of 16 ~ 235.

		Converted format: Cb0Y0Cr0Y1 Cb2Y2Cr2Y3 ...
	*/

//	printf("Converting RGB565 to BT.656 YCbCr ... \n");

	for(h = 0; h < SRC_HEIGHT; h++)
	{
		u32 	*middleBuf = NULL;
		middleBuf = (u32 *)(middleBufA + (SRC_HEIGHT - h - 1) * SRC_WIDTH_ALIGNED * 2);

		for(w = 0; w < SRC_WIDTH_ALIGNED; w++)
		{
			/* retrieve individual RGB components */
			rgb565Pixel = backBuf[w + SRC_WIDTH * h];
			r = (rgb565Pixel >> 8) & 0xF8;
			g = (rgb565Pixel >> 3) & 0xFC;
			b = (rgb565Pixel << 3) & 0xF8;
			/* convert rgb to luma component */
			Ynp1 = (u8)( 16 + (( 65 * r + 129 * g +  25 * b) >> 8));
			if(w & 1) rgb565Pixel = AVERAGE_RGB565(rgb565PrevPixel, rgb565Pixel);
			r = (rgb565Pixel >> 8) & 0xF8;
			g = (rgb565Pixel >> 3) & 0xFC;
			b = (rgb565Pixel << 3) & 0xF8;
			/* convert rgb to chroma components */
			Cb =   (u8)(128 - (( 37 * r +  74 * g - 112 * b) >> 8));
			Cr =   (u8)(128 + ((112 * r -  94 * g -  18 * b) >> 8));
			/* combine CbYCrY */
			if(w & 1)
			{
				middleBuf[w >> 1] = (Cb << 0) | (Yn << 8) | (Cr << 16) | (Ynp1 << 24);  // TODO - make sure 0xFF 0x00
			}
			/* remember the current pxiel values in order to combine CbYCrY in the next iteration */
			rgb565PrevPixel = rgb565Pixel;
			Yn = Ynp1;
		}
	}
}

int main(int argc, char *argv[])
{
	int ch, i = 0;
	u16* BackBuffr;
	u8* middleBufA;
	u8* buf;

	if (argc > 1)
		printf("const char %s[] %s=\n",
			argv[1], argc > 2 ? argv[2] : "");

	BackBuffr = malloc(BACKBUF_SIZE);
	if (BackBuffr == NULL)
		return -1;

	/* allocate memory for the 'middle' buffer */
	middleBufA = malloc(MIDDLEBUF_SIZE);
	if (middleBufA == NULL)
		return -1;

	/* fill black */
	memset(middleBufA, 0x80, MIDDLEBUF_SIZE);

	buf = (u8 *)BackBuffr;
	while ((ch = getchar()) != EOF)	{
		if (i > BACKBUF_SIZE) {
			printf("RGB 565 image larger then 428*240*2=205440\n");
			goto out;
		}
		i++;
		*buf = ch;
		buf++;
	}

	convert(BackBuffr, middleBufA);

	buf = middleBufA;
	for (i = 0; i < MIDDLEBUF_SIZE; i++) {
		printf("%c",*buf);
		buf++;
	}

out:
	free(BackBuffr);
	free(middleBufA);

	return 0;
}
