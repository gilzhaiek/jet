#include <Tfa98xx.h>
#include "Tfa98xx_internals.h"

Tfa98xx_Error_t Tfa98xx_specific(Tfa98xx_handle_t handle)
{
        Tfa98xx_Error_t error;
        unsigned short value;

        if (!handle_is_open(handle))
                return Tfa98xx_Error_NotOpen;

        /* reset all i2C registers to default */
        error = Tfa98xx_WriteRegister16(handle, 0x09, 0x0002);

        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_ReadRegister16(handle, 0x09, &value);
        }
        if (Tfa98xx_Error_Ok == error) {
                /* DSP must be in control of the amplifier to avoid plops */
                value |= TFA98XX_SYSCTRL_SEL_ENBL_AMP;
                error = Tfa98xx_WriteRegister16(handle, 0x09, value);
        }

        /* some other registers must be set for optimal amplifier behaviour */
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x40, 0x5A6B);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x05, 0x13AB);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x06, 0x001F);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x08, 0x3C4E);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x09, 0x025D);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x0A, 0x3EC3);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x41, 0x0308);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x48, 0x0180);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x49, 0x0E82);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x52, 0x0000);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x40, 0x0000);
        }

        return error;
}

