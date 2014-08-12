#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <malloc.h>
#include <stdio.h>

#include "md5.h"
#include "rc4.h"
#include "sshbn.h"
#include "miniz.h"

#include "tdi_prot.h"



#ifdef BIG_ENDIAN

//#define HTONS(n) (n)
//#define NTOHS(n) (n)
//#define HTONL(n) (n)
//#define NTOHL(n) (n)
#undef NTOHL
#undef NTOHS
#undef HTONL
#undef HTONS
#define	NTOHL(x)	ntohl((__uint32_t)x)
#define	NTOHS(x)	ntohs((__uint16_t)x)
#define	HTONL(x)	htonl((__uint32_t)x)
#define	HTONS(x)	htons((__uint16_t)x)

//#define LTOH(n) n=(((((unsigned long)(n) & 0xFF)) << 24) | \
//((((unsigned long)(n) & 0xFF00)) << 8) | \
//((((unsigned long)(n) & 0xFF0000)) >> 8) | \
//((((unsigned long)(n) & 0xFF000000)) >> 24))
//
//#define STOH(n) n=(((((unsigned short)(n) & 0xFF)) << 8) | (((unsigned short)(n) & 0xFF00) >> 8))
#define LTOH(n)
#define STOH(n)

#else
#define LTOH(n)
#define STOH(n)

#define HTONS(n) (((((unsigned short)(n) & 0xFF)) << 8) | (((unsigned short)(n) & 0xFF00) >> 8))
#define NTOHS(n) (((((unsigned short)(n) & 0xFF)) << 8) | (((unsigned short)(n) & 0xFF00) >> 8))

#define HTONL(n) (((((unsigned long)(n) & 0xFF)) << 24) | \
((((unsigned long)(n) & 0xFF00)) << 8) | \
((((unsigned long)(n) & 0xFF0000)) >> 8) | \
((((unsigned long)(n) & 0xFF000000)) >> 24))

#define NTOHL(n) (((((unsigned long)(n) & 0xFF)) << 24) | \
((((unsigned long)(n) & 0xFF00)) << 8) | \
((((unsigned long)(n) & 0xFF0000)) >> 8) | \
((((unsigned long)(n) & 0xFF000000)) >> 24))

#endif



#define DTI_VERSION 3

#define DTI_CMD_BEGIN 1
#define DTI_CMD_COMMIT 2
#define DTI_CMD_ROLLBACK 3
#define DTI_CMD_QUERY 4
#define DTI_CMD_REWRITE_PROC 5
#define DTI_CMD_COMPRESS_QUERY 6

#define BUFFER_SIZE 10240
#define RAND_KEY_SIZE 20
#define SID_MAX_SIZE 256
#define MAX_STRING_SIZE 4096

typedef struct
{
	char nType;
	char * pszName;
} COLUMN;


typedef struct
{
	char szSID[SID_MAX_SIZE];
	char szIP[16];
	unsigned char szRnd[RAND_KEY_SIZE];	//随机数
	UINT32 uVersion;					//客户端版本
	rc4_state rc4Key;
	INT32 nUseCount;					//SQL绑定参数个数
	INT32 nRows;						//行数
	INT32 nCurRows;						//行数
	INT32 nColumns;						//列数
	COLUMN * pszColumns;					//
	char ** pszRows;				//
	int nPos;							//读取位置
	char * pszData;						//读取缓冲区
	int nLen;							//数据长度
	unsigned char szBuf[BUFFER_SIZE]; //构建请求包
} PROT_DATA;

static unsigned char s_a_dh_p [] = 
{
	160,190,133,80,24,48,125,53,145,171,121,157,
	126,74,2,8,22,27,137,123,130,72,114,57,235,193,188,59,
	194,168,179,170,208,203,75,208,86,225,149,147,166,41,71,
	182,71,173,196,242,199,138,242,137,98,125,235,214,18,55,
	1,92,151,242,253,161,248,119,120,47,145,30,44,154,230,
	170,216,182,36,102,230,78,113,121,199,205,173,100,252,
	27,236,133,7,177,51,208,88,193,177,84,49,201,244,3,84,
	189,137,113,253,123,229,91,179,252,223,12,215,100,150,
	49,232,112,62,59,252,229,250,136,0,35
};

