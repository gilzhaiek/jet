/*
 * (C) Copyright 2009
 * Texas Instruments, <www.ti.com>
 *
 * See file CREDITS for list of people who contributed to this
 * project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

#ifndef __TWL6030_H__
#define __TWL6030_H__

#include <common.h>
#include <i2c.h>

#define RECON_DEBUG	0

/* I2C chip addresses */
#define TWL6030_CHIP_PM		0x48

#define TWL6030_CHIP_USB	0x49
#define TWL6030_CHIP_ADC	0x49
#define TWL6030_CHIP_CHARGER	0x49
#define TWL6030_CHIP_PWM	0x49
#define TWL6030_CHIP_GASGAUGE	0x49

#define TWL6032_GPSELECT_ISB	0x35

#define MISC1			0xE4
#define VAC_MEAS		(1 << 2)
#define VBAT_MEAS		(1 << 1)
#define BB_MEAS			(1 << 0)

#define TWL6032_GPADC_CTRL2		0x2f
#define GPADC_CTRL2_CH18_SCALER_EN	(1 << 2)

/* LED Control */
#define PMC_PWM2ON			 0xBD
#define PMC_PWM2OFF			0xBE
#define PMC_TOGGLE3			0x92
#define PWM2EN_CLOCK_INPUT	 (1 << 5)
#define PWM2EN_SIGNAL		  (1 << 4)
#define PWM2DISABLE_SIGNAL	 (1 << 3)

/* POWER CONTROL REGISTERS */
#define PMC_MASTER_PHOENIX_START_CONDITION 0x1F
#define PMC_MASTER_STS_HW_CONDITIONS 0x21
#define PMC_MASTER_PHOENIX_DEV_ON 0x25

/*PHOENIX_START_CONDITION MASKS*/
#define STRT_ON_PLUG_DET (1 << 3)
#define STRT_ON_USB_ID (1 << 2)

// STS_HW_CONDITIONS MASKS
#define STS_PREQ3 (1 << 7)
#define STS_PREQ2 (1 << 6)
#define STS_PREQ1 (1 << 5)
#define STS_PLUG_DET (1 << 3)
#define STS_RPWRON (1 << 1)
#define STS_PWRON (1 << 0)

/*PHOENIX_DEV_ON MASKS*/
#define SW_RESET (1 << 6)
#define DEVOFF (1 << 0)

/* Battery CHARGER REGISTERS */
#define CONTROLLER_VSEL_COMP	0xDB
#define CONTROLLER_INT_MASK	0xE0
#define CONTROLLER_CTRL1	0xE1
#define CONTROLLER_WDG		0xE2
#define CONTROLLER_STAT1	0xE3
#define CHARGERUSB_INT_STATUS	0xE4
#define CHARGERUSB_INT_MASK	0xE5
#define CHARGERUSB_STATUS_INT1	0xE6
#define CHARGERUSB_STATUS_INT2	0xE7
#define CHARGERUSB_CTRL1	0xE8
#define CHARGERUSB_CTRL2	0xE9
#define CHARGERUSB_CTRL3	0xEA
#define CHARGERUSB_STAT1	0xEB
#define CHARGERUSB_VOREG	0xEC
#define CHARGERUSB_VICHRG	0xED
#define CHARGERUSB_CINLIMIT	0xEE
#define CHARGERUSB_CTRLLIMIT1	0xEF
#define CHARGERUSB_CTRLLIMIT2   0xF0

/* FUEL GAUGE REGISTERS */
#define FG_REG_00	0xC0
#define FG_REG_01	0xC1
#define FG_REG_02	0xC2
#define FG_REG_03	0xC3
#define FG_REG_04	0xC4
#define FG_REG_05	0xC5
#define FG_REG_06	0xC6
#define FG_REG_07	0xC7
#define FG_REG_08	0xC8
#define FG_REG_09	0xC9
#define FG_REG_10	0xCA
#define FG_REG_11	0xCB

