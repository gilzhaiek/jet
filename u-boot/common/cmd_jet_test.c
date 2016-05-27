#include <common.h>
#include <command.h>
#include <i2c.h>
#include <twl6030.h>

#if CONFIG_JET_TEST//Jet extra cmd
#include <asm/arch/gpio.h>
#include <asm/io.h>
#include <asm/arch/mux.h>
#define mdelay(n) ({ unsigned long msec = (n); while (msec--) udelay(1000); })

#define TWL6030_CHIP_CHARGER	0x49

#define LINEAR_CHRG_STS		0xDE
#define END_OF_CHARGE		0x20

#define CHARGERUSB_VSYSREG		0xDC
#define TRACKING_DISABLE		0x80
#define VSYS_4P4	0x2D
#define VSYS_3P54	0x2
/**
 * example:
 * charge to test PMIC end of charge
 * charge 0 to shut down device
 */
int do_charge(cmd_tbl_t * cmdtp, int flag, int argc, char *argv[])
{
	u8 val;
	ulong cmd;
	if(argc > 1){
		cmd = simple_strtoul(argv[1], NULL, 10);
		if(cmd==0){
			printf("shutdown!\n");
			twl6030_power_off();
			return 1;
		}
	}
	while(1){
		i2c_read(TWL6030_CHIP_CHARGER, LINEAR_CHRG_STS, 1, &val, 1);
		//printf("charge state=0x%2x\n",val);
		//udelay();
		
		if(val&END_OF_CHARGE){
#if 0//Test TRACKING_DISABLE only but it seems doesn't solve the issue
			u8 val1;
			i2c_read(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val, 1);
			val1 = val|TRACKING_DISABLE;
			i2c_write(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val1, 1);
			mdelay(1000);
			val1 = (val & (~CHARGERUSB_VSYSREG));
			i2c_write(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val1, 1);
#endif
			val = (VSYS_4P4|TRACKING_DISABLE);
			i2c_write(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val, 1);
			
			i2c_read(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val, 1);
			printf("CHARGERUSB_VSYSREG=0x%2x\n",val);

			mdelay(1000);

			val = VSYS_3P54;
			i2c_write(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val, 1);
			
			i2c_read(TWL6030_CHIP_CHARGER, CHARGERUSB_VSYSREG, 1, &val, 1);
			printf("CHARGERUSB_VSYSREG reset=0x%2x\n",val);
			return 1;
		}
		if (ctrlc()) {
			putc ('\n');
			return 1;
		}
		mdelay(10);
	}
}

#define GPIO_BUTTON_INPUT 143
#define POLLING_TIME 10
int do_button(cmd_tbl_t * cmdtp, int flag, int argc, char *argv[])
{
	int val;
	MV(CP(UART3_RX_IRRX) , ( IEN | M3));//Change to GPIO mode
	omap_set_gpio_direction(GPIO_BUTTON_INPUT, 1); //set input
	int counter=0;
	while(counter<POLLING_TIME){
		counter++;
		val = omap_get_gpio_datain(GPIO_BUTTON_INPUT);
		printf("Button read #%d time: value=%d\n",counter,val);
		mdelay(1000);
	}
	MV(CP(UART3_RX_IRRX) , ( IEN | M0));//Change back to UART3_RX mode
}

U_BOOT_CMD(charge, 2, 1, do_charge,
	   "charge\t- Test PMIC charge\n",
	   "[0]\n  Test full charge[turn off device]\n");

U_BOOT_CMD(button, 1, 1, do_button,
	   "button\t- Change UART3_RX_IRRX to button input\n",
	   "\n  Test second button\n");
#endif