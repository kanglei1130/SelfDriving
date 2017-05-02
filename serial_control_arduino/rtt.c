// compile with: gcc -O2 -Wall -o latency_test rtt.c

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <fcntl.h>
#include <poll.h>
#include <termios.h>
#include <unistd.h>

//#define PORT "/dev/cu.usbserial-A800daD3"       // Duemilanove
//#define PORT "/dev/cu.usbmodem411"            // Uno
//#define PORT "/dev/cu.usbmodem12341"          // Teensy

//#define PORT "/dev/ttyUSB0"                   // Duemilanove on Linux
#define PORT "/dev/ttyACM0"                   // Uno or Teensy on Linux
#define BAUD B115200

void die(const char *format, ...) __attribute__ ((format (printf, 1, 2)));

int main()
{
int r, fd, count;
struct termios tinfo;
unsigned char buf[7];
struct pollfd fds;
struct timeval begin, end;
double elapsed;
int i;

fd = open(PORT, O_RDWR);
if (fd < 0) die("unable to open port %s\n", PORT);
if (tcgetattr(fd, &tinfo) < 0) die("unable to get serial parms\n");
if (cfsetspeed(&tinfo, B115200) < 0) die("error in cfsetspeed\n");
if (tcsetattr(fd, TCSANOW, &tinfo) < 0) die("unable to set baud rate\n");

printf("port %s opened, waiting for board to boot up\n", PORT);
sleep(5);

for (count=0; count < 1000; count++) {
// send the ping request
buf[0] = 1;
buf[1] = 0;
buf[2] = 1;
buf[3] = 4;
buf[4] = 0x10;
buf[5] = 2;
buf[6] = 0x20;
printf("sending 7 bytes");
gettimeofday(&begin, NULL);
r = write(fd, buf, 7);
if (r != 7) die("unable to write, r = %d\n", r);

// wait for a responds
fds.fd = fd;
fds.events = POLLIN;
poll(&fds, 1, 10000);
r = read(fd, buf, 4);
for(i = 0; i < r; ++i)
  printf("%c\n", buf[i]);

gettimeofday(&end, NULL);
printf(", read %d bytes", r);
if (r != 4) die ("unable to read 4 bytes\n");

elapsed = (double)(end.tv_sec - begin.tv_sec) * 1000.0;
elapsed += (double)(end.tv_usec - begin.tv_usec) / 1000.0;
printf(", elased: %.2f ms\n", elapsed);
}
close(fd);
return 0;
}

void die(const char *format, ...)
{
va_list args;
va_start(args, format);
vfprintf(stderr, format, args);
exit(1);
}
