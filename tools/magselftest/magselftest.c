#include "i2c_control.h"
#include <stdio.h>
#include <unistd.h>

#define I2CBUS 2
#define DEVADDR 0x1D
#define US_PER_MS 1000

short verbosity = 0;

#define BYTETOBINARYPATTERN "%d%d%d%d%d%d%d%d"
#define BYTETOBINARYPATTERNS "%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d"
#define BYTETOBINARY(byte)  \
  (byte & 0x80 ? 1 : 0), \
  (byte & 0x40 ? 1 : 0), \
  (byte & 0x20 ? 1 : 0), \
  (byte & 0x10 ? 1 : 0), \
  (byte & 0x08 ? 1 : 0), \
  (byte & 0x04 ? 1 : 0), \
  (byte & 0x02 ? 1 : 0), \
  (byte & 0x01 ? 1 : 0) 
#define BYTETOBINARYS(byte)  \
  (byte & 0x80 ? 1 : 0), \
  (byte & 0x40 ? 1 : 0), \
  (byte & 0x20 ? 1 : 0), \
  (byte & 0x10 ? 1 : 0), \
  (byte & 0x08 ? 1 : 0), \
  (byte & 0x04 ? 1 : 0), \
  (byte & 0x02 ? 1 : 0), \
  (byte & 0x01 ? 1 : 0), \
  (byte & 0x80 ? 1 : 0), \
  (byte & 0x40 ? 1 : 0), \
  (byte & 0x20 ? 1 : 0), \
  (byte & 0x10 ? 1 : 0), \
  (byte & 0x08 ? 1 : 0), \
  (byte & 0x04 ? 1 : 0), \
  (byte & 0x02 ? 1 : 0), \
  (byte & 0x01 ? 1 : 0) 
#define OUT_X_L_M 0x08
#define OUT_Y_L_M 0x0a
#define OUT_Z_L_M 0x0c
#define OFFSET_X_L_M 0x16
#define OFFSET_Y_L_M 0x18
#define OFFSET_Z_L_M 0x1a
#define CTRL_REG5_XM 0x24
#define CTRL_REG6_XM 0x25
#define CTRL_REG7_XM 0x26

char readreg(short data_address)
{
    char ret = read_i2c(0x2,0x1d,data_address);
    if (verbosity > 1) {
        printf("reg[0x%02x]\t0x%02x\t"BYTETOBINARYPATTERN"\n",data_address,ret,BYTETOBINARY(ret));
    }
    return ret;
}
/* note: must pass address of low byte, will calculate address of high */
signed short readhalfword(short start_address)
{
    return (readreg(start_address) | (readreg(start_address+1) << 8));
}
int writereg(short data_address, short value)
{
    short ret = write_i2c(I2CBUS,DEVADDR,data_address,value);
    if ((value != readreg(data_address)))
    {
        if (verbosity) {
                printf("** Error writing %d to 0x%02x\n",value,data_address);
        }
        return -4;
    }
    return ret;
}
int setbit(short data_address, short bit_offset)
{
    if (verbosity > 1) {
        printf("** Setting bit %d of reg 0x%02x\n",bit_offset,data_address);
    }
    if (bit_offset > 15) return -1;
    short current_val = readreg(data_address);
    return writereg(data_address, current_val | (1<<bit_offset));
}
int clearbit(short data_address, short bit_offset)
{
	if (verbosity > 1) 
	{
				printf("** Clearing  bit %d of reg 0x%02x\n",bit_offset,data_address);
	}
    if (bit_offset > 15) return -1;
    short current_val = readreg(data_address);
    return writereg(data_address, current_val & ~(1<<bit_offset));
}

