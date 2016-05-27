#ifndef __OMAP_4430_JET_1H__
#define __OMAP_4430_JET_1H__

//#define JET_VAUX1_ENABLED
#define JET_VAUX2_ENABLED  //For temperature and motion sensors
#ifdef CONFIG_SUN
#define JET_VAUX3_ENABLED  //To turn on cammera power at early stage as suggested by Alex D.
// At 5% what will be our low voltage
// must be over 3.4V (PMIC system boot threshold) to allow boot to proceed.
#define JET_SHUTDOWN_MV_25		3620 /*EDV1 */
#define JET_SHUTDOWN_MV_15		3593 /*EDV1 */
#define JET_SHUTDOWN_MV_10		3460 /*EDV1 */
#define JET_SHUTDOWN_MV_5		3405 /*EDV1 */
#define JET_SHUTDOWN_MV_0		3406 /*EDV1 */
#define JET_SHUTDOWN_MV_5N		3406 /*EDV1 */
#define JET_SHUTDOWN_MV_10N		3427 /*EDV1 */
#define JET_SHUTDOWN_MV_15N		3411 /*EDV1 */
#else // Snow2
// At 5% what will be our low voltage
#define JET_SHUTDOWN_MV_25		3613
#define JET_SHUTDOWN_MV_15		3594
#define JET_SHUTDOWN_MV_10		3575
#define JET_SHUTDOWN_MV_5		3556
#define JET_SHUTDOWN_MV_0		3514
#define JET_SHUTDOWN_MV_5N		3454
#define JET_SHUTDOWN_MV_10N		3400
#define JET_SHUTDOWN_MV_15N		3400
#endif

#define OMAP44XX_WKUP_CTRL_BASE		0x4A31E000

#define PMIC_VMEM_CFG_STATE		0x66
#define PMIC_SYSEN_CFG_STATE		0xB5

#define PMIC_VAUX1_CFG_TRANS		0x85
#define PMIC_VAUX1_CFG_STATE		0x86
#define PMIC_VAUX1_CFG_VOLTAGE		0x87

#define PMIC_VAUX2_CFG_TRANS		0x89
#define PMIC_VAUX2_CFG_STATE		0x8A
#define PMIC_VAUX2_CFG_VOLTAGE		0x8B

#define PMIC_VAUX3_CFG_TRANS		0x8D
#define PMIC_VAUX3_CFG_STATE		0x8E
#define PMIC_VAUX3_CFG_VOLTAGE		0x8F

#define PMIC_LDO5_CFG_VOLTAGE		0x9B

#define PMIC_CLK32KG_CFG_STATE		0xBE

#define PMIC_BBSPOR_CFG			0xE6

#ifdef CONFIG_SUN
#define PWM2ON_PEROID			0x72
#else
#define PWM2ON_PEROID			0x60
#endif
#define PWM2OFF_PEROID			0x7F

#define PMIC_LED_PWM_CTRL1		0xF4
#define PMIC_LED_PWM_CTRL2		0xF5

#define CHARGE_LED_DUTY_CYCLE		0x60
#define CHARGE_LED_CURRENT_1MA		0x10
#define CHARGE_LED_CURRENT		0x30

#ifdef CONFIG_TWL6032
#define PMIC_STATE_DISABLE		0x00
#define PMIC_STATE_ENABLE		0x01
#else
#define PMIC_STATE_DISABLE		0xE0
#define PMIC_STATE_ENABLE		0xE1
#endif
#define DELAY_SECOND			3
#define PWRBUTTON_LEVEL_HIGH		1

#define SENSOR_I2C_SPEED		400
#define SENSOR_I2C_BUS			3//Fourth i2c bus

#define BOARD_TMP102_ADDRESS		0x49
#define TEMPERATURE_THRESHOLD		80

#define M0_SAFE M0
#define M1_SAFE M1
#define M2_SAFE M2
#define M4_SAFE M4
#define M7_SAFE M7
#define M3_SAFE M3
#define M5_SAFE M5
#define M6_SAFE M6

//// JET GPIO /////
#ifdef JET_V2_CONFIG
#define OMAP4430_JET_GPIO_FUEL_GAUGE	127  // YES
#define OMAP4430_JET_GPIO_CLK_TV_EN	41   // YES
#define OMAP4430_JET_GPIO_DISP_RES	42   // YES
#define OMAP4430_JET_GPIO_A3_6V_EN	52   // YES
#define OMAP4430_JET_GPIO_RST_12MP	83   // YES
#define OMAP4430_JET_GPIO_RESET_OFN	37   // OFN-Power down
#define OMAP4430_JET_GPIO_SHUTDOWN_OFN	49   // OFN-STANDBY
#define OMAP4430_JET_GPIO_LED_FLEX	32   // Debug pin

#else
#define OMAP4430_JET_GPIO_RST_12MP	37   // Actually, this is OFN-POWERDOWN. Camera is GPIO83
#define OMAP4430_JET_GPIO_RESET_OFN	47   // This is not used in B3
#define OMAP4430_JET_GPIO_SHUTDOWN_OFN	49   // OFN-STANDBY
#define OMAP4430_JET_GPIO_AUD_PWRON	127  // This is the one wire fuel gauge and not used debug pin in B3
#endif

#define OMAP4430_JET_GPIO_GPADC_START	39   // YES
#define OMAP4430_JET_GPIO_WLAN_EN	43   // YES
#define OMAP4430_JET_GPIO_RESET_MFI	44   // YES
#define OMAP4430_JET_GPIO_DEN_G		45   // YES (9-axis enable)
#define OMAP4430_JET_GPIO_BT_ENABLE	46   // YES
#define OMAP4430_JET_GPIO_TCXO_CLK_REQ_IN 48
#define OMAP4430_JET_GPIO_RFBI_A16	163  // YES
#endif
