To create an image h file:

To create RGB565:
From JPG/BMP565:
JPG to BMP: Create Windows BMP RGB565 428x240 from jpg by GIMP
# dd if=logo_428x240.bmp of=logo_428x240.rgb565 skip=70 bs=1

From PNG:
# ffmpeg -vcodec png -i logo.png -vcodec rawvideo -f rawvideo -pix_fmt rgb565 logo_428x240.rgb565

Covert RGB565 to YCbCr to h file:
# ./rgb2ycbcr < logo_428x240.rgb565 | ./bin2buf > logo_428x240.ycbcr.h

Convert to RGB565 to h file:
# cat logo_428x240.rgb565 | ./bin2buf > logo_428x240.rgb565.h


In the c code:
static const u8 logo_rgb[] =
{
#include "logo_428x240.rgb565.h"
};

Where the data is arranged: LSB_PIX1, MSB_PIX1, LSB_PIX2, MSB_PIX2, LSB_PIX3...