/*CONTROLLER_VSEL_COMP MASKS*/
#define VBAT_SHORT_2P8	0x3
#define VBAT_SHORT_2P1	0x00
#define VBATFULL_CHRG_3P35	(0x7<<2)
#define VBATFULL_CHRG_2P65	0x0
/*CONTROLLER_WDG*/
#define CONTROLLER_WDG_RESET	  0x0
/* CHARGERUSB_VICHRG */
/* Values for POP = 1 */
#define CHARGERUSB_VICHRG_300		0xF
#define CHARGERUSB_VICHRG_400		0x3
#define CHARGERUSB_VICHRG_500		0x4
#define CHARGERUSB_VICHRG_600		0x5
#define CHARGERUSB_VICHRG_800		0x7
#define CHARGERUSB_VICHRG_900		0x08
#define CHARGERUSB_VICHRG_1500		0xE
/* CHARGERUSB_CINLIMIT */
#define CHARGERUSB_CIN_LIMIT_100	0x1
#define CHARGERUSB_CIN_LIMIT_300	0x5
#define CHARGERUSB_CIN_LIMIT_400	0x7
#define CHARGERUSB_CIN_LIMIT_500	0x9
#define CHARGERUSB_CIN_LIMIT_550	0xA
#define CHARGERUSB_CIN_LIMIT_750	0x0E
#define CHARGERUSB_CIN_LIMIT_900	0x28
#define CHARGERUSB_CIN_LIMIT_1000	0x29
#define CHARGERUSB_CIN_LIMIT_NONE	0xF
/* CHARGERUSB_CTRLLIMIT1 */
#define VOREGL_4P2					  0x23
/* CHARGERUSB_CTRLLIMIT2 */
#define LOCK_LIMIT (1 << 4)
#define VICHRGL_900 0x08
#define VICHRGL_800 0x07
#define VICHRGL_700 0x06
#define VICHRGL_500 0x04
#define VICHRGL_400 0x03
#define VICHRGL_100 0x00
/* CONTROLLER_INT_MASK */
#define MVAC_FAULT		(1 << 6)
#define MAC_EOC			(1 << 5)
#define MBAT_REMOVED	(1 << 4)
#define MFAULT_WDG		(1 << 3)
#define MBAT_TEMP		(1 << 2)
#define MVBUS_DET		(1 << 1)
#define MVAC_DET		(1 << 0)
/* CHARGERUSB_INT_MASK */
#define MASK_MCURRENT_TERM		(1 << 3)
#define MASK_MCHARGERUSB_STAT		(1 << 2)
#define MASK_MCHARGERUSB_THMREG		(1 << 1)
#define MASK_MCHARGERUSB_FAULT		(1 << 0)
/* CHARGERUSB_VOREG */
#define CHARGERUSB_VOREG_3P52		0x01
#define CHARGERUSB_VOREG_4P0		0x19
#define CHARGERUSB_VOREG_4P2		0x23
#define CHARGERUSB_VOREG_4P76		0x3F
/* CHARGERUSB_CTRL1 */
#define CHARGERUSB_CTRL1_HZ_MODE	(1 << 5)
/* CHARGERUSB_CTRL2 */
#define CHARGERUSB_CTRL2_VITERM_50	(0 << 5)
#define CHARGERUSB_CTRL2_VITERM_100	(1 << 5)
#define CHARGERUSB_CTRL2_VITERM_150	(2 << 5)
#define CHARGERUSB_CTRL2_VITERM_400	(7 << 5)
/* CONTROLLER_CTRL1 */
#define CONTROLLER_CTRL1_EN_CHARGER	(1 << 4)
#define CONTROLLER_CTRL1_SEL_CHARGER	(1 << 3)

/* CONTROLLER_STAT1 */
#define CHRG_EXTCHRG_STATZ	(1 << 7)
#define CHRG_DET_N		(1 << 5)
#define VAC_DET			(1 << 3)
#define VBUS_DET		(1 << 2)

/* LINEAR CHARGER STATUS */
#define LINEAR_CHRG_STS			0xDE
#define LINEAR_CHRG_STS_DPPM_STS	0x04
#define LINEAR_CHRG_STS_CV_STS		0x02
#define LINEAR_CHRG_STS_CC_STS		0x01
#define LINEAR_CHRG_STS_END_OF_CHARGE	0x20

#define TOGGLE1		0x90
#define FGS		(1 << 5)
#define FGR		(1 << 4)
#define GPADCS		(1 << 1)
#define GPADCR		(1 << 0)

#define TWL6032_CTRL_P1		0x36
#define CTRL_P1_SP1	(1 << 3)

#define CTRL_P2		0x34
#define CTRL_P2_SP2	(1 << 2)
#define CTRL_P2_EOCP2	(1 << 1)
#define CTRL_P2_BUSY	(1 << 0)

#define TWL6030_GPADC_EOC_SW		(1 << 1)
#define TWL6030_GPADC_BUSY		(1 << 0)

#define GPADC_CTRL_ISOURCE_EN		(1 << 7)

#define GPADC_CTRL	0x2e

#define GPCH0_LSB	0x57
#define GPCH0_MSB	0x58

#define TWL6032_GPCH0_LSB	0x3b
#define TWL6032_GPCH0_MSB	0x3c

#define VUSB_CFG_STATE		0xA2
#define MISC2			0xE5

#define USB_PRODUCT_ID_LSB	0x02

#define TWL6030_GPADC_VBAT_CHNL	0x07
#define TWL6032_GPADC_VBAT_CHNL	0x12

#define BATTERY_RESISTOR	10000
#define SIMULATOR_RESISTOR	5000
#define BATTERY_DETECT_THRESHOLD	((BATTERY_RESISTOR + SIMULATOR_RESISTOR) / 2)

#define GPADC_ISOURCE_22uA		22
#define GPADC_ISOURCE_7uA		7


#define TWL6032_GPADC_VBAT_TEMP_CHNL	0x1

typedef enum {
	chip_TWL6030,
	chip_TWL6032,
	chip_TWL603X_cnt
}t_TWL603X_chip_type;

typedef struct{
	t_TWL603X_chip_type twl_chip_type;
	u8 rbase;
	u8 ctrl;
	u8 enable;
}t_twl6030_gpadc_data;

#define SHUTDOWN	0
#define BOOT_CHARGER	1
#define BOOT_ANDROID	2

void twl6030_init_battery_charging(void);
void twl6030_usb_device_settings(void);
int twl6032_start_usb_charging(unsigned char *boot_state_ptr);
void twl6030_start_usb_charging();
int twl6032_get_battery_voltage(void);
int twl6032_get_battery_current(void);
int twl6030_get_battery_voltage(t_twl6030_gpadc_data * gpadc);
int twl6032_get_battery_temp(void);
int is_charging_ended(void);
void twl6030_power_off(void);
void twl6032_set_usb_hw_charging(int mode);
#endif /* __TWL6030_H__ */


