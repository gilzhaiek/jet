
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
#include <asm/arch/cpu.h>
#include <asm/io.h>
#include <asm/arch/bits.h>
#include <asm/arch/mux.h>
#include <asm/arch/sys_proto.h>
#include <asm/arch/sys_info.h>
#include <asm/arch/clocks.h>
#include <asm/arch/mem.h>
#include <i2c.h>
#include <asm/mach-types.h>
#if (CONFIG_COMMANDS & CFG_CMD_NAND) && defined(CFG_NAND_LEGACY)
#include <linux/mtd/nand_legacy.h>
#endif

#define CONFIG_OMAP4_SDC        1

/*****************************************
 * Routine: board_init
 * Description: Early hardware init.
 *****************************************/
int board_init(void)
{
    return 0;
}


#if 1
#define M0_SAFE M0
#define M1_SAFE M1
#define M2_SAFE M2
#define M4_SAFE M4
#define M7_SAFE M7
#define M3_SAFE M3
#define M5_SAFE M5
#define M6_SAFE M6
#else
#define M0_SAFE M7
#define M1_SAFE M7
#define M2_SAFE M7
#define M4_SAFE M7
#define M7_SAFE M7
#define M3_SAFE M7
#define M5_SAFE M7
#define M6_SAFE M7
#endif

/*
 * IEN  - Input Enable
 * IDIS - Input Disable
 * PTD  - Pull type Down
 * PTU  - Pull type Up
 * DIS  - Pull type selection is inactive
 * EN   - Pull type selection is active
 * M0   - Mode 0
 * The commented string gives the final mux configuration for that pin
 */