static unsigned char s_a_rsa_n[]= 
{
  178,213,46,63,169,1,31,1,6,94,228,223,66,113,142,205,
  111,249,125,156,158,38,17,206,72,116,200,120,12,188,186,
  14,145,149,253,158,28,149,146,75,19,33,13,205,47,158,
  251,120,253,254,142,201,181,216,24,99,135,199,172,166,
  251,234,243,240,105,239,150,27,152,137,10,91,45,55,137,
  46,168,104,82,91,73,8,253,55,102,164,46,119,51,247,145,
  113,11,129,113,132,246,33,228,201,29,136,183,229,98,193,
  236,12,24,66,239,128,37,85,138,58,114,207,129,169,243,
  119,36,101,205,79,238,229
};

static Bignum s_rsa_n; //(s_a_rsa_n, sizeof(s_a_rsa_n));
static Bignum s_rsa_e; //(65537);


static UINT64 HTONLL(UINT64 n)
{
	return ( (((UINT64)HTONL(n)) << 32) + HTONL(n/* >> 32*/) );
}

static void rand_buf(unsigned char * buf, int len)
{
	int i;
	for(i=0; i<len; i++)
		buf[i] = (char)rand();
}

#define CHECK_RR(c) if(!c) return DBE_INVALID_PACKET;

static int read_char(PROT_DATA * pCtx, char * c)
{
	if(pCtx->nPos + 1 <= pCtx->nLen)
	{
		*c = *(char*)&pCtx->pszData[pCtx->nPos];
		pCtx->nPos += 1;
		return 1;
	}

	return 0;
}

static int read_int16(PROT_DATA * pCtx, INT16 * i)
{
	if(pCtx->nPos + 1 <= pCtx->nLen)
	{
		*i = *(INT16*)&pCtx->pszData[pCtx->nPos];
		pCtx->nPos += 2;
		STOH(*i);
		return 1;
	}

	return 0;
}

static int read_int32(PROT_DATA * pCtx, INT32 * i)
{
	if(pCtx->nPos + 4 <= pCtx->nLen)
	{
		memcpy(i, &pCtx->pszData[pCtx->nPos], 4);
		pCtx->nPos += 4;
		LTOH(*i);
		return 1;
	}

	return 0;
}

static int read_dbl(PROT_DATA * pCtx, double * d)
{
	if(pCtx->nPos + sizeof(double) <= pCtx->nLen)
	{
		memcpy(d, &pCtx->pszData[pCtx->nPos], sizeof(double));
		pCtx->nPos += sizeof(double);
		//有字节序问题
		//LTOH(*i);
		return 1;
	}

	return 0;
}

static int read_str(PROT_DATA * pCtx, char * pszBuf, int nBufLen)
{
	INT32 len;

	if(!read_int32(pCtx, &len) || len >= nBufLen || len >= MAX_STRING_SIZE) {
		return 0;
	}

	if(len > 0)
	{
		if(len > BUFFER_SIZE - pCtx->nPos) {
			return 0;
		}

		memcpy(pszBuf, &pCtx->pszData[pCtx->nPos], len); 
		pszBuf[len] = 0;
		pCtx->nPos += len;
	}
	else
	{
		pszBuf[0] = 0;
	}

	return 1;
}

static int read_data(PROT_DATA * pCtx, char * pszBuf, int nLen)
{
	if(nLen > BUFFER_SIZE - pCtx->nPos)
		return 0;

	memcpy(pszBuf, &pCtx->pszData[pCtx->nPos], nLen); 
	pszBuf[nLen] = 0;
	pCtx->nPos += nLen;

	return 1;
}

static void begin_write(PROT_DATA * pCtx)
{
	pCtx->nPos = 4;
}

static void end_write(PROT_DATA * pCtx)
{
	INT32 len = pCtx->nPos - 4;
	rc4(&pCtx->szBuf[4], len, &pCtx->rc4Key);
	
	len = HTONL(len);
	memcpy(pCtx->szBuf, &len, 4);

	pCtx->nLen = pCtx->nPos;
}

static int write_char(PROT_DATA * pCtx, char c)
{
	if(pCtx->nPos >= BUFFER_SIZE)
		return 0;
	pCtx->szBuf[pCtx->nPos] = c;
	++pCtx->nPos;
	return 1;
}

static int write_int(PROT_DATA * pCtx, INT32 i)
{
	if(pCtx->nPos + sizeof(i) > BUFFER_SIZE)
		return 0;
	LTOH(i);
	memcpy(&pCtx->szBuf[pCtx->nPos], &i, sizeof(i));
	pCtx->nPos += sizeof(i);
	return 1;
}

