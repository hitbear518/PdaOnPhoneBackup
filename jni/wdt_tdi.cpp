// wdt_tdi.cpp : Defines the entry point for the console application.
//
#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <jni.h>

#include "md5.h"
#include "tdi_prot.h"

#include "wdt_tdi.h"
#include "wdt_tdi_login.h"
#include "wdt_tdi_update.h"
#include "wdt_tdi_query.h"

//接收SOCKET缓冲区
char g_recvBuf[8192];
//收到的数据长度
int g_recvLen;

// Hold login status
void *g_tdi;

JavaVM *g_jvm = NULL;

const char *g_stringSig = "Ljava/lang/String;";

const char *g_wdtLoginPath = "com/zsxj/pda/wdt/WDTLogin";
const char *g_wdtQueryPath = "com/zsxj/pda/wdt/WDTQuery";
const char *g_wdtUpdatePath = "com/zsxj/pda/wdt/WDTUpdate";

const char *g_accountPath = "com/zsxj/pda/wdt/Account";

CACHE g_accountCache;
JNINativeMethod g_loginMethodTable[] =
{
	{"initTDI", "()I", (void *) initTDI},
	{"tdiProtSh1", "(Ljava/lang/String;)[B", (void *) tdiProtSh1},
	{"tdiProtSh2", "([BLjava/lang/String;)[B", (void *) tdiProtSh2},
	{"tdiProtSh3", "([BLjava/lang/String;Ljava/lang/String;)[B", (void *) tdiProtSh3},
	{"tdiProtSh4", "([B)[B", (void *) tdiProtSh4},
	{"prepareGetUserId", "(Ljava/lang/String;)[B", (void *) prepareGetUserId},
	{"getUserId", "([B)I", (void *) getUserId},
	{"prepareGetAccounts", "()[B", (void *) prepareGetAccounts},
	{"getAccounts", "([B)[Lcom/zsxj/pda/wdt/Account;", (void *) getAccounts},
	{"isLogin", "()Z", (void *) isLogin},

	{"logout", "()V", (void *) logout},
	{"preGetUser", "(Ljava/lang/String;)[B", (void *) preGetUser},
	{"getUser", "([B)Lcom/zsxj/pda/wdt/User;", (void *) getUser}
};

const char *g_warehousePath = "com/zsxj/pda/wdt/Warehouse";
const char *g_stockTakingEntryPath = "com/zsxj/pda/wdt/PdEntry";
const char *g_specPath = "com/zsxj/pda/wdt/Spec";
const char *g_positionPath = "com/zsxj/pda/wdt/Position";
const char *g_exPath = "com/zsxj/pda/wdt/WDTException";

const char *g_supplierPath = "com/zsxj/pda/wdt/Supplier";
const char *g_pricePath = "com/zsxj/pda/wdt/Price";
const char *g_logisticsPath = "com/zsxj/pda/wdt/Logistics";
const char *g_tradeInfoPath = "com/zsxj/pda/wdt/TradeInfo";
const char *g_userPath = "com/zsxj/pda/wdt/User";
const char *g_tradeGoodPath = "com/zsxj/pda/wdt/TradeGoods";
const char *g_shopPath = "com/zsxj/pda/wdt/Shop";
const char *g_cashSaleSpecPath = "com/zsxj/pda/wdt/CashSaleSpec";
const char *g_customerPath = "com/zsxj/pda/wdt/Customer";

CACHE g_warehouseCache;
CACHE g_PdEntryCache;
CACHE g_goodsCache;
CACHE g_specCache;
CACHE g_positionCache;
CACHE g_pdSpecCache;
CACHE g_wdtExceptionCache;
CACHE g_supplierCache;
CACHE g_priceCache;
CACHE g_logisticsCache;
CACHE g_tradeInfoCache;
CACHE g_userCache;
CACHE g_tradeGoodCache;
CACHE g_shopCache;
CACHE g_cashSaleSpecCache;
CACHE g_customerCache;

