#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define DEBUG 1
#define DEFAULT_GYRO_LIMIT 0.22
#define DATA_FROM_SENSTEST
#ifdef DATA_FROM_SENSTEST
#define ACC_FACTOR  .0011768f
#define GYRO_FACTOR 0.0012211f
#endif
const char* getfield(char* line, int num);
float norm(float x, float y, float z);
int parsefile(char* filename, float* result, float cutoff);
float offset(float cpu, float mem);

int main(int argc, const char* argv[])
{
    float *cpu = malloc(sizeof(float)*3);
    float *mem = malloc(sizeof(float)*3);
    float avg[3],off[3];
    float cutoff=DEFAULT_GYRO_LIMIT;
    if (argc > 1) cutoff = atof(argv[1]);
    printf("gyro threshold = %f\n",cutoff);
    if (parsefile("cpu.csv",cpu,cutoff) != 0)
    {
        printf("An error ocurred reading cpu.csv\n");
        return 0;
    }
    if (parsefile("mem.csv",mem,cutoff) != 0)
    {
        printf("An error ocurred reading mem.csv\n");
        return 0;
    }
#ifdef DEBUG
    printf("cpu = %f %f %f\n", cpu[0], cpu[1], cpu[2]);
    printf("mem = %f %f %f\n", mem[0], mem[1], mem[2]);
#endif
    avg[0] = (cpu[0] - mem[0])/2;
    avg[1] = (cpu[1] - mem[1])/2;
    avg[2] = (cpu[2] - mem[2])/2;
    off[0] = offset(cpu[0],mem[0]);
    off[1] = offset(cpu[1],mem[1]);
    off[2] = offset(cpu[2],mem[2]);
#ifdef DEBUG
    printf("avg = %f %f %f\n", avg[0], avg[1], avg[2]);
    printf("new cpu = %f %f %f\n", cpu[0] + off[0], cpu[1] + off[1], cpu[2] + off[2]);
    printf("new mem = %f %f %f\n", mem[0] + off[0], mem[1] + off[1], mem[2] + off[2]);
#endif
// #ifdef DATA_FROM_SENSTEST
//     off[0] *= ACC_FACTOR;
//     off[1] *= ACC_FACTOR;
//     off[2] *= ACC_FACTOR;
// #endif
    printf("conv_B = %f %f %f\n", off[0], off[1], off[2]);
    return 0;
}

inline float offset(float cpu, float mem)
{
#ifdef DEBUG
    printf("cpu = %f mem = %f\n",cpu,mem);
#endif
    return -( (cpu + mem) / 2);
}
const char* getfield(char* line, int num)
{
    const char* tok;
    for (tok = strtok(line, ",");
            tok && *tok;
            tok = strtok(NULL, ",\n"))
    {
        if (!--num)
            return tok;
    }
    return NULL;
}

float norm(float x, float y, float z)
{
    return sqrt((x*x)+(y*y)+(z*z));
}

int parsefile(char* filename, float* result, float cutoff)
{
    char line[1024];
    const char* tmp;
    const char *type;
    float accum_x=0,accum_y=0,accum_z=0;
    float gyro_threshold=cutoff;
#ifdef DATA_FROM_SENSTEST
    gyro_threshold /= GYRO_FACTOR;
#endif
    float norm_g=-1;
    float x=0,y=0,z=0;
    int cnt = 0,tot=0;
    int gyro_valid=0;
    int field_offset = 0;
//#ifdef DATA_FROM_SENSTEST
//    field_offset = 0;
//#endif

    FILE* stream = fopen(filename, "r");
    if (!stream) return -1; /* file was not opened sucessfully */
    while (fgets(line, 1024, stream))
    {
#if DEBUG > 1
        printf("%s",line);
#endif
        char* var = strdup(line);
        type = getfield(var,1);
        char* sz_x = strdup(line);
        tmp = getfield(sz_x, 2 + field_offset);
        if (tmp == NULL) break;
        x = atof(tmp);
        char* sz_y = strdup(line);
        tmp = getfield(sz_y, 3 + field_offset);
        if (tmp == NULL) break;
        y = atof(tmp);
        char* sz_z = strdup(line);
        tmp = getfield(sz_z, 4 + field_offset);
        if (tmp == NULL) break;
        z = atof(tmp);
#if DEBUG > 1
        printf("x = %f, y = %f, z = %f\n",x,y,z);
#endif
        if (strcmp(type,"recongyr") == 0)
        {
            norm_g = norm(x,y,z);
            if ( norm_g > gyro_threshold) gyro_valid = 0;
            else gyro_valid = 1;
#if DEBUG > 1
            printf("%f is %s\n",norm_g,(gyro_valid ? "valid" : "invalid"));
#endif
        }
        if ((strcmp(type,"reconacc") == 0) )
        {
            tot++;
            if (gyro_valid) {
                cnt++;
#ifdef DATA_FROM_SENSTEST
                accum_x += ACC_FACTOR*x;
                accum_y += ACC_FACTOR*y;
                accum_z += ACC_FACTOR*z;
#else
                accum_x += x;
                accum_y += y;
                accum_z += z;
#endif
#if DEBUG > 1
                printf("accum_x = %f accum_y = %f accum_z = %f cnt = %i\n",accum_x,accum_y,accum_z,cnt);
#endif
            }
        }
    }
#ifdef DEBUG
    printf("Used %i/%i (%.1f%%) lines from %s\n",cnt,tot,(float)100*(float)cnt/(float)tot,filename);
#endif
    accum_x /= (float)cnt;
    accum_y /= (float)cnt;
    accum_z /= (float)cnt;
    result[0] = accum_x;
    result[1] = accum_y;
    result[2] = accum_z;
    return 0;
}

