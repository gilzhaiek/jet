/*
 * (C) Copyright 2004-2009
 * Texas Instruments, <www.ti.com>
 * Richard Woodruff <r-woodruff2@ti.com>
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

#include <common.h>
#include <asm/arch/mux.h>
#include <asm/arch/cpu.h>
#include <asm/io.h>
#include <asm/arch/sys_proto.h>
#include <asm/arch/gpio.h>
#include <twl6030.h>
#include <fastboot.h>
#include "jet.h"
#define mdelay(n) ({ unsigned long msec = (n); while (msec--) udelay(1000); })
/* function protypes */
extern int omap4_mmc_init(void);
extern int select_bus(int, int);

#if defined(OMAP44XX_TABLET_CONFIG)
static int tablet_check_display_boardid(void);
#endif

static u8 g_boot_state;
static int g_battery_volt;
static int g_battery_current;
static int g_battery_temperature;

//#define JET_VAUX1_2_3_ENABLED  //Disable VAUX because kernel will turn it off
#undef  JET_AUDIO_ENABLED

#define MUX_DEFAULT_OMAP4()

#define MUX_DEFAULT_OMAP4_ALL()
#ifdef CONFIG_SNOW
#define MUX_SNOW_3_0_UPDATE() \
	MV(CP(GPMC_A17),	(JET_OUTPUT | OFF_EN| OFF_PD)) /* gpio_41 -CLK_TV_EN output */ \
	MV(CP(GPMC_A24) , JET_OUTPUT)  /* gpio_48 -TCXO CLK REQ IN*/ \
	MV(CP(GPMC_NCS2) , JET_OUTPUT|PTU)  /* gpio_52 -+A3.6V_EN Enable pin for Kopin, set high to work around for power surge*/ \
	MV(CP(GPMC_NADV_ALE) ,JET_FLOAT)  /* gpio_56 not used*/ \
	MV(CP(ABE_MCBSP2_CLKX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_clkx */ \
	MV(CP(ABE_MCBSP2_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dr */ \
	MV(CP(ABE_MCBSP2_DX) ,  ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dx */ \
	MV(CP(ABE_MCBSP2_FSX) ,  ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_fsx */ \
	MV(CP(ABE_MCBSP1_CLKX) , (IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_clkx */ \
	MV(CP(ABE_MCBSP1_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dr */ \
	MV(CP(ABE_MCBSP1_DX) , ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dx */ \
	MV(CP(ABE_MCBSP1_FSX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_fsx */ \
	MV(CP(HDQ_SIO) , JET_OUTPUT )  /* gpio_127 -HDQ0_1-Wire_Fuel_GAUGE, debug pin */ \
	MV1(WK(PAD1_FREF_CLK4_REQ),	(JET_FLOAT)) /* The Kopin clock will be turned on in the kernel driver*/
#endif
/**********************************************************
 * Routine: set_muxconf_regs
 * Description: Setting up the configuration Mux registers
 *			  specific to the hardware. Many pins need
 *			  to be moved from protect to primary mode.
 *********************************************************/
void set_muxconf_regs(void)
{
	MUX_DEFAULT_OMAP4();
	return;
}

/******************************************************************************
 * Routine: update_mux()
 * Description:Update balls which are different between boards.  All should be
 *			 updated to match functionality.  However, I'm only updating ones
 *			 which I'll be using for now.  When power comes into play they
 *			 all need updating.
 *****************************************************************************/
void update_mux(u32 btype, u32 mtype)
{
	/* REVISIT  */
	return;

}

/*****************************************
 * Routine: board_init
 * Description: Early hardware init.
 *****************************************/
int board_init(void)
{
	DECLARE_GLOBAL_DATA_PTR;
	gpmc_init();
	//dmm_init();
//#if 1 /* No eMMC env partition for now */
	/* Intializing env functional pointers with eMMC */
	boot_env_get_char_spec = mmc_env_get_char_spec;
	boot_env_init = mmc_env_init;
	boot_saveenv = mmc_saveenv;
	boot_env_relocate_spec = mmc_env_relocate_spec;
	env_ptr = (env_t *) (CFG_ENV_ADDR);
	env_name_spec = mmc_env_name_spec;
//#endif

	/* board id for Linux */
	gd->bd->bi_arch_number = MACH_TYPE_OMAP4_JET;
	gd->bd->bi_boot_params = (0x80000000 + 0x100); /* boot param addr */
	return 0;
}

void set_gpio_output(int gpio, int value) {
	omap_set_gpio_direction(gpio, 0);	// Output
	omap_set_gpio_dataout(gpio, value);  // Value
}

//for unused gpio pins:
void set_unused_gpio(void) {
	set_gpio_output(7, 0);
	set_gpio_output(40, 0);
#ifndef CONFIG_SUN
	set_gpio_output(56, 0);  //snow didn't use EYE_TRACING
#endif
#ifdef JET_V2_CONFIG
	set_gpio_output(37, 0);  //OFN-POWERDOWN in sun B2 b3; not used in snow
	set_gpio_output(50, 0);  //ok
	set_gpio_output(55, 0);  //ok
#else
	set_gpio_output(8, 0);   // gpio_wk8
	set_gpio_output(13, 0);  // ok
	set_gpio_output(17, 0);  // ok
	set_gpio_output(33, 0);  // ok
	set_gpio_output(34, 0);  // ok
	set_gpio_output(35, 0);  // ok
	set_gpio_output(36, 0);  // ok
	set_gpio_output(38, 0);  // ok
	set_gpio_output(158, 0); // ok
	set_gpio_output(159, 0); // ok
	set_gpio_output(160, 0); // ok
	set_gpio_output(161, 0); // ok
	set_gpio_output(162, 0); // ok
#endif
}


void set_gpio_outputs(void)
{
	set_gpio_output(OMAP4430_JET_GPIO_GPADC_START, 0);   //Don't initiate ADC measurements
	set_gpio_output(OMAP4430_JET_GPIO_TCXO_CLK_REQ_IN, 0); //Wilink
	set_gpio_output(OMAP4430_JET_GPIO_WLAN_EN, 0);	   //Keep the WIFI OFF
	set_gpio_output(OMAP4430_JET_GPIO_RESET_MFI, 1);	 //Keep the MFI OFF


	set_gpio_output(OMAP4430_JET_GPIO_BT_ENABLE, 0);	 //Keep the BT OFF
	set_gpio_output(OMAP4430_JET_GPIO_RFBI_A16, 0);	  //MSB of kopin address.. not sure why it has to be a 0...
#ifdef JET_V2_CONFIG
	set_gpio_output(OMAP4430_JET_GPIO_FUEL_GAUGE, 0);	//Debug Pin
	set_gpio_output(OMAP4430_JET_GPIO_LED_FLEX, 0);  //LED Flex in B3 for debug or ACT_BUSY in B2 and snow,not used
	set_gpio_output(OMAP4430_JET_GPIO_CLK_TV_EN, 0);	 //Keep Kopin clock off for now <AD> This should be off for all versions until it is time to initialize the Kopin driver 
	set_gpio_output(OMAP4430_JET_GPIO_DISP_RES, 0);	  //Keep Kopin in reset for now <AD> This should be off for all versions until it is time to initialize the Kopin driver 
	
#ifdef CONFIG_SUN//Sun b2,b3
	set_gpio_output(OMAP4430_JET_GPIO_A3_6V_EN, 0);	  //The pull down resister is added in the sun b3 board
	set_gpio_output(OMAP4430_JET_GPIO_RST_12MP, 0);	  //Hold camera in reset
	set_gpio_output(OMAP4430_JET_GPIO_RESET_OFN, 1);	 //Keep OFN-POWERDOWN high
	set_gpio_output(OMAP4430_JET_GPIO_SHUTDOWN_OFN, 1);  //Keep OFN-STANDBY high 
#else
	set_gpio_output(OMAP4430_JET_GPIO_A3_6V_EN, 1);	  //set high to work around for power surge
#endif

#else //Sun b1
	set_gpio_output(OMAP4430_JET_GPIO_RST_12MP, 0);	  //Hold camera in reset
	set_gpio_output(OMAP4430_JET_GPIO_RESET_OFN, 1);	 //Keep OFN-POWERDOWN high
	set_gpio_output(OMAP4430_JET_GPIO_SHUTDOWN_OFN, 1);  //Keep OFN-STANDBY high 

#if defined(JET_AUDIO_ENABLED)
	set_gpio_output(OMAP4430_JET_GPIO_AUD_PWRON, 1);	 //Debug pin - not used in B3
#else
	set_gpio_output(OMAP4430_JET_GPIO_AUD_PWRON, 0);	 //Debug pin - not used in B3
#endif

#endif
	set_unused_gpio();
}

int is_pwrbutton_hold(void)
{
	int i=DELAY_SECOND;
	unsigned char data;

	while(i) {
		i2c_read(TWL6030_CHIP_PM,PMC_MASTER_STS_HW_CONDITIONS, 1,&data,1);
		printf("pwrbutton reg=0x%.2x\n",data);
		if( (data&STS_PWRON)==PWRBUTTON_LEVEL_HIGH)
		{
			return 0;
		}
		i--;
	   // mdelay(1000);
	}
	return 1;
}

int is_pwrbutton_down(void)
{
	unsigned char data;
	i2c_read(TWL6030_CHIP_PM,PMC_MASTER_STS_HW_CONDITIONS, 1, &data, 1);
	if((data & STS_PWRON) == PWRBUTTON_LEVEL_HIGH) {
		return 0;
	}
#if RECON_DEBUG
	printf("STS_PWRON is low - power button is pressed\n");
#endif
	return 1;
}

int is_pwrbutton_hold_short(void)
{
	int i;

	for (i = 0 ; i < 3 ; i++)
	{
		if (!is_pwrbutton_down())
		{
			return 0;
		}
	}
	return 1;
}

int is_usb_connected(void)
{
	unsigned char data;
	i2c_read(TWL6030_CHIP_PM, PMC_MASTER_STS_HW_CONDITIONS , 1, &data, 1);

	if ((data & STS_PLUG_DET) > 0) {
#if RECON_DEBUG
		printf("STS_PLUG_DET is high - USB is plugged \n");
#endif
		return 1;
	}
	return 0;
}

int is_pwr_usb(void)
{
	unsigned char data;
	i2c_read(TWL6030_CHIP_PM, PMC_MASTER_PHOENIX_START_CONDITION , 1,&data,1);

	if((data & STRT_ON_PLUG_DET) > 0){
		printf("STRT_ON_PLUG_DET is high - USB insertion is detected\n");
		data=0;
		i2c_write(TWL6030_CHIP_PM, PMC_MASTER_PHOENIX_START_CONDITION, 1, &data, 1);
		return 1;
	}
	printf("STRT_ON_PLUG_DET is low - USB insertion is not detected\n");
	return 0;
}
/**
 * Check if reboot detected and modify boot_state if needed
*/
int check_reboot(unsigned char *state_ptr)
{
	if (__raw_readl(PRM_RSTST) & PRM_RSTST_RESET_WARM_BIT
		|| (!strcmp(PUBLIC_SAR_RAM_1_FREE, "reboot")) ){  //This is for "fastboot reboot command"
		//If the reboot is due to user shutdown and charge, there is no need to reboot
		if(!strcmp(PUBLIC_SAR_RAM_1_FREE, "ChargeShutdown")){
			//The reboot didn't turn off power led, so turn off here
			turn_off_power_led();
		}
		else
			*state_ptr = BOOT_ANDROID;
#if RECON_DEBUG
		printf("[%s:%u]reboot reason=%s\n",__FUNCTION__,__LINE__,PUBLIC_SAR_RAM_1_FREE);
#endif
		return 1;
	}
	return 0;
}

//report onboard temperature -128~127 degree range
#define TMP102_TEMP_REG		0x00
#define TMP102_CONF_REG		0x01

#define TMP102_CONF_R0		0x20
#define TMP102_CONF_R1		0x40

#define TMP102_CONF_CR1		0x80
signed char get_temperature(void)
{
	unsigned char buf[2]={0};
	unsigned short temp;
	signed char ret;
	select_bus(SENSOR_I2C_BUS, SENSOR_I2C_SPEED);

	buf[0] = (TMP102_CONF_R0|TMP102_CONF_R1);
	buf[1] = TMP102_CONF_CR1;
	i2c_write(BOARD_TMP102_ADDRESS,TMP102_CONF_REG,1,buf,2);

	if(i2c_read(BOARD_TMP102_ADDRESS,TMP102_TEMP_REG, 1,buf,2)) {
		printf("fail to read temperature\n");
		ret = 0xFFFF;
	} else {
		//Revert MSB to high position and convert to 12bit adc data
		temp= (buf[0]<<8);
		temp=((temp|buf[1])& 0xFFFF);
		temp= ((temp>>4)&0xFFF);//12bit data

		if((temp&(0x800))!=0) //negative data
		{
			temp=(0x1000)-temp;
			temp=(temp>>4);
			temp=0x100-temp; //temp*(-1)
		}
		else
		{
			temp=(temp>>4);
		}
		ret=(signed char)temp;
		printf("temperature read %dC\n",ret);
	}

	select_bus(0, CFG_I2C_SPEED);//change to default i2c bus

	return ret;
}

int is_temperature_high(void)
{
	return (get_temperature() >= TEMPERATURE_THRESHOLD);
}

int is_shutdown_voltage(int voltage, int battery_temp) {
	//Round to closest integer
	if(battery_temp>=0)
		battery_temp=(battery_temp+5)/10;
	else
		battery_temp=(battery_temp-5)/10;

#if RECON_DEBUG
	printf("[%s:%u] No USB @%dmv %dc\n",__FUNCTION__,__LINE__, voltage, battery_temp);
#endif
	/* If Battery Voltage could not be read - we should boot the device just in case it has some juice in it */
	if(voltage <= 0) {
		return 1;
	}

	if(battery_temp >= 25) {
		return (voltage < JET_SHUTDOWN_MV_25);
	} else if(battery_temp >= 15) {
		return (voltage < JET_SHUTDOWN_MV_15);
	} else if(battery_temp >= 10) {
		return (voltage < JET_SHUTDOWN_MV_10);
	} else if(battery_temp >= 5) {
		return (voltage < JET_SHUTDOWN_MV_5);
	} else if(battery_temp >= 0) {
		return (voltage < JET_SHUTDOWN_MV_0);
	} else if(battery_temp >= -5) {
		return (voltage < JET_SHUTDOWN_MV_5N);
	} else if(battery_temp >= -10) {
		return (voltage < JET_SHUTDOWN_MV_10N);
	} else {
		return (voltage < JET_SHUTDOWN_MV_15N);
	}
}

void turn_on_power_led(void) {
	unsigned char data = PWM2ON_PEROID;
	i2c_write(TWL6030_CHIP_PWM, PMC_PWM2ON, 1, &data, 1);

	data = PWM2OFF_PEROID;
	i2c_write(TWL6030_CHIP_PWM, PMC_PWM2OFF, 1, &data, 1);

	data = (PWM2EN_CLOCK_INPUT | PWM2EN_SIGNAL);
	i2c_write(TWL6030_CHIP_PWM, PMC_TOGGLE3, 1, &data, 1);

#if RECON_DEBUG
	printf("[%s:%u] PWM2ON_PEROID= 0x%X (%d), PWM2OFF_PEROID= 0x%X (%d), brightnessPct= %d%%\n",
			__FUNCTION__,__LINE__,PWM2ON_PEROID,PWM2ON_PEROID,PWM2OFF_PEROID,PWM2OFF_PEROID,
			(((PWM2OFF_PEROID-PWM2ON_PEROID)*100)/PWM2OFF_PEROID));
#endif
}

void turn_off_power_led(void) {
	unsigned char data = (PWM2EN_CLOCK_INPUT | PWM2DISABLE_SIGNAL);
	i2c_write(TWL6030_CHIP_PWM, PMC_TOGGLE3, 1, &data, 1);
}

/* Set the mode for the charging LED. Possible modes are:
 * 0x00 - LED driver controlled by charging modes 
 * 0x01 - LED driver enabled (LED ON, indepedent of charging modes)
 * 0x02 - LED driver disabled (LED OFF, indepedent of charging modes)
 * 0x03 - LED driver controlled by charging modes
 */
void set_mode_charge_led(unsigned char mode) {
	unsigned char reg;

	i2c_read(TWL6030_CHIP_PWM, PMIC_LED_PWM_CTRL2, 1, &reg, 1);

	reg = (reg & ~(0x03)) | mode;

	i2c_write(TWL6030_CHIP_PWM, PMIC_LED_PWM_CTRL2, 1, &reg, 1);
}

void adjust_charge_led_power(void) {
	unsigned char data = CHARGE_LED_DUTY_CYCLE;
	i2c_write(TWL6030_CHIP_PWM, PMIC_LED_PWM_CTRL1, 1, &data, 1);

	data = CHARGE_LED_CURRENT;
	i2c_write(TWL6030_CHIP_PWM, PMIC_LED_PWM_CTRL2, 1, &data, 1);
}

#if defined(CONFIG_OMAP4_ANDROID_CMD_LINE)
#define BOOTMODE_CHARGE " androidboot.mode=reconcharger"
#define BOOTMODE_LEGACY "bootmode="
#define INIT_VOLTAGE "androidboot.voltage="
#define INIT_CURRENT "androidboot.current="
#define INIT_TEMPERATURE "androidboot.temperature="
void add_extra_bootargs(u8 boot_state, int battery_volt, int battery_current, int battery_temperature){
	char buf[256] = { 0 };
	sprintf(buf, " %s%d %s%04d %s%04d %s%04d", BOOTMODE_LEGACY, boot_state, INIT_VOLTAGE, battery_volt, INIT_CURRENT, battery_current, INIT_TEMPERATURE, battery_temperature);
	if(boot_state == BOOT_CHARGER)
		strcat(buf, BOOTMODE_CHARGE);

	setenv("android.bootargs.extra", buf);
	return;
}
#endif

#define USB_ENABLE	1
#define USB_DISABLE	0
int early_misc_init_r(void) {
	int i = 0;
	int usb_connected   = 0;
	int pwrbutton_down  = 0;
	u8 boot_state = 0;
	int battery_volt = 0;
	int battery_current = 0;
	int battery_temperature = 0;

#if RECON_DEBUG
	int elapsed_time_msec = 0;
	ulong start_time = 0;
	start_time = get_timer(0);
#endif

	/* Setup I2C bus pull-up resistors registers */
#ifdef CONFIG_SUN
	/* I2C_1 is 4.5 kΩ 
	   I2C_2/3/4 is 860 Ω (50-150 pF) */
	__raw_writel(0xCCC8CCC8, 0x4A100604);
	/* I2C_5 is 860 Ω (50-150 pF) */
	__raw_writel(0xCC000000, 0x4A31E604);
#else
	/* I2C is 4.5 kΩ */
	__raw_writel(0x88888888, 0x4A100604);
	__raw_writel(0x88000000, 0x4A31E604);
#endif
	/* Now that the pull-ups are set, we can initialize
	   i2c */
	i2c_init(CFG_I2C_SPEED, CFG_I2C_SLAVE);

#ifdef CONFIG_SNOW
	/**
	 * Add all xloader changes to uboot
	 * since TAG: 3.0_final_factory xloader release
	**/
	MUX_SNOW_3_0_UPDATE();
#endif

	/* Get state of the power button */
	pwrbutton_down = is_pwrbutton_down();

	/* Get state of USB connection */
	usb_connected = is_usb_connected();

	if (usb_connected && pwrbutton_down) {
		/* Boot up reason is not from USB, but USB is connected */
		boot_state = BOOT_ANDROID;
	}
	else if (pwrbutton_down && is_pwrbutton_down()) {
		/* USB is not connected and power button is still pressed */
		boot_state = BOOT_ANDROID;
	}
	else {
		check_reboot(&boot_state);
	}

	/* TODO: Also should check if temperature is below freezing which means we can't charge (twl6032_get_battery_temp() < 0) */

	if (usb_connected == 0) {
		/* USB is disconnected */
		battery_volt = twl6032_get_battery_voltage();
		if (boot_state != BOOT_ANDROID) {
			/* Someone connected the USB and unplugged right away, or 
			   pressed power button and released */
#if RECON_DEBUG
			printf("[%s:%u] volt: %dmV, temp: %dC No USB or Power Button. Powering Off...\n",
					__FUNCTION__,__LINE__,battery_volt,twl6032_get_battery_temp());
#endif
			twl6030_power_off();
		}
		battery_temperature = twl6032_get_battery_temp();

		if(is_shutdown_voltage(battery_volt, battery_temperature)) {
			/* Voltage is too low to boot. Measure again just in case before shutting down */
			if(is_shutdown_voltage(twl6032_get_battery_voltage(), twl6032_get_battery_temp())) {
				/* Voltage is still too low */
				/* Blink the charge LED to tell the user the voltage is too low to boot */
				for(i = 0; i < 3; i++) {
					set_mode_charge_led(0x01);
					mdelay(100);
					set_mode_charge_led(0x02);
					mdelay(100);
				}

				/* Give back control of the charge led to HW */
				set_mode_charge_led(0x00);
#if RECON_DEBUG
				printf("[%s:%u] %dmV Voltage too low at temperature %dC. Powering Off...\n",
						__FUNCTION__,__LINE__,battery_volt,twl6032_get_battery_temp());
#endif
				twl6030_power_off();
			}
		}
		/* If execution gets here, the voltage is high enough to power up */
		turn_on_power_led();
#if RECON_DEBUG
		printf("[%s:%u] Android Boot - USB disconnected\n",__FUNCTION__,__LINE__);
#endif

		battery_current = twl6032_get_battery_current();
	}
	else {
		/* USB is connected */
		if(boot_state == BOOT_ANDROID) {
			turn_on_power_led();
#if RECON_DEBUG
			printf("[%s:%u] Android Boot - USB Connected\n",__FUNCTION__,__LINE__);
#endif
		} else {
			/* We don't turn on the LED during MTP Boot */
#if RECON_DEBUG
			printf("[%s:%u] Charge Boot - USB Connected\n",__FUNCTION__,__LINE__);
#endif
		}

		/* Initialize USB charging parameters */
		/* Turn ON charge LED. (JET-768) */
		set_mode_charge_led(0x01);

#if RECON_DEBUG
		printf("[%s:%u] Init USB charging parameters\n",__FUNCTION__,__LINE__);
#endif
		twl6032_init_usb_charging(&boot_state);

		/* Disable HW charging to measure voltage */
		twl6032_set_usb_hw_charging(USB_DISABLE);

		/* Get battery voltage initial capacity estimate */
		battery_volt = twl6032_get_battery_voltage();
		battery_current = twl6032_get_battery_current();
		battery_temperature = twl6032_get_battery_temp();

		/* Re-enable HW charging  */
		twl6032_set_usb_hw_charging(USB_ENABLE);
		/* Give back control of the charge LED to HW (JET-768)*/
		set_mode_charge_led(0x00);
	}

	/* Adjust the charge led brightness */
	adjust_charge_led_power();

	/* Update global variables */
	g_battery_volt = battery_volt;
	g_battery_current = battery_current;
	g_battery_temperature = battery_temperature;
	g_boot_state = boot_state;

	printf("[%s:%u] battery volt: %d mV, current: %d mA, temperature: %d C\n",
			__FUNCTION__,__LINE__, g_battery_volt, g_battery_current, g_battery_temperature);

#if RECON_DEBUG
	elapsed_time_msec = get_timer(start_time)/(get_tbclk()/1000);
	printf("[%s:%u] elapsed_time=%d msec\n",__FUNCTION__,__LINE__,elapsed_time_msec);
#endif

	return 0;
}


/*****************************************
 * Routine: board_late_init
 * Description: Late hardware init.
 *****************************************/
int board_late_init(void)
{
	int status;
	unsigned char data = PMIC_STATE_DISABLE;

	printf("board_late_init\n");

	/* mmc */
	if( (status = omap4_mmc_init()) != 0) {
		return status;
	}

#ifndef JET_V2_CONFIG
	// VMEM_CFG_STATE off
	i2c_write(TWL6030_CHIP_PM, PMIC_VMEM_CFG_STATE, 1, &data, 1);


	//data=PMIC_STATE_DISABLE;//turn off KOPIN power supply
	data=PMIC_STATE_ENABLE; //turn on KOPIN power supply
	i2c_write(TWL6030_CHIP_PM, PMIC_SYSEN_CFG_STATE, 1, &data, 1);

	//32KHz clock for Wilink
	data = PMIC_STATE_ENABLE;
	i2c_write(TWL6030_CHIP_PM, PMIC_CLK32KG_CFG_STATE, 1, &data, 1);

	//Backup battery config
	data = 0x5A;//BB_CHG_EN enable and set BB_SEL for 2.5V
	i2c_write(TWL6030_CHIP_PM, PMIC_BBSPOR_CFG, 1, &data, 1);
#else // JET_V2_CONFIG
	data = 0x95; // Keep the voltage configuration settings, same VSEL [5:0] value just before the warm reset event.
	i2c_write(TWL6030_CHIP_PM, PMIC_LDO5_CFG_VOLTAGE, 1, &data, 1);
#endif

#if defined(JET_VAUX1_ENABLED)
	//VAUX1------
	//data = 0x15; //3.0V
	data = 0x13; //2.8V
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX1_CFG_VOLTAGE, 1, &data, 1);
	data = 0x03;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX1_CFG_TRANS, 1, &data, 1);
	data = PMIC_STATE_DISABLE; // Disable it in case the device reboots and the power rail is still on
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX1_CFG_STATE, 1, &data, 1);
#endif
#if defined(JET_VAUX2_ENABLED)
	//VAUX2------
	data = 0x10;//2.5V
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX2_CFG_VOLTAGE, 1, &data, 1);
	data = 0x03;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX2_CFG_TRANS, 1, &data, 1);
	data = PMIC_STATE_DISABLE; // Disable it in case the device reboots and the power rail is still on
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX2_CFG_STATE, 1, &data, 1);
#endif
#if defined(JET_VAUX3_ENABLED)
	//VAUX3------
	data = 0x13;//2.8V
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX3_CFG_VOLTAGE, 1, &data, 1);
	data = 0x03;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX3_CFG_TRANS, 1, &data, 1);
	data = PMIC_STATE_DISABLE; // Disable it in case the device reboots and the power rail is still on
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX3_CFG_STATE, 1, &data, 1);
#endif

	set_gpio_outputs();

#if defined(JET_VAUX1_ENABLED)
	data = PMIC_STATE_ENABLE;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX1_CFG_STATE, 1, &data, 1);
#endif
#if defined(JET_VAUX2_ENABLED)
	data = PMIC_STATE_ENABLE;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX2_CFG_STATE, 1, &data, 1);
#endif
#if defined(JET_VAUX3_ENABLED)
	data = PMIC_STATE_ENABLE;
	i2c_write(TWL6030_CHIP_PM, PMIC_VAUX3_CFG_STATE, 1, &data, 1);
#endif
	// For Fastboot hack - TODO - remove this
	// If it is held - we turn off the power led and go into Fastboot
	/*if(is_pwrbutton_hold()){
		//based on manual, need to disable input PWM generator signal first then input clock
		data = (PWM2EN_CLOCK_INPUT | PWM2DISABLE_SIGNAL);
		i2c_write(TWL6030_CHIP_PWM, PMC_TOGGLE3, 1, &data, 1);
		data = 0;
		i2c_write(TWL6030_CHIP_PWM, PMC_TOGGLE3, 1, &data, 1);

		do_fastboot(NULL, 0, 0, NULL);
	}*/

	//On board temperature read
	if(is_temperature_high()){
		printf("on board temperature too high\n");
		twl6030_power_off();
	}
#if defined(CONFIG_OMAP4_ANDROID_CMD_LINE)
	add_extra_bootargs(g_boot_state, g_battery_volt, g_battery_current, g_battery_temperature);
#endif
	return status;
}

/***********************************************************************
 * get_board_type() - get board type based on current production stats.
 *  - NOTE-1-: 2 I2C EEPROMs will someday be populated with proper info.
 *	when they are available we can get info from there.  This should
 *	be correct of all known boards up until today.
 *  - NOTE-2- EEPROMs are populated but they are updated very slowly.  To
 *	avoid waiting on them we will use ES version of the chip to get info.
 *	A later version of the FPGA migth solve their speed issue.
 ************************************************************************/
u32 get_board_type(void)
{
	return SDP_4430_V1;
}