short g_failure;
const char addr[] = {OUT_X_L_M,OUT_Y_L_M,OUT_Z_L_M,OFFSET_X_L_M,OFFSET_Y_L_M,OFFSET_Z_L_M};
void init()
{
    writereg(CTRL_REG6_XM,0x6);
    setbit(0x10,7); // SECRET REGISTER
    clearbit(CTRL_REG7_XM,0);
    clearbit(CTRL_REG7_XM,1);
    return;
} 
void reset()
{
	return;
}
int runtest()
{
    g_failure = 0;
    short i;
    printf("Running compass self test: "); fflush(stdout);
    if (verbosity) printf("\n");
    reset(); 
    init(); /* Initialize Sensor, turn on sensor, enable X/Y/Z axes. */
    usleep(100*US_PER_MS); /* wait 100 ms for stable output */

    /* read values */
    signed short T[6];
    for (i=0;i<6;i++)
    {
        T[i] = readhalfword(addr[i]);
        if ((T[i] == 0) || (T[i] == -1))
        {
            g_failure = 1;
        }
    }
    if (verbosity) {
        printf("OUT_X\t%d\nOUT_Y\t%d\nOUT_Z\t%d\nOFFSET_X\t%d\nOFFSET_Y\t%d\nOFFSET_Z\t%d\n",T[0],T[1],T[2],T[3],T[4],T[5]);
    } 
    if (!g_failure)
    {
        if (verbosity) {
                printf("** stage 1 success. begin stage 2\n");
        }
    }
    else { 
        goto test_end;
    }
    setbit(0x26,1);
    clearbit(0x26,0);
    usleep(200*US_PER_MS);
    /* switch = bits 6:5 of reg 0x24 (CTRL_REG5_XM) magnetic resolution spec */ 
    char sw = (readreg(CTRL_REG5_XM) & (3<<5)) >> 5;
    float factor;
    switch (sw)
    {
    case 0:
        factor = 0.5; 
        break;
    case 1:
        factor = 1;
        break;
    case 2:
        factor = 2;
        break;
    case 3:
        factor = 4;
        break;
    default:
        break;
    }
    float sens = (((float)479/(float)1000))/factor;
    if (verbosity > 1) {
        printf("sw\t%i\nfactor\t%.2f\nsens\t%.2f\n",sw,factor,sens);
    }
    /* Power up, wait 100ms for stable output
        ... whatever that means
    */
    usleep(100*US_PER_MS);

    float SET_X = ((float) (readhalfword(OUT_X_L_M))) * sens;
    float SET_Y = ((float) (readhalfword(OUT_Y_L_M))) * sens;
    float SET_Z = ((float) (readhalfword(OUT_Z_L_M))) * sens;
    float RES_X = ((float) (readhalfword(OFFSET_X_L_M))) * sens;
    float RES_Y = ((float) (readhalfword(OFFSET_Y_L_M))) * sens;
    float RES_Z = ((float) (readhalfword(OFFSET_Z_L_M))) * sens;
    
    float T2_X = (SET_X+RES_X)/(float)2;
    float T2_Y = (SET_Y+RES_Y)/(float)2;
    float T2_Z = (SET_Z+RES_Z)/(float)2;
    
    if (verbosity) 
    {
        printf("SET_X\t%.2f\nSET_Y\t%.2f\nSET_Z\t%.2f\nRES_X\t%.2f\nRES_Y\t%.2f\nRES_Z\t%.2f\n",SET_X,SET_Y,SET_Z,RES_X,RES_Y,RES_Z);
        printf("T2_X\t%.2f\nT2_Y\t%.2f\nT2_Z\t%.2f\n",T2_X,T2_Y,T2_Z);
    }
    if (( T2_X <= -1000.0) ||  (T2_X >= 1000.0)) g_failure = 1;
    if (( T2_Y <= -1000.0) ||  (T2_Y >= 1000.0)) g_failure = 1;
    if (( T2_Z <= -1000.0) ||  (T2_Z >= 1000.0)) g_failure = 1;
    if (verbosity) printf("Compass self test ");
test_end:
    return g_failure;

}
int main(int argc, char** argv)
{
    /* check for verbosity */
    if (argc > 1)
    {
        char *arg = argv[1];
        if ((arg[0] == '-') && (arg[1] == 'v')) verbosity = 1;
        if ((arg[2] == 'v')) verbosity = 2;
    }
    int it=0;
    if (runtest() == 1)
    {
        printf("failed\n");
    }
    else
    {
        printf("passed\n");
    }
    return 0;
}