static int write_str(PROT_DATA * pCtx, const char * str)
{
	INT32 len2;
	INT32 len = strlen(str);
	if(pCtx->nPos + 4 + len > BUFFER_SIZE)
		return 0;

	len2 = len;
	LTOH(len);
	memcpy(&pCtx->szBuf[pCtx->nPos], &len, sizeof(len));
	memcpy(&pCtx->szBuf[pCtx->nPos+4], str, len2);
	pCtx->nPos += len2 + 4;
	
	return 1;
}

static int uncompress2(unsigned char *pDest, mz_ulong *pDest_len, const unsigned char *pSource, mz_ulong source_len)
{
  mz_stream stream;
  int status;
  memset(&stream, 0, sizeof(stream));

  // In case mz_ulong is 64-bits (argh I hate longs).
  if ((source_len | *pDest_len) > 0xFFFFFFFFU) return MZ_PARAM_ERROR;

  stream.next_in = pSource;
  stream.avail_in = (mz_uint32)source_len;
  stream.next_out = pDest;
  stream.avail_out = (mz_uint32)*pDest_len;

  status = mz_inflateInit2(&stream, -MZ_DEFAULT_WINDOW_BITS);
  if (status != MZ_OK)
    return status;

  status = mz_inflate(&stream, MZ_FINISH);
  if (status != MZ_STREAM_END)
  {
    mz_inflateEnd(&stream);
    return ((status == MZ_BUF_ERROR) && (!stream.avail_in)) ? MZ_DATA_ERROR : status;
  }
  *pDest_len = stream.total_out;

  return mz_inflateEnd(&stream);
}

//初始化库
void tdi_prot_init()
{
	srand((unsigned int)time(0));

	s_rsa_n = bignum_from_bytes(s_a_rsa_n, sizeof(s_a_rsa_n));
	s_rsa_e = bignum_from_long(65537);

}

void tdi_prot_term()
{
	freebn(s_rsa_n);
	freebn(s_rsa_e);
}


//分配一个协议缓冲区
void * tdi_prot_new()
{
	PROT_DATA * pCtx;
	//if(strlen(pszSID) >= SID_MAX_SIZE)
	//	return NULL;

	pCtx = (PROT_DATA*)malloc(sizeof(PROT_DATA));
	memset(pCtx, 0, sizeof(PROT_DATA));

	//strcpy(pCtx->szSID, pszSID);

	return pCtx;
}

//释放协议缓冲区
void tdi_prot_delete(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	free(pProt);
}

//取得要数据
char * tdi_prot_data(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	
	return (char*)pProt->szBuf;
}

//取得数据长度
int tdi_prot_len(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	
	return pProt->nLen;
}

//
int tdi_prot_sh_1(void * pCtx, const char * pszIP, UINT32 uVersion)
{
//	printf("tdi_prot_sh_1\n");
	INT32 nLen, nLen2;
	unsigned char szTmp;
	int i;

	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	//发送版本号
	//Rnd(1) + DTI_VERSION(1) + ClientVersion(4) + ip
	nLen2 = 6 + strlen(pszIP) + 1;
	nLen = HTONL(nLen2);
	memcpy(pProt->szBuf, &nLen, sizeof(nLen));
	
	szTmp = (unsigned char)rand();
	pProt->szBuf[4] = szTmp;
	pProt->szBuf[5] = DTI_VERSION;
	uVersion = HTONL(uVersion);
	memcpy(&pProt->szBuf[6], &uVersion, sizeof(uVersion));

	//ip
	strcpy((char*)&pProt->szBuf[10], pszIP);

	nLen2 += 4;
	
	for(i=5; i<nLen2; i++)
	{
		pProt->szBuf[i] ^= szTmp;
	}

	pProt->nLen = nLen2;

	strcpy(pProt->szIP, pszIP);
	pProt->uVersion = uVersion;
	
//	printf("\n<<<<<<<<<<\n");
//	printf("pProt->szSID=%s\n",		pProt->szSID);
//	printf("pProt->szIP=%s\n",		pProt->szIP);
//
////	printf("pProt->szRnd=");
////	for (int i = 0; i < RAND_KEY_SIZE; i++ && '\0' != pProt->szRnd[i]) {
////		printf("%u", pProt->szRnd[i]);
////	}
////	printf("    ");
//
//	printf("pProt->uVersion=%d\n",	pProt->uVersion);
//
//	printf("pProt->nLen=%d\n",		pProt->nLen);
//
////	printf("pProt->szBuf=");
////	for (int i = 0; i < BUFFER_SIZE; i++ && '\0' != pProt->szBuf[i]) {
////		printf("%u", pProt->szBuf[i]);
////	}
////	printf("    ");
//	printf(">>>>>>>>>>\n\n");

	return 0;
}

