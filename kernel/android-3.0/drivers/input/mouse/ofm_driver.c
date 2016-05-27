/* 
 * ST VD5377 Ultra-low power motion sensor for optical finger navigation
 * 
 * This driver will detect swipe motion (up, down, left right) on the
 * OFN. There is also support for the module's dome push button.  
 */

#include <linux/i2c.h>
#include <linux/gpio.h>
#include <linux/interrupt.h>
#include <linux/input.h>
#include <linux/time.h>
#include <linux/regulator/consumer.h>
#include <linux/delay.h>

/* Local constants */

/* Set OFM_DEBUG to 1 to enable debug printouts */
#define OFM_DEBUG 0

#define DRIVER_NAME   "ofm_driver"

/* Swipe duration time in miliseconds */
#define SWIPE_TIME_MS		200 /* 200ms */

/* Minimum threshold for movement to be considered a Swipe */
#define SWIPE_MOVEMENT_MIN_THRESHOLD_X 35 
#define SWIPE_MOVEMENT_MIN_THRESHOLD_Y 45

/* GPIO pin numbers */
#define OFM_MOTION_GPIO		176 /* OMAP pin[J25] GPI_176 connected to Partron M33C01A/MS37C01A pin.4 MOTION */
#define OFM_STANDBY_GPIO 	49  /* OMAP pin[D20] GPIO_49 connected to Partron M33C01A/MS37C01A pin.3 STANDBY */
#define OFM_PWRDWN_GPIO		37  /* OMAP pin[D18] GPIO_37 connected to Partron MS37C01A pin.11 POWERDOWN */

#define MAX_KEYPAD_CNT	7

#define NO_KEY		-1
#define DOWN_KEY	0
#define RIGHT_KEY	1
#define LEFT_KEY	2
#define UP_KEY		3
#define ENTER_KEY	4
#define BACK_KEY	5
#define HOME_KEY	6

#define KEY_PRESSED	1
#define KEY_RELEASED	0

/* Static function declarations */
static int ofm_i2c_write(struct i2c_client *client, u_int8_t index, u_int8_t data);
static void ofm_swipe_func(struct work_struct *work);
static int ofm_get_device_ver(void);
static int ofm_VD5377_init(void);
static void ofm_AMF_patch(void);
static void ofm_key_press(int code);
static int __devinit ofm_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id);
static __devexit int ofm_i2c_remove(struct i2c_client *client);

/* Local Struct definitions */
struct ofm {
	struct 	delayed_work swipe_work;

	struct	i2c_client *client;
	struct	input_dev *input_dev;

	struct	ofm_pin *ofm_dome_key;
	struct	ofm_pin *ofm_motion;
	struct	ofm_pin *ofm_standby;
	struct	ofm_pin *ofm_pwrdwn;
	
	struct	hrtimer timer_long_press;
	ktime_t long_press_time;
};

/* Struct describing all the pins used in the OFM driver */
struct ofm_pin {
	int 	pin;
	int 	irq;
	char	*name;
};

/* Local variables */
struct ofm ofm_global;

/* Motion detected pin */
static struct ofm_pin ofm_motion = {
	OFM_MOTION_GPIO,
	-1,
	"ofm_motion"
};

/* Standby pin */
static struct ofm_pin ofm_standby = {
	OFM_STANDBY_GPIO,
	-1,
	"ofm_standby"
};

/* Powerdown pin */
static struct ofm_pin ofm_pwrdwn = {
	OFM_PWRDWN_GPIO,
	-1,
	"ofm_pwrdwn"
};

/* keypad to keycode mapping */
unsigned int ofm_keypad_keycode_map[MAX_KEYPAD_CNT] = {
	KEY_DOWN,
	KEY_RIGHT,
	KEY_LEFT,
	KEY_UP,
	KEY_ENTER,
	KEY_BACK,
	KEY_HOME,
};

