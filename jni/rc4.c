
#include "rc4.h"


#define swap_byte(x,y) t = *(x); *(x) = *(y); *(y) = t

void rc4_init(const unsigned char *key_data_ptr, int key_data_len, rc4_state *key)
{
	//int i;
	unsigned char t;
	//unsigned char swapByte;
	unsigned char index1;
	unsigned char index2;
	unsigned char* state;
	unsigned long counter;

	state = &key->state[0];
	for(counter = 0; counter < 256; counter++)
		state[counter] = (unsigned char)counter;
	
	key->x = 0;
	key->y = 0;
	index1 = 0;
	index2 = 0;
	
	for(counter = 0; counter < 256; counter++)
	{
		index2 = (key_data_ptr[index1] + state[counter] + index2) & 0xff;
		swap_byte(&state[counter], &state[index2]);
		index1 = (index1 + 1) % key_data_len;
	}
}

void rc4(unsigned char *buffer_ptr, int buffer_len, rc4_state *key)
{
	unsigned char t;
	unsigned char x;
	unsigned char y;
	unsigned char* state;
	unsigned char xorIndex;
	int counter;

	x = key->x;
	y = key->y;
	state = &key->state[0];
	for(counter = 0; counter < buffer_len; counter++)
	{
		x = (x + 1) & 0xff;
		y = (state[x] + y) & 0xff;
		swap_byte(&state[x], &state[y]);
		xorIndex = (state[x] + state[y]) & 0xff;
		buffer_ptr[counter] ^= state[xorIndex];
	}
	key->x = x;
	key->y = y;
}

#if 0
#include <stdio.h>

int main(int argc, char* argv[])
{
#define buf_size 1024

  char seed[256];
  char data[512];
  char buf[buf_size];
  char digit[5];
  int hex, rd,i;
  int n;
  rc4_key key;

  if (argc < 2)
  {
    fprintf(stderr,"%s key <in >out\n",argv[0]);
    exit(1);
  }
  strcpy(data,argv[1]);
  n = strlen(data);
  if (n&1)
  {
    strcat(data,"0");
    n++;
  }
  n/=2;
  strcpy(digit,"AA");
  digit[4]='\0';
  for (i=0;i<n;i++)
  {
    digit[2] = data[i*2];
    digit[3] = data[i*2+1];
    sscanf(digit,"%x",&hex);
    seed[i] = hex;
  }

  prepare_key(seed,n,&key);
  rd = fread(buf,1,buf_size,stdin);
  while (rd>0)
  {
    rc4(buf,rd,&key);
    fwrite(buf,1,rd,stdout);
    rd = fread(buf,1,buf_size,stdin);
  }
}
#endif