//
int tdi_prot_sh_2(void * pCtx, const char * pszSID, char * pData, int nLen)
{
//	printf("tdi_prot_sh_2\n");

//	printf("pData=");
//	for (int i = 0; i < 20; i++) {
//		printf("pData[%d]=%d", i, (int)pData[i]);
//	}
//	printf("    ");

	unsigned char cRnd;
	int nPKL;
	struct MD5Context ctx;
	unsigned char digest[16];
	int i, nMD5Len, nLen2;
	unsigned char * pMD5;
	char * p, *p2;
	int n;
	INT64 nValidTime;
	
	Bignum bnSign, bnMD5;
	Bignum bnPK, bnRand, bnTransfer;
	unsigned char * pEncData;

	//随机数
	unsigned char szClientRnd[24];
	
	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	if(nLen <= 24)
		return DBE_INVALID_PACKET;

	if(strlen(pszSID) >= SID_MAX_SIZE)
		return DBE_OUT_OF_RANGE;

	//20字节随机数
	//recv_data((char*)szRnd, sizeof(szRnd));
	memcpy(pProt->szRnd, pData, RAND_KEY_SIZE);
	
//	printf("pProt->szRnd=");
//	for (int i = 0; i < RAND_KEY_SIZE; i++ && '\0' != pProt->szRnd[i]) {
//		printf("%u", pProt->szRnd[i]);
//	}
//	printf("    ");

	//干扰数据
	cRnd = pData[RAND_KEY_SIZE];
	
//	printf("cRnd=%u\n", cRnd);
	
	nLen -= 21; //Rnd(20)+Rnd(1)
	pData += RAND_KEY_SIZE + 1;
	//Sign
	for(i=0; i<nLen; i++)
		pData[i] ^= cRnd;
	
	//给结尾加0,保证字符串函数的安全
	pData[nLen] = 0;

	//结构
	//	PKL:4 PK SIGN

//	printf("sizeof(nPKL)=%d\n", sizeof(nPKL));
	memcpy(&nPKL, pData, sizeof(nPKL));
//	printf("nPKL=%d\n", nPKL);
	nPKL = NTOHL(nPKL);
//	printf("nPKL=%d\n", nPKL);
//	printf("nLen=%d\n", nLen);
	if(nPKL <= 0 || nPKL + 4 >= nLen)
		return DBE_INVALID_PACKET;

	//计算PK的md5值
	MD5Init(&ctx);
	MD5Update(&ctx, (unsigned char*)&pData[4], nPKL);
	MD5Final(digest, &ctx);
	//验证签名
	bnSign = bignum_from_bytes((unsigned char*)&pData[nPKL+4], nLen-nPKL-4);
	bnMD5 = modpow(bnSign, s_rsa_e, s_rsa_n);
	
	pMD5 = bignum_to_bytes(bnMD5, &nMD5Len);
	
	freebn(bnSign);
	freebn(bnMD5);

	if((nMD5Len != 16 || memcmp(pMD5, digest, 16) ) &&
		(nMD5Len != 15 || digest[0] || memcmp(pMD5, &digest[1], 15)))
	{
		sfree(pMD5);
		return DBE_INVALID_LICENSE;
	}
	sfree(pMD5);

	//ip检查
	p = (char*)&pData[4];
	do
	{
		p2 = strchr(p, ',');
		if(p2)
		{
			*p2 = 0;
			++p2;
		}

		n = strlen(p) + 1;
		nPKL -= n;

		if(strcmp(p, pProt->szIP) == 0)
		{
			p += n;
			if(p2)
			{
				n = strlen(p2) + 1;
				nPKL -= n;
				p += n;
			}
			break;
		}
		else if(strcmp(p, "0.0.0.0") == 0)
		{
			p += n;
			if(p2)
			{
				n = strlen(p2) + 1;
				nPKL -= n;
				p += n;
			}
			break;
		}
		
		if(p2)
			p = p2;
		else
			return DBE_INVALID_LICENSE;

	} while(1);
	
	if(nPKL < 13)
	{
		return DBE_INVALID_PACKET;
	}

	//商户检查
	if(*p)
	{
		do
		{
			char * p2 = strchr(p, ',');
			if(p2)
			{
				*p2 = 0;
				++p2;
			}

			n = strlen(p) + 1;
			nPKL -= n;

			if(strcmp(p, pszSID) == 0)
			{
				p += n;
				if(p2)
				{
					n = strlen(p2) + 1;
					nPKL -= n;
					p += n;
				}
				break;
			}
		
			if(p2)
				p = p2;
			else
				return DBE_INVALID_LICENSE;

		} while(1);
	}
	else
	{
		++p;
		--nPKL;
	}

	if(nPKL < 12)
	{
		return DBE_INVALID_PACKET;
	}

	//有效期
	memcpy(&nValidTime, p, sizeof(INT64));
	nValidTime = HTONLL(nValidTime);
	if(nValidTime > 0 && nValidTime < time(NULL))
		return DBE_SIGN_EXPIRED;

	p += sizeof(int);
	nPKL -= sizeof(int);
	if(nPKL < 8)
		return DBE_INVALID_PACKET;

	//取pkl
	bnPK = bignum_from_bytes((unsigned char*)p, nPKL);
	//随机数
	memcpy(szClientRnd, "HELO", 4);
	rand_buf(&szClientRnd[4], 20);
	bnRand = bignum_from_bytes(szClientRnd, sizeof(szClientRnd));

	bnTransfer = modpow(bnRand, s_rsa_e, bnPK);

	pEncData = bignum_to_bytes(bnTransfer, &nLen);
	freebn(bnPK);
	freebn(bnRand);
	freebn(bnTransfer);

	if(nLen > sizeof(pProt->szBuf) - 4)
	{
		sfree(pEncData);
		return DBE_INVALID_PACKET;
	}
	memcpy(&pProt->szBuf[4], pEncData, nLen);

	sfree(pEncData);

	nLen2 = HTONL(nLen);
	memcpy(pProt->szBuf, &nLen2, 4);

	pProt->nLen = nLen + 4;

	//
	MD5Init(&ctx);
	MD5Update(&ctx, &szClientRnd[4], 20);
	MD5Update(&ctx, pProt->szRnd, RAND_KEY_SIZE);
	MD5Update(&ctx, (const unsigned char*)"MSHOP", 5);
	MD5Final(digest, &ctx);

	rc4_init(digest, sizeof(digest), &pProt->rc4Key);

	strcpy(pProt->szSID, pszSID);
	
//	printf("\n<<<<<<<<<<\n");
//	printf("pProt->szSID=%s\n",		pProt->szSID);
//	printf("pProt->szIP=%s\n",		pProt->szIP);
//
////	printf("pProt->szRnd=");
////	for (int i = 0; i < RAND_KEY_SIZE; i++ && '\0' != pProt->szRnd[i]) {
////		printf("%u", pProt->szRnd[i]);
////	}
////	printf("    ");
//
//	printf("pProt->uVersion=%d\n",	pProt->uVersion);
//
//	printf("pProt->nLen=%d\n",		pProt->nLen);
//
////	printf("pProt->szBuf=");
////	for (int i = 0; i < BUFFER_SIZE; i++ && '\0' != pProt->szBuf[i]) {
////		printf("%u", pProt->szBuf[i]);
////	}
////	printf("    ");
//	printf(">>>>>>>>>>\n\n");

	return 0;
}