JNINativeMethod g_queryMethodTable[] =
{
	{"preGetWarehouses", "()[B", (void *) preGetWarehouses},
	{"getWarehouses", "([B)[Lcom/zsxj/pda/wdt/Warehouse;", (void *) getWarehouses},
	{"preGetPdEntries", "(I)[B", (void *) preGetPdEntries},
	{"getPdEntries", "([B)[Lcom/zsxj/pda/wdt/PdEntry;", (void *) getPdEntries},
	{"preGetSpecs", "(ILjava/lang/String;)[B", (void *) preGetSpecs},
	{"getSpecs", "([B)[Lcom/zsxj/pda/wdt/Spec;", (void *) getSpecs},
	{"preGetPositionCount", "(I)[B", (void *) preGetPositionCount},
	{"getPositionCount", "([B)I", (void *) getPositionCount},
	{"preGetPositions", "(III)[B", (void *) preGetPositions},
	{"getPositions", "([B)[Lcom/zsxj/pda/wdt/Position;", (void *) getPositions},

	{"preGetPdDetailCount", "(I)[B", (void *) preGetPdDetailCount},
	{"getPdDetailCount", "([B)I", (void *) getPdDetailCount},
	{"preGetPdDetails", "(III)[B", (void *) preGetPdDetails},
	{"getPdDetails", "([B)[Lcom/zsxj/pda/wdt/Spec;", (void *) getPdDetails},
	{"preGetPdSpecs", "(ILjava/lang/String;)[B", (void *) preGetPdSpecs},
	{"getPdSpecs", "([B)[Lcom/zsxj/pda/wdt/Spec;", (void *) getPdSpecs},
	{"preGetSuppliers", "(II)[B", (void *) preGetSuppliers},
	{"getSuppliers", "([B)[Lcom/zsxj/pda/wdt/Supplier;", (void *) getSuppliers},
	{"preGetPrice", "(II)[B", (void *) preGetPrice},
	{"getPrice", "([B)Lcom/zsxj/pda/wdt/Price;", (void *) getPrice},

	{"preGetLogistics", "()[B", (void *) preGetLogistics},
	{"getLogistics", "([B)[Lcom/zsxj/pda/wdt/Logistics;", (void *) getLogistics},
	{"preGetTradeInfo", "(Ljava/lang/String;)[B", (void *) preGetTradeInfo},
	{"getTradeInfo", "([B)Lcom/zsxj/pda/wdt/TradeInfo;", (void *) getTradeInfo},
	{"preGetPickers", "()[B", (void *) preGetPickers},
	{"getPickers", "([B)[Lcom/zsxj/pda/wdt/User;", (void *) getPickers},
	{"preGetTradeGoods", "(II)[B", (void *) preGetTradeGoods},
	{"getTradeGoods", "([B)[Lcom/zsxj/pda/wdt/TradeGoods;", (void *) getTradeGoods},
	{"preGetInExamSpecs", "(Ljava/lang/String;)[B", (void *) preGetInExamSpecs},
	{"getInExamSpecs", "([B)[Lcom/zsxj/pda/wdt/Spec;", (void *) getInExamSpecs},

	{"preGetSupplierCount", "()[B", (void *) preGetSupplierCount},
	{"getSupplierCount", "([B)I", (void *) getSupplierCount},
	{"preGetShops", "()[B", (void *) preGetShops},
	{"getShops", "([B)[Lcom/zsxj/pda/wdt/Shop;", (void *) getShops},
	{"preGetCashSaleSpecs", "(ILjava/lang/String;)[B", (void *) preGetCashSaleSpecs},
	{"getCashSaleSpecs", "([B)[Lcom/zsxj/pda/wdt/CashSaleSpec;", (void *) getCashSaleSpecs},
	{"preGetInterfaceWarehouses", "(Ljava/lang/String;)[B", (void *) preGetInterfaceWarehouses},
	{"preGetCashSaleSpecStock", "(II)[B", (void *) preGetCashSaleSpecStock},
	{"getCashSaleSpecStock", "([B)Ljava/lang/String;", (void *) getCashSaleSpecStock},
	{"preGetCustomers", "(Ljava/lang/String;)[B", (void *) preGetCustomers},

	{"getCustomers", "([B)[Lcom/zsxj/pda/wdt/Customer;", (void *) getCustomers},
	{"preGetCashSaleSpecsByTerm", "(ILjava/lang/String;)[B", (void *) preGetCashSaleSpecsByTerm},
	{"preGetSpecsByTerm", "(ILjava/lang/String;)[B", (void *) preGetSpecsByTerm}
};

JNINativeMethod g_updateMethodTable[] =
{
	{"preFastPd", "(IILjava/lang/String;I)[B", (void *) preFastPd},
	{"preSetStock", "(IIILjava/lang/String;)[B", (void *) preSetStock},
	{"preNewPosition", "(III)[B", (void *) preNewPosition},
	{"prePdSubmit", "(IIIILjava/lang/String;)[B", (void *) prePdSubmit},
	{"preFastInExamineGoodsSubmit", "(IIIIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)[B", (void *) preFastInExamineGoodsSubmit},
	{"preStockOut", "(III)[B", (void *) preStockOut},
	{"preUpdateTradeBscan", "(I)[B", (void *) preUpdateStradeBscan},
	{"prePickError", "(I)[B", (void *) prePickError},
	{"completeUpdate", "([B)V", (void *) completeUpdate}
};


