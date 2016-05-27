#include <linux/module.h>
#include <linux/irq.h>
#include <linux/interrupt.h>
#include <linux/sched.h>
#include <linux/delay.h>
#include <linux/platform_device.h>
#include <linux/input.h>
#include <linux/slab.h>
#include <linux/gpio.h>

#define JET_KEY_DEBUG 0
#define BUTTON_GPIO 143
#define BUTTON_NAME "jet-keys"
#define BUTTON_DEBOUNCE_MS 50
#define BUTTON_HOLDTIME_MS 400
#define BUTTON_SHORT_CLICK_CODE		KEY_102ND//KEY_BACK //KEY_ENTER

#undef JET_TWO_BUTTON_CODE //Let android handle long press and button hold event
//#define JET_TWO_BUTTON_CODE
#ifdef JET_TWO_BUTTON_CODE
#define BUTTON_LONG_PRESS_CODE		KEY_RO	//

enum button_state {
	BUTTON_INIT=0,
	BUTTON_LOW,
	BUTTON_HIGH,
};
#endif

struct jet_keys_drvdata {
	int irq;
	struct delayed_work button_work;
	struct input_dev *input_dev;
#ifdef JET_TWO_BUTTON_CODE
	unsigned long last_update;
	enum button_state last_state;
#endif
};

#ifdef JET_TWO_BUTTON_CODE
static void jet_report_key(struct input_dev *input_dev, unsigned int code)
{
#if JET_KEY_DEBUG
	printk(KERN_INFO "%s:code=%d\n",__func__,code);
#endif
	input_report_key(input_dev, code, 1);
	input_sync(input_dev);

	input_report_key(input_dev, code, 0);
	input_sync(input_dev);
}

static void button_work_func(struct work_struct *work)
{
	enum button_state new_button_state;
	struct jet_keys_drvdata *jet_keys_ddata=
	container_of(work, struct jet_keys_drvdata, button_work.work);
	int state = gpio_get_value_cansleep(BUTTON_GPIO);
#if JET_KEY_DEBUG
	printk(KERN_INFO "%s:button gpio read=%d\n",__func__,state);
#endif
	new_button_state = state ? BUTTON_HIGH : BUTTON_LOW;
	//check short or long click
	if(new_button_state==BUTTON_LOW){
		jet_keys_ddata->last_update = jiffies;
	} else {//BUTTON_HIGH
		if(jet_keys_ddata->last_state == BUTTON_INIT)
			return;
		if(time_after(jiffies,
			jet_keys_ddata->last_update + msecs_to_jiffies(BUTTON_HOLDTIME_MS))){
			jet_report_key(jet_keys_ddata->input_dev, BUTTON_LONG_PRESS_CODE);//long press
		} else
			jet_report_key(jet_keys_ddata->input_dev, BUTTON_SHORT_CLICK_CODE);
	}

	jet_keys_ddata->last_state= new_button_state;
}

#else

static void button_work_func(struct work_struct *work)
{
	struct jet_keys_drvdata *jet_keys_ddata=
	container_of(work, struct jet_keys_drvdata, button_work.work);
	int state = gpio_get_value_cansleep(BUTTON_GPIO);//BUTTON_GPIO idle high
#if JET_KEY_DEBUG
	printk(KERN_INFO "%s:button gpio read=%d\n",__func__,state);
#endif
	input_report_key(jet_keys_ddata->input_dev, BUTTON_SHORT_CLICK_CODE, !state);
	input_sync(jet_keys_ddata->input_dev);
}

#endif
static irqreturn_t button_irq_handler(int irq, void *dev_id)
{
	struct jet_keys_drvdata *jet_keys_ddata=dev_id;
	//disable_irq_nosync(jet_keys_ddata->irq);
	schedule_delayed_work(&jet_keys_ddata->button_work,msecs_to_jiffies(BUTTON_DEBOUNCE_MS));
	return IRQ_HANDLED;
}



