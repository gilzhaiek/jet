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
#include <config.h>
#include <asm/io.h>
#ifdef CONFIG_TWL6030

#include <twl6030.h>

const int batt_table[] = {
	/* adc threshold,correponding temperature(0.01C) and slope */
	896, -2888, 16,
	882, -2678, 15,
	866, -2453, 14,
	846, -2193, 13,
	819, -1868, 12,
	780, -1439, 11,
	715, -792, 10,
	468, 1458, 9,
	393, 2203, 10,
	346, 2718, 11,
	311, 3136, 12,
	284, 3485, 13,
	262, 3792, 14,
	244, 4061, 15,
	228, 4315, 16,
	215, 4535, 17,
	203, 4750, 18,
	192, 4958, 19,
	183, 5137, 20,
	175, 5304, 21,
	167, 5479, 22,
	160, 5639, 23,
	154, 5782, 24,
	149, 5906, 25,
	143, 6061, 26,
	138, 6196, 27,
	134, 6307, 28,
	130, 6422, 29,
	122, 6663, 31,
	116, 6856, 33,
	110, 7061, 35,
};

/* Functions to read and write from TWL6030 */
static inline int twl6030_i2c_write_u8(u8 chip_no, u8 val, u8 reg)
{
	return i2c_write(chip_no, reg, 1, &val, 1);
}

static inline int twl6030_i2c_read_u8(u8 chip_no, u8 *val, u8 reg)
{
	return i2c_read(chip_no, reg, 1, val, 1);
}

static inline int twl6030_i2c_read(u8 chip_no, u8 *val, u8 reg, int len)
{
	return i2c_read(chip_no, reg, 1, val, len);
}

static int twl6030_gpadc_sw2_trigger(t_twl6030_gpadc_data * gpadc)
{
	u8 val;
	int ret = 0;

	ret = twl6030_i2c_write_u8(TWL6030_CHIP_ADC, gpadc->enable, gpadc->ctrl);
	if (ret)
		return -1;

	/* Waiting until the SW1 conversion ends*/
	val =  TWL6030_GPADC_BUSY;

	while (!((val & TWL6030_GPADC_EOC_SW) && (!(val & TWL6030_GPADC_BUSY)))) {
		ret = twl6030_i2c_read_u8(TWL6030_CHIP_ADC, &val, gpadc->ctrl);
		if (ret)
			return -1;
		udelay(1000);
	}

	return 0;
}

static int twl6030_gpadc_read_channel(t_twl6030_gpadc_data * gpadc, u8 channel_no)
{
	u8 lsb = 0;
	u8 msb = 0;
	int ret;
	u8 channel = channel_no;

	if (gpadc->twl_chip_type == chip_TWL6032) {
		ret = twl6030_i2c_write_u8(TWL6030_CHIP_ADC, channel_no,
				TWL6032_GPSELECT_ISB);
		if (ret)
			return -1;
	}

	ret = twl6030_gpadc_sw2_trigger(gpadc);
	if (ret)
		return ret;

	if (gpadc->twl_chip_type == chip_TWL6032)
		channel = 0;

	ret = twl6030_i2c_read_u8(TWL6030_CHIP_ADC, &lsb,
			gpadc->rbase + channel * 2);
	if (ret)
		return -1;

	ret = twl6030_i2c_read_u8(TWL6030_CHIP_ADC, &msb,
			gpadc->rbase + 1 + channel * 2);
	if (ret)
		return -1;

	return (msb << 8) | lsb;
}
//OMAP registers
#define CONTROL_USB2PHYCORE		0x4A100620
#define USBPHY_TX_TEST_CHRG_DET		0x4A0AD090
#define USBPHY_CHRG_DET			0x4A0AD094

#define USB2PHY_DISCHGDET		(1<<30)
#define USB2PHY_RESTARTCHGDET		(1<<15)
#define USB2PHY_CHGDETECTED		(1<<13)

#define CHARGER_TYPE_APPLE		0x1

#define CHARGER_WAIT_STATE		0x0
#define CHARGER_TYPE_PS2		0x2
#define CHARGER_TYPE_DEDICATED		0x4
#define CHARGER_TYPE_HOST		0x5
#define CHARGER_TYPE_PC			0x6
//USBPHY_TX_TEST_CHRG_DET MASKS
#define USB_PC_MASK 0xA0
//USBPHY_CHRG_DET MASKS