/* keypad strings */
unsigned char ofm_keypad_keycode_str[MAX_KEYPAD_CNT][20] = {
	"KEY_DOWN",
	"KEY_RIGHT",
	"KEY_LEFT",
	"KEY_UP",
	"KEY_ENTER",
	"KEY_BACK",
	"KEY_HOME",
};

/* i2c_device_id struct */
static const struct i2c_device_id ofm_i2c_id[]={
	{DRIVER_NAME, 0},
	{},
};

MODULE_DEVICE_TABLE(i2c, ofm_i2c_id);

/* OFM i2c driver struct */
static struct i2c_driver ofm_i2c_driver = {
	.probe	= ofm_i2c_probe,
	.remove	= ofm_i2c_remove,
	.id_table = ofm_i2c_id,
    .driver =
    {
        .owner = THIS_MODULE,
        .name = DRIVER_NAME,
    },
};

/***********************************************************************
* NAME :            
*	ofm_swipe_func
*
* DESCRIPTION : 
* 	Callback function to performs the swipe detection.
* 	This function is scheduled on a kernel work queue. 
*
* INPUTS :
*	work - work_struct on the work queue. (not used).
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	None
*
* NOTES :
* 	None          
***********************************************************************/
static void ofm_swipe_func(struct work_struct *work)
{
	s32 reg; 
	s8 x = 0;
	s8 y = 0;
	u8 abs_x = 0;
	u8 abs_y = 0;

#if OFM_DEBUG
	s32 features = 0, feat_count = 0, expo_time = 0;
	
	printk("[OFM] [%s::%s:%d]\n", __FILE__, __FUNCTION__, __LINE__ );

	/* Read the feature count */
	feat_count = i2c_smbus_read_byte_data(ofm_global.client,0x31);
	if(feat_count < 0) {
		printk(KERN_INFO "[OFM] ofm_swipe_func features read error status=%d (0x%x)\n",feat_count, feat_count);
		feat_count = 0;
	}
 
	/* From data sheet, feat_count must be multiplied by 16 to get
	 * actual feature count */
	features = feat_count * 16;

	/* Read the expo time */
	expo_time = i2c_smbus_read_byte_data(ofm_global.client,0x47);
	if(expo_time < 0) {
		printk(KERN_INFO "[OFM] ofm_swipe_func expotime read error status=%d \n",expo_time);
		expo_time = 0;
	}
#endif

	/* Read the X,Y movement register */
	reg = i2c_smbus_read_word_data(ofm_global.client,0x21);

	x = reg & 0xFF;
	y = (reg>>8) & 0xFF;

#if OFM_DEBUG
	printk(KERN_INFO "[OFM] ofm_swipe_func  x=%d  y=%d features=%d expo_time=%d\n", x, y, features, expo_time);
#endif
	
	/* Get the absolute value of each measurement */
	abs_x = abs(x);
	abs_y = abs(y);
	
	/* Determine if the movement was big enough to be a swipe and
	 * find the swipe direction */
	if (abs_x > abs_y) {
		/* Check if movement in X is large enough to be a swipe */
		if (abs_x > SWIPE_MOVEMENT_MIN_THRESHOLD_X) {
			if (x < 0) {
				/* Movement in X is negative, left key! */
				ofm_key_press(LEFT_KEY);
			} 
			else {
				/* Movement in y is positive, right key! */
				ofm_key_press(RIGHT_KEY);
			}
		}
	}
	else if (abs_y > abs_x) {
		/* Check if movement in Y is large enough to be a swipe and
		 * find the swipe direction */
		if (abs_y > SWIPE_MOVEMENT_MIN_THRESHOLD_Y) {
			if (y < 0) {
				/* Movement in Y is negative, down key! */
				ofm_key_press(DOWN_KEY);
			} 
			else {
				/* Movement in Y is positive, up key! */
				ofm_key_press(UP_KEY);
			}
		}	
	}
	
	/* If there is more xy data available, flush it. */
	reg = i2c_smbus_read_byte_data(ofm_global.client,0x23);
	
	/* Bit 3 of the register at 0x23, indicates if there is more
	 * motion available */
	if( ((reg>>3) & 0x01) == 0 )
	{
		// set motion_acc_flush_en (bit 5) to flushes the motion accumulators
		ofm_i2c_write(ofm_global.client,0x23, (u8)(reg | 0x20));
	}
	
	/* Re-enable the motion pin interrupt */
	enable_irq(ofm_global.ofm_motion->irq);

	return;
}

