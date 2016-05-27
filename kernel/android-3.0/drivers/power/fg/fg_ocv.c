/*
 * linux/drivers/power/fg/fg_ocv.c
 *
 * TI Fuel Gauge driver for Linux
 *
 * Copyright (C) 2008-2009 Texas Instruments, Inc.
 * Author: Texas Instruments, Inc.
 *
 * This package is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * THIS PACKAGE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/time.h>
#include <linux/device.h>
#include <linux/power/ti-fg.h>

#include "fg_ocv.h"
#include "fg_math.h"


/* OCV Lookup table */
#define INTERPOLATE_MAX		1000
unsigned short interpolate(unsigned short value,
				unsigned short *table,
				unsigned char size)
{
	unsigned char i;
	unsigned short d;
#ifdef CONFIG_JET_V2
	int delta= INTERPOLATE_MAX/(size-1);
#endif
	for (i = 0; i < size; i++)
		if (value < table[i])
			break;

#ifdef CONFIG_JET_V2
	if (i==0) {
		d=0;
	} else if(i<size){
		d = (value - table[i-1]) * delta;
		d /=  table[i] - table[i-1];
		d = d + (i-1) * delta;
	} else {
		d=1000;
	}
#else
	if ((i > 0)  && (i < size)) {
		d = (value - table[i-1]) * (INTERPOLATE_MAX/(size-1));
		d /=  table[i] - table[i-1];
		d = d + (i-1) * (INTERPOLATE_MAX/(size-1));
	} else {
		d = i * DIV_ROUND_CLOSEST(INTERPOLATE_MAX, size);
	}
#endif
	if (d > 1000)
		d = 1000;

	return d;
}


/*
 * Open Circuit Voltage (OCV) correction routine. This function estimates SOC,
 * based on the voltage.
 */
