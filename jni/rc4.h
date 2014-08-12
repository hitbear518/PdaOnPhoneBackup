#ifndef _RC4_H_
#define _RC4_H_


typedef struct rc4_state
{      
   unsigned char state[256];       
   unsigned char x;        
   unsigned char y;
} rc4_state;

void rc4_init(const unsigned char *key_data_ptr, int key_data_len, rc4_state *key);
void rc4(unsigned char *buffer_ptr, int buffer_len, rc4_state *key);

#endif