INT32 tdi_prot_parse_status(void * pCtx, char * pData, int nLen)
{
//	return nLen;
	INT32 code;
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	
	pProt->pszData = pData;
	pProt->nPos = 0;
	pProt->nLen = nLen;

	rc4((unsigned char*)pData, nLen, &pProt->rc4Key);

	CHECK_RR(read_int32(pProt, &code));
	CHECK_RR(read_str(pProt, (char*)pProt->szBuf, sizeof(pProt->szBuf)));
	
//	printf("tdi_prot_parse_status 2222 code=%d\n", code);
//	printf("tdi_prot_parse_status 2222 pProt->szBuf=%s\n", (char *)pProt->szBuf);
	
	return code;
}

int tdi_prot_login(void * pCtx, const char * pszUser, const char * pszPwd, int bMD5, int bReconn)
{
//	printf("tdi_prot_login\n");
	
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	begin_write(pProt);

	CHECK_RR(write_str(pProt, pProt->szSID));
	CHECK_RR(write_str(pProt, pszUser));

	if(bMD5)
	{
		CHECK_RR(write_str(pProt, pszPwd));
	}
	else
	{
		char szMD5[33];
		MD5String((const unsigned char*)pszPwd, strlen(pszPwd), szMD5);
		CHECK_RR(write_str(pProt, szMD5));
	}

	CHECK_RR(write_char(pProt, bReconn?1:0));
	
	end_write(pProt);

	return 0;
}

