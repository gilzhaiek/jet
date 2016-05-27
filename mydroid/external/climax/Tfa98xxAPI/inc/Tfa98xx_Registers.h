#ifndef TFA98XX_REGISTERS_H
#define TFA98XX_REGISTERS_H

#ifdef __cplusplus
extern "C"
{
#endif

#define TFA98XX_STATUS         (unsigned short)0x00

#define TFA98XX_MTP            (unsigned short)0x80

/* STATUS bits */
#define TFA98XX_STATUS_VDDS       (1<<0) /*  */
#define TFA98XX_STATUS_PLLS       (1<<1) /* plls locked */
#define TFA98XX_STATUS_OTDS       (1<<2) /*  */
#define TFA98XX_STATUS_OVDS       (1<<3) /*  */
#define TFA98XX_STATUS_UVDS       (1<<4) /*  */
#define TFA98XX_STATUS_OCDS       (1<<5) /*  */
#define TFA98XX_STATUS_CLKS       (1<<6) /* clocks stable */
//
#define TFA98XX_STATUS_MTPB       (1<<8) /* MTP operation busy */
//
#define TFA98XX_STATUS_DCCS       (1<<9) /*  */

#define TFA98XX_STATUS_ACS        (1<<11) /* cold started */
#define TFA98XX_STATUS_SWS        (1<<12) /* amplifier switching */

/* MTP bits */
#define TFA98XX_MTP_MTPOTC        (1<<0)  /* one time calibration */
#define TFA98XX_MTP_MTPEX         (1<<1)  /* one time calibration done */

/*
 * generated defines
 */
#define TFA98XX_STATUSREG (0x00)
#define TFA98XX_BATTERYVOLTAGE (0x01)
#define TFA98XX_TEMPERATURE (0x02)
#define TFA98XX_I2SREG (0x04)
#define TFA98XX_BAT_PROT (0x05)
#define TFA98XX_AUDIO_CTR (0x06)
#define TFA98XX_DCDCBOOST (0x07)
#define TFA98XX_SPKR_CALIBRATION (0x08)
#define TFA98XX_SYS_CTRL (0x09)
#define TFA98XX_I2S_SEL_REG (0x0a)
#define TFA98XX_REVISIONNUMBER (0x03)
#define TFA98XX_HIDE_UNHIDE_KEY (0x40)
#define TFA98XX_PWM_CONTROL (0x41)
#define TFA98XX_CURRENTSENSE1 (0x46)
#define TFA98XX_CURRENTSENSE2 (0x47)
#define TFA98XX_CURRENTSENSE3 (0x48)
#define TFA98XX_CURRENTSENSE4 (0x49)
#define TFA98XX_ABISTTEST (0x4c)
#define TFA98XX_RESERVE1 (0x0c)
#define TFA98XX_MTP_COPY (0x62)
#define TFA98XX_CF_CONTROLS (0x70)
#define TFA98XX_CF_MAD (0x71)
#define TFA98XX_CF_MEM (0x72)
#define TFA98XX_CF_STATUS (0x73)
#define TFA98XX_RESERVE2 (0x0d)

#ifdef __cplusplus
}
#endif

#endif // TFA98XX_REGISTERS_H

