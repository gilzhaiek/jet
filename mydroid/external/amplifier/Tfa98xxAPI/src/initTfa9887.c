#include <Tfa98xx.h>
#include "Tfa98xx_Registers.h"

Tfa98xx_Error_t Tfa9887_specific(Tfa98xx_handle_t handle)
{
        Tfa98xx_Error_t error = Tfa98xx_Error_Ok;
        unsigned short value;

        if (!handle_is_open(handle))
                return Tfa98xx_Error_NotOpen;

        /* all i2C registers are already set to default */

        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_ReadRegister16(handle, 0x09, &value);
        }
        if (Tfa98xx_Error_Ok == error) {
                /* DSP must be in control of the amplifier to avoid plops */
                value |= TFA98XX_SYS_CTRL_AMPE_POS;
                error = Tfa98xx_WriteRegister16(handle, 0x09, value);
        }

        /* some other registers must be set for optimal amplifier behaviour */
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x05, 0x13AB);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x06, 0x001F);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x08, 0x3C4E);
        }
        if (Tfa98xx_Error_Ok == error) { /*TFA98XX_SYSCTRL_DCA=0*/
                error = Tfa98xx_WriteRegister16(handle, 0x09, 0x024D);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x0A, 0x3EC3);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x41, 0x0308);
        }
        if (Tfa98xx_Error_Ok == error) {
                error = Tfa98xx_WriteRegister16(handle, 0x49, 0x0E82);
        }

        return error;
}