void twl6032_set_usb_hw_charging(int mode)
{
	u8 reg = 0;
	int ret;

	ret = twl6030_i2c_read_u8(TWL6030_CHIP_CHARGER, &reg, CHARGERUSB_CTRL1);
	if (ret) {
		printf("I2C error %d Unable to read CHARGEUSB_CTRL_1 register", ret);
		return;
	}

	if(mode) {
		/* Enable HW USB charging */
		reg  &= ~CHARGERUSB_CTRL1_HZ_MODE;
#if RECON_DEBUG
		printf("[%s:%u] Enable HW charging.\n",__FUNCTION__,__LINE__);
#endif
	}
	else {
		/* Disable HW USB charging */
		reg  |= CHARGERUSB_CTRL1_HZ_MODE;
#if RECON_DEBUG
		printf("[%s:%u] Disable HW charging.\n",__FUNCTION__,__LINE__);
#endif
	}

	ret = twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, reg, CHARGERUSB_CTRL1);
	if (ret) {
		printf("I2C error %d Unable to write HZ_MODE %d to CHARGEUSB_CTRL_1 register", ret,mode);
	}
}


int twl6032_init_usb_charging(unsigned char *boot_state_ptr)
{
	int vbus_draw = 0;
	unsigned char data,data1,data2;
	u32 chargertype,timeout;
	u32 usb2phycore=0;

	twl6030_i2c_read_u8(TWL6030_CHIP_USB, &data, CONTROLLER_STAT1);

	if (data&VBUS_DET){
		/* enable charger detection and restart it */
		usb2phycore = __raw_readl(CONTROL_USB2PHYCORE);
		usb2phycore &= ~USB2PHY_DISCHGDET;
		usb2phycore |= USB2PHY_RESTARTCHGDET;
		__raw_writel(usb2phycore, CONTROL_USB2PHYCORE);
		udelay (2000);
		usb2phycore = __raw_readl(CONTROL_USB2PHYCORE);
		usb2phycore &= ~USB2PHY_RESTARTCHGDET;
		__raw_writel(usb2phycore, CONTROL_USB2PHYCORE);
		//wait for charge type detection finish or timeout
		timeout=200;
		while(timeout)
		{
			udelay (10000);
			usb2phycore = __raw_readl(CONTROL_USB2PHYCORE);
			chargertype = ((usb2phycore >> 21) & 0x7);
			if (chargertype){
				// Give things time to settle.
				udelay(5000);
				usb2phycore = __raw_readl(CONTROL_USB2PHYCORE);
				chargertype = ((usb2phycore >> 21) & 0x7);
				break;
			}
			timeout--;
		}

		if(*boot_state_ptr == 0){
			//printf("boot to midkernel charge\n");
			*boot_state_ptr=BOOT_CHARGER;
		}
		switch(chargertype)
		{
			case CHARGER_TYPE_APPLE:
				data=CHARGERUSB_CIN_LIMIT_550;
#ifdef CONFIG_SUN
				data1=CHARGERUSB_VICHRG_400;
				vbus_draw = 400;
#else
				data1=CHARGERUSB_VICHRG_600;
				vbus_draw = 600;
#endif
				data2=VICHRGL_500;
				printf("APPLE CHARGE->limit 600mA Snow2/400mA Sun reg=0x%.2x\n",data);
			break;
			case CHARGER_TYPE_PC:
				//TODO: follow USB compliant, ask for only 100mA at beginning and turn to 500mA after successfull enumeration in low power uboot with usb stack running
				data=CHARGERUSB_CIN_LIMIT_500;
				data1=CHARGERUSB_VICHRG_500;
				data2=VICHRGL_500;
				printf("PC CHARGE->limit 500mA to start reg=0x%.2x\n",data);
				vbus_draw = 500;
			break;
			case CHARGER_TYPE_HOST:
			case CHARGER_TYPE_DEDICATED:
#ifdef CONFIG_SUN
				data=CHARGERUSB_CIN_LIMIT_1000;
				data1=CHARGERUSB_VICHRG_400;
				data2=VICHRGL_400;
				printf("WALL CHARGE->limit 400mA reg=0x%.2x\n",data);
				vbus_draw = 400;
#else
				data=CHARGERUSB_CIN_LIMIT_750;
				data1=CHARGERUSB_VICHRG_800;
				data2=VICHRGL_700;
				printf("WALL CHARGE->limit 800mA reg=0x%.2x\n",data);
				vbus_draw = 800;
#endif
			break;
			default:
				data=CHARGERUSB_CIN_LIMIT_500;
				data1=CHARGERUSB_VICHRG_500;
				data2=VICHRGL_500;
				printf("Unknown Charger->limit 500mA reg=0x%.2x,charge=%d\n",data,chargertype);
				vbus_draw = 500;
			break;
		}
	}
	else{
		data=CHARGERUSB_CIN_LIMIT_100;//by default set charging limit to 500mA?
		data1=CHARGERUSB_VICHRG_300;
		data2=VICHRGL_100;
	}
//unlcok first
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER,VICHRGL_700,CHARGERUSB_CTRLLIMIT2);
// Set max current from VBus
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, data, CHARGERUSB_CINLIMIT);//from charger
// Set max battery charge current 
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, data1,CHARGERUSB_VICHRG);  // to battery