#define CHECK_SD(x) if(!(x)) { printf("数据包错误"); exit(-1); }

void cacheWarehouse(JNIEnv *env)
{
	jclass warehouseCls = env->FindClass(g_warehousePath);
	CHECK_NULL(warehouseCls);
	g_warehouseCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(warehouseCls));
	CHECK_NULL(g_warehouseCache.cls);
	g_warehouseCache.initId = env->GetMethodID(warehouseCls, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");
	CHECK_NULL(g_warehouseCache.initId);
}

void cachePdEntry(JNIEnv *env)
{
	jclass stockTakingEntryCls = env->FindClass(g_stockTakingEntryPath);
	CHECK_NULL(stockTakingEntryCls);
	g_PdEntryCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(stockTakingEntryCls));
	CHECK_NULL(g_PdEntryCache.cls);
	g_PdEntryCache.initId = env->GetMethodID(stockTakingEntryCls, "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	CHECK_NULL(g_PdEntryCache.initId);
}

void cacheSpec(JNIEnv *env)
{
	// Cache Spec
	jclass specCls = env->FindClass(g_specPath);
	CHECK_NULL(specCls);
	g_specCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(specCls));
	CHECK_NULL(g_specCache.cls);
	char specInitSig[strlen(g_stringSig) * 7 + 20];
	sprintf(specInitSig, "(I%s%s%s%s%sI%s%s)V", g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig);
	g_specCache.initId = env->GetMethodID(specCls, "<init>", specInitSig);
	CHECK_NULL(g_specCache.initId);
}

void cacheAccount(JNIEnv *env)
{
	jclass accountCls = env->FindClass(g_accountPath);
	CHECK_NULL(accountCls);
	g_accountCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(accountCls));
	CHECK_NULL(g_accountCache.cls);
	const char *accountInitSig = "(ILjava/lang/String;)V";
	g_accountCache.initId = env->GetMethodID(accountCls, "<init>", accountInitSig);
	CHECK_NULL(g_accountCache.initId);
}

void cachePosition(JNIEnv *env)
{
	jclass positionCls = env->FindClass(g_positionPath);
	CHECK_NULL(positionCls);
	g_positionCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(positionCls));
	CHECK_NULL(g_positionCache.cls);
	const char *positionInitSig = "(ILjava/lang/String;)V";
	g_positionCache.initId = env->GetMethodID(positionCls, "<init>", positionInitSig);
	CHECK_NULL(g_positionCache.initId);
}

void cachePdSpec(JNIEnv *env)
{
	jclass specCls = env->FindClass(g_specPath);
	CHECK_NULL(specCls);
	g_pdSpecCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(specCls));
	CHECK_NULL(g_pdSpecCache.cls);
	char pdSpecInitSig[strlen(g_stringSig) * 10 + 20];
	sprintf(pdSpecInitSig, "(II%s%s%s%s%s%sI%s%s%s%s)V", g_stringSig, g_stringSig,
			g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig,
			g_stringSig, g_stringSig, g_stringSig);
	g_pdSpecCache.initId = env->GetMethodID(specCls, "<init>", pdSpecInitSig);
	CHECK_NULL(g_pdSpecCache.initId);
}

void cacheWdtException(JNIEnv *env)
{
	jclass exCls = env->FindClass(g_exPath);
	CHECK_NULL(exCls);
	g_wdtExceptionCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(exCls));
	CHECK_NULL(g_wdtExceptionCache.cls);
	g_wdtExceptionCache.initId =
		env->GetMethodID(exCls, "<init>", "(ILjava/lang/String;)V");
	CHECK_NULL(g_wdtExceptionCache.initId);
}

void cacheSupplier(JNIEnv *env)
{
	jclass supplierCls = env->FindClass(g_supplierPath);
	CHECK_NULL(supplierCls);
	g_supplierCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(supplierCls));
	CHECK_NULL(g_supplierCache.cls);
	g_supplierCache.initId =
		env->GetMethodID(supplierCls, "<init>", "(ILjava/lang/String;)V");
	CHECK_NULL(g_supplierCache.initId);
}