/***********************************************************************
* NAME :            ofm_key_press
*
* DESCRIPTION :     Performs the desired key press 
*
* INPUTS :
*	code - Key to press
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	None
*
* NOTES :
* 	None          
***********************************************************************/
static void ofm_key_press(int code)
{
#if OFM_DEBUG	
	printk("[OFM] ofm_key_press %s KEY_PRESSED\n", (char *)&ofm_keypad_keycode_str[code]);
#endif
	/* Press desired key */
	input_report_key(ofm_global.input_dev, ofm_keypad_keycode_map[code], KEY_PRESSED);
	input_sync(ofm_global.input_dev);
	/* Release it */
	input_report_key(ofm_global.input_dev, ofm_keypad_keycode_map[code], KEY_RELEASED);
	input_sync(ofm_global.input_dev);
}

/***********************************************************************
* NAME :            
* 	ofm_motion_event
*
* DESCRIPTION :     
* 	ISR when the motion pin is asserted 
*
* INPUTS :
*	irq - IRQ number
* 	dev_id - Device ID
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	IRQ_HANDLED
*
* NOTES :
* 	None          
***********************************************************************/
static irqreturn_t ofm_motion_event(int irq, void *dev_id)
{
	struct ofm *ofm = (struct ofm *)dev_id;
	
#if OFM_DEBUG
	printk("[OFM] [%s::%s:%d]\n", __FILE__, __FUNCTION__, __LINE__ );
#endif

	if (!ofm)
	{
		printk("[OFM] ofm_motion_event interrupt error \n");
		return IRQ_HANDLED;
	}
	
	/* Disable the motion pin interrupt */
	disable_irq_nosync(irq);
	
	/* Wait for SWIPE_TIME_MS before running the swipe detection
	 * algorithm */
	schedule_delayed_work(&ofm_global.swipe_work, msecs_to_jiffies(SWIPE_TIME_MS));
	
	
	return IRQ_HANDLED;
}

/***********************************************************************
* NAME :
* 	ofm_get_device_ver
*
* DESCRIPTION :
* 	Reads device version from VD5377 OFN module
*
* INPUTS :
*	None
*
* OUTPUTS :
* 	None
*
* RETURN:
* 	version	- success
* 	erno	- on any error
*
* NOTES :
* 	None
***********************************************************************/
static int ofm_get_device_ver(void)
{
	s32 major = -1;
	s32 minor = -1;
	int version = -1;

	/* Partron OFM device MS37C01A_Rev_1.5 with VD5377 (version 1.0) */

	major = i2c_smbus_read_byte_data(ofm_global.client,0x00); /* MAJOR_REVISION */
	if(major < 0) {
		printk("[OFM] %s:%d Read Error!!! (%d)\n",__FUNCTION__,__LINE__,major);
		return major;
	}

	minor = i2c_smbus_read_byte_data(ofm_global.client,0x01); /* MINOR_REVISOION */
	if(minor < 0) {
		printk("[OFM] %s:%d Read Error!!! (%d)\n",__FUNCTION__,__LINE__,minor);
		return minor;
	}

	version = (int)(minor|major<<8);
	return version;

}