// Set Hardware charge global Max voltage to 4.2V
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, VOREGL_4P2, CHARGERUSB_CTRLLIMIT1);

// Set CHARGERUSB_VOREG to 4.20V
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CHARGERUSB_VOREG_4P2,CHARGERUSB_VOREG);

// Set Hardware charge Limit 700mA and lock it, later on may be need to use VICHRGL_800
	if((*boot_state_ptr)==0)
		twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, (data2|LOCK_LIMIT),CHARGERUSB_CTRLLIMIT2);
	else
		twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, data2,CHARGERUSB_CTRLLIMIT2);
// reset watchdog time
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CONTROLLER_WDG_RESET ,CONTROLLER_WDG);

	//twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, MBAT_TEMP, CONTROLLER_INT_MASK);
	//twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, MASK_MCHARGERUSB_THMREG, CHARGERUSB_INT_MASK);
	return vbus_draw;
}

void twl6030_start_usb_charging(void)
{
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CHARGERUSB_VICHRG_1500, CHARGERUSB_VICHRG);
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CHARGERUSB_CIN_LIMIT_NONE, CHARGERUSB_CINLIMIT);
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, MBAT_TEMP, CONTROLLER_INT_MASK);
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, MASK_MCHARGERUSB_THMREG, CHARGERUSB_INT_MASK);
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CHARGERUSB_VOREG_4P0, CHARGERUSB_VOREG);
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CHARGERUSB_CTRL2_VITERM_100, CHARGERUSB_CTRL2);
	/* Enable USB charging */
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, CONTROLLER_CTRL1_EN_CHARGER, CONTROLLER_CTRL1);

	return;
}

int is_battery_present(t_twl6030_gpadc_data * gpadc)
{
	int bat_id_val;
	unsigned int current_src_val;
	u8 reg;
	int ret;

	bat_id_val = twl6030_gpadc_read_channel(gpadc, 0);
	if (bat_id_val < 0) {
		printf("Failed to read GPADC\n");
		return bat_id_val;
	}

	if (gpadc->twl_chip_type == chip_TWL6030)
		bat_id_val = (bat_id_val* 5 * 1000) >> (10 + 2);
	else
		bat_id_val = (bat_id_val * 5 * 1000) >> (12 + 2);

	ret = twl6030_i2c_read_u8(TWL6030_CHIP_ADC, &reg, GPADC_CTRL);
	if (ret) {
		printf("Failed to read GPADC\n");
		return -1;
	}

	current_src_val = (reg & GPADC_CTRL_ISOURCE_EN) ?
				GPADC_ISOURCE_22uA :
				GPADC_ISOURCE_7uA;

	bat_id_val = (bat_id_val * 1000) / current_src_val;

	if (bat_id_val < BATTERY_DETECT_THRESHOLD)
		return 0;

	return 1;
}

int twl6032_get_battery_temp(void)
{
	int i,size,temperature;
	int temp_adc;

	t_twl6030_gpadc_data gpadc;
	gpadc.twl_chip_type = chip_TWL6032;
	gpadc.rbase = TWL6032_GPCH0_LSB;
	gpadc.ctrl = TWL6032_CTRL_P1;
	gpadc.enable = CTRL_P1_SP1;

	temp_adc  = twl6030_gpadc_read_channel(&gpadc, TWL6032_GPADC_VBAT_TEMP_CHNL);
	size = sizeof(batt_table);

	/*Covert adc to real temperature data*/
	temp_adc >>=2;

	for (i = 0; i < size; ) {
		if (temp_adc >= batt_table[i])
			break;
		i+=3;
	}

	temperature= temp_adc-batt_table[i];
	temperature= temperature*batt_table[i+2];
	temperature= batt_table[i+1]-temperature;

	temperature /= 10; /* in tenths of degree Celsius */
	return temperature;
}

