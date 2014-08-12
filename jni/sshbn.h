#ifndef _SSH_H_
#define _SSH_H_

#ifndef BIGNUM_INTERNAL
//typedef void *Bignum;
typedef unsigned short *Bignum;
#endif
/*
extern Bignum Zero;
extern Bignum One;
*/
#define snewn(n, T) ((T*)malloc((n)*sizeof(T)))
#define sfree(p) if(p) free(p);

Bignum copybn(Bignum b);
Bignum bn_power_2(int n);
void bn_restore_invariant(Bignum b);
Bignum bignum_from_long(unsigned long n);
void freebn(Bignum b);
Bignum modpow(Bignum base, Bignum exp, Bignum mod);
Bignum modmul(Bignum a, Bignum b, Bignum mod);
void decbn(Bignum n);

Bignum bignum_from_bytes(const unsigned char *data, int nbytes);
int bignum_bitcount(Bignum bn);
int bignum_byte(Bignum bn, int i);
int bignum_bit(Bignum bn, int i);
void bignum_set_bit(Bignum bn, int i, int value);
int ssh1_write_bignum(void *data, Bignum bn);
Bignum biggcd(Bignum a, Bignum b);
unsigned short bignum_mod_short(Bignum number, unsigned short modulus);
Bignum bignum_add_long(Bignum number, unsigned long addend);
Bignum bigmul(Bignum a, Bignum b);
Bignum bigmuladd(Bignum a, Bignum b, Bignum addend);
Bignum bigdiv(Bignum a, Bignum b);
Bignum bigmod(Bignum a, Bignum b);
Bignum modinv(Bignum number, Bignum modulus);
Bignum bignum_bitmask(Bignum number);
Bignum bignum_rshift(Bignum number, int shift);

//compare to bigint
int bignum_cmp(Bignum a, Bignum b);

unsigned char * bignum_to_bytes(Bignum x, int * nbytes);
//convert to decimal string
char *bignum_decimal(Bignum x);

int ssh1_read_bignum(const unsigned char *data, int len, Bignum * result);
int ssh1_bignum_length(Bignum bn);
int ssh2_bignum_length(Bignum bn);

#endif