/***********************************************************************
* NAME :            
* 	ofm_VD5377_init
*
* DESCRIPTION :     
* 	Initialize the VD5377 OFN module, with our desired setting 
*
* INPUTS :
*	None
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0		- success
* 	erno	- on any error
*
* NOTES :
* 	None          
***********************************************************************/
static int ofm_VD5377_init(void)
{
	int status = 0;

	/* Set pwrdwn_pin to low, to power up the VD5377 */
	gpio_set_value_cansleep(ofm_global.ofm_pwrdwn->pin, 0); 
	
	/* delay 2ms */
	mdelay(2);

	/* De-assert the STANDBY pin */
	gpio_set_value_cansleep(ofm_global.ofm_standby->pin, 0); 
	
	/* delay 2ms */
	mdelay(2);

	/* Reset the module (software reset) */
	status = ofm_i2c_write(ofm_global.client, 0x16, 0x1E);
	if(status < 0)
		return status;
	
	/* delay 2ms */
	mdelay(2); 

	/* Apply the Automatic Movement Filter Patch (Requireed for automatic power mode) */
	ofm_AMF_patch(); 

	/* Apply our settings */
	
	/* Use the 5x5 high pass filter */ 
	status = ofm_i2c_write(ofm_global.client, 0x28, 0x74);
	if(status < 0)
		return status;
	
	/* Set Min feature (0x19 25x16=400) */
	status = ofm_i2c_write(ofm_global.client, 0x29, 0x19);
	if(status < 0)
		return status;
	
	/* Set Motion pin. PAD output, IO not pulled down, 
	 * Output values from motion detect IP, motion PAD normal config */ 
	status = ofm_i2c_write(ofm_global.client, 0x0C, 0x5A);
	if(status < 0)
		return status;
	
	 /* CPI 400 CPI for X data */
	status = ofm_i2c_write(ofm_global.client, 0x2A, 0x04);
	if(status < 0)
		return status;
	
	/* CPI 400 CPI for Y data */
	status = ofm_i2c_write(ofm_global.client, 0x2B, 0x04);
	if(status < 0)
		return status;
	
	/* Set sunlight mode 0x01(Automatic) */	
	status = ofm_i2c_write(ofm_global.client, 0x51, 0x01);
	if(status < 0)
		return status;
	
	/* X Y Direction invert_x, invert_y [DN=-y, UP=+y, LT=+x, RT=-x] */
	status = ofm_i2c_write(ofm_global.client, 0x27, 0x03);
	if(status < 0)
		return status;

	/* Automatic exposure control */
	/* Auto exposure control 1=Enable */
	status = ofm_i2c_write(ofm_global.client, 0x43, 0x01);
	if(status < 0)
		return status;

	/* Exposure update frequency (0x00 = every frame) */
	status = ofm_i2c_write(ofm_global.client, 0x4b, 0x00);
	if(status < 0)
		return status;

	/* Set ANALOG_CTRL2 DMIB DAC Vref setting = 1.6V
	 * This is recommended register overwrite */
	status = ofm_i2c_write(ofm_global.client, 0x03, 0xFC);
	if(status < 0)
		return status;

	/* Automatic movement filter setting */
	/* Enable auto movement filter, 63 frames, do not reject high light */
	status = ofm_i2c_write(ofm_global.client, 0x8d, 0x7F);
	if(status < 0)
		return status;

	/* 2 ms, 15 sequence */ 
	status = ofm_i2c_write(ofm_global.client, 0x8e, 0xF3);
	if(status < 0)
		return status;

	/* Auto mode sleep latencies */
	/* Sleep 1 (1ms) */
	status = ofm_i2c_write(ofm_global.client, 0x8a, 0x01);
	if(status < 0)
		return status;

	/* Sleep 2 (10ms) */
	status = ofm_i2c_write(ofm_global.client, 0x8b, 0x05);
	if(status < 0)
		return status;

	/* Sleep 3 (10ms) */
	/* We do not want to go any higher than this or else
 	   we might miss the first swipe when the device is sleeping */
	status = ofm_i2c_write(ofm_global.client, 0x8c, 0x05);
	if(status < 0)
		return status;

	/* Start OFN Automatic Mode, MOTION pin active HIGH, STANDBY used,
	 * set host_config_done */
	status = ofm_i2c_write(ofm_global.client, 0x05, 0x1D);
	if(status < 0)
		return status;

	mdelay(2);
	return status;

}

