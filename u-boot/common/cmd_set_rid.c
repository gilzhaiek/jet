#include <common.h>
#include <command.h>

extern void cmd_setmodid(const char *secret_number_str, const char *serial_number_str);

int do_set_rid(cmd_tbl_t * cmdtp, int flag, int argc, char *argv[])
{
    if(argc != 3)
        return -1;

    cmd_set_rid(argv[1], argv[2]);

    return(0);
}


U_BOOT_CMD(
    set_rid,   3,  1,  do_set_rid,
    "set_rid secret_number recon_id",
    ""
);