void cachePrice(JNIEnv *env)
{
	jclass priceCls = env->FindClass(g_pricePath);
	CHECK_NULL(priceCls);
	g_priceCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(priceCls));
	CHECK_NULL(g_priceCache.cls);
	char priceInitSig[strlen(g_stringSig) * 5 + 10];
	sprintf(priceInitSig, "(%s%s%s%s%s)V", g_stringSig, g_stringSig,
		g_stringSig, g_stringSig, g_stringSig);
	g_priceCache.initId =
		env->GetMethodID(priceCls, "<init>", priceInitSig);
	CHECK_NULL(g_priceCache.initId);
}

void cacheLogistics(JNIEnv *env)
{
	jclass logisticsCls = env->FindClass(g_logisticsPath);
	CHECK_NULL(logisticsCls);
	g_logisticsCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(logisticsCls));
	CHECK_NULL(g_logisticsCache.cls);
	g_logisticsCache.initId =
		env->GetMethodID(logisticsCls, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");
	CHECK_NULL(g_logisticsCache.initId);
}

void cacheTradeInfo(JNIEnv *env)
{
	jclass tradeInfoCls = env->FindClass(g_tradeInfoPath);
	CHECK_NULL(tradeInfoCls);
	g_tradeInfoCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(tradeInfoCls));
	CHECK_NULL(g_tradeInfoCache.cls);
	g_tradeInfoCache.initId =
		env->GetMethodID(tradeInfoCls, "<init>", "(IIIIIILjava/lang/String;I)V");
	CHECK_NULL(g_tradeInfoCache.initId);
}

void cacheUser(JNIEnv *env)
{
	jclass userCls = env->FindClass(g_userPath);
	CHECK_NULL(userCls);
	g_userCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(userCls));
	CHECK_NULL(g_userCache.cls);
	g_userCache.initId =
		env->GetMethodID(userCls, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");
	CHECK_NULL(g_userCache.initId);
}

void cacheTradeGood(JNIEnv *env)
{
	jclass tradeGoodsCls = env->FindClass(g_tradeGoodPath);
	CHECK_NULL(tradeGoodsCls);
	g_tradeGoodCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(tradeGoodsCls));
	CHECK_NULL(g_tradeGoodCache.cls);
	char tradeGoodSig[strlen(g_stringSig) * 6 + 10];
	sprintf(tradeGoodSig, "(I%s%s%s%s%s%s%s)V", g_stringSig, g_stringSig, g_stringSig,
			g_stringSig, g_stringSig, g_stringSig, g_stringSig);
	g_tradeGoodCache.initId =
		env->GetMethodID(tradeGoodsCls, "<init>", tradeGoodSig);
	CHECK_NULL(g_tradeGoodCache.initId);
}

void cacheShop(JNIEnv *env)
{
	jclass shopCls = env->FindClass(g_shopPath);
	CHECK_NULL(shopCls);
	g_shopCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(shopCls));
	CHECK_NULL(g_shopCache.cls);
	g_shopCache.initId = env->GetMethodID(shopCls, "<init>", "(ILjava/lang/String;)V");
	CHECK_NULL(g_shopCache.initId);
}

void cacheCashSaleSpec(JNIEnv *env)
{
	jclass cashSaleSpecCls = env->FindClass(g_cashSaleSpecPath);
	CHECK_NULL(cashSaleSpecCls);
	g_cashSaleSpecCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(cashSaleSpecCls));
	CHECK_NULL(g_cashSaleSpecCache.cls);
	char cashSaleSpecSig[strlen(g_stringSig) * 13 + 10];
	sprintf(cashSaleSpecSig, "(I%s%s%s%s%s%s%s%s%s%s%s%s%sI)V", g_stringSig, g_stringSig, g_stringSig,
		g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig);
	g_cashSaleSpecCache.initId = env->GetMethodID(cashSaleSpecCls, "<init>",cashSaleSpecSig);
	CHECK_NULL(g_cashSaleSpecCache.initId);
}

