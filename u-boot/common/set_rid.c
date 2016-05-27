#include <malloc.h> /* malloc, free, realloc*/
#include <linux/ctype.h>/* isalpha, isdigit */
#include <common.h>/* readline */
#include <command.h>/* find_cmd */
#include <asm/io.h>
#include <asm/arch/mem.h>   /* get mem tables */
#include <asm/arch/sys_proto.h>

#define DIE_ID_REG_BASE     (OMAP44XX_L4_IO_BASE + 0x2000)
#define DIE_ID_REG_OFFSET       0x200

void cmd_set_rid(const char *secret_number_str, const char *rid_str)
{
	unsigned int i = 0;
	int secret_number = 0;
	int serial_number = 0;
	unsigned int hash = 0xAAAAAAAA;
    unsigned int id[4] = { 0 };
    unsigned int reg;
	char rid_hash_str[8];

    reg = DIE_ID_REG_BASE + DIE_ID_REG_OFFSET;

    id[0] = __raw_readl(reg);
    id[1] = __raw_readl(reg + 0x8);
    id[2] = __raw_readl(reg + 0xC);
    id[3] = __raw_readl(reg + 0x10);

	for(i = 0; i < 4; i++)
	{
		hash ^= ((i & 1) == 0) ? (  (hash <<  7) ^ (id[i]) * (hash >> 3)) :
			 (~((hash << 11) + ((id[i]) ^ (hash >> 5))));
	}

	i = 0;
	while(secret_number_str[i] != 0)
	{
		hash ^= ((i++ & 1) == 0) ? (  (hash <<  7) ^ (secret_number_str[i]) * (hash >> 3)) :
			(~((hash << 11) + ((secret_number_str[i]) ^ (hash >> 5))));
		i++;
	}

	i = 0;
	while(secret_number_str[i] != 0)
	{
		hash ^= ((i++ & 1) == 0) ? (  (hash <<  7) ^ (rid_str[i]) * (hash >> 3)) :
			(~((hash << 11) + ((rid_str[i]) ^ (hash >> 5))));
		i++;
	}

	sprintf(rid_hash_str, "%x", hash);

	setenv("RID", rid_str);
	setenv("RID_HASH", rid_hash_str);
}