//分配命令
void tdi_prot_st_prepare(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	pProt->nUseCount = 0;
	pProt->nPos = 0;

	begin_write(pProt);
	write_char(pProt, DTI_CMD_QUERY);
	write_int(pProt, 0); //usecount
}

//释放一个命名
void tdi_prot_st_close(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	//
	
	//pszColumns
	if(pProt->pszColumns)
	{
		int i;
		for(i = 0; i<pProt->nColumns; i++)
		{
			if(pProt->pszRows && pProt->pszRows[i])
			{
				free(pProt->pszRows[i]);
			}

			if(pProt->pszColumns[i].pszName)
				free(pProt->pszColumns[i].pszName);
		}

		free(pProt->pszColumns);
		pProt->pszColumns = NULL;

		free(pProt->pszRows);
		pProt->pszRows = NULL;
	}

	pProt->nUseCount = 0;
	pProt->nRows = 0;
	pProt->nCurRows = 0;
	pProt->nColumns = 0;
	pProt->nPos = 0;
	pProt->nLen = 0;
}

//绑定一个整数参数
int tdi_prot_st_bind_int(void * pCtx, const char * pszName, INT32 nValue, int bNull)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	++pProt->nUseCount;

	CHECK_RR(write_str(pProt, pszName));
	CHECK_RR(write_char(pProt, 3)); //int
	CHECK_RR(write_char(pProt, bNull?1:0));
	if(bNull) return 0;
	CHECK_RR(write_int(pProt, nValue));
	return 0;
}

//绑定一个字符参数
int tdi_prot_st_bind_str(void * pCtx, const char * pszName, const char * pszValue)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	++pProt->nUseCount;

	CHECK_RR(write_str(pProt, pszName));
	CHECK_RR(write_char(pProt, 1)); //string
	CHECK_RR(write_char(pProt, pszValue?0:1));
	if(!pszValue) return 0;
	CHECK_RR(write_str(pProt, pszValue));
	return 0;
}

//构造执行命令
int tdi_prot_st_execute(void * pCtx, const char * pszSQL)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	INT32 len = pProt->nUseCount;
	LTOH(len);
	memcpy(&pProt->szBuf[5], &len, 4);

	CHECK_RR(write_int(pProt, 0)); //use vector
	CHECK_RR(write_str(pProt, pszSQL));
	CHECK_RR(write_char(pProt, 0)); //rewrite
	CHECK_RR(write_char(pProt, 1)); //exchange

	end_write(pProt);

	return 0;
}

//分析执行结果
int tdi_prot_st_result(void * pCtx, char * pData, int nLen)
{
	int i;
	char cmp;
	INT32 code;
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	
	pProt->pszData = pData;
	pProt->nPos = 0;
	pProt->nLen = nLen;

	rc4((unsigned char*)pData, nLen, &pProt->rc4Key);

	CHECK_RR(read_char(pProt, &cmp));

	if(cmp)
	{
		mz_ulong nUncompLen = sizeof(pProt->szBuf);
		int testRet;
		if((testRet = uncompress2(pProt->szBuf, &nUncompLen, (unsigned char*)(pData + 1), nLen - 1)) != Z_OK)
		{
			printError("Uncompress error %d", testRet);
			return DBE_INVALID_PACKET;
		}
		
		pProt->nLen = nUncompLen;
		pProt->pszData = (char*)pProt->szBuf;
		pProt->nPos = 0;
	}

	CHECK_RR(read_int32(pProt, &code));

	if(code)
	{
		CHECK_RR(read_str(pProt, (char*)pProt->szBuf, sizeof(pProt->szBuf)));
		return code;
	}
	else
	{
		char szTmp[16];
		CHECK_RR(read_str(pProt, szTmp, sizeof(szTmp)));
	}

	CHECK_RR(read_int32(pProt, &pProt->nRows));
	CHECK_RR(read_int32(pProt, &code));			//int64
	CHECK_RR(read_int32(pProt, &pProt->nColumns)); //int32

	if(pProt->nColumns < 0 || pProt->nColumns > 256) {
		printError("Too much column");
		return DBE_INVALID_PACKET;
	}

	
	if(pProt->nColumns == 0)
		return 0;

	pProt->pszColumns = (COLUMN*)malloc(sizeof(COLUMN)*pProt->nColumns);
	memset(pProt->pszColumns, 0, sizeof(COLUMN)*pProt->nColumns);

	//读列数据
	for(i=0; i<pProt->nColumns; i++)
	{
		INT32 len;
		if(!read_char(pProt, &pProt->pszColumns[i].nType) ||
			!read_int32(pProt, &len) ||
			len <= 0 ||
			len >= MAX_STRING_SIZE ||
			!(pProt->pszColumns[i].pszName = (char*)malloc(len+1)) ||
			!read_data(pProt, pProt->pszColumns[i].pszName, len))
		{
			tdi_prot_st_close(pProt);
			printError("Reading fail");
			return DBE_INVALID_PACKET;
		}

	}
	
	return 0;
}