void cacheCustomer(JNIEnv *env)
{
	jclass cls = env->FindClass(g_customerPath);
	CHECK_NULL(cls);
	g_customerCache.cls = reinterpret_cast<jclass>(env->NewGlobalRef(cls));
	CHECK_NULL(g_customerCache.cls);
	char sig[strlen(g_stringSig) * 9 + 10];
	sprintf(sig, "(I%s%s%s%s%s%s%s%s%s)V", g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig, g_stringSig);
	g_customerCache.initId = env->GetMethodID(cls, "<init>", sig);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
#ifdef DEBUG
	__android_log_write(ANDROID_LOG_DEBUG, "Load", "start load");
#endif

	JNIEnv* env = NULL;
	jint result = -1;

	// 获取JNI版本
	if (JNI_OK != vm->GetEnv((void**)&env, JNI_VERSION_1_6)) {
		printf("GetEnv failed!");
		return result;
	}

	// 保存全局JVM以便在子线程中使用
	g_jvm = vm;

	cacheWarehouse(env);
	cachePdEntry(env);
	cacheSpec(env);
	cacheAccount(env);
	cachePosition(env);
	cachePdSpec(env);
	cacheWdtException(env);
	cacheSupplier(env);
	cachePrice(env);
	cacheLogistics(env);
	cacheTradeInfo(env);
	cacheUser(env);
	cacheTradeGood(env);
	cacheShop(env);
	cacheCashSaleSpec(env);
	cacheCustomer(env);

	// Register native methods
	jclass loginCls = env->FindClass(g_wdtLoginPath);
	CHECK_NULL(loginCls);
	jint returnValue = env->RegisterNatives(loginCls, g_loginMethodTable, 13);
	if (returnValue < 0)
	{
		env->Throw(env->ExceptionOccurred());
	}

	jclass queryCls = env->FindClass(g_wdtQueryPath);
	CHECK_NULL(queryCls);
	returnValue = env->RegisterNatives(queryCls, g_queryMethodTable, 43);
	if (returnValue < 0)
	{
		env->Throw(env->ExceptionOccurred());
	}

	jclass updateCls = env->FindClass(g_wdtUpdatePath);
	CHECK_NULL(updateCls);
	returnValue = env->RegisterNatives(updateCls, g_updateMethodTable, 9);
	if (returnValue < 0)
	{
		env->Throw(env->ExceptionOccurred());
	}

	return JNI_VERSION_1_6;
}

void transjbArrToBuf(JNIEnv *env, jbyteArray jbArray) {

	// Convert
	jbyte* arrayBody = env->GetByteArrayElements(jbArray, 0);
	if (NULL == arrayBody) {
		printError("%s: get byte array fails", __FUNCTION__);
		return;
	}

	// s_recvLen
	INT32 len;
	memcpy((char*)&len, arrayBody, sizeof(len));
	g_recvLen = NTOHL(len);

	// s_recvBuf
	memcpy(g_recvBuf, arrayBody + 4, g_recvLen);

	// release
	env->ReleaseByteArrayElements(jbArray, arrayBody, 0);
}

jbyteArray transTdiTojbArr(JNIEnv *env) {

	jbyteArray jbArr = env->NewByteArray(tdi_prot_len(g_tdi));
	if (NULL == jbArr) {
		printError("%s: array can not be constructed", __FUNCTION__);
		return NULL;
	}
	env->SetByteArrayRegion(jbArr, 0, tdi_prot_len(g_tdi), (jbyte*)tdi_prot_data(g_tdi));
	CHECK_EXCEPTION(env->ExceptionOccurred());
	return jbArr;
}

JNIEXPORT jint JNICALL initTDI(JNIEnv *env, jobject obj) {

	int status;
	// 初始化协议库
	tdi_prot_init();

	// 分配会话
	g_tdi = tdi_prot_new();

	return 1;
}

jint throwEx(JNIEnv *env, jint status, const char *message)
{
	jstring jMessage = env->NewStringUTF(message);
	jobject obj = env->NewObject(
		g_wdtExceptionCache.cls, g_wdtExceptionCache.initId,
		status, jMessage);
	jthrowable ex = static_cast<jthrowable> (obj);
	return env->Throw(ex);
}

int getSqlResult(JNIEnv *env, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("************************** getSqlResult, status = %d", status);
	CHECK_STATUS(status);
	return status;
}

jstring getStrData(JNIEnv *env, int row, int col)
{
	const char *colName;
	const char *str;
	jstring data;
	colName = tdi_prot_st_field_name(g_tdi, col);
	if (!tdi_prot_st_is_null(g_tdi, col))
		str = tdi_prot_st_str_field(g_tdi, col);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
#ifdef DEBUG
	printf("row = %d -- %s = %s", row, colName, str);
#endif
	data = env->NewStringUTF(str);
	CHECK_NULL(data);

	return data;
}

jint getIntData(int row, int col)
{
	const char *colName;
	jint data;
	colName = tdi_prot_st_field_name(g_tdi, col);
	if (!tdi_prot_st_is_null(g_tdi, col))
			data = tdi_prot_st_int_field(g_tdi, col);
	else
	{
			printError("%s = null", colName);
			data = -1;
	}
#ifdef DEBUG
			printf("row = %d -- %s = %d", row, colName, data);
#endif
	return data;
}