int twl6032_get_battery_voltage(void)
{
	int battery_volt = 0;
	t_twl6030_gpadc_data gpadc;

	gpadc.twl_chip_type = chip_TWL6032;
	gpadc.rbase = TWL6032_GPCH0_LSB;
	gpadc.ctrl = TWL6032_CTRL_P1;
	gpadc.enable = CTRL_P1_SP1;

	/* Enable VBAT measurement */
	twl6030_i2c_write_u8(TWL6030_CHIP_ADC, GPADC_CTRL2_CH18_SCALER_EN, TWL6032_GPADC_CTRL2);

	/* Enable GPADC module */
	twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, FGS | GPADCS, TOGGLE1);

	/* Make dummy conversion for the TWL6032 (first conversion may be failed) */
	twl6030_gpadc_sw2_trigger(&gpadc);

	/* measure Vbat voltage */
	battery_volt = twl6030_gpadc_read_channel(&gpadc, TWL6032_GPADC_VBAT_CHNL);
	if (battery_volt < 0) {
		printf("twl6032_get_battery_voltage: Failed to read battery voltage\n");
		return 0;
	}

	battery_volt = ((battery_volt * 25 * 1000) >> (12 + 2));
#if RECON_DEBUG
	printf("[u-boot/%s:%u] battery_volt raw: %dmV\n",__FUNCTION__,__LINE__,battery_volt);
#endif
	return battery_volt;
}

int twl6032_get_battery_current(void)
{
	int ret = 0;
	u16 read_value = 0;
	s16 cc_offset = 0;
	s16 temp = 0;
	int current_now = 0;

	/* FG_REG_08, 09 is 10 bit signed calibration offset value */
	ret = twl6030_i2c_read(TWL6030_CHIP_GASGAUGE, (u8 *) &cc_offset, FG_REG_08, 2);
	if (ret < 0) {
		printf("[%s:%u] I2C uBoot failed to read FG_REG_08 \n",__FUNCTION__,__LINE__);
		return 0;
	}
	cc_offset = ((s16)(cc_offset << 6) >> 6);

	/* FG_REG_10, 11 is 14 bit signed instantaneous current sample value */
	ret = twl6030_i2c_read(TWL6030_CHIP_GASGAUGE, (u8 *)&read_value, FG_REG_10, 2);
	if (ret < 0) {
		printf("[%s:%u] I2C uBoot failed to read FG_REG_10 \n",__FUNCTION__,__LINE__);
		return 0;
	}

	temp = ((s16)(read_value << 2) >> 2);
	current_now = temp - cc_offset;

	/* kernel log: fuelgauge_rate=1, fuelgauge_mode=0, current_max_scale=3100 */

	/* current drawn per sec */
	//current_now = current_now * fuelgauge_rate[di->fuelgauge_mode];

	/* current in mAmperes */
	current_now = (current_now * 3100) >> 13;
#if RECON_DEBUG
	printf("[u-boot/%s:%u] battery_current: %dmA\n",__FUNCTION__,__LINE__,current_now);
#endif
	return current_now; //mA
}

int twl6030_get_battery_voltage(t_twl6030_gpadc_data * gpadc)
{
	int battery_volt = 0;

	u8 vbatch = TWL6030_GPADC_VBAT_CHNL;

	if (gpadc->twl_chip_type == chip_TWL6032)
		vbatch = TWL6032_GPADC_VBAT_CHNL;

	/* measure Vbat voltage */
	battery_volt = twl6030_gpadc_read_channel(gpadc, vbatch);
	if (battery_volt < 0) {
		printf("Failed to read battery voltage\n");
		return battery_volt;
	}

	if (gpadc->twl_chip_type == chip_TWL6030)
		battery_volt = (battery_volt * 25 * 1000) >> (10 + 2);
	else
		battery_volt = ((battery_volt * 25 * 1000) >> (12 + 2));

	return battery_volt;
}