#define JET_INPUT (PTU | IEN |M3)
#define JET_OUTPUT (M3)
#define JET_FLOAT (M3|OFF_EN|OFF_PD)
/*  Anywhere that has Jet Float needs to have GPIO set as an output and driven output low*/
#define JET_FLOAT_NOGPIO (M0)
#define JET_SAFEMODE (PTD|M7)
#ifdef JET_V2_CONFIG
#ifdef JET_SUN_CONFIG //Sun B2
#define MUX_DEFAULT_OMAP4() \
    MV(CP(GPMC_AD0) , ( PTU | IEN | M1))  /* sdmmc2_dat0 */ \
    MV(CP(GPMC_AD1) , ( PTU | IEN | M1))  /* sdmmc2_dat1 */ \
    MV(CP(GPMC_AD2) , ( PTU | IEN | M1))  /* sdmmc2_dat2 */ \
    MV(CP(GPMC_AD3) , ( PTU | IEN | M1))  /* sdmmc2_dat3 */ \
    MV(CP(GPMC_AD4) , ( PTU | IEN | M1))  /* sdmmc2_dat4 */ \
    MV(CP(GPMC_AD5) , ( PTU | IEN | M1))  /* sdmmc2_dat5 */ \
    MV(CP(GPMC_AD6) , ( PTU | IEN | M1))  /* sdmmc2_dat6 */ \
    MV(CP(GPMC_AD7) , ( PTU | IEN | M1))  /* sdmmc2_dat7 */ \
    MV(CP(GPMC_AD8) , JET_OUTPUT)  /* gpio_32 output for debug*/ \
    MV(CP(GPMC_AD9) , JET_FLOAT)   /*gpio_33 */ \
    MV(CP(GPMC_AD10) , JET_FLOAT)  /* gpio_34 */ \
    MV(CP(GPMC_AD11) , JET_FLOAT)  /* gpio_35 */ \
    MV(CP(GPMC_AD12) , JET_FLOAT)  /* gpio_36 */ \
    MV(CP(GPMC_AD13) , JET_OUTPUT)  /* gpio_37 finger nav power down*/ \
    MV(CP(GPMC_AD14) , JET_FLOAT)  /* gpio_38 */ \
    MV(CP(GPMC_AD15) , JET_OUTPUT)  /* gpio_39 -GPADC_START TWL6030, output no safe mode, drive low*/ \
    MV(CP(GPMC_A16) , JET_FLOAT)  /* gpio_40 */ \
    MV(CP(GPMC_A17) , JET_OUTPUT|OFF_EN|OFF_PD)  /* gpio_41 -CLK_TV_EN output low by default*/  \
    MV(CP(GPMC_A18) , JET_OUTPUT)  /* gpio_42 -Disp_RES output, Kopin reset */ \
    MV(CP(GPMC_A19) , JET_OUTPUT)  /* gpio_43 -WLAN EN output safemode low to disable wlan */ \
    MV(CP(GPMC_A20) , JET_OUTPUT)  /* gpio_44 -RES_MFI chip should be high */ \
    MV(CP(GPMC_A21) , JET_INPUT)  /*gpio_45 -DEN_G tied to 1.8V, set input by dafault*/  \
    MV(CP(GPMC_A22) , JET_OUTPUT)  /* gpio_46 -BT EN */ \
    MV(CP(GPMC_A23) , JET_SAFEMODE)  /* gpio_47 -RES_OFN not used*/ \
    MV(CP(GPMC_A24) , JET_OUTPUT)  /* gpio_48 -TCXO CLK REQ IN*/ \
    MV(CP(GPMC_A25) , JET_OUTPUT)  /* OFM (Finger Navigation) gpio_49 -SHTDWN/standby - high value*/ \
    MV(CP(GPMC_NCS0) , JET_FLOAT)  /* gpio_50 not used */ \
    MV(CP(GPMC_NCS1) ,JET_SAFEMODE)  /* not used*/ \
    MV(CP(GPMC_NCS2) , JET_OUTPUT|PTD)  /* gpio_52 -+A3.6V_EN Enable pin for Kopin, set low to remove power surge for boot since we have pull down resistor on board*/ \
    MV(CP(GPMC_NCS3) ,JET_INPUT)  /* gpio_53 -WLAN_IRQ_SDIO Wlan SDIO IRQ */ \
    MV(CP(GPMC_NWP) , JET_INPUT)  /* gpio_54 -Pressure INT1*/ \
    MV(CP(GPMC_CLK) , JET_FLOAT)  /* gpio_55 not used*/ \
    MV(CP(GPMC_NADV_ALE) ,JET_INPUT)  /* gpio_56 -EYE_TRACING*/ \
    MV(CP(GPMC_NOE) , ( PTU | IEN | M1))  /* sdmmc2_clk*/ \
    MV(CP(GPMC_NWE) , ( PTU | IEN | M1))  /* sdmmc2_cmd */ \
    MV(CP(GPMC_NBE0_CLE) , JET_INPUT)  /* Free Fall IRQ gpio_59 */ \
    MV(CP(GPMC_NBE1) , JET_INPUT)  /* TAP TAP IRQ gpio_60 */ \
    MV(CP(GPMC_WAIT0) , JET_INPUT)  /* gpio_61 */ \
    MV(CP(GPMC_WAIT1) , JET_INPUT)  /* gpio_62 */ \
    MV(CP(C2C_DATA11) , JET_SAFEMODE)  /* gpio_100 */ \
    MV(CP(C2C_DATA12) , JET_SAFEMODE)  /* dsi1_te0 */ \
    MV(CP(C2C_DATA13) , JET_SAFEMODE)  /* gpio_102 */ \
    MV(CP(C2C_DATA14) , JET_SAFEMODE)  /* dsi2_te0 */ \
    MV(CP(C2C_DATA15) , JET_SAFEMODE)  /* gpio_104 */ \
    MV(CP(HDMI_HPD) , JET_SAFEMODE)  /* hdmi_hpd */ \
    MV(CP(HDMI_CEC) , JET_SAFEMODE)  /* hdmi_cec */ \
    MV(CP(HDMI_DDC_SCL) , JET_SAFEMODE)  /* hdmi_ddc_scl */ \
    MV(CP(HDMI_DDC_SDA) , JET_SAFEMODE)  /* hdmi_ddc_sda */ \
    MV(CP(CSI21_DX0) , (M0))  /* csi21_dx0 Camera CLK (N) */ \
    MV(CP(CSI21_DY0) , (M0))  /* csi21_dy0 Camera CLK (P) */ \
    MV(CP(CSI21_DX1) , ( PTU | IEN | M0)) /* csi21_dx1 Camera Data1 (N) */ \
    MV(CP(CSI21_DY1) , ( PTU | IEN | M0)) /* csi21_dy1 Camera Data1 (P) */ \
    MV(CP(CSI21_DX2) , ( PTU | IEN | M0)) /* csi21_dx2 Camera Data2 (N) */ \
    MV(CP(CSI21_DY2) , ( PTU | IEN | M0)) /* csi21_dy2 Camera Data2 (P) */ \
    MV(CP(CSI21_DX3) , JET_SAFEMODE)  /* csi21_dx3 Not-Used */ \
    MV(CP(CSI21_DY3) , JET_SAFEMODE)  /* csi21_dy3 Not-Used */ \
    MV(CP(CSI21_DX4) , JET_SAFEMODE)  /* csi21_dx4 Not-Used */ \
    MV(CP(CSI21_DY4) , JET_SAFEMODE)  /* csi21_dy4 Not-Used */ \
    MV(CP(CSI22_DX0) , JET_SAFEMODE)  /* csi22_dx0 Not-Used */ \
    MV(CP(CSI22_DY0) , JET_SAFEMODE)  /* csi22_dy0 Not-Used */ \
    MV(CP(CSI22_DX1) , JET_SAFEMODE)  /* csi22_dx1 Not-Used */ \
    MV(CP(CSI22_DY1) , JET_SAFEMODE)  /* csi22_dy1 Not-Used */ \
    MV(CP(CAM_SHUTTER) , JET_SAFEMODE) /* cam_shutter - Not-Used */ \
    MV(CP(CAM_STROBE) , JET_SAFEMODE)  /* cam_strobe - Not-Used */ \
    MV(CP(CAM_GLOBALRESET) , (JET_OUTPUT|OFF_EN|OFF_PD)) /* gpio_83 Camera Global Reset */ \
    MV(CP(USBB1_ULPITLL_CLK) , JET_SAFEMODE)  /* hsi1_cawake */ \
    MV(CP(USBB1_ULPITLL_STP) , JET_SAFEMODE)  /* hsi1_cadata */ \
    MV(CP(USBB1_ULPITLL_DIR) , JET_SAFEMODE)  /* hsi1_caflag */ \
    MV(CP(USBB1_ULPITLL_NXT) , JET_SAFEMODE)  /* hsi1_acready */ \
    MV(CP(USBB1_ULPITLL_DAT0) , JET_SAFEMODE)  /* hsi1_acwake */ \
    MV(CP(USBB1_ULPITLL_DAT1) , JET_SAFEMODE)  /* hsi1_acdata */ \
    MV(CP(USBB1_ULPITLL_DAT2) , JET_SAFEMODE)  /* hsi1_acflag */ \
    MV(CP(USBB1_ULPITLL_DAT3) , JET_SAFEMODE)  /* hsi1_caready */ \
    MV(CP(USBB1_ULPITLL_DAT4) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat4 */ \
    MV(CP(USBB1_ULPITLL_DAT5) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat5 */ \
    MV(CP(USBB1_ULPITLL_DAT6) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat6 */ \
    MV(CP(USBB1_ULPITLL_DAT7) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat7 */ \
    MV(CP(USBB1_HSIC_DATA) , JET_SAFEMODE)  /* usbb1_hsic_data */ \
    MV(CP(USBB1_HSIC_STROBE) ,JET_SAFEMODE)  /* usbb1_hsic_strobe */ \
    MV(CP(USBC1_ICUSB_DP) , JET_SAFEMODE)  /* usbc1_icusb_dp */ \
    MV(CP(USBC1_ICUSB_DM) , JET_SAFEMODE)  /* usbc1_icusb_dm */ \
    MV(CP(SDMMC1_CLK) , JET_SAFEMODE)  /* sdmmc1_clk not used*/ \
    MV(CP(SDMMC1_CMD) , JET_SAFEMODE)  /* sdmmc1_cmd not used-  has external pullups on eMMC board*/ \
    MV(CP(SDMMC1_DAT0) , JET_SAFEMODE)  /* sdmmc1_dat0 not used*/ \
    MV(CP(SDMMC1_DAT1) , JET_SAFEMODE)  /* sdmmc1_dat1 not used*/ \
    MV(CP(SDMMC1_DAT2) , JET_SAFEMODE)  /* sdmmc1_dat2 not used*/ \
    MV(CP(SDMMC1_DAT3) , JET_SAFEMODE)  /* sdmmc1_dat3 not used*/ \
    MV(CP(SDMMC1_DAT4) , JET_SAFEMODE)  /* sdmmc1_dat4 not used*/ \
    MV(CP(SDMMC1_DAT5) , JET_SAFEMODE)  /* sdmmc1_dat5 not used*/ \
    MV(CP(SDMMC1_DAT6) , JET_SAFEMODE)  /* sdmmc1_dat6 not used*/ \
    MV(CP(SDMMC1_DAT7) , JET_SAFEMODE)  /* sdmmc1_dat7 not used*/ \
    MV(CP(ABE_MCBSP2_CLKX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_clkx */ \
    MV(CP(ABE_MCBSP2_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dr */ \
    MV(CP(ABE_MCBSP2_DX) ,  ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dx */ \
    MV(CP(ABE_MCBSP2_FSX) ,  ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_fsx */ \
    MV(CP(ABE_MCBSP1_CLKX) , (IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_clkx */ \
    MV(CP(ABE_MCBSP1_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dr */ \
    MV(CP(ABE_MCBSP1_DX) , ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dx */ \
    MV(CP(ABE_MCBSP1_FSX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_fsx */ \
    MV(CP(ABE_PDM_UL_DATA) , JET_SAFEMODE)  /* abe_pdm_ul_data */ \
    MV(CP(ABE_PDM_DL_DATA) , JET_SAFEMODE)  /* abe_pdm_dl_data */ \
    MV(CP(ABE_PDM_FRAME) , JET_SAFEMODE)  /* abe_pdm_frame */ \
    MV(CP(ABE_PDM_LB_CLK) , JET_SAFEMODE)  /* abe_pdm_lb_clk */ \
    MV(CP(ABE_CLKS) , ( M0))  /* abe_clks */ \
    MV(CP(ABE_DMIC_CLK1) , ( M0))  /* abe_dmic_clk1 */ \
    MV(CP(ABE_DMIC_DIN1) , ( IEN | M0))  /* abe_dmic_din1 */ \
    MV(CP(ABE_DMIC_DIN2) , ( IEN | M0))  /* abe_dmic_din2 */ \
    MV(CP(ABE_DMIC_DIN3) , JET_SAFEMODE)  /* abe_dmic_din3 */ \
    MV(CP(UART2_CTS) , ( PTU | IEN | M0))  /* uart2_cts */ \
    MV(CP(UART2_RTS) , ( M0))  /* uart2_rts */ \
    MV(CP(UART2_RX) , ( PTU | IEN | M0))  /* uart2_rx */ \
    MV(CP(UART2_TX) , ( M0))  /* uart2_tx */ \
    MV(CP(HDQ_SIO) , JET_OUTPUT )  /* gpio_127 -HDQ0_1-Wire_Fuel_GAUGE, debug pin */ \
    MV(CP(I2C1_SCL) , ( PTU | IEN | M0))  /* i2c1_scl */ \
    MV(CP(I2C1_SDA) , ( PTU | IEN | M0))  /* i2c1_sda */ \
    MV(CP(I2C2_SCL) , ( PTU | IEN | M0))  /* i2c2_scl */ \
    MV(CP(I2C2_SDA) , ( PTU | IEN | M0))  /* i2c2_sda */ \
    MV(CP(I2C3_SCL) , ( PTU | IEN | M0))  /* i2c3_scl */ \
    MV(CP(I2C3_SDA) , ( PTU | IEN | M0))  /* i2c3_sda */ \
    MV(CP(I2C4_SCL) , ( PTU | IEN | M0))  /* i2c4_scl */ \
    MV(CP(I2C4_SDA) , ( PTU | IEN | M0))  /* i2c4_sda */ \
    MV(CP(MCSPI1_CLK) , JET_SAFEMODE)  /* mcspi1_clk */ \
    MV(CP(MCSPI1_SOMI) , JET_SAFEMODE)  /* mcspi1_somi */ \
    MV(CP(MCSPI1_SIMO) , JET_SAFEMODE)  /* mcspi1_simo */ \
    MV(CP(MCSPI1_CS0) , JET_SAFEMODE)  /* mcspi1_cs0 */ \
    MV(CP(MCSPI1_CS1) , JET_SAFEMODE)  /* not used */ \
    MV(CP(MCSPI1_CS2) , JET_SAFEMODE)  /* not used  */ \
    MV(CP(MCSPI1_CS3) , JET_SAFEMODE)  /* not used  */ \
    MV(CP(UART3_CTS_RCTX) ,JET_SAFEMODE)  /* uart1_tx */ \
    MV(CP(UART3_RTS_SD) , JET_SAFEMODE)  /* uart3_rts_sd */ \
    MV(CP(UART3_RX_IRRX) , ( IEN | M0))  /* uart3_rx  (IEN | M0)*/ \
    MV(CP(UART3_TX_IRTX) , ( IEN | M0))  /* uart3_tx ( M0)*/ \
    MV(CP(SDMMC5_CLK) , ( PTU | IEN |  M0))  /* sdmmc5_clk */ \
    MV(CP(SDMMC5_CMD) , ( PTU | IEN | M0))  /* sdmmc5_cmd */ \
    MV(CP(SDMMC5_DAT0) , ( PTU | IEN |M0))  /* sdmmc5_dat0 */ \
    MV(CP(SDMMC5_DAT1) , ( PTU | IEN | M0))  /* sdmmc5_dat1 */ \
    MV(CP(SDMMC5_DAT2) , ( PTU | IEN | M0))  /* sdmmc5_dat2 */ \
    MV(CP(SDMMC5_DAT3) , ( PTU | IEN |  M0))  /* sdmmc5_dat3 */ \
    MV(CP(MCSPI4_CLK) , JET_SAFEMODE)  /* not used */ \
    MV(CP(MCSPI4_SIMO) , JET_SAFEMODE)  /* not used*/ \
    MV(CP(MCSPI4_SOMI) , JET_SAFEMODE)  /*  not used*/ \
    MV(CP(MCSPI4_CS0) , JET_SAFEMODE)  /* not used*/ \
    MV(CP(UART4_RX) , JET_SAFEMODE)  /* not used */ \
    MV(CP(UART4_TX) , JET_SAFEMODE)  /* not used */ \
    MV(CP(USBB2_ULPITLL_CLK) , JET_SAFEMODE)  /* safemode */ \
    MV(CP(USBB2_ULPITLL_STP) , JET_FLOAT)  /* gpio_158 */ \
    MV(CP(USBB2_ULPITLL_DIR) , JET_FLOAT)  /* gpio_159 */ \
    MV(CP(USBB2_ULPITLL_NXT) , JET_FLOAT)  /* gpio_160 */ \
    MV(CP(USBB2_ULPITLL_DAT0) , JET_FLOAT)  /* gpio_161 */ \
    MV(CP(USBB2_ULPITLL_DAT1) , JET_FLOAT)  /* gpio_162 */ \
    MV(CP(USBB2_ULPITLL_DAT2) , JET_OUTPUT)  /* gpio_163 - Control RFBI A16 */ \
    MV(CP(USBB2_ULPITLL_DAT3) , ( IEN | M6))  /* dispc2_data15 */ \
    MV(CP(USBB2_ULPITLL_DAT4) , ( IEN | M6))  /* dispc2_data14 */ \
    MV(CP(USBB2_ULPITLL_DAT5) , ( IEN | M6))  /* dispc2_data13 */ \
    MV(CP(USBB2_ULPITLL_DAT6) , ( IEN | M6))  /* dispc2_data12 */ \
    MV(CP(USBB2_ULPITLL_DAT7) , ( IEN | M6))  /* dispc2_data11 */ \
    MV(CP(USBB2_HSIC_DATA) , JET_SAFEMODE)  /* gpio_169 */ \
    MV(CP(USBB2_HSIC_STROBE) , JET_SAFEMODE)  /* gpio_170 */ \
    MV(CP(UNIPRO_TX0) , JET_SAFEMODE)  /* kpd_col0 */ \
    MV(CP(UNIPRO_TY0) , JET_SAFEMODE)  /* kpd_col1 */ \
    MV(CP(UNIPRO_TX1) , JET_SAFEMODE)  /* kpd_col2 */ \
    MV(CP(UNIPRO_TY1) , JET_SAFEMODE)  /* kpd_col3 */ \
    MV(CP(UNIPRO_TX2) , JET_SAFEMODE)  /* kpd_col4 */ \
    MV(CP(UNIPRO_TY2) , JET_SAFEMODE)  /* kpd_col5 */ \
    MV(CP(UNIPRO_RX0) , JET_SAFEMODE)  /* Finger Navigation dome key GPIO_175, not used  */ \
    MV(CP(UNIPRO_RY0) , JET_INPUT)  /* Finger Navigation GPIO_176 EVENT_INT (MOTION_INT) */ \
    MV(CP(UNIPRO_RX1) , JET_SAFEMODE)  /* kpd_row2 */ \
    MV(CP(UNIPRO_RY1) , JET_SAFEMODE)  /* kpd_row3 */ \
    MV(CP(UNIPRO_RX2) , JET_SAFEMODE)  /* kpd_row4 */ \
    MV(CP(UNIPRO_RY2) , JET_SAFEMODE)  /* kpd_row5 */ \
    MV(CP(USBA0_OTG_CE) , ( IEN | PTD |  M0))  /* usba0_otg_ce  - ENABLE CHARGER BY DEFAULT */ \
    MV(CP(USBA0_OTG_DP) , ( IEN |  M0))  /* usba0_otg_dp */ \
    MV(CP(USBA0_OTG_DM) , ( IEN |  M0))  /* usba0_otg_dm */ \
    MV(CP(FREF_CLK1_OUT) , (M0))  /* fref_clk1_out */ \
    MV(CP(FREF_CLK2_OUT) , (M0))  /* fref_clk2_out */ \
    MV(CP(SYS_NIRQ1) , ( PTU | IEN | M0))  /* sys_nirq1 */ \
    MV(CP(SYS_NIRQ2) , (M7_SAFE))  /* sys_nirq2 */ \
    MV(CP(SYS_BOOT0) , (  M7))  /* gpio_184 */ \
    MV(CP(SYS_BOOT1) , (  M7))  /* gpio_185 */ \
    MV(CP(SYS_BOOT2) , (  M7))  /* gpio_186 */ \
    MV(CP(SYS_BOOT3) , (  M7))  /* gpio_187 */ \
    MV(CP(SYS_BOOT4) , (  M7))  /* gpio_188 */ \
    MV(CP(SYS_BOOT5) , (  M7))  /* gpio_189 */ \
    MV(CP(DPM_EMU0) , JET_SAFEMODE)  /* dpm_emu0 */ \
    MV(CP(DPM_EMU1) , JET_SAFEMODE)  /* dpm_emu1 */ \
    MV(CP(DPM_EMU2) , (JET_FLOAT))  /* gpio_13 */ \
    MV(CP(DPM_EMU3) , ( IEN | M4))  /* dispc2_data10 */ \
    MV(CP(DPM_EMU4) , ( IEN | M4))  /* dispc2_data9 */ \
    MV(CP(DPM_EMU5) , ( IEN | M4))  /* dispc2_data16 */ \
    MV(CP(DPM_EMU6) , JET_FLOAT)  /* gpio_17 */ \
    MV(CP(DPM_EMU7) , ( IEN| M4))  /* dispc2_hsync */ \
    MV(CP(DPM_EMU8) , ( IEN| M4))  /* dispc2_pclk */ \
    MV(CP(DPM_EMU9) , ( IEN| M4))  /* dispc2_vsync */ \
    MV(CP(DPM_EMU10) , ( IEN| M4))  /* dispc2_de */ \
    MV(CP(DPM_EMU11) , ( IEN| M4))  /* dispc2_data8 */ \
    MV(CP(DPM_EMU12) , ( IEN| M4))  /* dispc2_data7 */ \
    MV(CP(DPM_EMU13) , ( IEN| M4))  /* dispc2_data6 */ \
    MV(CP(DPM_EMU14) , ( IEN| M4))  /* dispc2_data5 */ \
    MV(CP(DPM_EMU15) , ( IEN| M4))  /* dispc2_data4 */ \
    MV(CP(DPM_EMU16) , ( IEN| M4))  /* gpio_27 */ \
    MV(CP(DPM_EMU17) , ( IEN| M4))  /* dispc2_data2 */ \
    MV(CP(DPM_EMU18) , ( IEN| M4))  /* dispc2_data1 */ \
    MV(CP(DPM_EMU19) , ( IEN| M4))  /* dispc2_data0 */ \
    MV1(WK(PAD0_SIM_IO) , JET_SAFEMODE)  /* sim_io */ \
    MV1(WK(PAD1_SIM_CLK) , JET_SAFEMODE)  /* sim_clk */ \
    MV1(WK(PAD0_SIM_RESET) , JET_SAFEMODE)  /* sim_reset */ \
    MV1(WK(PAD1_SIM_CD) , JET_SAFEMODE)  /* sim_cd */ \
    MV1(WK(PAD0_SIM_PWRCTRL) , JET_SAFEMODE)  /* sim_pwrctrl */ \
    MV1(WK(PAD1_SR_SCL) , ( PTU | IEN | M0))  /* sr_scl */ \
    MV1(WK(PAD0_SR_SDA) , ( PTU | IEN | M0))  /* sr_sda */ \
    MV1(WK(PAD1_FREF_XTAL_IN) , JET_FLOAT_NOGPIO)  /* # */ \
    MV1(WK(PAD0_FREF_SLICER_IN) , ( IEN | M0))  /* fref_slicer_in */ \
    MV1(WK(PAD1_FREF_CLK_IOREQ) , ( IEN | M0))  /* fref_clk_ioreq */ \
    MV1(WK(PAD0_FREF_CLK0_OUT) , ( IEN | M2))  /* sys_drm_msecure (M3 is another option)*/ \
    MV1(WK(PAD1_FREF_CLK3_REQ), JET_SAFEMODE) /* gpio_wk30 */ \
    MV1(WK(PAD0_FREF_CLK3_OUT) , JET_SAFEMODE)  /* fref_clk3_out */ \
    MV1(WK(PAD1_FREF_CLK4_REQ), JET_FLOAT) /* Not used*/ \
    MV1(WK(PAD0_FREF_CLK4_OUT), JET_FLOAT) /* gpio_wk8 */ \
    MV1(WK(PAD1_SYS_32K) , ( IEN | M0))  /* sys_32k */ \
    MV1(WK(PAD0_SYS_NRESPWRON) , (IEN | M0))  /* sys_nrespwron - has external pull down*/ \
    MV1(WK(PAD1_SYS_NRESWARM) , (IEN |M0))  /* sys_nreswarm - has external pullup */ \
    MV1(WK(PAD0_SYS_PWR_REQ) , ( IEN | M0))  /* sys_pwr_req */ \
    MV1(WK(PAD1_SYS_PWRON_RESET) , JET_INPUT)  /* gpio_wk29*/ \
    MV1(WK(PAD0_SYS_BOOT6) , ( IEN | M7))  /* gpio_wk9 */ \
    MV1(WK(PAD1_SYS_BOOT7) , ( IEN | M7))  /* gpio_wk10 */

#else //Snow
#define MUX_DEFAULT_OMAP4() \
    MV(CP(GPMC_AD0) , ( PTU | IEN | M1))  /* sdmmc2_dat0 */ \
    MV(CP(GPMC_AD1) , ( PTU | IEN | M1))  /* sdmmc2_dat1 */ \
    MV(CP(GPMC_AD2) , ( PTU | IEN | M1))  /* sdmmc2_dat2 */ \
    MV(CP(GPMC_AD3) , ( PTU | IEN | M1))  /* sdmmc2_dat3 */ \
    MV(CP(GPMC_AD4) , ( PTU | IEN | M1))  /* sdmmc2_dat4 */ \
    MV(CP(GPMC_AD5) , ( PTU | IEN | M1))  /* sdmmc2_dat5 */ \
    MV(CP(GPMC_AD6) , ( PTU | IEN | M1))  /* sdmmc2_dat6 */ \
    MV(CP(GPMC_AD7) , ( PTU | IEN | M1))  /* sdmmc2_dat7 */ \
    MV(CP(GPMC_AD8) , JET_FLOAT)  /* gpio_32 output low for float pin*/ \
    MV(CP(GPMC_AD9) , JET_FLOAT)   /*gpio_33 */ \
    MV(CP(GPMC_AD10) , JET_FLOAT)  /* gpio_34 */ \
    MV(CP(GPMC_AD11) , JET_FLOAT)  /* gpio_35 */ \
    MV(CP(GPMC_AD12) , JET_FLOAT)  /* gpio_36 */ \
    MV(CP(GPMC_AD13) , JET_FLOAT)  /* gpio_37 */ \
    MV(CP(GPMC_AD14) , JET_FLOAT)  /* gpio_38 */ \
    MV(CP(GPMC_AD15) , JET_OUTPUT)  /* gpio_39 -GPADC_START TWL6030, output no safe mode, drive low*/ \
    MV(CP(GPMC_A16) , JET_FLOAT)  /* gpio_40 */ \
    MV(CP(GPMC_A17) , JET_OUTPUT|OFF_EN|OFF_PD)  /* gpio_41 -CLK_TV_EN output*/  \
    MV(CP(GPMC_A18) , JET_OUTPUT)  /* gpio_42 -Disp_RES output, Kopin reset */ \
    MV(CP(GPMC_A19) , JET_OUTPUT)  /* gpio_43 -WLAN EN output safemode low to disable wlan */ \
    MV(CP(GPMC_A20) , JET_OUTPUT)  /* gpio_44 -RES_MFI chip should be high */ \
    MV(CP(GPMC_A21) , JET_INPUT)  /*gpio_45 -DEN_G tied to 1.8V, set input by dafault*/  \
    MV(CP(GPMC_A22) , JET_OUTPUT)  /* gpio_46 -BT EN */ \
    MV(CP(GPMC_A23) , JET_SAFEMODE)  /* gpio_47 -RES_OFN not used*/ \
    MV(CP(GPMC_A24) , JET_OUTPUT)  /* gpio_48 -TCXO CLK REQ IN*/ \
    MV(CP(GPMC_A25) , JET_OUTPUT)  /* OFM (Finger Navigation) gpio_49 -SHTDWN/standby - high value*/ \
    MV(CP(GPMC_NCS0) , JET_FLOAT)  /* gpio_50 not used */ \
    MV(CP(GPMC_NCS1) ,JET_SAFEMODE)  /* not used*/ \
    MV(CP(GPMC_NCS2) , JET_OUTPUT|PTU)  /* gpio_52 -+A3.6V_EN Enable pin for Kopin, set high to work around for power surge*/ \
    MV(CP(GPMC_NCS3) ,JET_INPUT)  /* gpio_53 -WLAN_IRQ_SDIO Wlan SDIO IRQ */ \
    MV(CP(GPMC_NWP) , JET_INPUT)  /* gpio_54 -Pressure INT1*/ \
    MV(CP(GPMC_CLK) , JET_FLOAT)  /* gpio_55 not used*/ \
    MV(CP(GPMC_NADV_ALE) ,JET_FLOAT)  /* gpio_56 not used*/ \
    MV(CP(GPMC_NOE) , ( PTU | IEN | M1))  /* sdmmc2_clk*/ \
    MV(CP(GPMC_NWE) , ( PTU | IEN | M1))  /* sdmmc2_cmd */ \
    MV(CP(GPMC_NBE0_CLE) , JET_INPUT)  /* Free Fall IRQ gpio_59 */ \
    MV(CP(GPMC_NBE1) , JET_INPUT)  /* TAP TAP IRQ gpio_60 */ \
    MV(CP(GPMC_WAIT0) , JET_INPUT)  /* gpio_61 */ \
    MV(CP(GPMC_WAIT1) , JET_INPUT)  /* gpio_62 */ \
    MV(CP(C2C_DATA11) , JET_SAFEMODE)  /* gpio_100 */ \
    MV(CP(C2C_DATA12) , JET_SAFEMODE)  /* dsi1_te0 */ \
    MV(CP(C2C_DATA13) , JET_SAFEMODE)  /* gpio_102 */ \
    MV(CP(C2C_DATA14) , JET_SAFEMODE)  /* dsi2_te0 */ \
    MV(CP(C2C_DATA15) , JET_SAFEMODE)  /* gpio_104 */ \
    MV(CP(HDMI_HPD) , JET_SAFEMODE)  /* hdmi_hpd */ \
    MV(CP(HDMI_CEC) , JET_SAFEMODE)  /* hdmi_cec */ \
    MV(CP(HDMI_DDC_SCL) , JET_SAFEMODE)  /* hdmi_ddc_scl */ \
    MV(CP(HDMI_DDC_SDA) , JET_SAFEMODE)  /* hdmi_ddc_sda */ \
    MV(CP(CSI21_DX0) , JET_SAFEMODE)   /* csi21_dx0   -  DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY0) ,  JET_SAFEMODE)  /* csi21_dy0 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX1) ,  JET_SAFEMODE)  /* csi21_dx1 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY1) , JET_SAFEMODE)   /* csi21_dy1 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX2) ,JET_SAFEMODE)  /* csi21_dx2 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY2) , JET_SAFEMODE)  /* csi21_dy2 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX3) , JET_SAFEMODE)  /* csi21_dx3 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY3) ,JET_SAFEMODE)  /* csi21_dy3 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX4) , JET_SAFEMODE)  /* csi21_dx4 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY4) , JET_SAFEMODE)  /* csi21_dy4 *DISABLE CAMERA FOR NOW */ \
    MV(CP(CSI22_DX0) , JET_SAFEMODE)  /* csi22_dx0 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI22_DY0) , JET_SAFEMODE)  /* csi22_dy0DISABLE CAMERA FOR NOW */ \
    MV(CP(CSI22_DX1) , JET_SAFEMODE)  /* csi22_dx1 */ \
    MV(CP(CSI22_DY1) , JET_SAFEMODE)  /* csi22_dy1 */ \
    MV(CP(CAM_SHUTTER) , JET_SAFEMODE)  /* cam_shutter */ \
    MV(CP(CAM_STROBE) , JET_SAFEMODE)  /* cam_strobe */ \
    MV(CP(CAM_GLOBALRESET) , JET_SAFEMODE)  /* gpio_83 */ \
    MV(CP(USBB1_ULPITLL_CLK) , JET_SAFEMODE)  /* hsi1_cawake */ \
    MV(CP(USBB1_ULPITLL_STP) , JET_SAFEMODE)  /* hsi1_cadata */ \
    MV(CP(USBB1_ULPITLL_DIR) , JET_SAFEMODE)  /* hsi1_caflag */ \
    MV(CP(USBB1_ULPITLL_NXT) , JET_SAFEMODE)  /* hsi1_acready */ \
    MV(CP(USBB1_ULPITLL_DAT0) , JET_SAFEMODE)  /* hsi1_acwake */ \
    MV(CP(USBB1_ULPITLL_DAT1) , JET_SAFEMODE)  /* hsi1_acdata */ \
    MV(CP(USBB1_ULPITLL_DAT2) , JET_SAFEMODE)  /* hsi1_acflag */ \
    MV(CP(USBB1_ULPITLL_DAT3) , JET_SAFEMODE)  /* hsi1_caready */ \
    MV(CP(USBB1_ULPITLL_DAT4) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat4 */ \
    MV(CP(USBB1_ULPITLL_DAT5) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat5 */ \
    MV(CP(USBB1_ULPITLL_DAT6) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat6 */ \
    MV(CP(USBB1_ULPITLL_DAT7) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat7 */ \
    MV(CP(USBB1_HSIC_DATA) , JET_SAFEMODE)  /* usbb1_hsic_data */ \
    MV(CP(USBB1_HSIC_STROBE) ,JET_SAFEMODE)  /* usbb1_hsic_strobe */ \
    MV(CP(USBC1_ICUSB_DP) , JET_SAFEMODE)  /* usbc1_icusb_dp */ \
    MV(CP(USBC1_ICUSB_DM) , JET_SAFEMODE)  /* usbc1_icusb_dm */ \
    MV(CP(SDMMC1_CLK) , (IEN | M0))  /* sdmmc1_clk */ \
    MV(CP(SDMMC1_CMD) , ( PTU| IEN | M0))  /* sdmmc1_cmd  -  has external pullups on eMMC board*/ \
    MV(CP(SDMMC1_DAT0) , ( PTU| IEN | M0))  /* sdmmc1_dat0 */ \
    MV(CP(SDMMC1_DAT1) , ( PTU|  IEN | M0))  /* sdmmc1_dat1 */ \
    MV(CP(SDMMC1_DAT2) , ( PTU| IEN | M0))  /* sdmmc1_dat2 */ \
    MV(CP(SDMMC1_DAT3) , ( PTU | IEN | M0))  /* sdmmc1_dat3 */ \
    MV(CP(SDMMC1_DAT4) , ( PTU | IEN | M0))  /* sdmmc1_dat4 */ \
    MV(CP(SDMMC1_DAT5) , ( PTU | IEN | M0))  /* sdmmc1_dat5 */ \
    MV(CP(SDMMC1_DAT6) , ( PTU | IEN |  M0))  /* sdmmc1_dat6 */ \
    MV(CP(SDMMC1_DAT7) , ( PTU | IEN | M0))  /* sdmmc1_dat7 */ \
    MV(CP(ABE_MCBSP2_CLKX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_clkx */ \
    MV(CP(ABE_MCBSP2_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dr */ \
    MV(CP(ABE_MCBSP2_DX) ,  ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dx */ \
    MV(CP(ABE_MCBSP2_FSX) ,  ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_fsx */ \
    MV(CP(ABE_MCBSP1_CLKX) , (IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_clkx */ \
    MV(CP(ABE_MCBSP1_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dr */ \
    MV(CP(ABE_MCBSP1_DX) , ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dx */ \
    MV(CP(ABE_MCBSP1_FSX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_fsx */ \
    MV(CP(ABE_PDM_UL_DATA) , JET_SAFEMODE)  /* abe_pdm_ul_data */ \
    MV(CP(ABE_PDM_DL_DATA) , JET_SAFEMODE)  /* abe_pdm_dl_data */ \
    MV(CP(ABE_PDM_FRAME) , JET_SAFEMODE)  /* abe_pdm_frame */ \
    MV(CP(ABE_PDM_LB_CLK) , JET_SAFEMODE)  /* abe_pdm_lb_clk */ \
    MV(CP(ABE_CLKS) , ( M0))  /* abe_clks */ \
    MV(CP(ABE_DMIC_CLK1) , ( M0))  /* abe_dmic_clk1 */ \
    MV(CP(ABE_DMIC_DIN1) , ( IEN | M0))  /* abe_dmic_din1 */ \
    MV(CP(ABE_DMIC_DIN2) , ( IEN | M0))  /* abe_dmic_din2 */ \
    MV(CP(ABE_DMIC_DIN3) , JET_SAFEMODE)  /* abe_dmic_din3 */ \
    MV(CP(UART2_CTS) , ( PTU | IEN | M0))  /* uart2_cts */ \
    MV(CP(UART2_RTS) , ( M0))  /* uart2_rts */ \
    MV(CP(UART2_RX) , ( PTU | IEN | M0))  /* uart2_rx */ \
    MV(CP(UART2_TX) , ( M0))  /* uart2_tx */ \
    MV(CP(HDQ_SIO) , JET_OUTPUT )  /* gpio_127 -HDQ0_1-Wire_Fuel_GAUGE, debug pin */ \
    MV(CP(I2C1_SCL) , ( PTU | IEN | M0))  /* i2c1_scl */ \
    MV(CP(I2C1_SDA) , ( PTU | IEN | M0))  /* i2c1_sda */ \
    MV(CP(I2C2_SCL) , ( PTU | IEN | M0))  /* i2c2_scl */ \
    MV(CP(I2C2_SDA) , ( PTU | IEN | M0))  /* i2c2_sda */ \
    MV(CP(I2C3_SCL) , ( PTU | IEN | M0))  /* i2c3_scl */ \
    MV(CP(I2C3_SDA) , ( PTU | IEN | M0))  /* i2c3_sda */ \
    MV(CP(I2C4_SCL) , ( PTU | IEN | M0))  /* i2c4_scl */ \
    MV(CP(I2C4_SDA) , ( PTU | IEN | M0))  /* i2c4_sda */ \
    MV(CP(MCSPI1_CLK) , JET_SAFEMODE)  /* mcspi1_clk */ \
    MV(CP(MCSPI1_SOMI) , JET_SAFEMODE)  /* mcspi1_somi */ \
    MV(CP(MCSPI1_SIMO) , JET_SAFEMODE)  /* mcspi1_simo */ \
    MV(CP(MCSPI1_CS0) , JET_SAFEMODE)  /* mcspi1_cs0 */ \
    MV(CP(MCSPI1_CS1) , JET_SAFEMODE)  /* not used */ \
    MV(CP(MCSPI1_CS2) , JET_SAFEMODE)  /* not used  */ \
    MV(CP(MCSPI1_CS3) , JET_SAFEMODE)  /* not used  */ \
    MV(CP(UART3_CTS_RCTX) ,JET_SAFEMODE)  /* uart1_tx */ \
    MV(CP(UART3_RTS_SD) , JET_SAFEMODE)  /* uart3_rts_sd */ \
    MV(CP(UART3_RX_IRRX) , ( IEN | M0))  /* uart3_rx  (IEN | M0)*/ \
    MV(CP(UART3_TX_IRTX) , ( IEN | M0))  /* uart3_tx ( M0)*/ \
    MV(CP(SDMMC5_CLK) , ( PTU | IEN |  M0))  /* sdmmc5_clk */ \
    MV(CP(SDMMC5_CMD) , ( PTU | IEN | M0))  /* sdmmc5_cmd */ \
    MV(CP(SDMMC5_DAT0) , ( PTU | IEN |M0))  /* sdmmc5_dat0 */ \
    MV(CP(SDMMC5_DAT1) , ( PTU | IEN | M0))  /* sdmmc5_dat1 */ \
    MV(CP(SDMMC5_DAT2) , ( PTU | IEN | M0))  /* sdmmc5_dat2 */ \
    MV(CP(SDMMC5_DAT3) , ( PTU | IEN |  M0))  /* sdmmc5_dat3 */ \
    MV(CP(MCSPI4_CLK) , JET_SAFEMODE)  /* not used */ \
    MV(CP(MCSPI4_SIMO) , JET_SAFEMODE)  /* not used*/ \
    MV(CP(MCSPI4_SOMI) , JET_SAFEMODE)  /*  not used*/ \
    MV(CP(MCSPI4_CS0) , JET_SAFEMODE)  /* not used*/ \
    MV(CP(UART4_RX) , JET_SAFEMODE)  /* not used */ \
    MV(CP(UART4_TX) , JET_SAFEMODE)  /* not used */ \
    MV(CP(USBB2_ULPITLL_CLK) , JET_SAFEMODE)  /* safemode */ \
    MV(CP(USBB2_ULPITLL_STP) , JET_FLOAT)  /* gpio_158 */ \
    MV(CP(USBB2_ULPITLL_DIR) , JET_FLOAT)  /* gpio_159 */ \
    MV(CP(USBB2_ULPITLL_NXT) , JET_FLOAT)  /* gpio_160 */ \
    MV(CP(USBB2_ULPITLL_DAT0) , JET_FLOAT)  /* gpio_161 */ \
    MV(CP(USBB2_ULPITLL_DAT1) , JET_FLOAT)  /* gpio_162 */ \
    MV(CP(USBB2_ULPITLL_DAT2) , JET_OUTPUT)  /* gpio_163 - Control RFBI A16 */ \
    MV(CP(USBB2_ULPITLL_DAT3) , ( IEN | M6))  /* dispc2_data15 */ \
    MV(CP(USBB2_ULPITLL_DAT4) , ( IEN | M6))  /* dispc2_data14 */ \
    MV(CP(USBB2_ULPITLL_DAT5) , ( IEN | M6))  /* dispc2_data13 */ \
    MV(CP(USBB2_ULPITLL_DAT6) , ( IEN | M6))  /* dispc2_data12 */ \
    MV(CP(USBB2_ULPITLL_DAT7) , ( IEN | M6))  /* dispc2_data11 */ \
    MV(CP(USBB2_HSIC_DATA) , JET_SAFEMODE)  /* gpio_169 */ \
    MV(CP(USBB2_HSIC_STROBE) , JET_SAFEMODE)  /* gpio_170 */ \
    MV(CP(UNIPRO_TX0) , JET_SAFEMODE)  /* kpd_col0 */ \
    MV(CP(UNIPRO_TY0) , JET_SAFEMODE)  /* kpd_col1 */ \
    MV(CP(UNIPRO_TX1) , JET_SAFEMODE)  /* kpd_col2 */ \
    MV(CP(UNIPRO_TY1) , JET_SAFEMODE)  /* kpd_col3 */ \
    MV(CP(UNIPRO_TX2) , JET_SAFEMODE)  /* kpd_col4 */ \
    MV(CP(UNIPRO_TY2) , JET_SAFEMODE)  /* kpd_col5 */ \
    MV(CP(UNIPRO_RX0) , JET_SAFEMODE)  /* kpd_row0 */ \
    MV(CP(UNIPRO_RY0) , JET_SAFEMODE)  /* kpd_row1 */ \
    MV(CP(UNIPRO_RX1) , JET_SAFEMODE)  /* kpd_row2 */ \
    MV(CP(UNIPRO_RY1) , JET_SAFEMODE)  /* kpd_row3 */ \
    MV(CP(UNIPRO_RX2) , JET_SAFEMODE)  /* kpd_row4 */ \
    MV(CP(UNIPRO_RY2) , JET_SAFEMODE)  /* kpd_row5 */ \
    MV(CP(USBA0_OTG_CE) , ( IEN | PTD |  M0))  /* usba0_otg_ce  - ENABLE CHARGER BY DEFAULT */ \
    MV(CP(USBA0_OTG_DP) , ( IEN |  M0))  /* usba0_otg_dp */ \
    MV(CP(USBA0_OTG_DM) , ( IEN |  M0))  /* usba0_otg_dm */ \
    MV(CP(FREF_CLK1_OUT) , JET_SAFEMODE)  /* fref_clk1_out */ \
    MV(CP(FREF_CLK2_OUT) , ( M0))  /* fref_clk2_out */ \
    MV(CP(SYS_NIRQ1) , ( PTU | IEN | M0))  /* sys_nirq1 */ \
    MV(CP(SYS_NIRQ2) , (M7_SAFE))  /* sys_nirq2 */ \
    MV(CP(SYS_BOOT0) , (  M7))  /* gpio_184 */ \
    MV(CP(SYS_BOOT1) , (  M7))  /* gpio_185 */ \
    MV(CP(SYS_BOOT2) , (  M7))  /* gpio_186 */ \
    MV(CP(SYS_BOOT3) , (  M7))  /* gpio_187 */ \
    MV(CP(SYS_BOOT4) , (  M7))  /* gpio_188 */ \
    MV(CP(SYS_BOOT5) , (  M7))  /* gpio_189 */ \
    MV(CP(DPM_EMU0) , JET_SAFEMODE)  /* dpm_emu0 */ \
    MV(CP(DPM_EMU1) , JET_SAFEMODE)  /* dpm_emu1 */ \
    MV(CP(DPM_EMU2) , (JET_FLOAT))  /* gpio_13 */ \
    MV(CP(DPM_EMU3) , ( IEN | M4))  /* dispc2_data10 */ \
    MV(CP(DPM_EMU4) , ( IEN | M4))  /* dispc2_data9 */ \
    MV(CP(DPM_EMU5) , ( IEN | M4))  /* dispc2_data16 */ \
    MV(CP(DPM_EMU6) , JET_FLOAT)  /* gpio_17 */ \
    MV(CP(DPM_EMU7) , ( IEN| M4))  /* dispc2_hsync */ \
    MV(CP(DPM_EMU8) , ( IEN| M4))  /* dispc2_pclk */ \
    MV(CP(DPM_EMU9) , ( IEN| M4))  /* dispc2_vsync */ \
    MV(CP(DPM_EMU10) , ( IEN| M4))  /* dispc2_de */ \
    MV(CP(DPM_EMU11) , ( IEN| M4))  /* dispc2_data8 */ \
    MV(CP(DPM_EMU12) , ( IEN| M4))  /* dispc2_data7 */ \
    MV(CP(DPM_EMU13) , ( IEN| M4))  /* dispc2_data6 */ \
    MV(CP(DPM_EMU14) , ( IEN| M4))  /* dispc2_data5 */ \
    MV(CP(DPM_EMU15) , ( IEN| M4))  /* dispc2_data4 */ \
    MV(CP(DPM_EMU16) , ( IEN| M4))  /* gpio_27 */ \
    MV(CP(DPM_EMU17) , ( IEN| M4))  /* dispc2_data2 */ \
    MV(CP(DPM_EMU18) , ( IEN| M4))  /* dispc2_data1 */ \
    MV(CP(DPM_EMU19) , ( IEN| M4))  /* dispc2_data0 */ \
    MV1(WK(PAD0_SIM_IO) , JET_SAFEMODE)  /* sim_io */ \
    MV1(WK(PAD1_SIM_CLK) , JET_SAFEMODE)  /* sim_clk */ \
    MV1(WK(PAD0_SIM_RESET) , JET_SAFEMODE)  /* sim_reset */ \
    MV1(WK(PAD1_SIM_CD) , JET_SAFEMODE)  /* sim_cd */ \
    MV1(WK(PAD0_SIM_PWRCTRL) , JET_SAFEMODE)  /* sim_pwrctrl */ \
    MV1(WK(PAD1_SR_SCL) , ( PTU | IEN | M0))  /* sr_scl */ \
    MV1(WK(PAD0_SR_SDA) , ( PTU | IEN | M0))  /* sr_sda */ \
    MV1(WK(PAD1_FREF_XTAL_IN) , JET_FLOAT_NOGPIO)  /* # */ \
    MV1(WK(PAD0_FREF_SLICER_IN) , ( IEN | M0))  /* fref_slicer_in */ \
    MV1(WK(PAD1_FREF_CLK_IOREQ) , ( IEN | M0))  /* fref_clk_ioreq */ \
    MV1(WK(PAD0_FREF_CLK0_OUT) , ( IEN | M2))  /* sys_drm_msecure (M3 is another option)*/ \
    MV1(WK(PAD1_FREF_CLK3_REQ), JET_SAFEMODE) /* gpio_wk30 */ \
    MV1(WK(PAD0_FREF_CLK3_OUT) , JET_SAFEMODE)  /* fref_clk3_out */ \
    MV1(WK(PAD1_FREF_CLK4_REQ), JET_FLOAT) /* not used*/ \
    MV1(WK(PAD0_FREF_CLK4_OUT), JET_FLOAT) /* gpio_wk8 */ \
    MV1(WK(PAD1_SYS_32K) , ( IEN | M0))  /* sys_32k */ \
    MV1(WK(PAD0_SYS_NRESPWRON) , (IEN | M0))  /* sys_nrespwron - has external pull down*/ \
    MV1(WK(PAD1_SYS_NRESWARM) , (IEN |M0))  /* sys_nreswarm - has external pullup */ \
    MV1(WK(PAD0_SYS_PWR_REQ) , ( IEN | M0))  /* sys_pwr_req */ \
    MV1(WK(PAD1_SYS_PWRON_RESET) , JET_INPUT)  /* gpio_wk29*/ \
    MV1(WK(PAD0_SYS_BOOT6) , ( IEN | M7))  /* gpio_wk9 */ \
    MV1(WK(PAD1_SYS_BOOT7) , ( IEN | M7))  /* gpio_wk10 */
#endif
#else // Helio
#define MUX_DEFAULT_OMAP4() \
    MV(CP(GPMC_AD0) , JET_FLOAT_NOGPIO)  /* */ \
    MV(CP(GPMC_AD1) , JET_FLOAT_NOGPIO)  /*  */ \
    MV(CP(GPMC_AD2) , JET_FLOAT_NOGPIO)  /*  */ \
    MV(CP(GPMC_AD3) , JET_FLOAT_NOGPIO)  /* */ \
    MV(CP(GPMC_AD4) , JET_FLOAT_NOGPIO)  /*  */ \
    MV(CP(GPMC_AD5) , JET_FLOAT_NOGPIO)  /* */ \
    MV(CP(GPMC_AD6) , JET_FLOAT_NOGPIO)  /*  */ \
    MV(CP(GPMC_AD7) , JET_FLOAT_NOGPIO)  /*  */ \
    MV(CP(GPMC_AD8) , JET_INPUT)    /* gpio_32 input ACT_BUSY (Camera)*/ \
    MV(CP(GPMC_AD9) , JET_FLOAT)    /*gpio_33*/                               /* gpio_33 output low for float pin*/ \
    MV(CP(GPMC_AD10) , JET_FLOAT)  /* gpio_34 */ \
    MV(CP(GPMC_AD11) , JET_FLOAT)  /* gpio_35 */ \
    MV(CP(GPMC_AD12) , JET_FLOAT)  /* gpio_36 */ \
    MV(CP(GPMC_AD13) , JET_OUTPUT) /* gpio_37 output RST_12MP (Camera) */ \
    MV(CP(GPMC_AD14) , JET_FLOAT)  /* gpio_38 */ \
    MV(CP(GPMC_AD15) , JET_OUTPUT)  /* gpio_39 output GPADC_START TWL6030, no safe mode, drive low*/ \
    MV(CP(GPMC_A16) , JET_FLOAT)  /* gpio_40 */ \
    MV(CP(GPMC_A17) , JET_INPUT)  /* gpio_41 input   input from the GPS timestamp*/  \
    MV(CP(GPMC_A18) , JET_SAFEMODE)  /* gpio_42 output  PA enable - disable in safemode for now, add Pull down for now*/ \
    MV(CP(GPMC_A19) , JET_OUTPUT)  /* gpio_43 output WLAN EN safemode low to disable wlan */ \
    MV(CP(GPMC_A20) , JET_OUTPUT)  /* gpio_44 Reset MFI chip   - should be  high */ \
    MV(CP(GPMC_A21) , JET_INPUT)  /* gpio_45 -DEN_G tied to 1.8V, set input by dafault*/ \
    MV(CP(GPMC_A22) , JET_OUTPUT)  /* gpio_46  -BT EN*/ \
    MV(CP(GPMC_A23) , JET_OUTPUT)  /* gpio_47 - optical finger navigation.. assign high by default since active low signal*/ \
    MV(CP(GPMC_A24) ,  M7)  /* gpio_48  TCXO CLK REQ IN  - just let the clock run, has an external pullup*/ \
    MV(CP(GPMC_A25) , JET_OUTPUT)  /* Finger Navigation gpio_49  SHTDWN Shutdown*/ \
    MV(CP(GPMC_NCS0) , JET_INPUT)  /* input GPS PPS */ \
    MV(CP(GPMC_NCS1) ,JET_INPUT)  /* gpio_51   GPS IRQ*/ \
    MV(CP(GPMC_NCS2) , JET_OUTPUT)  /* gpio_52 KOPIN ANALOG 3.6V POWER  */ \
    MV(CP(GPMC_NCS3) ,JET_INPUT)  /* gpio_53 Wlan SDIO IRQ */ \
    MV(CP(GPMC_NWP) , JET_INPUT)  /* gpio_54 */ \
    MV(CP(GPMC_CLK) , JET_INPUT)  /* gpio_55 */ \
    MV(CP(GPMC_NADV_ALE) ,JET_INPUT)  /* gpio_56 */ \
    MV(CP(GPMC_NOE) , JET_FLOAT_NOGPIO)  /* output low for float pin*/ \
    MV(CP(GPMC_NWE) , JET_FLOAT_NOGPIO)  /* sdmmc2_cmd */ \
    MV(CP(GPMC_NBE0_CLE) , JET_INPUT)  /* gpio_59 */ \
    MV(CP(GPMC_NBE1) , JET_INPUT)  /* gpio_60 */ \
    MV(CP(GPMC_WAIT0) , JET_INPUT)  /* gpio_61 */ \
    MV(CP(GPMC_WAIT1) , JET_INPUT)  /* gpio_62 */ \
    MV(CP(C2C_DATA11) , JET_SAFEMODE)  /* gpio_100 */ \
    MV(CP(C2C_DATA12) , JET_SAFEMODE)  /* dsi1_te0 */ \
    MV(CP(C2C_DATA13) , JET_SAFEMODE)  /* gpio_102 */ \
    MV(CP(C2C_DATA14) , JET_SAFEMODE)  /* dsi2_te0 */ \
    MV(CP(C2C_DATA15) , JET_SAFEMODE)  /* gpio_104 */ \
    MV(CP(HDMI_HPD) , JET_SAFEMODE)  /* hdmi_hpd */ \
    MV(CP(HDMI_CEC) , JET_SAFEMODE)  /* hdmi_cec */ \
    MV(CP(HDMI_DDC_SCL) , JET_SAFEMODE)  /* hdmi_ddc_scl */ \
    MV(CP(HDMI_DDC_SDA) , JET_SAFEMODE)  /* hdmi_ddc_sda */ \
    MV(CP(CSI21_DX0) , JET_SAFEMODE)   /* csi21_dx0   -  DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY0) ,  JET_SAFEMODE)  /* csi21_dy0 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX1) ,  JET_SAFEMODE)  /* csi21_dx1 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY1) , JET_SAFEMODE)   /* csi21_dy1 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX2) ,JET_SAFEMODE)  /* csi21_dx2 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY2) , JET_SAFEMODE)  /* csi21_dy2 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX3) , JET_SAFEMODE)  /* csi21_dx3 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY3) ,JET_SAFEMODE)  /* csi21_dy3 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DX4) , JET_SAFEMODE)  /* csi21_dx4 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI21_DY4) , JET_SAFEMODE)  /* csi21_dy4 *DISABLE CAMERA FOR NOW */ \
    MV(CP(CSI22_DX0) , JET_SAFEMODE)  /* csi22_dx0 DISABLE CAMERA FOR NOW*/ \
    MV(CP(CSI22_DY0) , JET_SAFEMODE)  /* csi22_dy0DISABLE CAMERA FOR NOW */ \
    MV(CP(CSI22_DX1) , JET_SAFEMODE)  /* csi22_dx1 */ \
    MV(CP(CSI22_DY1) , JET_SAFEMODE)  /* csi22_dy1 */ \
    MV(CP(CAM_SHUTTER) , JET_SAFEMODE)  /* cam_shutter */ \
    MV(CP(CAM_STROBE) , JET_SAFEMODE)  /* cam_strobe */ \
    MV(CP(CAM_GLOBALRESET) , JET_SAFEMODE)  /* gpio_83 */ \
    MV(CP(USBB1_ULPITLL_CLK) , JET_SAFEMODE)  /* hsi1_cawake */ \
    MV(CP(USBB1_ULPITLL_STP) , JET_SAFEMODE)  /* hsi1_cadata */ \
    MV(CP(USBB1_ULPITLL_DIR) , JET_SAFEMODE)  /* hsi1_caflag */ \
    MV(CP(USBB1_ULPITLL_NXT) , JET_SAFEMODE)  /* hsi1_acready */ \
    MV(CP(USBB1_ULPITLL_DAT0) , JET_SAFEMODE)  /* hsi1_acwake */ \
    MV(CP(USBB1_ULPITLL_DAT1) , JET_SAFEMODE)  /* hsi1_acdata */ \
    MV(CP(USBB1_ULPITLL_DAT2) , JET_SAFEMODE)  /* hsi1_acflag */ \
    MV(CP(USBB1_ULPITLL_DAT3) , JET_SAFEMODE)  /* hsi1_caready */ \
    MV(CP(USBB1_ULPITLL_DAT4) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat4 */ \
    MV(CP(USBB1_ULPITLL_DAT5) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat5 */ \
    MV(CP(USBB1_ULPITLL_DAT6) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat6 */ \
    MV(CP(USBB1_ULPITLL_DAT7) , JET_SAFEMODE)  /* usbb1_ulpiphy_dat7 */ \
    MV(CP(USBB1_HSIC_DATA) , JET_SAFEMODE)  /* usbb1_hsic_data */ \
    MV(CP(USBB1_HSIC_STROBE) ,JET_SAFEMODE)  /* usbb1_hsic_strobe */ \
    MV(CP(USBC1_ICUSB_DP) , JET_SAFEMODE)  /* usbc1_icusb_dp */ \
    MV(CP(USBC1_ICUSB_DM) , JET_SAFEMODE)  /* usbc1_icusb_dm */ \
    MV(CP(SDMMC1_CLK) , (IEN | M0))  /* sdmmc1_clk */ \
    MV(CP(SDMMC1_CMD) , ( PTU| IEN | M0))  /* sdmmc1_cmd  -  has external pullups on eMMC board*/ \
    MV(CP(SDMMC1_DAT0) , ( PTU| IEN | M0))  /* sdmmc1_dat0 */ \
    MV(CP(SDMMC1_DAT1) , ( PTU|  IEN | M0))  /* sdmmc1_dat1 */ \
    MV(CP(SDMMC1_DAT2) , ( PTU| IEN | M0))  /* sdmmc1_dat2 */ \
    MV(CP(SDMMC1_DAT3) , ( PTU | IEN | M0))  /* sdmmc1_dat3 */ \
    MV(CP(SDMMC1_DAT4) , ( PTU | IEN | M0))  /* sdmmc1_dat4 */ \
    MV(CP(SDMMC1_DAT5) , ( PTU | IEN | M0))  /* sdmmc1_dat5 */ \
    MV(CP(SDMMC1_DAT6) , ( PTU | IEN |  M0))  /* sdmmc1_dat6 */ \
    MV(CP(SDMMC1_DAT7) , ( PTU | IEN | M0))  /* sdmmc1_dat7 */ \
    MV(CP(ABE_MCBSP2_CLKX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_clkx */ \
    MV(CP(ABE_MCBSP2_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dr */ \
    MV(CP(ABE_MCBSP2_DX) ,  ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp2_dx */ \
    MV(CP(ABE_MCBSP2_FSX) ,  ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp2_fsx */ \
    MV(CP(ABE_MCBSP1_CLKX) , (IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_clkx */ \
    MV(CP(ABE_MCBSP1_DR) , ( IEN | OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dr */ \
    MV(CP(ABE_MCBSP1_DX) , ( OFF_EN | OFF_OUT_PTD | M0))  /* abe_mcbsp1_dx */ \
    MV(CP(ABE_MCBSP1_FSX) , ( IEN | OFF_EN | OFF_PD | OFF_IN | M0))  /* abe_mcbsp1_fsx */ \
    MV(CP(ABE_PDM_UL_DATA) , JET_SAFEMODE)  /* abe_pdm_ul_data */ \
    MV(CP(ABE_PDM_DL_DATA) , JET_SAFEMODE)  /* abe_pdm_dl_data */ \
    MV(CP(ABE_PDM_FRAME) , JET_SAFEMODE)  /* abe_pdm_frame */ \
    MV(CP(ABE_PDM_LB_CLK) , JET_SAFEMODE)  /* abe_pdm_lb_clk */ \
    MV(CP(ABE_CLKS) , JET_SAFEMODE)  /* abe_clks */ \
    MV(CP(ABE_DMIC_CLK1) , ( M0))  /* abe_dmic_clk1 */ \
    MV(CP(ABE_DMIC_DIN1) , ( IEN | M0))  /* abe_dmic_din1 */ \
    MV(CP(ABE_DMIC_DIN2) , JET_SAFEMODE)  /* abe_dmic_din2 */ \
    MV(CP(ABE_DMIC_DIN3) , JET_SAFEMODE)  /* abe_dmic_din3 */ \
    MV(CP(UART2_CTS) , JET_SAFEMODE)  /* uart2_cts */ \
    MV(CP(UART2_RTS) , JET_SAFEMODE)  /* uart2_rts */ \
    MV(CP(UART2_RX) , ( IEN | M0))  /* uart2_rx */ \
    MV(CP(UART2_TX) , ( IEN|M0))  /* uart2_tx */ \
    MV(CP(HDQ_SIO) , JET_OUTPUT )  /* gpio_127   - AUD_PWRON */ \
    MV(CP(I2C1_SCL) , ( PTU | IEN | M0))  /* i2c1_scl */ \
    MV(CP(I2C1_SDA) , ( PTU | IEN | M0))  /* i2c1_sda */ \
    MV(CP(I2C2_SCL) , ( PTU | IEN | M0))  /* i2c2_scl */ \
    MV(CP(I2C2_SDA) , ( PTU | IEN | M0))  /* i2c2_sda */ \
    MV(CP(I2C3_SCL) , ( PTU | IEN | M0))  /* i2c3_scl */ \
    MV(CP(I2C3_SDA) , ( PTU | IEN | M0))  /* i2c3_sda */ \
    MV(CP(I2C4_SCL) , ( PTU | IEN | M0))  /* i2c4_scl */ \
    MV(CP(I2C4_SDA) , ( PTU | IEN | M0))  /* i2c4_sda */ \
    MV(CP(MCSPI1_CLK) , JET_SAFEMODE)  /* mcspi1_clk */ \
    MV(CP(MCSPI1_SOMI) , JET_SAFEMODE)  /* mcspi1_somi */ \
    MV(CP(MCSPI1_SIMO) , JET_SAFEMODE)  /* mcspi1_simo */ \
    MV(CP(MCSPI1_CS0) , JET_SAFEMODE)  /* mcspi1_cs0 */ \
    MV(CP(MCSPI1_CS1) , ( IEN | M1))  /* uart1_rx */ \
    MV(CP(MCSPI1_CS2) , ( IEN | M1))  /* uart1_cts */ \
    MV(CP(MCSPI1_CS3) , ( IEN | M1))  /* uart1_rts */ \
    MV(CP(UART3_CTS_RCTX) ,( IEN | M1))  /* uart1_tx */ \
    MV(CP(UART3_RTS_SD) , JET_SAFEMODE)  /* uart3_rts_sd */ \
    MV(CP(UART3_RX_IRRX) , ( IEN | M0))  /* uart3_rx  (IEN | M0)*/ \
    MV(CP(UART3_TX_IRTX) , ( IEN | M0))  /* uart3_tx ( M0)*/ \
    MV(CP(SDMMC5_CLK) , ( PTU | IEN |  M0))  /* sdmmc5_clk */ \
    MV(CP(SDMMC5_CMD) , ( PTU | IEN | M0))  /* sdmmc5_cmd */ \
    MV(CP(SDMMC5_DAT0) , ( PTU | IEN |M0))  /* sdmmc5_dat0 */ \
    MV(CP(SDMMC5_DAT1) , ( PTU | IEN | M0))  /* sdmmc5_dat1 */ \
    MV(CP(SDMMC5_DAT2) , ( PTU | IEN | M0))  /* sdmmc5_dat2 */ \
    MV(CP(SDMMC5_DAT3) , ( PTU | IEN |  M0))  /* sdmmc5_dat3 */ \
    MV(CP(MCSPI4_CLK) , M7)  /* program pin disconnect for default */ \
    MV(CP(MCSPI4_SIMO) , M7)  /* program pin disconnect for default */ \
    MV(CP(MCSPI4_SOMI) , ( PTD |  M7))  /*  program pin high for default -  HOLD IN RESET FOR NOW*/ \
    MV(CP(MCSPI4_CS0) , JET_SAFEMODE)  /* mcspi4_cs0 */ \
    MV(CP(UART4_RX) , (  IEN | M0))  /* uart4_rx */ \
    MV(CP(UART4_TX) , (  IEN | M0))  /* uart4_tx */ \
    MV(CP(USBB2_ULPITLL_CLK) , JET_SAFEMODE)  /* safemode */ \
    MV(CP(USBB2_ULPITLL_STP) , JET_FLOAT)  /* gpio_158 */ \
    MV(CP(USBB2_ULPITLL_DIR) , JET_FLOAT)  /* gpio_159 */ \
    MV(CP(USBB2_ULPITLL_NXT) , JET_FLOAT)  /* gpio_160 */ \
    MV(CP(USBB2_ULPITLL_DAT0) , JET_FLOAT)  /* gpio_161 */ \
    MV(CP(USBB2_ULPITLL_DAT1) , JET_FLOAT)  /* gpio_162 */ \
    MV(CP(USBB2_ULPITLL_DAT2) , JET_OUTPUT)  /* gpio_163 - Control RFBI A16 */ \
    MV(CP(USBB2_ULPITLL_DAT3) , ( IEN | M6))  /* dispc2_data15 */ \
    MV(CP(USBB2_ULPITLL_DAT4) , ( IEN | M6))  /* dispc2_data14 */ \
    MV(CP(USBB2_ULPITLL_DAT5) , ( IEN | M6))  /* dispc2_data13 */ \
    MV(CP(USBB2_ULPITLL_DAT6) , ( IEN | M6))  /* dispc2_data12 */ \
    MV(CP(USBB2_ULPITLL_DAT7) , ( IEN | M6))  /* dispc2_data11 */ \
    MV(CP(USBB2_HSIC_DATA) , JET_SAFEMODE)  /* gpio_169 */ \
    MV(CP(USBB2_HSIC_STROBE) , JET_SAFEMODE)  /* gpio_170 */ \
    MV(CP(UNIPRO_TX0) , JET_SAFEMODE)  /* kpd_col0 */ \
    MV(CP(UNIPRO_TY0) , JET_SAFEMODE)  /* kpd_col1 */ \
    MV(CP(UNIPRO_TX1) , JET_SAFEMODE)  /* kpd_col2 */ \
    MV(CP(UNIPRO_TY1) , JET_SAFEMODE)  /* kpd_col3 */ \
    MV(CP(UNIPRO_TX2) , JET_SAFEMODE)  /* kpd_col4 */ \
    MV(CP(UNIPRO_TY2) , JET_SAFEMODE)  /* kpd_col5 */ \
    MV(CP(UNIPRO_RX0) , JET_INPUT)  /* Finger Navigation GPIO_175 SW_CAM_ON */ \
    MV(CP(UNIPRO_RY0) , JET_INPUT)  /* Finger Navigation GPIO_176 EVENT_INT (MOTION_INT) */ \
    MV(CP(UNIPRO_RX1) , JET_SAFEMODE)  /* kpd_row2 */ \
    MV(CP(UNIPRO_RY1) , JET_SAFEMODE)  /* kpd_row3 */ \
    MV(CP(UNIPRO_RX2) , JET_SAFEMODE)  /* kpd_row4 */ \
    MV(CP(UNIPRO_RY2) , JET_SAFEMODE)  /* kpd_row5 */ \
    MV(CP(USBA0_OTG_CE) , ( IEN | PTD |  M0))  /* usba0_otg_ce  - ENABLE CHARGER BY DEFAULT */ \
    MV(CP(USBA0_OTG_DP) , ( IEN |  M0))  /* usba0_otg_dp */ \
    MV(CP(USBA0_OTG_DM) , ( IEN |  M0))  /* usba0_otg_dm */ \
    MV(CP(FREF_CLK1_OUT) , JET_SAFEMODE)  /* fref_clk1_out */ \
    MV(CP(FREF_CLK2_OUT) , JET_SAFEMODE)  /* fref_clk2_out */ \
    MV(CP(SYS_NIRQ1) , ( PTU | IEN | M0))  /* sys_nirq1 */ \
    MV(CP(SYS_NIRQ2) , (M7_SAFE))  /* sys_nirq2 */ \
    MV(CP(SYS_BOOT0) , (  M7))  /* gpio_184 */ \
    MV(CP(SYS_BOOT1) , (  M7))  /* gpio_185 */ \
    MV(CP(SYS_BOOT2) , (  M7))  /* gpio_186 */ \
    MV(CP(SYS_BOOT3) , (  M7))  /* gpio_187 */ \
    MV(CP(SYS_BOOT4) , (  M7))  /* gpio_188 */ \
    MV(CP(SYS_BOOT5) , (  M7))  /* gpio_189 */ \
    MV(CP(DPM_EMU0) , JET_SAFEMODE)  /* dpm_emu0 */ \
    MV(CP(DPM_EMU1) , JET_SAFEMODE)  /* dpm_emu1 */ \
    MV(CP(DPM_EMU2) , (JET_FLOAT))  /* gpio_13 */ \
    MV(CP(DPM_EMU3) , ( IEN | M4))  /* dispc2_data10 */ \
    MV(CP(DPM_EMU4) , ( IEN | M4))  /* dispc2_data9 */ \
    MV(CP(DPM_EMU5) , ( IEN | M4))  /* dispc2_data16 */ \
    MV(CP(DPM_EMU6) , JET_FLOAT)  /* gpio_17 */ \
    MV(CP(DPM_EMU7) , ( IEN| M4))  /* dispc2_hsync */ \
    MV(CP(DPM_EMU8) , ( IEN| M4))  /* dispc2_pclk */ \
    MV(CP(DPM_EMU9) , ( IEN| M4))  /* dispc2_vsync */ \
    MV(CP(DPM_EMU10) , ( IEN| M4))  /* dispc2_de */ \
    MV(CP(DPM_EMU11) , ( IEN| M4))  /* dispc2_data8 */ \
    MV(CP(DPM_EMU12) , ( IEN| M4))  /* dispc2_data7 */ \
    MV(CP(DPM_EMU13) , ( IEN| M4))  /* dispc2_data6 */ \
    MV(CP(DPM_EMU14) , ( IEN| M4))  /* dispc2_data5 */ \
    MV(CP(DPM_EMU15) , ( IEN| M4))  /* dispc2_data4 */ \
    MV(CP(DPM_EMU16) , ( IEN| M4))  /* gpio_27 */ \
    MV(CP(DPM_EMU17) , ( IEN| M4))  /* dispc2_data2 */ \
    MV(CP(DPM_EMU18) , ( IEN| M4))  /* dispc2_data1 */ \
    MV(CP(DPM_EMU19) , ( IEN| M4))  /* dispc2_data0 */ \
    MV1(WK(PAD0_SIM_IO) , JET_SAFEMODE)  /* sim_io */ \
    MV1(WK(PAD1_SIM_CLK) , JET_SAFEMODE)  /* sim_clk */ \
    MV1(WK(PAD0_SIM_RESET) , JET_SAFEMODE)  /* sim_reset */ \
    MV1(WK(PAD1_SIM_CD) , JET_SAFEMODE)  /* sim_cd */ \
    MV1(WK(PAD0_SIM_PWRCTRL) , JET_SAFEMODE)  /* sim_pwrctrl */ \
    MV1(WK(PAD1_SR_SCL) , ( PTU | IEN | M0))  /* sr_scl */ \
    MV1(WK(PAD0_SR_SDA) , ( PTU | IEN | M0))  /* sr_sda */ \
    MV1(WK(PAD1_FREF_XTAL_IN) , JET_FLOAT_NOGPIO)  /* # */ \
    MV1(WK(PAD0_FREF_SLICER_IN) , ( IEN | M0))  /* fref_slicer_in */ \
    MV1(WK(PAD1_FREF_CLK_IOREQ) , ( IEN | M0))  /* fref_clk_ioreq */ \
    MV1(WK(PAD0_FREF_CLK0_OUT) , ( IEN | M2))  /* sys_drm_msecure (M3 is another option)*/ \
    MV1(WK(PAD1_FREF_CLK3_REQ), JET_SAFEMODE) /* gpio_wk30 */ \
    MV1(WK(PAD0_FREF_CLK3_OUT) , JET_SAFEMODE)  /* fref_clk3_out */ \
    MV1(WK(PAD1_FREF_CLK4_REQ), JET_FLOAT) /* gpio_wk7   -  These need to be enabled as inputs*/ \
    MV1(WK(PAD0_FREF_CLK4_OUT), JET_FLOAT) /* gpio_wk8 */ \
    MV1(WK(PAD1_SYS_32K) , ( IEN | M0))  /* sys_32k */ \
    MV1(WK(PAD0_SYS_NRESPWRON) , (IEN | M0))  /* sys_nrespwron - has external pull down*/ \
    MV1(WK(PAD1_SYS_NRESWARM) , (IEN |M0))  /* sys_nreswarm - has external pullup */ \
    MV1(WK(PAD0_SYS_PWR_REQ) , ( IEN | M0))  /* sys_pwr_req */ \
    MV1(WK(PAD1_SYS_PWRON_RESET) , JET_INPUT)  /* gpio_wk29  -  MAKE AN INPUT WITH PULL DOWN FOR NOW?*/ \
    MV1(WK(PAD0_SYS_BOOT6) , ( IEN | M7))  /* gpio_wk9 */ \
    MV1(WK(PAD1_SYS_BOOT7) , ( IEN | M7))  /* gpio_wk10 */
#endif
/**********************************************************
 * Routine: set_muxconf_regs
 * Description: Setting up the configuration Mux registers
 *              specific to the hardware. Many pins need
 *              to be moved from protect to primary mode.
 *********************************************************/
void set_muxconf_regs(void)
{
    MUX_DEFAULT_OMAP4();

    /*
     * Changes for OMAP4460:
     * gpio_wk7 is used for TPS controlling
     */
    if (omap_revision() >= OMAP4460_ES1_0)
        writew(M3, OMAP44XX_CTRL_PADCONF_WKUP_BASE + CONTROL_WKUP_PAD1_FREF_CLK4_REQ);

    return;
}

/* optionally do something like blinking LED */
void board_hang (void)
{ while (0) {};}
