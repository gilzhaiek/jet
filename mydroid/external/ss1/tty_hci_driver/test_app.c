#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/poll.h>
#include <errno.h>

unsigned char bt_ver_cmd[] = { 0x01, 0x01, 0x10, 0x00 };
unsigned char read_bdaddr[] = { 0x01, 0x09, 0x10, 0x00 };
unsigned char read_features[] = {0x01, 0x04, 0x10, 0x01, 0x00};
unsigned char put_into_piscan[] = {0x01, 0x1a, 0x0c, 0x01, 0x03};
unsigned char put_into_noscan[] = {0x01, 0x1a, 0x0c, 0x01, 0x00};
unsigned char start_scan[] = {0x01, 0x01, 0x04, 0x05,
	0x33, 0x8b, 0x9e, 0x08, 0x00};

void read_write(int fd, unsigned char *cmd, int bytes)
{
	int nread, i, err, to_read = 0;
	struct	pollfd p;
	unsigned char resp[50] = { 0 };

	memset(&p, 0, sizeof(p));

	p.fd = fd;
	p.events = POLLIN | POLLERR | POLLHUP;
	p.revents = 0;

	write(fd, cmd, bytes);

	fprintf(stderr, "polling for data..\n");
	err = poll(&p, 1, -1);
	if (err < 0 || err == 0) {
		fprintf(stderr, "poll error %s\n", strerror(errno));
		return;
	}

	fprintf(stderr, "poll broke..\n");
	err = ioctl(fd, FIONREAD, &to_read);
	if (err) {
		fprintf(stderr, "ioctl error %s\n", strerror(errno));
		return;
	}

	fprintf(stderr, "reading %d bytes\n", to_read);
	nread = read(fd, &resp, to_read);

	fprintf(stderr, "data read... \n");
	for (i=0; i<nread; i++)
		fprintf(stderr,"0x%02x|", resp[i]);
	fprintf(stderr, "\n****\n");
}

int main()
{
	int fd = -1, nread, i;

	fd = open("/dev/hci_tty", O_RDWR);
	if (fd < 0) {
		fprintf(stderr, "error %s\n", strerror(errno));
		return -1;
	}

	read_write(fd, read_bdaddr, 4);
	read_write(fd, read_features, 5);
	read_write(fd, put_into_piscan, 5);
	read_write(fd, start_scan, 9);

	return 0;
}