void twl6030_init_battery_charging(void)
{
	int battery_volt = 0;
	int ret = 0;
	t_twl6030_gpadc_data gpadc;
	u8 val;
	int abort = 0;
	int chargedelay = 5;

	gpadc.twl_chip_type = chip_TWL6030;
	gpadc.rbase = GPCH0_LSB;
	gpadc.ctrl = CTRL_P2;
	gpadc.enable = CTRL_P2_SP2;

	ret = twl6030_i2c_read_u8(TWL6030_CHIP_USB, &val, USB_PRODUCT_ID_LSB);

	if (ret == 0) {
		if(val == 0x32)
		{
			gpadc.twl_chip_type = chip_TWL6032;
			gpadc.rbase = TWL6032_GPCH0_LSB;
			gpadc.ctrl = TWL6032_CTRL_P1;
			gpadc.enable = CTRL_P1_SP1;
		}
	} else {
		printf("twl6030_init_battery_charging(): "
				"could not determine chip! "
				"TWL6030 will be used\n");
	}

	/* Enable VBAT measurement */
	if (gpadc.twl_chip_type == chip_TWL6030)
		twl6030_i2c_write_u8(TWL6030_CHIP_PM, VBAT_MEAS, MISC1);
	else{
		twl6030_i2c_write_u8(TWL6030_CHIP_ADC, GPADC_CTRL2_CH18_SCALER_EN, TWL6032_GPADC_CTRL2);
	}
	/* Enable GPADC module */
	ret = twl6030_i2c_write_u8(TWL6030_CHIP_CHARGER, FGS | GPADCS, TOGGLE1);
	if (ret) {
		printf("Failed to enable GPADC\n");
		return;
	}

	/*
	 * Make dummy conversion for the TWL6032
	 * (first conversion may be failed)
	 */
	if (gpadc.twl_chip_type == chip_TWL6032)
		twl6030_gpadc_sw2_trigger(&gpadc);
	/*
	 * In case if battery is absent or error occurred while the battery
	 * detection we will not turn on the battery charging
	 */
#ifndef CONFIG_TWL6032
	if (is_battery_present(&gpadc) <= 0)
		return;
	twl6030_start_usb_charging();

	battery_volt = twl6030_get_battery_voltage(&gpadc);
	if (battery_volt < 0)
		return;

	if (battery_volt < 3400) {
#ifdef CONFIG_SILENT_CONSOLE
		if (gd->flags & GD_FLG_SILENT) {
			/* Restore serial console */
			console_assign (stdout, "serial");
			console_assign (stderr, "serial");
		}
#endif

		printf("Main battery voltage too low!\n");
		printf("Hit any key to stop charging: %2d ", chargedelay);

		if (tstc()) {	/* we got a key press	*/
			(void) getc();  /* consume input	*/
		}

		while ((chargedelay > 0) && (!abort)) {
			int i;

			--chargedelay;
			/* delay 100 * 10ms */
			for (i=0; !abort && i<100; ++i) {
				if (tstc()) {	/* we got a key press	*/
					abort  = 1;	/* don't auto boot	*/
					chargedelay = 0;	/* no more delay	*/
					(void) getc();  /* consume input	*/
					break;
				}
				udelay (10000);
			}
			printf ("\b\b\b%2d ", chargedelay);
		}
		putc ('\n');

#ifdef CONFIG_SILENT_CONSOLE
		if (gd->flags & GD_FLG_SILENT) {
			/* Restore silent console */
			console_assign (stdout, "nulldev");
			console_assign (stderr, "nulldev");
		}
#endif

		if (!abort)
		{
			printf("Charging...\n");

			/* wait for battery to charge to the level when kernel can boot */
			while (battery_volt < 3400) {
				battery_volt = twl6030_get_battery_voltage(&gpadc);
				printf("\rBattery Voltage: %d mV", battery_volt);
			}
			printf("\n");
		}
	}
#endif
}

void twl6030_usb_device_settings()
{
	u8 data = 0;

	/* Select APP Group and set state to ON */
	twl6030_i2c_write_u8(TWL6030_CHIP_PM, 0x21, VUSB_CFG_STATE);

	twl6030_i2c_read_u8(TWL6030_CHIP_PM, &data, MISC2);
	data |= 0x10;

	/* Select the input supply for VBUS regulator */
	twl6030_i2c_write_u8(TWL6030_CHIP_PM, data, MISC2);
}

void twl6030_power_off(void)
{
	u8 val;
	int err;

	err = twl6030_i2c_read_u8(TWL6030_CHIP_PM, &val, PMC_MASTER_PHOENIX_DEV_ON);
	if (err) {
		printf("I2C error %d while reading PMC_MASTER_MODULE's PHOENIX_DEV_ON Register\n", err);
	}

	val |= DEVOFF;
	err = twl6030_i2c_write_u8(TWL6030_CHIP_PM, val, PMC_MASTER_PHOENIX_DEV_ON);
	if (err) {
		printf("I2C error %d while writing PMC_MASTER_MODULE's PHOENIX_DEV_ON Register\n", err);
		err = twl6030_i2c_write_u8(TWL6030_CHIP_PM, val, PMC_MASTER_PHOENIX_DEV_ON); // One More Try
		return;
	}

	return;
}

#endif
