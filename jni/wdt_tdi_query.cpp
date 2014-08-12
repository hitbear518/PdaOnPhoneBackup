#include <stdio.h>
#include <stdlib.h>
#include "tdi_prot.h"
#include "wdt_tdi.h"
#include "wdt_tdi_query.h"

JNIEXPORT jbyteArray JNICALL preGetWarehouses(JNIEnv *env, jobject)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT WarehouseID, WarehouseNO, WarehouseName";
	const char *from = "FROM g_cfg_warehouselist";
	const char *where = "WHERE bBlockUp=0 AND WarehouseType<1 ORDER BY WarehouseID ASC";

	char sql[strlen(select) + strlen(from) + strlen(where) + 3];
	sprintf(sql, "%s %s %s", select, from, where);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	printf("-------------------preGetWarehouses status=%d\n", status);
	CHECK_STATUS(status);

	return transTdiTojbArr(env);
}

JNIEXPORT jobjectArray JNICALL getWarehouses(JNIEnv *env, jobject, jbyteArray jbArray)
{
	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("*************************getWarehouses status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	// Build a warehouse array
	jobjectArray warehouseArr = env->NewObjectArray(row, g_warehouseCache.cls, NULL);
	CHECK_NULL(warehouseArr);

	// Fill the warehouse array
	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint warehouseId;
		jstring warehouseNO;
		jstring warehouseName;
		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			warehouseId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			warehouseId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, warehouseId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		warehouseNO = env->NewStringUTF(str);
		CHECK_NULL(warehouseName);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		warehouseName = env->NewStringUTF(str);
		CHECK_NULL(warehouseName);

		jobject warehouse = env->NewObject(g_warehouseCache.cls, g_warehouseCache.initId,
			warehouseId, warehouseNO, warehouseName);
		CHECK_NULL(warehouse);
		env->SetObjectArrayElement(warehouseArr, row, warehouse);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return warehouseArr;
}

jbyteArray preGetPdEntries(JNIEnv *env, jobject thiz, jint warehouseId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT pd.PdID, pd.PdNO, u1.UserName AS CreateUser, pd.CreateTime";
	const char *from = "FROM g_stock_pd pd";
	const char *left_join = "LEFT JOIN g_sys_userlist u1 ON pd.OperatorID=u1.UID";
	const char *where = "WHERE pd.Status=0 AND pd.WarehouseID = (%d) ORDER BY pd.CreateTime DESC";

	char temp[strlen(select) + strlen(from) + strlen(left_join) + strlen(where) + 4];
	sprintf(temp, "%s %s %s %s", select, from, left_join, where);
	char sql[sizeof(temp) + 10];
	sprintf(sql, temp, warehouseId);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("query1 status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getPdEntries(JNIEnv *env, jobject thiz, jbyteArray jbArray)
{
	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("query2 status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	// Build a stockTakingEntry array
	jobjectArray stockTakingEntryArr = env->NewObjectArray(row, g_PdEntryCache.cls, NULL);
	CHECK_NULL(stockTakingEntryArr);
	// Fill the warehouse array
	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint entryId;
		jstring entryNumber;
		jstring creater;
		jstring createTime;

		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			entryId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			entryId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, entryId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		entryNumber = env->NewStringUTF(str);
		CHECK_NULL(entryNumber);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		creater = env->NewStringUTF(str);
		CHECK_NULL(creater);

		colName = tdi_prot_st_field_name(g_tdi, 3);
		if (!tdi_prot_st_is_null(g_tdi, 3))
			str = tdi_prot_st_str_field(g_tdi, 3);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		createTime = env->NewStringUTF(str);
		CHECK_NULL(createTime);

		jobject stockTakingEntry = env->NewObject(g_PdEntryCache.cls, g_PdEntryCache.initId, entryId, entryNumber, creater, createTime);
		CHECK_NULL(stockTakingEntry);
		env->SetObjectArrayElement(stockTakingEntryArr, row, stockTakingEntry);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return stockTakingEntryArr;
}

jbyteArray preGetSpecs(JNIEnv *env, jobject thiz, jint warehouseId, jstring barcode)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT DISTINCT gs.SpecID, g.GoodsNO, g.GoodsName, gs.SpecCode, gs.SpecName, gs.SpecBarcode,pl.PositionsID,pl.PositionsName,sp.Stock";
	const char *from = "FROM g_goods_goodsspec gs";
	const char *left_join1 = "LEFT JOIN g_goods_barcode gbar ON gs.SpecID=gbar.SpecID";
	const char *left_join2 = "LEFT JOIN g_goods_goodslist g ON gs.GoodsID=g.GoodsID";
	const char *left_join3 = "LEFT JOIN g_stock_positions sp ON sp.SpecID=gs.SpecID";
	const char *left_join4 = "LEFT JOIN g_cfg_positionslist pl ON pl.PositionsID=sp.PositionsID";
	const char *where = "WHERE g.bFit=0 AND g.bBlockUp=0 AND gs.bBlockUp=0 AND gbar.Barcode='%s' and sp.WarehouseID=%d AND sp.PositionsID<>0";
	const char *orderBy = "ORDER BY sp.Stock DESC";

	char temp[strlen(select) + strlen(from) + strlen(left_join1) + strlen(left_join2) + strlen(left_join3) + strlen(left_join4) + strlen(where) + strlen(orderBy) + 1];
	sprintf(temp, "%s %s %s %s %s %s %s %s", select, from, left_join1, left_join2, left_join3, left_join4, where, orderBy);
	char sql[sizeof(temp) + 50];
	const char *cBarcode = env->GetStringUTFChars(barcode, NULL);
	sprintf(sql, temp, cBarcode, warehouseId);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetPdSpecs status=%d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getSpecs(JNIEnv *env, jobject thiz, jbyteArray jbArray)
{
	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("query2 status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray SpecArr = env->NewObjectArray(row, g_specCache.cls, NULL);
	CHECK_NULL(SpecArr);

	row = 0;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint specId;
		jstring goodsNumber;
		jstring goodsName;
		jstring specCode;
		jstring specName;
		jstring specBarcode;
		jint positonId;
		jstring positionName;
		jstring stock;

		const char *colName;
		const char *str;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			specId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			printError("specId = null");
			specId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, specId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		goodsNumber = env->NewStringUTF(str);
		CHECK_NULL(goodsNumber);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		goodsName = env->NewStringUTF(str);
		CHECK_NULL(goodsName);

		colName = tdi_prot_st_field_name(g_tdi, 3);
		if (!tdi_prot_st_is_null(g_tdi, 3))
			str = tdi_prot_st_str_field(g_tdi, 3);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		specCode = env->NewStringUTF(str);
		CHECK_NULL(specCode);

		colName = tdi_prot_st_field_name(g_tdi, 4);
		if (!tdi_prot_st_is_null(g_tdi, 4))
			str = tdi_prot_st_str_field(g_tdi, 4);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specName = env->NewStringUTF(str);
		CHECK_NULL(specName);

		colName = tdi_prot_st_field_name(g_tdi, 5);
		if (!tdi_prot_st_is_null(g_tdi, 5))
			str = tdi_prot_st_str_field(g_tdi, 5);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specBarcode = env->NewStringUTF(str);
		CHECK_NULL(specBarcode);

		colName = tdi_prot_st_field_name(g_tdi, 6);
		if (!tdi_prot_st_is_null(g_tdi, 6))
			positonId = tdi_prot_st_int_field(g_tdi, 6);
		else
		{
			printError("positonId = null");
			positonId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, positonId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 7);
		if (!tdi_prot_st_is_null(g_tdi, 7))
			str = tdi_prot_st_str_field(g_tdi, 7);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		positionName = env->NewStringUTF(str);
		CHECK_NULL(positionName);

		colName = tdi_prot_st_field_name(g_tdi, 8);
		if (!tdi_prot_st_is_null(g_tdi, 8))
			str = tdi_prot_st_str_field(g_tdi, 8);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		stock = env->NewStringUTF(str);
		CHECK_NULL(stock);

		jobject spec = env->NewObject(g_specCache.cls, g_specCache.initId,
				specId, goodsNumber, goodsName, specCode, specName, specBarcode, positonId, positionName, stock);
		CHECK_NULL(spec);
		env->SetObjectArrayElement(SpecArr, row, spec);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}
	tdi_prot_st_close(g_tdi);

	return SpecArr;
}

jbyteArray preGetInExamSpecs(JNIEnv *env, jobject thiz, jstring barcode)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT DISTINCT gs.SpecID, g.GoodsNO, g.GoodsName, gs.SpecCode, gs.SpecName, gs.SpecBarcode";
	const char *from = "FROM g_goods_goodsspec gs";
	const char *left_join1 = "LEFT JOIN g_goods_barcode gbar ON gs.SpecID=gbar.SpecID";
	const char *left_join2 = "LEFT JOIN g_goods_goodslist g ON gs.GoodsID=g.GoodsID";
	const char *where = "WHERE g.bFit=0 AND g.bBlockUp=0 AND gs.bBlockUp=0 AND gbar.Barcode='%s' ";

	char temp[strlen(select) + strlen(from) + strlen(left_join1) + strlen(left_join2) + strlen(where) + 1];
	sprintf(temp, "%s %s %s %s %s", select, from, left_join1, left_join2, where);
	char sql[sizeof(temp) + 50];
	const char *cBarcode = env->GetStringUTFChars(barcode, NULL);
	sprintf(sql, temp, cBarcode);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("---------------------preGetInExamSpecs status=%d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getInExamSpecs(JNIEnv *env, jobject thiz, jbyteArray jbArray)
{
	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("*******************getInExamSpecs status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray SpecArr = env->NewObjectArray(row, g_specCache.cls, NULL);
	CHECK_NULL(SpecArr);

	row = 0;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint specId;
		jstring goodsNumber;
		jstring goodsName;
		jstring specCode;
		jstring specName;
		jstring specBarcode;

		const char *colName;
		const char *str;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			specId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			printError("specId = null");
			specId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, specId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		goodsNumber = env->NewStringUTF(str);
		CHECK_NULL(goodsNumber);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		goodsName = env->NewStringUTF(str);
		CHECK_NULL(goodsName);

		colName = tdi_prot_st_field_name(g_tdi, 3);
		if (!tdi_prot_st_is_null(g_tdi, 3))
			str = tdi_prot_st_str_field(g_tdi, 3);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		specCode = env->NewStringUTF(str);
		CHECK_NULL(specCode);

		colName = tdi_prot_st_field_name(g_tdi, 4);
		if (!tdi_prot_st_is_null(g_tdi, 4))
			str = tdi_prot_st_str_field(g_tdi, 4);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specName = env->NewStringUTF(str);
		CHECK_NULL(specName);

		colName = tdi_prot_st_field_name(g_tdi, 5);
		if (!tdi_prot_st_is_null(g_tdi, 5))
			str = tdi_prot_st_str_field(g_tdi, 5);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specBarcode = env->NewStringUTF(str);
		CHECK_NULL(specBarcode);

		jobject spec = env->NewObject(g_specCache.cls, g_specCache.initId,
				specId, goodsNumber, goodsName, specCode, specName, specBarcode, -1, NULL, NULL);
		CHECK_NULL(spec);
		env->SetObjectArrayElement(SpecArr, row, spec);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}
	tdi_prot_st_close(g_tdi);

	return SpecArr;
}

jbyteArray preGetPositionCount(JNIEnv *env, jobject thiz, jint warehouseId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT COUNT(*)";
	const char *from = "FROM g_cfg_positionslist pl";
	const char *left_join = "LEFT JOIN g_cfg_shelflist sl using(ShelfID)";
	const char *where = "WHERE sl.WarehouseID=%d";

	char temp[strlen(select) + strlen(from) + strlen(left_join) + strlen(where) + 4];
	sprintf(temp, "%s %s %s %s", select, from, left_join, where);
	char sql[sizeof(temp) + 10];
	sprintf(sql, temp, warehouseId);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preGetPositionCount status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jint getPositionCount(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPositionCount status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return 0;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return -1;
	}

	row = 0;
	int count;
	while (0 == tdi_prot_next(g_tdi))
	{
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			count = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			count = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, count);
#endif

		row++;
	}

	tdi_prot_st_close(g_tdi);

	return count;
}

jbyteArray preGetPositions(JNIEnv *env, jobject thiz, jint warehouseId, jint pos, jint pageSize)
{

	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT pl.PositionsID,pl.PositionsName";
	const char *from = "FROM g_cfg_positionslist pl";
	const char *left_join = "LEFT JOIN g_cfg_shelflist sl using(ShelfID)";
	const char *where = "WHERE sl.WarehouseID=%d ORDER BY pl.PositionsID LIMIT %d,%d";

	char temp[strlen(select) + strlen(from) + strlen(left_join) + strlen(where) + 4];
	sprintf(temp, "%s %s %s %s", select, from, left_join, where);
	char sql[sizeof(temp) + 10];
	sprintf(sql, temp, warehouseId, pos, pageSize);
	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preGetPositions status=%d\n", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getPositions(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPositions status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	// Build a warehouse array
	jobjectArray positions = env->NewObjectArray(row, g_positionCache.cls, NULL);
	CHECK_NULL(positions);

	// Fill the warehouse array
	row = 0;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint positionId;
		jstring positionName;
		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			positionId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			positionId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, positionId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		positionName = env->NewStringUTF(str);
		CHECK_NULL(positionName);

		jobject position = env->NewObject(g_positionCache.cls, g_positionCache.initId, positionId, positionName);
		CHECK_NULL(position);
		env->SetObjectArrayElement(positions, row, position);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return positions;
}

jbyteArray preGetPdDetailCount(JNIEnv *env, jobject thiz, jint pdId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT COUNT(*) FROM g_stock_pddetail pdd WHERE PdID = %d";
	char sql[strlen(temp) + 10];
	sprintf(sql, temp, pdId);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetPdDetailCount status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jint getPdDetailCount(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPdDetailCount status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return 0;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return -1;
	}

	row = 0;
	int count;
	while (0 == tdi_prot_next(g_tdi))
	{
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			count = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			count = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, count);
#endif

		row++;
	}

	tdi_prot_st_close(g_tdi);

	return count;
}

jbyteArray preGetPdDetails(JNIEnv *env, jobject thiz, jint pdId, jint pos, jint pageSize)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT pdd.RecId, s.specId, s.SpecBarcode, g.GoodsNO,g.GoodsName, \
			s.Barcode, s.SpecCode, s.SpecName, pl.PositionsId, pl.PositionsName, \
			pdd.StockOld, pdd.StockPd, pdd.Remark \
            FROM g_stock_pddetail pdd\
            LEFT JOIN g_goods_goodsspec s ON pdd.SpecID = s.SpecID \
            LEFT JOIN g_goods_goodslist g ON s.GoodsID = g.GoodsID \
            LEFT JOIN g_cfg_positionslist pl ON pl.PositionsID = pdd.PositionsID \
            WHERE PdID = %d  ORDER BY pdd.RecId LIMIT %d, %d";
	char sql[strlen(temp) + 10];
	sprintf(sql, temp, pdId, pos, pageSize);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetPdDetails status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getPdDetails(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPdDetails status = %d", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray pdSpecs = env->NewObjectArray(row, g_pdSpecCache.cls, NULL);
	CHECK_NULL(pdSpecs);

	row = 0;
	while(0 == tdi_prot_next(g_tdi))
	{
		jint recId;
		jint specId;
		jstring specBarcode;
		jstring goodsNum;
		jstring goodsName;
		jstring barcode;
		jstring specCode;
		jstring specName;
		jint positionId;
		jstring positionName;
		jstring stockOld;
		jstring stockPd;
		jstring remark;

		const char *colName;
		const char *str;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			recId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			printError("specId = null");
			recId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, recId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			specId = tdi_prot_st_int_field(g_tdi, 1);
		else
		{
			printError("specId = null");
			specId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, specId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specBarcode = env->NewStringUTF(str);
		CHECK_NULL(specBarcode);

		colName = tdi_prot_st_field_name(g_tdi, 3);
		if (!tdi_prot_st_is_null(g_tdi, 3))
			str = tdi_prot_st_str_field(g_tdi, 3);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		goodsNum = env->NewStringUTF(str);
		CHECK_NULL(goodsNum);

		colName = tdi_prot_st_field_name(g_tdi, 4);
		if (!tdi_prot_st_is_null(g_tdi, 4))
			str = tdi_prot_st_str_field(g_tdi, 4);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		goodsName = env->NewStringUTF(str);
		CHECK_NULL(goodsName);

		colName = tdi_prot_st_field_name(g_tdi, 5);
		if (!tdi_prot_st_is_null(g_tdi, 5))
			str = tdi_prot_st_str_field(g_tdi, 5);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		barcode = env->NewStringUTF(str);
		CHECK_NULL(goodsName);

		colName = tdi_prot_st_field_name(g_tdi, 6);
		if (!tdi_prot_st_is_null(g_tdi, 6))
			str = tdi_prot_st_str_field(g_tdi, 6);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		specCode = env->NewStringUTF(str);
		CHECK_NULL(specCode);

		colName = tdi_prot_st_field_name(g_tdi, 7);
		if (!tdi_prot_st_is_null(g_tdi, 7))
			str = tdi_prot_st_str_field(g_tdi, 7);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specName = env->NewStringUTF(str);
		CHECK_NULL(specName);

		colName = tdi_prot_st_field_name(g_tdi, 8);
		if (!tdi_prot_st_is_null(g_tdi, 8))
			positionId = tdi_prot_st_int_field(g_tdi, 8);
		else
		{
			positionId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, positionId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 9);
		if (!tdi_prot_st_is_null(g_tdi, 9))
			str = tdi_prot_st_str_field(g_tdi, 9);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		positionName = env->NewStringUTF(str);
		CHECK_NULL(positionName);

		colName = tdi_prot_st_field_name(g_tdi, 10);
		if (!tdi_prot_st_is_null(g_tdi, 10))
			str = tdi_prot_st_str_field(g_tdi, 10);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		stockOld = env->NewStringUTF(str);
		CHECK_NULL(stockOld);

		colName = tdi_prot_st_field_name(g_tdi, 11);
		if (!tdi_prot_st_is_null(g_tdi, 11))
			str = tdi_prot_st_str_field(g_tdi, 11);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		stockPd = env->NewStringUTF(str);
		CHECK_NULL(stockPd);

		colName = tdi_prot_st_field_name(g_tdi, 12);
		if (!tdi_prot_st_is_null(g_tdi, 12))
			str = tdi_prot_st_str_field(g_tdi, 12);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		remark = env->NewStringUTF(str);
		CHECK_NULL(remark);

		jobject pdSpec = env->NewObject(g_pdSpecCache.cls, g_pdSpecCache.initId,
				recId, specId, specBarcode, goodsNum, goodsName, barcode, specCode, specName,
				positionId, positionName, stockOld, stockPd, remark);
		CHECK_NULL(pdSpec);

		env->SetObjectArrayElement(pdSpecs, row, pdSpec);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}

	tdi_prot_st_close(g_tdi);

	return pdSpecs;
}

jbyteArray preGetPdSpecs(JNIEnv *env, jobject thiz, jint pdId, jstring jbarcode)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT s.SpecID, s.SpecBarcode, g.GoodsNO, g.GoodsName, \
			s.SpecCode, s.SpecName, pl.PositionsId, pl.PositionsName, pdd.StockOld, \
			pdd.StockPd, pdd.Remark \
            FROM g_stock_pddetail pdd \
            LEFT JOIN g_goods_goodsspec s ON pdd.SpecID = s.SpecID \
            LEFT JOIN g_goods_goodslist g ON s.GoodsID = g.GoodsID \
            LEFT JOIN g_cfg_positionslist pl ON pl.PositionsID = pdd.PositionsID \
            LEFT JOIN g_goods_barcode  gbar on s.SpecID=gbar.SpecID \
            WHERE PdID = %d AND gbar.Barcode = '%s'";
	char sql[strlen(temp) + 20];
	const char *barcode = env->GetStringUTFChars(jbarcode, NULL);
	sprintf(sql, temp, pdId, barcode);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetPdSpecs status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getPdSpecs(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPdSpecs status = %d", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray pdSpecs = env->NewObjectArray(row, g_pdSpecCache.cls, NULL);
	CHECK_NULL(pdSpecs);

	row = 0;
	while(0 == tdi_prot_next(g_tdi))
	{
		jint specId;
		jstring specBarcode;
		jstring goodsNum;
		jstring goodsName;
		jstring specCode;
		jstring specName;
		jint positionId;
		jstring positionName;
		jstring stockOld;
		jstring stockPd;
		jstring remark;

		const char *colName;
		const char *str;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			specId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			printError("specId = null");
			specId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, specId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specBarcode = env->NewStringUTF(str);
		CHECK_NULL(specBarcode);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		goodsNum = env->NewStringUTF(str);
		CHECK_NULL(goodsNum);

		colName = tdi_prot_st_field_name(g_tdi, 3);
		if (!tdi_prot_st_is_null(g_tdi, 3))
			str = tdi_prot_st_str_field(g_tdi, 3);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		goodsName = env->NewStringUTF(str);
		CHECK_NULL(goodsName);

		colName = tdi_prot_st_field_name(g_tdi, 4);
		if (!tdi_prot_st_is_null(g_tdi, 4))
			str = tdi_prot_st_str_field(g_tdi, 4);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s", row, colName, str);
#endif
		specCode = env->NewStringUTF(str);
		CHECK_NULL(specCode);

		colName = tdi_prot_st_field_name(g_tdi, 5);
		if (!tdi_prot_st_is_null(g_tdi, 5))
			str = tdi_prot_st_str_field(g_tdi, 5);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		specName = env->NewStringUTF(str);
		CHECK_NULL(specName);

		colName = tdi_prot_st_field_name(g_tdi, 6);
		if (!tdi_prot_st_is_null(g_tdi, 6))
			positionId = tdi_prot_st_int_field(g_tdi, 6);
		else
		{
			positionId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, positionId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 7);
		if (!tdi_prot_st_is_null(g_tdi, 7))
			str = tdi_prot_st_str_field(g_tdi, 7);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		positionName = env->NewStringUTF(str);
		CHECK_NULL(positionName);

		colName = tdi_prot_st_field_name(g_tdi, 8);
		if (!tdi_prot_st_is_null(g_tdi, 8))
			str = tdi_prot_st_str_field(g_tdi, 8);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		stockOld = env->NewStringUTF(str);
		CHECK_NULL(stockOld);

		colName = tdi_prot_st_field_name(g_tdi, 9);
		if (!tdi_prot_st_is_null(g_tdi, 9))
			str = tdi_prot_st_str_field(g_tdi, 9);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		stockPd = env->NewStringUTF(str);
		CHECK_NULL(stockPd);

		colName = tdi_prot_st_field_name(g_tdi, 10);
		if (!tdi_prot_st_is_null(g_tdi, 10))
			str = tdi_prot_st_str_field(g_tdi, 10);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		remark = env->NewStringUTF(str);
		CHECK_NULL(remark);

		jobject pdSpec = env->NewObject(g_pdSpecCache.cls, g_pdSpecCache.initId,
				specId, specBarcode, goodsNum, goodsName, specCode, specName,
				positionId, positionName, stockOld, stockPd, remark);
		CHECK_NULL(pdSpec);

		env->SetObjectArrayElement(pdSpecs, row, pdSpec);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}

	tdi_prot_st_close(g_tdi);

	return pdSpecs;
}

//jbyteArray preGetSuppliers(JNIEnv *env, jobject thiz)
//{
//	int status;
//
//	// 查询
//	tdi_prot_st_prepare(g_tdi);
//
//	const char *sql = "SELECT ProviderID,ProviderName FROM g_cfg_providerlist \
//WHERE bBlockUp=0 ORDER BY ProviderID ASC";
//
//	status = tdi_prot_st_execute(g_tdi, sql);
//	if (status)
//	{
//		throwEx(env, status, tdi_prot_data(g_tdi));
//		return NULL;
//	}
//	printf("%s", sql);
//	printf("preGetSuppliers status=%d\n", status);
//	CHECK_STATUS(status);
//
//	return transTdiTojbArr(env);
//}

jbyteArray preGetSuppliers(JNIEnv *env, jobject thiz, jint pos, jint pageSize)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT ProviderID,ProviderName FROM g_cfg_providerlist \
WHERE bBlockUp=0 ORDER BY ProviderID ASC LIMIT %d, %d";
	char sql[strlen(temp) + 20];
	sprintf(sql, temp, pos, pageSize);

	status = tdi_prot_st_execute(g_tdi, sql);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	printf("%s", sql);
	printf("preGetSuppliers status=%d\n", status);
	CHECK_STATUS(status);

	return transTdiTojbArr(env);
}

jobjectArray getSuppliers(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getSuppliers status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray supplierArr = env->NewObjectArray(row, g_supplierCache.cls, NULL);
	CHECK_NULL(supplierArr);

	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint supplierId;
		jstring supplierName;
		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			supplierId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			supplierId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, supplierId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		supplierName = env->NewStringUTF(str);
		CHECK_NULL(supplierName);

		jobject supplier = env->NewObject(
			g_supplierCache.cls, g_supplierCache.initId,
			supplierId, supplierName);
		CHECK_NULL(supplier);
		env->SetObjectArrayElement(supplierArr, row, supplier);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return supplierArr;
}

jbyteArray preGetPrice(JNIEnv *env, jobject thiz, jint specId, jint supplierId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT IFNULL(Price, 0.0000) AS SupplyPrice, \
		IFNULL(SpecPriceDetail, 0.0000) AS RetailPrice, \
		IFNULL(SpecPriceWholesale, 0.000) AS WholesalePrice, \
		IFNULL(SpecPriceMember, 0.0000) AS MemberPrice, \
		IFNULL(SpecPricePurchase, 0.0000) AS PurchasePrice \
		FROM g_goods_goodsspec gs LEFT JOIN g_goods_provider gp \
		ON gp.SpecID = gs.SpecID  AND gp.`ProviderID` = %d \
		WHERE gs.SpecID = %d";

	char sql[strlen(temp) + 10];
	sprintf(sql, temp, supplierId, specId);

	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("--------------------------preGetPrice status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobject getPrice(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;
	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPrice status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	if (0 != tdi_prot_next(g_tdi))
		return NULL;

	jstring supplyPrice;
	jstring retailPrice;
	jstring whosalePrice;
	jstring memberPrice;
	jstring purchasePrice;

	const char *str;
	const char *colName;

	colName = tdi_prot_st_field_name(g_tdi, 0);
	if (!tdi_prot_st_is_null(g_tdi, 0))
		str = tdi_prot_st_str_field(g_tdi, 0);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
	printf("str");
#ifdef DEBUG
	printf("row = %d -- %s = %s",row, colName, str);
#endif
	supplyPrice = env->NewStringUTF(str);
	CHECK_NULL(supplyPrice);

	colName = tdi_prot_st_field_name(g_tdi, 1);
	if (!tdi_prot_st_is_null(g_tdi, 1))
		str = tdi_prot_st_str_field(g_tdi, 1);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
#ifdef DEBUG
	printf("row = %d -- %s = %s",row, colName, str);
#endif
	retailPrice = env->NewStringUTF(str);
	CHECK_NULL(retailPrice);

	colName = tdi_prot_st_field_name(g_tdi, 2);
	if (!tdi_prot_st_is_null(g_tdi, 2))
		str = tdi_prot_st_str_field(g_tdi, 2);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
#ifdef DEBUG
	printf("row = %d -- %s = %s",row, colName, str);
#endif
	whosalePrice = env->NewStringUTF(str);
	CHECK_NULL(whosalePrice);

	colName = tdi_prot_st_field_name(g_tdi, 3);
	if (!tdi_prot_st_is_null(g_tdi, 3))
		str = tdi_prot_st_str_field(g_tdi, 3);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
#ifdef DEBUG
	printf("row = %d -- %s = %s",row, colName, str);
#endif
	memberPrice = env->NewStringUTF(str);
	CHECK_NULL(memberPrice);

	colName = tdi_prot_st_field_name(g_tdi, 4);
	if (!tdi_prot_st_is_null(g_tdi, 4))
		str = tdi_prot_st_str_field(g_tdi, 4);
	else
	{
		str = "null value";
		printError("%s is null in row%d", colName, row);
	}
#ifdef DEBUG
	printf("row = %d -- %s = %s",row, colName, str);
#endif
	purchasePrice = env->NewStringUTF(str);
	CHECK_NULL(purchasePrice);

	jobject price = env->NewObject(g_priceCache.cls, g_priceCache.initId,
		supplyPrice, retailPrice, whosalePrice,
		memberPrice, purchasePrice);
	CHECK_NULL(price);

	tdi_prot_st_close(g_tdi);
	return price;
}

jbyteArray preGetLogistics(JNIEnv *env, jobject thiz)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *sql = "SELECT LogisticID, LogisticName \
			FROM g_cfg_logisticlist Where bBlockUp=0  \
			ORDER BY LogisticID ASC";
	status = tdi_prot_st_execute(g_tdi, sql);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	printf("preGetLogistices status=%d\n", status);
	CHECK_STATUS(status);

	return transTdiTojbArr(env);
}

jobjectArray getLogistics(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getSuppliers status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray logisticsArr = env->NewObjectArray(row,
		g_logisticsCache.cls, NULL);

	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint logisticId;
		jstring logisticName;
		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			logisticId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			logisticId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, logisticId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		logisticName = env->NewStringUTF(str);
		CHECK_NULL(logisticName);

		jobject logstics = env->NewObject(
			g_logisticsCache.cls, g_logisticsCache.initId,
			logisticId, logisticName);
		CHECK_NULL(logstics);
		env->SetObjectArrayElement(logisticsArr, row, logstics);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}
	tdi_prot_st_close(g_tdi);

	return logisticsArr;
}

jbyteArray preGetTradeInfo(JNIEnv *env, jobject thiz, jstring jTradeOrPost)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT TradeID, PickerID, TradeStatus, bStockOut, bFreezed, RefundStatus, PostID, WarehouseID FROM g_trade_tradelist WHERE TradeNO='%s' OR PostID='%s'";
	char sql[strlen(temp) + 40];
	const char *tradeOrPost = env->GetStringUTFChars(jTradeOrPost, NULL);
	sprintf(sql, temp, tradeOrPost, tradeOrPost);
	status = tdi_prot_st_execute(g_tdi, sql);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	printf("%s", sql);
	printf("preGetTradeInfo status=%d\n", status);
	CHECK_STATUS(status);

	return transTdiTojbArr(env);
}

jobject getTradeInfo(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getTradeInfo status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}


	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	if (row > 1) {
		throwEx(env, 9999, "物流单号对应系统内两个或以上订单,请扫描系统内订单编号");
		return NULL;
	}
	row = 0;

	if (0 != tdi_prot_next(g_tdi))
		return NULL;

	jint tradeId;
	jint pickerId;
	jint tradeStatus;
	jint bStockOut;
	jint bFreezed;
	jint refundStatus;
	jstring postId;
	jint warehouseId;
	const char *colName;
	const char *str;

	// 0, int, tradeId
	tradeId = getIntData(row, 0);

	// 1, int, pickerId
	pickerId = getIntData(row, 1);

	// 2, int, tradeStatus
	tradeStatus = getIntData(row, 2);

	// 3, int, bStockOut
	bStockOut = getIntData(row, 3);

	// 4, int, bFreezed
	bFreezed = getIntData(row, 4);

	// 5, int, refundStatus
	refundStatus = getIntData(row, 5);

	// 6, string, postId
	postId = getStrData(env, row, 6);

	// 7, int, warehouseId
	warehouseId = getIntData(row, 7);

	jobject tradeInfo = env->NewObject(g_tradeInfoCache.cls, g_tradeInfoCache.initId,
		tradeId, pickerId, tradeStatus, bStockOut, bFreezed, refundStatus, postId, warehouseId);
	CHECK_NULL(tradeInfo);

	tdi_prot_st_close(g_tdi);
	return tradeInfo;
}