//取行数
int tdi_prot_st_rows(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	return pProt->nRows;
}

//取列个数
int tdi_prot_st_fields(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	return pProt->nColumns;
}

//取列名
const char* tdi_prot_st_field_name(void * pCtx, int nIndex)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	return pProt->pszColumns[nIndex].pszName;
}

//取列值类型
int tdi_prot_st_field_type(void * pCtx, int nIndex)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	switch(pProt->pszColumns[nIndex].nType)
	{
		//dt_string, dt_date, dt_double, dt_decimal, dt_integer, dt_unsigned_long, dt_long_long, dt_unsigned_long_long
	case 4:
	case 5:
	case 6:
	case 7:
		return COLUMN_TYPE_INT;
	}

	return COLUMN_TYPE_STRING;
}

//取整数值
int tdi_prot_st_int_field(void * pCtx, int nIndex)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	switch(pProt->pszColumns[nIndex].nType)
	{
		//dt_string, dt_date, dt_double, dt_decimal, dt_integer, dt_unsigned_long, dt_long_long, dt_unsigned_long_long
	case 4:
	case 5:
	case 6:
	case 7:
		return *(int*)pProt->pszRows[nIndex];
	}

	return 0;
}

//取字符串值
const char* tdi_prot_st_str_field(void * pCtx, int nIndex)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	switch(pProt->pszColumns[nIndex].nType)
	{
		//dt_string, dt_date, dt_double, dt_decimal, dt_integer, dt_unsigned_long, dt_long_long, dt_unsigned_long_long
	case 4:
	case 5:
	case 6:
	case 7:
		return NULL;
	}

	return pProt->pszRows[nIndex];
}

int tdi_prot_st_is_null(void * pCtx, int nIndex)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	return (pProt->pszRows[nIndex]==NULL) ? 1: 0;
}

