#include <stdio.h>
#include <stdlib.h>
#include "tdi_prot.h"
#include "wdt_tdi.h"
#include "wdt_tdi_update.h"



jbyteArray preFastPd(JNIEnv *env, jobject thiz, jint userId, jint warehouseId, jstring inforJstr, jint accountId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *infoStr = env->GetStringUTFChars(inforJstr, NULL);

	const char *callProcedure = "CALL SP_FAST_PD_ENTRY(@errcode, @errmsg, %d, %d, '%s', %d);";
	char sql[strlen(callProcedure) + 20];
	sprintf(sql, callProcedure, userId, warehouseId, infoStr, accountId);

	__android_log_write(ANDROID_LOG_DEBUG, "fast pd", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("uPrepareUpdateStock status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

void completeUpdate(JNIEnv *env, jobject thiz, jbyteArray jbArray)
{
	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("uResult status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return;
	}
}

jbyteArray preSetStock(JNIEnv *env, jobject thiz, jint warehouseId, jint specId, jint positionId, jstring jstock)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *stock = env->GetStringUTFChars(jstock, NULL);

	const char *update = "UPDATE g_stock_positions SET Stock='%s' WHERE SpecID=%d AND PositionsID=%d AND WarehouseID=%d";
	char sql[strlen(update) + 30];
	sprintf(sql, update, stock, specId, positionId, warehouseId);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("prepareSetStock status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	return transTdiTojbArr(env);
}

jbyteArray preNewPosition(JNIEnv *env, jobject thiz, jint warehouseId, jint specId, jint positionId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *insert = "INSERT IGNORE INTO g_stock_positions(WarehouseID, SpecID, PositionsID, Priority) VALUES(%d, %d, %d, (unix_timestamp(NOW())))";
	char sql[strlen(insert) + 20];
	sprintf(sql, insert, warehouseId, specId, positionId);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("prepareNewPosition status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jbyteArray prePdSubmit(JNIEnv *env, jobject thiz, jint userId, jint pdId,
		jint accountId, jint bLast, jstring jInfoStr)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *submit = "CALL SP_PD_ENTRY(@errcode, @errmsg, %d, %d, '%s', %d, %d)";
	char sql[strlen(submit) + 4000];
	const char *infoStr = env->GetStringUTFChars(jInfoStr, NULL);
	sprintf(sql, submit, userId, pdId, infoStr, bLast, accountId);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("prePdSubmit status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jbyteArray preFastInExamineGoodsSubmit(JNIEnv *env, jobject thiz, jint userId,
	jint warehouseId, jint supplierId, jint logisticId, int accountId,
	jstring jOtherFee, jstring jGoodsTotal, jstring jAllTotal, jstring jPostageFee,
	jstring jCashFee, jstring jPostId, jint bLast, jstring jInfoStr)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *submit = "CALL SP_PURCHASE_BALANCE(@errcode, @errmsg, @stock_in_id, \
		%d, %d, %d, 0, %d, %d, %s, %s, %s, %s, %s, '%s', '', '%s', %d)";

	char sql[strlen(submit) + 4000];
	const char *otherFee = env->GetStringUTFChars(jOtherFee, NULL);
	const char *goodsTotal = env->GetStringUTFChars(jGoodsTotal, NULL);
	const char *allTotal = env->GetStringUTFChars(jAllTotal, NULL);
	const char *postageFee = env->GetStringUTFChars(jPostageFee, NULL);
	const char *cashFee = env->GetStringUTFChars(jCashFee, NULL);
	const char *postId = env->GetStringUTFChars(jPostId, NULL);
	const char *infoStr = env->GetStringUTFChars(jInfoStr, NULL);

	sprintf(sql, submit, userId, warehouseId, supplierId, logisticId,
		accountId, otherFee, goodsTotal, allTotal, postageFee, cashFee,
		postId, infoStr, bLast);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preFastInExamineGoodsSubmit status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jbyteArray preStockOut(JNIEnv *env, jobject thiz, jint tradeId, jint pickerId,
	jint scannerId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "CALL SP_STOCKOUT_TRADE(@Code, @ErrorMsg, %d, %d, %d)";
	char sql[strlen(temp) + 20];
	sprintf(sql, temp, tradeId, pickerId, scannerId);
	printf("%s", sql);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preStockOut status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jbyteArray preUpdateStradeBscan(JNIEnv *env, jobject thiz, jint tradeId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "update g_trade_goodslist set bScan=1 where tradeid=%d";
	char sql[strlen(temp) + 20];
	sprintf(sql, temp, tradeId);
	printf("%s", sql);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("---------------------preUpdateStradeBscan status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jbyteArray prePickError(JNIEnv *env, jobject thiz, jint tradeId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "UPDATE g_trade_tradelist SET PickErrorCount=PickErrorCount+1 WHERE TradeID=%d";
	char sql[strlen(temp) + 20];
	sprintf(sql, temp, tradeId);
	printf("%s", sql);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preStockOut status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}