#ifdef CONFIG_JET_V2
#if defined(CONFIG_JET_SUN)
//ocv_table: "SunglassBatteryTest_02Sep14_05Sep14.xlsx"
//Supported operating range is ambient temperature -10C to +40C
static struct ocv_temperature_config ocv_table[]={
	{	//use 25C 100mA discharge curve "20+ degC" i=0
		.table={
				3470,3620,3648,3668,3688,
				3703,3715,3726,3737,3751,
				3766,3783,3803,3827,3854,
				3885,3916,3944,3987,4032,
				4067,
		},
	},
	{	//use 15C 100mA discharge curve "13 to 19 degC" i=1
		.table={
				3445,3593,3633,3651,3670,
				3685,3697,3707,3718,3732,
				3748,3765,3786,3809,3836,
				3865,3893,3921,3967,4017,
				4050,
		},
	},
	{	//use 10C 100mA discharge curve "8 to 12 degC" i=2
		.table={
				3275,3460,3530,3579,3614,
				3640,3663,3681,3694,3708,
				3720,3735,3755,3780,3806,
				3835,3865,3899,3936,3981,
				4027,
		},
	},
	{	//use 5C 100mA discharge curve "3 to 7 degC" i=3
		.table={
				3250,3405,3481,3520,3569,
				3597,3624,3649,3666,3680,
				3694,3713,3727,3755,3780,
				3808,3836,3873,3905,3953,
				4000,
		},
	},
	{	//use 0C 100mA discharge curve "-2 to 2 degC" i=4
		.table={
				3250,3347,3406,3458,3495,
				3537,3564,3597,3621,3640,
				3654,3672,3687,3714,3737,
				3769,3801,3832,3868,3914,
				3962,
		},
	},
	{	//use -5C 100mA discharge curve "-7 to -3 degC" i=5
		.table={
				3250,3305,3350,3384,3423,
				3463,3495,3520,3551,3575,
				3593,3615,3636,3655,3677,
				3705,3738,3771,3801,3844,
				3904,
		},
	},
	{	//use -10C 100mA discharge curve "-12 to -8 degC" i=6
		.table={
				3250,3279,3305,3333,3363,
				3390,3418,3444,3468,3492,
				3516,3536,3554,3575,3598,
				3623,3650,3684,3719,3757,
				3808,
		},
	},
	{	//use -10C 100mA discharge curve "-17 to -13 degC" i=7
		.table={
				3250,3279,3305,3333,3363,
				3390,3418,3444,3468,3492,
				3516,3536,3554,3575,3598,
				3623,3650,3684,3719,3757,
				3808,
		},
	},
	{	//use -10C (for -20C) 100mA discharge curve "below -19 degC" i=8
		.table={
				3250,3279,3305,3333,3363,
				3390,3418,3444,3468,3492,
				3516,3536,3554,3575,3598,
				3623,3650,3684,3719,3757,
				3808,
		},
	},

};
#else //CONFIG_JET_SNOW
//ocv_table: "SnowConstantPowerTest.xlsx"
//Supported operating range is ambient temperature -20C to +40C
static struct ocv_temperature_config ocv_table[]={
	{	//use ENV25,BAT25 curve "20+ degC" i=0
		.table={
				3572,3613,3641,3665,3683,
				3699,3712,3722,3733,3746,
				3761,3780,3803,3830,3860,
				3889,3920,3952,3986,4023,
				4062,
		},
	},
	{	//use ENV5,BAT6 curve "5C to 17C" i=1
		.table={
				3487,3556,3588,3610,3628,
				3643,3655,3667,3680,3695,
				3713,3732,3755,3779,3807,
				3836,3868,3902,3938,3976,
				4017,
		},
	},
	{	//use ENV0,BAT1 curve "0C to 4C" i=2
		.table={
				3445,3514,3551,3575,3593,
				3608,3621,3634,3648,3664,
				3682,3703,3726,3750,3777,
				3807,3838,3873,3909,3948,
				3989,
		},
	},
	{	//use ENV-5,BAT-3 curve "-4C to -1C" i=3
		.table={
				3386,3454,3493,3520,3540,
				3557,3571,3586,3601,3619,
				3638,3659,3682,3707,3734,
				3763,3795,3828,3866,3906,
				3952,
		},
	},
	{	//use ENV-10,BAT-7 curve "-8C to -5C" i=4
		.table={
				3356,3400,3432,3456,3476,
				3493,3510,3526,3544,3563,
				3583,3605,3629,3654,3681,
				3709,3740,3773,3808,3847,
				3892,
		},
	},
	{	//use ENV-15,BAT-10 "-14C to -9C" i=5
		.table={
				3300,3325,3348,3369,3387,
				3406,3424,3443,3463,3483,
				3504,3526,3550,3575,3602,
				3630,3660,3693,3728,3767,
				3813,
		},
	},
		{	//use ENV-20,BAT-16  "-15C and below" i=6
		.table={
				3250,3263,3278,3290,3305,
				3318,3332,3345,3360,3373,
				3390,3400,3414,3427,3440,
				3452,3465,3475,3489,3524,
				3628,
		},
	},
};
#endif

short voltage_cap_table(unsigned int temp_index, short voltage)
{
	int tmp;
	unsigned short *table= ocv_table[temp_index].table;
	tmp = interpolate(voltage, table,OCV_TABLE_SIZE);
	return (DIV_ROUND_CLOSEST(tmp * MAX_PERCENTAGE, INTERPOLATE_MAX));
}

bool fg_current_debounce_check(struct cell_state *cell)
{
	if(cell->cur <= FG_CURRENT_CHECK){
		if(cell->current_bounce_counter<FG_CURRENT_MAX_COUNTER){
			cell->current_bounce_counter++;
			dev_dbg(cell->dev, "%s\n", __func__);
			return false;//current too high, may cause voltage read unstable
		}
	}
	cell->current_bounce_counter=0;
	return true;
}
bool fg_ocv_check(struct cell_state *cell)
{
	short soc,delta;
	/*Must in discharge*/
	if(cell->cur > 0)
		return false;
	if(cell->prev_voltage - cell->av_voltage < FG_DELTAV_CHECK)
		return false;

	dev_dbg(cell->dev, "%s:%d\n", __func__,cell->prev_voltage);
	if(fg_current_debounce_check(cell)==false)
		return false;

	cell->prev_voltage = cell->av_voltage;
	soc=voltage_cap_table(cell->temp_index, cell->av_voltage);
	delta=(cell->soc)-soc;
	if(delta>0){
		dev_dbg(cell->dev, "FG: OCV soc=%d, delta=%d\n", soc,delta);
		if(delta>5){
			cell->soc= soc+(delta/2);
		}else{
			cell->soc=soc;
		}
		cell->nac= DIV_ROUND_CLOSEST(cell->fcc * cell->soc, MAX_PERCENTAGE);
		return true;
	}
	return false;
}
#endif


