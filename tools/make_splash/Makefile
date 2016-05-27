#
# 1) convert rgb565 to ycbcr
# 2) create u8 buffer from binary file
#

all: rgb2ycbcr bin2buf

rgb2ycbcr: rgb2ycbcr.o
	gcc -o $@ $^

bin2buf: bin2buf.o
	gcc -o $@ $^

rgb2ycbcr.o: rgb2ycbcr.c
	gcc -c -o $@ $^

bin2buf.o: bin2buf.c
	gcc -c -o $@ $^

clean:
	rm -rf rgb2ycbcr rgb2ycbcrf.o bin2buf bin2buf.o