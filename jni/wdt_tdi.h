#include <jni.h>
#include <android/log.h>

#ifndef WDT_TDI_H
#define WDT_TDI_H

#ifdef __cplusplus
extern "C" {
#endif

#define DEBUG

#define CHECK_STATUS(status) \
{ \
	if(status) \
	{ \
		printf("error: %d %s\n", status, tdi_prot_data(g_tdi)); \
	} \
}

#define CHECK_NULL(pointer) \
{ \
	if (NULL == pointer) \
	{ \
		env->Throw(env->ExceptionOccurred()); \
	} \
}

#define CHECK_EXCEPTION(ex) \
{ \
	if (ex != NULL) \
	{ \
		env->Throw(ex); \
	} \
}

typedef struct {
	jclass cls;
	jmethodID initId;
} CACHE;

extern CACHE g_accountCache;

extern CACHE g_warehouseCache;
extern CACHE g_PdEntryCache;
extern CACHE g_goodsCache;
extern CACHE g_specCache;
extern CACHE g_positionCache;
extern CACHE g_pdSpecCache;
extern CACHE g_wdtExceptionCache;
extern CACHE g_supplierCache;
extern CACHE g_priceCache;
extern CACHE g_logisticsCache;
extern CACHE g_tradeInfoCache;
extern CACHE g_userCache;
extern CACHE g_tradeGoodCache;
extern CACHE g_shopCache;
extern CACHE g_cashSaleSpecCache;
extern CACHE g_customerCache;

jint initTDI(JNIEnv *, jobject);

extern void transjbArrToBuf(JNIEnv *env, jbyteArray jbArray);
extern jbyteArray transTdiTojbArr(JNIEnv *env);

//接收SOCKET缓冲区
extern char g_recvBuf[];
//收到的数据长度
extern int g_recvLen;

// Hold login status
extern void *g_tdi;

extern JavaVM *g_jvm;

extern jint throwEx(JNIEnv *env, jint status, const char *message);

extern int getSqlResult(JNIEnv *env, jbyteArray jbytes);

extern jstring getStrData(JNIEnv *env, int row, int col);

extern jint getIntData(int row, int col);

#ifdef __cplusplus
}
#endif

#endif
