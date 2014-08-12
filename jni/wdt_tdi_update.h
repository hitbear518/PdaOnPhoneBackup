#include <jni.h>

#ifndef WDT_TDI_UPDATE_H
#define WDT_TDI_UPDATE_H

#ifdef __cplusplus
extern "C" {
#endif

extern jbyteArray preFastPd(JNIEnv *env, jobject thiz, jint userId, jint warehouseId, jstring inforJstr, jint accountId);

extern jbyteArray preSetStock(JNIEnv *env, jobject thiz, jint warehouseId, jint specId, jint positionId, jstring jstock);

extern jbyteArray preNewPosition(JNIEnv *env, jobject thiz, jint warehouseId, jint specId, jint positionId);

extern jbyteArray preparePdDetails(JNIEnv *env, jobject thiz, jint pdId);

extern jbyteArray prePdSubmit(JNIEnv *env, jobject thiz, jint userId, jint pdId,
		jint accountId, jint bLast, jstring infoStr);

extern jbyteArray preFastInExamineGoodsSubmit(JNIEnv *env, jobject thiz, jint userId,
		jint warehouseId, jint supplierId, jint logisticId, int accountId,
		jstring otherFee, jstring goodsTotal, jstring allTotal, jstring postageFee,
		jstring cashFee, jstring postId, jint bLast, jstring infoStr);

extern jbyteArray preStockOut(JNIEnv *env, jobject thiz, jint tradeId, jint pickerId,
	jint scannerId);

extern jbyteArray preUpdateStradeBscan(JNIEnv *env, jobject thiz, jint tradeId);

extern jbyteArray prePickError(JNIEnv *env, jobject thiz, jint tradeId);

extern void completeUpdate(JNIEnv *env, jobject thiz, jbyteArray jbArray);

#ifdef __cplusplus
}
#endif

#endif