void fg_ocv(struct cell_state *cell)
{
#ifdef CONFIG_JET_V2
	cell->soc=voltage_cap_table(cell->temp_index, cell->av_voltage);
	cell->nac= DIV_ROUND_CLOSEST(cell->fcc * cell->soc, MAX_PERCENTAGE);
#else
	int tmp;
	tmp = interpolate(cell->av_voltage, cell->config->ocv->table,
		OCV_TABLE_SIZE);
	cell->soc = DIV_ROUND_CLOSEST(tmp * MAX_PERCENTAGE, INTERPOLATE_MAX);
	cell->nac = DIV_ROUND_CLOSEST(tmp * cell->fcc, INTERPOLATE_MAX);
#endif
	dev_dbg(cell->dev, "FG: OCV Correction (%dv, %dmAh, %d%%)\n",
			cell->av_voltage, cell->nac, cell->soc);
	/* Reset EL counter */
	cell->electronics_load = 0;
	cell->cumulative_sleep = 0;
	cell->prev_voltage = cell->av_voltage;
#ifdef SOC_CHANGE_ON_TEMPERATURE
	//cell->fcc_start=cell->fcc;
	cell->nac_start=cell->nac;
#endif
	if (!cell->ocv && cell->init) {
		cell->ocv = true;
		cell->ocv_enter_q = cell->nac;
		dev_dbg(cell->dev, "LRN: Entering OCV, OCVEnterQ = %dmAh\n",
			cell->ocv_enter_q);
	}

	getrawmonotonic(&cell->last_ocv);
}


/* Check if the cell is in Sleep */
bool fg_check_relaxed(struct cell_state *cell)
{
	struct timespec now;
	getrawmonotonic(&now);

	if (!cell->sleep) {
		if (abs(cell->cur) <=
			cell->config->ocv->sleep_enter_current) {

			if (cell->sleep_samples < MAX_UINT8)
				cell->sleep_samples++;

			if (cell->sleep_samples >=
				cell->config->ocv->sleep_enter_samples) {
				/* Entering sleep mode */
				cell->sleep_timer.tv_sec = now.tv_sec;
				cell->el_timer.tv_sec = now.tv_sec;
				cell->sleep = true;
				dev_dbg(cell->dev, "FG CHECK: Sleeping\n");
				cell->calibrate = true;
			}
		} else {
			cell->sleep_samples = 0;
		}
	} else {
		/* The battery cell is Sleeping, checking if need to exit
		   sleep mode count number of seconds that cell spent in
		   sleep */
		cell->cumulative_sleep += now.tv_sec - cell->el_timer.tv_sec;
		cell->el_timer.tv_sec = now.tv_sec;

		/* Check if we need to reset Sleep */
		if (abs(cell->av_current) >
			cell->config->ocv->sleep_exit_current) {

			if (abs(cell->cur) >
				cell->config->ocv->sleep_exit_current) {

				if (cell->sleep_samples < MAX_UINT8)
					cell->sleep_samples++;

			} else {
				cell->sleep_samples = 0;
			}

			/* Check if we need to reset a Sleep timer */
			if (cell->sleep_samples >
				cell->config->ocv->sleep_exit_samples) {
				/* Exit sleep mode */
				cell->sleep_timer.tv_sec = 0;
				cell->sleep = false;
				cell->relax = false;
				dev_dbg(cell->dev,
					"FG CHECK: Not relaxed and not sleeping\n");
			}
		} else {
			cell->sleep_samples = 0;

			if (!cell->relax) {

				if (now.tv_sec-cell->sleep_timer.tv_sec >
					cell->config->ocv->relax_period) {

					cell->relax = true;
					dev_dbg(cell->dev, "FG CHECK: Relaxed\n");
					cell->calibrate = true;
				}
			}
		}
	}

	return cell->relax;
}