jbyteArray preGetPickers(JNIEnv *env, jobject thiz)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *sql = "SELECT Uid, UserNO, Username FROM g_sys_userlist WHERE bPicker=1";
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("preGetPickers status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getPickers(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPickers status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray pickers = env->NewObjectArray(row, g_userCache.cls, NULL);

	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint userId;
		jstring userNo;
		jstring userName;
		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			userId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			userId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, userId);
#endif

		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		userNo = env->NewStringUTF(str);
		CHECK_NULL(userNo);

		colName = tdi_prot_st_field_name(g_tdi, 2);
		if (!tdi_prot_st_is_null(g_tdi, 2))
			str = tdi_prot_st_str_field(g_tdi, 2);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		userName = env->NewStringUTF(str);
		CHECK_NULL(userName);

		jobject picker = env->NewObject(g_userCache.cls, g_userCache.initId,
			userId, userNo, userName);
		CHECK_NULL(picker);
		env->SetObjectArrayElement(pickers, row, picker);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return pickers;
}

jbyteArray preGetTradeGoods(JNIEnv *env, jobject thiz, jint tradeId, jint warehouseId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT tgl.RecID, gs.Barcode, TradeGoodsNO, TradeGoodsName, TradeSpecCode, TradeSpecName, cpl.PositionsName, SellCount FROM g_trade_goodslist tgl LEFT JOIN g_goods_goodsspec gs ON tgl.SpecID = gs.SpecID LEFT JOIN g_stock_spec ss ON tgl.SpecID = ss.SpecID LEFT JOIN g_cfg_positionslist cpl ON ss.PositionsID = cpl.PositionsID WHERE TradeID = %d AND ss.WarehouseID = %d ORDER BY RecID";
	char sql[strlen(temp) + 30];
	sprintf(sql, temp, tradeId, warehouseId);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetPickers status = %d", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getTradeGoods(JNIEnv *env, jobject thize, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);
	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPickers status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray tradeGoodsArr = env->NewObjectArray(row, g_tradeGoodCache.cls, NULL);

	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint recId;
		jstring barcode;
		jstring goodsNo;
		jstring goodsName;
		jstring specCode;
		jstring specName;
		jstring positionName;
		jstring sellCount;

		const char *str;
		const char *colName;

		// 0, int, recId
		recId = getIntData(row, 0);

		// 1, string, barcode
		barcode = getStrData(env, row, 1);

		// 2, string goodsNo
		goodsNo = getStrData(env, row, 2);

		// 3, string, goodsName
		goodsName = getStrData(env, row, 3);

		// 4, string, specCode
		specCode = getStrData(env, row, 4);

		// 5, string, specName
		specName = getStrData(env, row, 5);

		// 6, string, positionName
		positionName = getStrData(env, row, 6);

		// 7, string, sellCount
		sellCount = getStrData(env, row, 7);

		jobject tradeGoods = env->NewObject(g_tradeGoodCache.cls, g_tradeGoodCache.initId,
			recId, barcode, goodsNo, goodsName, specCode, specName, positionName, sellCount);
		CHECK_NULL(tradeGoods);
		env->SetObjectArrayElement(tradeGoodsArr, row, tradeGoods);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}
	tdi_prot_st_close(g_tdi);

	return tradeGoodsArr;
}