/***********************************************************************
* NAME :            
* 	ofm_AMF_patch
*
* DESCRIPTION :     
*	AMF(Automatic Movement Filter) Patch 
*	Ref. VD5377 Porting Guide Rev 2.0.0
*
* INPUTS :
*	None
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	None
*
* NOTES :
* 	None          
***********************************************************************/
static void ofm_AMF_patch(void)
{

	int8_t tempI2CBUF1[3] = {0x02, 0x02, 0x88};
	int8_t tempI2CBUF2[42] = {
				0x30, 0x09, 0x1C, 0xE5, 0x18, 0x70, 0x16, 0xC2, 0x09, 0xE5,
				0x1C, 0x54, 0x0F, 0xC4, 0x54, 0xF0, 0xFF, 0x90, 0x00, 0x17,
				0xE0, 0x54, 0x0F, 0x4F, 0xF0, 0xC2, 0x08, 0x80, 0x02, 0x15,
				0x18, 0x90, 0x00, 0x17, 0xE0, 0x44, 0x08, 0xF0, 0x02, 0x05, 0x30, 0x22
	};

	int8_t tempI2CBUF3[3] = {0x02, 0x7D, 0x00};
	int8_t tempI2CBUF4[3] = {0x05, 0x29, 0x03};

	int i;
	uint8_t major, minor;

	/* VD5377 rev 1.0 (0.0)
	 * VD5377 rev 2.0 (1.0) */
	major = i2c_smbus_read_byte_data(ofm_global.client,0x00);
	minor = i2c_smbus_read_byte_data(ofm_global.client,0x01);

	if( (major == 1) && (minor == 0) )
	{
		ofm_i2c_write(ofm_global.client,0x14,0x0d);
		ofm_i2c_write(ofm_global.client,0x6f,0x02);

		for(i = 0; i < 3; i++ ){
			ofm_i2c_write(ofm_global.client,0xa0+i,tempI2CBUF1[ i ]);
		}

		for(i = 0 ; i < 42;i++ ){
			ofm_i2c_write(ofm_global.client,0xa3+i,tempI2CBUF2[ i ]);
		}

		for(i = 0 ; i < 3;i++ ){
			ofm_i2c_write(ofm_global.client,0x70+i,tempI2CBUF3[ i ]);
		}
		for(i = 0 ; i < 3;i++ ){
			ofm_i2c_write(ofm_global.client,0x73+i,tempI2CBUF4[ i ]);
		}
		ofm_i2c_write(ofm_global.client,0x6f,0x0d);
	}

}

/***********************************************************************
* NAME :            
* 	ofm_i2c_write
*
* DESCRIPTION :     
*	Helper function to perform i2c write
*
* INPUTS :
*	client	- i2c_client
* 	index 	- destination address
* 	data	- Data to write
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0 		- success
* 	erno	- on any error
*
* NOTES :
* 	None          
***********************************************************************/
static int ofm_i2c_write(struct i2c_client *client, u_int8_t index, u_int8_t data)
{
	int result;
	u_int8_t buf[2] = {index , data};

	result= i2c_master_send(client, buf, 2);
	
#if OFM_DEBUG	
	printk("[OFM] %s(0x%02x, 0x%02x), %d, ret : %d\n", __func__, index, data, __LINE__, result );
#endif

	if(result>=0)
		return 0;

	printk("[OFM] ERROR i2c send!!!index(%x) data(%x) return (%x)\n",index,data,result);
	return result;
}