int tdi_prot_next(void * pCtx)
{
	PROT_DATA * pProt = (PROT_DATA*)pCtx;
	int i;

	if(pProt->nCurRows >= pProt->nRows || pProt->nColumns == 0 || !pProt->pszColumns)
		return DBE_NO_RESULT;

	if(!pProt->pszRows)
	{
		pProt->pszRows = (char**)malloc(sizeof(char*)*pProt->nColumns);
		memset(pProt->pszRows, 0, sizeof(char*)*pProt->nColumns);
	}

	for(i=0; i<pProt->nColumns; i++)
	{
		switch(pProt->pszColumns[i].nType)
		{
		//dt_string, dt_date, dt_double, dt_decimal, dt_integer, dt_unsigned_long, dt_long_long, dt_unsigned_long_long
		case 4: //string
		case 5:
		case 6:
		case 7:
			if(pProt->pszRows[i] == NULL)
				pProt->pszRows[i] = (char*)malloc(sizeof(int));
			break;
		}
	}


	//读行数据
	for(i=0; i<pProt->nColumns; i++)
	{
		char null;
		CHECK_RR(read_char(pProt, &null));
		if(null)
		{
			if(pProt->pszRows[i])
			{
				free(pProt->pszRows[i]);
				pProt->pszRows[i] = NULL;
			}
			continue;
		}

		switch(pProt->pszColumns[i].nType)
		{
		case 4: //int
		case 5: //unsigned
			{
				INT32 x;
				CHECK_RR(read_int32(pProt, &x));
				*(int*)pProt->pszRows[i] = x;

			}
			break;
		case 6: //long long
		case 7: //unsigned long long
			{
				INT32 x, j;
				CHECK_RR(read_int32(pProt, &x));
				CHECK_RR(read_int32(pProt, &j));
				*(int*)pProt->pszRows[i] = x;

			}
			break;
		case 0: //string
		case 3: //decimal
			{
				INT32 len;
				CHECK_RR(read_int32(pProt, &len));
				if(pProt->pszRows[i])
				{
					if((int)strlen(pProt->pszRows[i]) < len)
					{
						free(pProt->pszRows[i]);
						pProt->pszRows[i] = (char*)malloc(len + 1);
					}
				}
				else
				{
					pProt->pszRows[i] = (char*)malloc(len + 1);
				}
				CHECK_RR(read_data(pProt, pProt->pszRows[i], len));
			}
			break;
		case 2: //double
			{
				double d;
				char tmp[32];
				INT32 len;
				CHECK_RR(read_dbl(pProt, &d));
				sprintf(tmp, "%f", d);
				len = strlen(tmp);

				if(pProt->pszRows[i])
				{
					if((int)strlen(pProt->pszRows[i]) < len)
					{
						free(pProt->pszRows[i]);
						pProt->pszRows[i] = (char*)malloc(len + 1);
					}
				}
				else
				{
					pProt->pszRows[i] = (char*)malloc(len + 1);
				}

				strcpy(pProt->pszRows[i], tmp);
			}
			break;
		case 1: //time
			{
				INT32 year;
				char mon, mday, hour, min, sec;
				char szTime[20];
				INT32 len;
				
				CHECK_RR(read_int32(pProt, &year));
				CHECK_RR(read_char(pProt, &mon));
				CHECK_RR(read_char(pProt, &mday));
				CHECK_RR(read_char(pProt, &hour));
				CHECK_RR(read_char(pProt, &min));
				CHECK_RR(read_char(pProt, &sec));

				sprintf(szTime, "%04d-%02d-%02d %02d:%02d:%02d", year + 1900, mon + 1, mday, hour, min, sec);

				len = strlen(szTime);

				if(pProt->pszRows[i])
				{
					if((int)strlen(pProt->pszRows[i]) < len)
					{
						free(pProt->pszRows[i]);
						pProt->pszRows[i] = (char*)malloc(len + 1);
					}
				}
				else
				{
					pProt->pszRows[i] = (char*)malloc(len + 1);
				}

				strcpy(pProt->pszRows[i], szTime);
			}
			break;
		}
	}

	pProt->nCurRows++;
	return 0;
}

/*
typedef struct
{
	char szSID[SID_MAX_SIZE];
	char szIP[16];
	unsigned char szRnd[RAND_KEY_SIZE];	//随机数
	UINT32 uVersion;					//客户端版本
	rc4_state rc4Key;
	INT32 nUseCount;					//SQL绑定参数个数
	INT32 nRows;						//行数
	INT32 nCurRows;						//行数
	INT32 nColumns;						//列数
	COLUMN * pszColumns;					//
	char ** pszRows;				//
	int nPos;							//读取位置
	char * pszData;						//读取缓冲区
	int nLen;							//数据长度
	unsigned char szBuf[BUFFER_SIZE]; //构建请求包
} PROT_DATA;
*/

int tdi_prot_print(void * pCtx, char *str) {

	PROT_DATA * pProt = (PROT_DATA*)pCtx;

	printf("----------------------------------------");
	printf("%s pProt->szSID=%s", 		str, pProt->szSID);
	printf("%s pProt->szIP=%s", 		str, pProt->szIP);
	printf("%s pProt->szRnd=%s", 		str, pProt->szRnd);
	printf("%s pProt->uVersion=%d", 	str, pProt->uVersion);
//	printf("%s pProt->rc4Key=%??", 		str, pProt->rc4Key);
	printf("%s pProt->nUseCount=%d",	str, pProt->nUseCount);
	printf("%s pProt->nRows=%d", 		str, pProt->nRows);
	printf("%s pProt->nCurRows=%d", 	str, pProt->nCurRows);
	printf("%s pProt->nColumns=%d", 	str, pProt->nColumns);
//	printf("%s pProt->pszColumns=%??", 	str, pProt->pszColumns);
//	printf("%s pProt->pszRows=%??",		str, pProt->pszRows);
	printf("%s pProt->nPos=%d", 		str, pProt->nPos);
	printf("%s pProt->pszData=%s", 		str, pProt->pszData);
	printf("%s pProt->nLen=%d", 		str, pProt->nLen);
	printf("%s pProt->szBuf=%s", 		str, pProt->szBuf);
	printf("========================================");
}