jbyteArray preGetSupplierCount(JNIEnv *env, jobject thiz)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *sql = "SELECT COUNT(*) FROM g_cfg_providerlist WHERE bBlockUp=0";
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("preGetSupplierCount status = %d", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jint getSupplierCount(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getSupplierCount status=%d\n", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return 0;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return -1;
	}

	row = 0;
	int count;
	while (0 == tdi_prot_next(g_tdi))
	{
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			count = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			count = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, count);
#endif

		row++;
	}

	tdi_prot_st_close(g_tdi);

	return count;
}

jbyteArray preGetShops(JNIEnv *env, jobject thiz)
{
	int status;

	tdi_prot_st_prepare(g_tdi);

	const char *sql = "select ShopID, ShopName from g_cfg_shoplist where bBlockUp=0 ORDER BY ShopID";
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("------------------------- preGetShops status = %d", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getShops(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);
	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("getPickers status=%d\n", status);
	CHECK_STATUS(status);
	if (status && 1064 != status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get column count
	int fields = tdi_prot_st_fields(g_tdi);

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray shopArr = env->NewObjectArray(row, g_shopCache.cls, NULL);
	row = 0;
	int i;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint shopId;
		jstring shopName;

		const char *colName;
		const char *str;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			shopId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			printError("specId = null");
			shopId = -1;
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d", row, colName, shopId);
#endif
		colName = tdi_prot_st_field_name(g_tdi, 1);
		if (!tdi_prot_st_is_null(g_tdi, 1))
			str = tdi_prot_st_str_field(g_tdi, 1);
		else
		{
			str = "null value";
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %s",row, colName, str);
#endif
		shopName = env->NewStringUTF(str);
		CHECK_NULL(shopName);

		jobject shop = env->NewObject(g_shopCache.cls, g_shopCache.initId, shopId, shopName);
		CHECK_NULL(shop);
		env->SetObjectArrayElement(shopArr, row, shop);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);

	return shopArr;
}

jbyteArray preGetCashSaleSpecs(JNIEnv *env, jobject thiz, jint warehouseId, jstring jbarcode)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT ss.specId,gs.specBarcode,gg.goodsNo, gg.goodsName, gs.specCode, gs.specName,gs.SpecPriceDetail, gs.SpecPriceWholesale, gs.SpecPriceMember, gs.SpecPricePurchase, gs.SpecPrice1, gs.SpecPrice2, gs.SpecPrice3, ss.Stock-ss.OrderCount-ss.SndCount, ss.WarehouseID";
	const char *from = "FROM g_stock_spec ss";
	const char *left_join1 = "LEFT JOIN g_goods_goodsspec gs ON ss.SpecID=gs.SpecID";
	const char *left_join2 = "LEFT JOIN g_goods_goodslist gg ON gg.GoodsID=gs.GoodsID";
	const char *left_join3 = "LEFT JOIN g_goods_barcode gb ON gb.SpecID=gs.SpecID";
	const char *where = "WHERE ss.WarehouseID=%d AND gb.barcode='%s' AND gg.bBlockUp=0 AND gs.bBlockUp=0";

	char temp[strlen(select) + strlen(from) + strlen(left_join1) + strlen(left_join2) + strlen(left_join3) + strlen(where) + 1];
	sprintf(temp, "%s %s %s %s %s %s", select, from, left_join1, left_join2, left_join3, where);
	char sql[sizeof(temp) + 50];
	const char *barcode = env->GetStringUTFChars(jbarcode, NULL);
	sprintf(sql, temp, warehouseId, barcode);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("----------------------------------------preGetCashSaleSpecs status=%d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}



jobjectArray getCashSaleSpecs(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;
	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("******************************getCashSaleSpecs status=%d", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray cashSaleSpecArr = env->NewObjectArray(row, g_cashSaleSpecCache.cls, NULL);
	CHECK_NULL(cashSaleSpecArr);

	row = 0;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint specId;
		jstring specBarcode;
		jstring goodsNum;
		jstring goodsName;
		jstring specCode;
		jstring specName;
		jstring retailPrice;
		jstring wholesalePrice;
		jstring memberPrice;
		jstring purchasePrice;
		jstring price1;
		jstring price2;
		jstring price3;
		jstring stock;
		jint warehouseId;

		const char *colName;
		const char *str;

		//0 int specId
		specId = getIntData(row, 0);

		//1 str specBarcode
		specBarcode = getStrData(env, row, 1);

		//2 str goodsNum
		goodsNum = getStrData(env, row, 2);

		//3 str goodsName
		goodsName = getStrData(env, row, 3);

		//4 str specCode
		specCode = getStrData(env, row, 4);

		//5 str specName
		specName = getStrData(env, row, 5);

		//6 str retailPrice
		retailPrice = getStrData(env, row, 6);

		//7 str wholePrice
		wholesalePrice = getStrData(env, row, 7);

		//8 str memberPrice
		memberPrice = getStrData(env, row, 8);

		//9 str purchasePrice
		purchasePrice = getStrData(env, row, 9);

		//10 str price1
		price1 = getStrData(env, row, 10);

		// 11 str price2
		price2 = getStrData(env, row, 11);

		// 12 str price3
		price3 = getStrData(env, row, 12);

		// 13  str stock
		stock = getStrData(env, row, 13);

		// 14 int warehouseId
		warehouseId = getIntData(row, 14);

		//create CashSaleSpec object
		jobject cashSaleSpec = env->NewObject(g_cashSaleSpecCache.cls, g_cashSaleSpecCache.initId,
			specId, specBarcode, goodsNum, goodsName, specCode, specName, retailPrice, wholesalePrice,
			memberPrice, purchasePrice, price1, price2, price3, stock, warehouseId);
		CHECK_NULL(cashSaleSpec);

		//set array value
		env->SetObjectArrayElement(cashSaleSpecArr, row, cashSaleSpec);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}
	tdi_prot_st_close(g_tdi);

	return cashSaleSpecArr;
}

jbyteArray preGetInterfaceWarehouses(JNIEnv *env, jobject thiz, jstring jInterfaceId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT iw.WarehouseID, iw.WarehouseNO, w.WarehouseName";
	const char *from = "FROM g_cfg_interfacewarehouse iw";
	const char *left_join = "LEFT JOIN g_cfg_warehouselist w ON iw.WarehouseID=w.warehouseid";
	const char *where = "WHERE w.bBlockUp=0 AND w.WarehouseType<1 AND iw.InterfaceID='%s' and iw.WarehouseNO<>'' ORDER BY w.WarehouseID ASC";

	char temp[strlen(select) + strlen(from) + strlen(left_join) + strlen(where) + 3];
	sprintf(temp, "%s %s %s %s", select, from, left_join, where);

	const char *interfaceId = env->GetStringUTFChars(jInterfaceId, NULL);
	char sql[strlen(temp) + 10];
	sprintf(sql, temp, interfaceId);

	printf("%s", sql);
	status = tdi_prot_st_execute(g_tdi, sql);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	printf("-------------------preGetWarehouses status=%d\n", status);
	CHECK_STATUS(status);

	return transTdiTojbArr(env);
}

jbyteArray preGetCashSaleSpecStock(JNIEnv *env, jobject thiz, int specId, int warehouseId)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *temp = ("SELECT Stock-OrderCount-SndCount FROM g_stock_spec WHERE SpecID=%d AND WarehouseID=%d");
	char sql[strlen(temp) + 20];
	sprintf(sql, temp, specId, warehouseId);

	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("----------------------------------------preGetCashSaleSpecStock status=%d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jstring getCashSaleSpecStock(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	transjbArrToBuf(env, jbytes);

	int status;
	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
	printf("******************************getCashSaleSpecStock status=%d", status);
	CHECK_STATUS(status);
	if (status && status != 1064)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// Get row count
	int row = 0;
	row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	row = 0;

	jstring stockCanOrder;

	if (0 == tdi_prot_next(g_tdi))
	{
		//0 str stock can order
		stockCanOrder = getStrData(env, row, 0);
		row++;
	}
	tdi_prot_st_close(g_tdi);

	return stockCanOrder;
}

jbyteArray preGetCustomers(JNIEnv *env, jobject thiz, jstring jsearchTerm)
{
	tdi_prot_st_prepare(g_tdi);

	const char *temp = "SELECT CustomerID, NickName, CustomerName, Tel, Zip, Province, City, Town, Adr, Email FROM g_customer_customerlist WHERE NickName LIKE '%%%s%%' OR CustomerName LIKE '%%%s%%' OR Tel LIKE '%%%s%%' LIMIT 10";
	char sql[strlen(temp) + 20];
	const char *searchTerm = env->GetStringUTFChars(jsearchTerm, NULL);
	sprintf(sql, temp, searchTerm, searchTerm, searchTerm);
	int status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("---------------------------- preGetCustomers status = %d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobjectArray getCustomers(JNIEnv *env, jobject thiz, jbyteArray jbytes)
{
	int status = getSqlResult(env, jbytes);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	// Get row count
	int row = tdi_prot_st_rows(g_tdi);
#ifdef DEBUG
	printf("There are %d rows", row);
#endif
	if (0 == row)
	{
		tdi_prot_st_close(g_tdi);
		return NULL;
	}

	jobjectArray arr = env->NewObjectArray(row, g_customerCache.cls, NULL);
	row = 0;
	while (tdi_prot_next(g_tdi) == 0)
	{
		jint customerId;
		jstring nickName;
		jstring customerName;
		jstring tel;
		jstring zip;
		jstring province;
		jstring city;
		jstring district;
		jstring address;
		jstring email;

		// 0, int, customerId
		customerId = getIntData(row, 0);

		// 1, string, nick name
		nickName = getStrData(env, row, 1);

		// 2, string, customer name
		customerName = getStrData(env, row, 2);

		// 3, string, tel
		tel = getStrData(env, row, 3);

		// 4, string, zip
		zip = getStrData(env, row, 4);

		// 5, string, province
		province = getStrData(env, row, 5);

		// 6, string, city
		city = getStrData(env, row, 6);

		// 7, string, district
		district = getStrData(env, row, 7);

		// 8, string, address
		address = getStrData(env, row, 8);

		// 9, string, email
		email = getStrData(env, row, 9);

		jobject customer = env->NewObject(g_customerCache.cls, g_customerCache.initId,
			customerId, nickName, customerName, tel, zip, province, city, district, address, email);
		CHECK_NULL(customer);

		env->SetObjectArrayElement(arr, row, customer);
		CHECK_EXCEPTION(env->ExceptionOccurred());

		row++;
	}
	tdi_prot_st_close(g_tdi);

	return arr;
}

jbyteArray preGetCashSaleSpecsByTerm(JNIEnv *env, jobject thiz, jint warehouseId, jstring jsearchTerm) {
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "SELECT ggs.SpecID, ggs.SpecBarcode, ggl.GoodsNo, ggl.goodsName, ggs.SpecCode, ggs.SpecName, ggs.SPecPriceDetail, ggs.SpecPriceWholesale, ggs.SpecPriceMember, ggs.SpecPricePurchase, ggs.SpecPrice1, ggs.SpecPrice2, ggs.SpecPrice3, ss.Stock-ss.OrderCount-ss.SndCount, ss.WarehouseID";
	const char *from = "FROM g_stock_spec ss LEFT JOIN g_goods_goodsspec ggs USING(SpecID) LEFT JOIN g_goods_goodslist ggl USING(GoodsID)";
	const char *where = "WHERE ss.WarehouseID=%d AND ggl.bBlockUp=0 AND ggs.bBlockUp=0 AND (ggl.goodsName LIKE '%%%s%%' OR ggl.GoodsNo LIKE '%%%s%%' OR ggs.SpecBarcode LIKE '%%%s%%')";
	const char *orderByLimit = "ORDER BY SpecID LIMIT 20";

	char temp[strlen(select) + strlen(from) + strlen(where) + strlen(orderByLimit) + 10];
	sprintf(temp, "%s %s %s %s", select, from, where, orderByLimit);
	char sql[strlen(temp) + 60];
	const char *searchTerm = env->GetStringUTFChars(jsearchTerm, NULL);
	sprintf(sql, temp, warehouseId, searchTerm, searchTerm, searchTerm);
	status = tdi_prot_st_execute(g_tdi, sql);
	printf("%s", sql);
	printf("----------------------------------------preGetCashSaleSpecsByTerm status=%d", status);
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}