/***********************************************************************
* NAME :            
* 	ofm_i2c_probe
*
* DESCRIPTION :     
*	OFM driver probe function
*
* INPUTS :
*	client	- i2c_client
* 	id	 	- i2c_device_id
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0 		- success
* 	erno	- on any error
*
* NOTES :
* 	None          
***********************************************************************/
static int __devinit ofm_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id)
{
	int result;
	int	key, code;
	static struct regulator *ofm_switch_reg;

	/* init i2c client */
	ofm_global.client = client;
	i2c_set_clientdata(client, &ofm_global);

	/* Allocation input device */
	ofm_global.input_dev = input_allocate_device();
	if (ofm_global.input_dev == NULL)
	{
		dev_err(&client->dev, "[OFM] Failed to allocate input device.\n");
		result = -ENOMEM;
		goto err_input_allocate;
	}

	/* Init input device */
	ofm_global.input_dev->name = DRIVER_NAME;
	ofm_global.input_dev->dev.init_name = DRIVER_NAME;
	ofm_global.input_dev->id.bustype = BUS_I2C;
	ofm_global.input_dev->dev.parent = &client->dev;
	ofm_global.input_dev->phys = "ofm";
	ofm_global.input_dev->id.vendor = 0x0001;
	ofm_global.input_dev->id.product = 0x0001;
	ofm_global.input_dev->id.version = 0x0100;

	/* Set event key bits */
	set_bit(EV_KEY, ofm_global.input_dev->evbit);
	
	/* Allocation key event bits */
	for(key = 0; key < MAX_KEYPAD_CNT; key++){
		code = ofm_keypad_keycode_map[key];
		if(code<=0)
			continue;
		set_bit(code & KEY_MAX, ofm_global.input_dev->keybit);
	}

	/* Register input device */
	result = input_register_device(ofm_global.input_dev);
	if (result){
		dev_err(&client->dev, "[OFM] %s(%s): Unable to register %s input device\n", __FILE__, __FUNCTION__, ofm_global.input_dev->name);
		goto err_input_reg;
	}

	/* Enable power regulator, device will shutdown during boot up
	 * if not enabled */
	ofm_switch_reg = regulator_get(&ofm_global.input_dev->dev, "ofm_switch");

	if (IS_ERR(ofm_switch_reg)) {
		result = PTR_ERR(ofm_switch_reg);
		printk(KERN_ERR "couldn't get OFM (Finger Nav) Switch regulator %d\n", result);
		goto regulator_err;
	}
	regulator_enable(ofm_switch_reg);

	/* gpio_request for standby and pwrdwn pins are defined in ../arch/arm/mach-omap2/board-4430jet.c */

	/* Init standby pin */
	ofm_global.ofm_standby = &ofm_standby;
	result = gpio_cansleep(ofm_global.ofm_standby->pin);
	if (result) {
		printk(KERN_INFO "[OFM] %s : Key:GPIO %d standby cansleep failed (result=%d)\n",__FUNCTION__, ofm_global.ofm_standby->pin, result);
		goto err_reg_gpio;
	}
	/* Set standby pin to low */
	gpio_set_value_cansleep(ofm_global.ofm_standby->pin, 0); 
	mdelay(1);

	/* Init pwrdwn pin */
	ofm_global.ofm_pwrdwn = &ofm_pwrdwn;
	result = gpio_cansleep(ofm_global.ofm_pwrdwn->pin);
	if (result) {
		printk(KERN_INFO "[OFM] %s : Key:GPIO %d pwrdwn cansleep failed (result=%d)\n",__FUNCTION__, ofm_global.ofm_pwrdwn->pin, result);
		goto err_reg_gpio;
	}
	/* Set pwrdwn pin to high to power down the OFN */
	gpio_set_value_cansleep(ofm_global.ofm_pwrdwn->pin, 1);
        /* Wait the minimum powerdown time of 10ms */
	mdelay(10);
	/*
	 * standby and pwrdwn pins must be initialized before reading OFM device version
	 * work queue and motion interrupts should only be initialized after a valid device found
	 */
	result = ofm_get_device_ver();
	if (result < 0) {
		printk(KERN_INFO "[OFM] %s : device not found error (%d) \n",__FUNCTION__,result);
		goto err_ofm_missing;
	}

	/* Init swipe work struct, must be init before interrupts */
	INIT_DELAYED_WORK(&ofm_global.swipe_work, ofm_swipe_func);

	/* set motion pin interrupt */
	ofm_motion.irq = gpio_to_irq(ofm_motion.pin);

	ofm_global.ofm_motion = &ofm_motion;
	result = request_irq (ofm_global.ofm_motion->irq, ofm_motion_event, IRQF_TRIGGER_RISING, ofm_global.ofm_motion->name, &ofm_global);
	if (result) {
		printk(KERN_INFO "[OFM] %s : Motion:Request IRQ  %d  failed\n",__FUNCTION__, ofm_global.ofm_motion->irq);
		goto err_reg_irq;
	}

	/* Initialize the VD5377 with desired settings */
	/* This needs to be after interrupts are set up or else we might miss the first
           interrupt */
	result = ofm_VD5377_init();
	if (result < 0) {
		printk(KERN_INFO "[OFM] %s : device init error (%d) \n",__FUNCTION__,result);
		goto err_ofm_init;
	}

	printk("[OFM] %s :complete!\n", __func__);

	return 0;
	
	/* Error cases */
err_ofm_init:
	cancel_delayed_work(&ofm_global.swipe_work);
	printk("[OFM] %s : Cancel delayed swipe work\n", __func__);
	free_irq(ofm_global.ofm_motion->irq, &ofm_global);
	printk(KERN_INFO "[OFM] %s : Motion IRQ %d Free\n",__func__, ofm_global.ofm_motion->irq);
err_ofm_missing:
err_reg_irq:
err_reg_gpio:
regulator_err:	
	input_unregister_device(ofm_global.input_dev);
err_input_reg:
	input_free_device(ofm_global.input_dev);
	dev_set_drvdata(&client->dev, NULL);
err_input_allocate:
	i2c_set_clientdata(client, NULL);
	printk("[OFM] I2C device probe error(%d) \n", result);
	
	return result;	

}