static int __devinit jet_keys_probe(struct platform_device *pdev)
{
	struct jet_keys_drvdata *jet_keys_ddata;
	int ret;

	jet_keys_ddata = kzalloc(sizeof(*jet_keys_ddata), GFP_KERNEL);
	jet_keys_ddata->input_dev = input_allocate_device();
	if (!jet_keys_ddata || !(jet_keys_ddata->input_dev))
	{
		dev_err(&pdev->dev,
				"failed to allocate memory for %s\n",__func__);
		return -ENOMEM;
	}

	platform_set_drvdata(pdev, jet_keys_ddata);
	//input_set_drvdata(input, ddata);
	/* Init input device */
	jet_keys_ddata->input_dev->name = BUTTON_NAME;
	jet_keys_ddata->input_dev->dev.init_name = BUTTON_NAME;
	jet_keys_ddata->input_dev->id.bustype = BUS_HOST;
	jet_keys_ddata->input_dev->dev.parent = &pdev->dev;
	jet_keys_ddata->input_dev->phys = BUTTON_NAME"/input0";
	jet_keys_ddata->input_dev->id.vendor = 0x0001;
	jet_keys_ddata->input_dev->id.product = 0x0001;
	jet_keys_ddata->input_dev->id.version = 0x0001;
	//set event
	set_bit(EV_KEY, jet_keys_ddata->input_dev->evbit);
	//set code
	set_bit( (BUTTON_SHORT_CLICK_CODE & KEY_MAX), jet_keys_ddata->input_dev->keybit);
#ifdef JET_TWO_BUTTON_CODE
	set_bit( (BUTTON_LONG_PRESS_CODE & KEY_MAX), jet_keys_ddata->input_dev->keybit);
	jet_keys_ddata->last_state=BUTTON_INIT;
#endif
	ret=input_register_device(jet_keys_ddata->input_dev);
	if(ret) {
		dev_err(&pdev->dev, "%s:Unable to register input device,error %d\n", __func__,ret);
		goto fail1;
	}

	/* Configure GPIO */
	ret = gpio_request(BUTTON_GPIO, BUTTON_NAME);
	if (ret < 0) {
		dev_err(&pdev->dev, "%s:failed to request GPIO,error %d\n", __func__,ret);
		goto fail1;
	}

	ret = gpio_direction_input(BUTTON_GPIO);
	if (ret < 0) {
		dev_err(&pdev->dev, "%s:failed direction change,error %d\n", __func__,ret);
		goto fail2;
	}

	/* Init work */
	INIT_DELAYED_WORK( &(jet_keys_ddata->button_work), button_work_func);

	/*Init IRQ*/
	jet_keys_ddata->irq=gpio_to_irq(BUTTON_GPIO);
	if(jet_keys_ddata->irq<0) {
		dev_err(&pdev->dev, "%s:failed get irq line,error %d\n", __func__,jet_keys_ddata->irq);
		goto fail2;
	}
	ret=request_irq(jet_keys_ddata->irq, button_irq_handler,
		IRQ_TYPE_EDGE_RISING|IRQ_TYPE_EDGE_FALLING, BUTTON_NAME, jet_keys_ddata);
	if(ret<0){
		dev_err(&pdev->dev, "%s:failed claim irq,error %d\n", __func__,ret);
		goto fail2;
	}
	printk(KERN_INFO "%s succeed\n",__func__);
	return 0;
fail2:
	gpio_free(BUTTON_GPIO);
fail1:
	input_free_device(jet_keys_ddata->input_dev);
	kfree(jet_keys_ddata);
	return ret;
}

static int __devexit jet_keys_remove(struct platform_device *pdev)
{
	struct jet_keys_drvdata *jet_keys_ddata = platform_get_drvdata(pdev);
	free_irq(jet_keys_ddata->irq, jet_keys_ddata);
	cancel_delayed_work_sync(&jet_keys_ddata->button_work);
	gpio_free(BUTTON_GPIO);
	input_unregister_device(jet_keys_ddata->input_dev);
	input_free_device(jet_keys_ddata->input_dev);
	kfree(jet_keys_ddata);
	return 0;
}



static struct platform_driver jet_keys_device_driver = {
	.probe		= jet_keys_probe,
	.remove		= __devexit_p(jet_keys_remove),
	.driver		= {
		.name	= BUTTON_NAME,
		.owner	= THIS_MODULE,
	}
};

static int __init jet_keys_init(void)
{
	return platform_driver_register(&jet_keys_device_driver);
}

static void __exit jet_keys_exit(void)
{
	platform_driver_unregister(&jet_keys_device_driver);
}

module_init(jet_keys_init);
module_exit(jet_keys_exit);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Li Chen <li@reconinstruments.com>");
MODULE_DESCRIPTION("2nd button for Jet");