/***********************************************************************
* NAME :            
* 	ofm_i2c_remove
*
* DESCRIPTION :     
*	OFM driver module remove function
*
* INPUTS :
*	client	- i2c_client
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0 		- success
*
* NOTES :
* 	None          
***********************************************************************/
static __devexit int ofm_i2c_remove(struct i2c_client *client)
{
	struct ofm *ofm = i2c_get_clientdata(client);

	input_unregister_device(ofm_global.input_dev);
	input_free_device(ofm_global.input_dev);
	dev_set_drvdata(&client->dev, NULL);
	i2c_set_clientdata(client, NULL);

	kfree(ofm);

	return 0;
}

/***********************************************************************
* NAME :            
* 	ofm_init
*
* DESCRIPTION :     
*	OFM driver module init function
*
* INPUTS :
*	None
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0 		- success
*
* NOTES :
* 	None          
***********************************************************************/
static int __init ofm_init(void)
{
	int ret;

	ret = i2c_add_driver(&ofm_i2c_driver);

	if(ret!=0)
		printk("[OFM] I2C device init Failed! return(%d) \n",  ret);

	return ret;
}

/***********************************************************************
* NAME :            
* 	ofm_exit
*
* DESCRIPTION :     
*	OFM driver module exit function
*
* INPUTS :
*	None
* 
* OUTPUTS :
* 	None
*       
* RETURN:
* 	0 		- success
*
* NOTES :
* 	None          
***********************************************************************/
static void __exit ofm_exit(void)
{
	printk("[OFM] %s, %d\n", __func__, __LINE__ );
	i2c_del_driver(&ofm_i2c_driver);
}

module_init(ofm_init);
module_exit(ofm_exit);
MODULE_DESCRIPTION("OFM Device Driver");
MODULE_AUTHOR("trung@reconinstruments.com");
MODULE_LICENSE("GPL");
