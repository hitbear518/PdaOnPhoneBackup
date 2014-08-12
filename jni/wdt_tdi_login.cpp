#include <stdio.h>
#include <stdlib.h>
#include "tdi_prot.h"
#include "wdt_tdi.h"
#include "wdt_tdi_login.h"

bool bLogin = false;

JNIEXPORT jbyteArray JNICALL tdiProtSh1(JNIEnv *env, jobject thiz, jstring jip) {

	int status;

	// 建立握手数据1
	// 服务器IP, 客户端版本
	const char *ip = env->GetStringUTFChars(jip, NULL);
	status = tdi_prot_sh_1(g_tdi, ip, 0);
	printf("tdiProtSh1 status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	jbyteArray jbArr = env->NewByteArray(tdi_prot_len(g_tdi));
	if (NULL == jbArr) return NULL;
	env->SetByteArrayRegion(jbArr, 0, tdi_prot_len(g_tdi), (jbyte*)tdi_prot_data(g_tdi));
	CHECK_EXCEPTION(env->ExceptionOccurred());

	return jbArr;
}

JNIEXPORT jbyteArray tdiProtSh2(JNIEnv *env, jobject thiz, jbyteArray jbArray, jstring sellerNick) {

	const char *sellerNickNative = env->GetStringUTFChars(sellerNick, NULL);

	transjbArrToBuf(env, jbArray);

	int status;

	printf("sellerNick=%s", sellerNickNative);

	// 建立握手包2
	status = tdi_prot_sh_2(g_tdi, sellerNickNative, g_recvBuf, g_recvLen);
	printf("tdiProtSh2 status=%d\n", status);

	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

//	// 发握手包2
//	CHECK_SD(send_data(s_sock, (char*)tdi_prot_data(s_tdi), tdi_prot_len(s_tdi)));
//	// 接收数据
//	CHECK_SD(recv_pack(s_sock));

	jbyteArray jbArr = env->NewByteArray(tdi_prot_len(g_tdi));
	if (NULL == jbArr) return NULL;
	env->SetByteArrayRegion(jbArr, 0, tdi_prot_len(g_tdi), (jbyte*)tdi_prot_data(g_tdi));
	CHECK_EXCEPTION(env->ExceptionOccurred());

	return jbArr;
}

JNIEXPORT jbyteArray JNICALL tdiProtSh3(JNIEnv *env, jobject thiz, jbyteArray jbArray, jstring userName, jstring password) {

	const char *userNameNative = env->GetStringUTFChars(userName, NULL);
	const char *passwordNative = env->GetStringUTFChars(password, NULL);

	transjbArrToBuf(env, jbArray);

	int status;

	printf("userName=%s", userNameNative);
	printf("password=%s", passwordNative);

	status = tdi_prot_parse_status(g_tdi, g_recvBuf, g_recvLen);
	printf("tdiProtSh3 status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	// 登录
	// 倒数第二个参数表示传进去的密码是还是md5之后的值
	// 最后一个参数，是断线后程序自动重连，设置为1
	status = tdi_prot_login(g_tdi, userNameNative, passwordNative, 0, 0);
	printf("tdiProtSh3 status=%d\n", status);
	CHECK_STATUS(status);
	if (status) {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

//	CHECK_SD(send_data(s_sock, (char*)tdi_prot_data(s_tdi), tdi_prot_len(s_tdi)));
//	CHECK_SD(recv_pack(s_sock));

	jbyteArray jbArr = env->NewByteArray(tdi_prot_len(g_tdi));
	if (NULL == jbArr) return NULL;
	env->SetByteArrayRegion(jbArr, 0, tdi_prot_len(g_tdi), (jbyte*)tdi_prot_data(g_tdi));
	CHECK_EXCEPTION(env->ExceptionOccurred());

	return jbArr;
}

JNIEXPORT jbyteArray JNICALL tdiProtSh4(JNIEnv *env, jobject thiz, jbyteArray jbArray) {

	transjbArrToBuf(env, jbArray);

	int status;

	status = tdi_prot_parse_status(g_tdi, g_recvBuf, g_recvLen);
	printf("tdiProtSh4 status=%d\n", status);
	CHECK_STATUS(status);
	if (0 == status) {
		bLogin = true;
	} else {
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	// 判断登录状态
	// 要处理重定向

	jbyteArray jbArr = env->NewByteArray(tdi_prot_len(g_tdi));
	if (NULL == jbArr) return NULL;
	env->SetByteArrayRegion(jbArr, 0, tdi_prot_len(g_tdi), (jbyte*)tdi_prot_data(g_tdi));
	CHECK_EXCEPTION(env->ExceptionOccurred());

	return jbArr;
}

jbyteArray prepareGetUserId(JNIEnv *env, jobject thiz, jstring juserName)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *userName = env->GetStringUTFChars(juserName, NULL);
	const char *select = "select UID  from g_sys_userlist where UserName='%s' and bBlockUp=0";
	char sql[strlen(select) + 10];
	sprintf(sql, select, userName);

	status = tdi_prot_st_execute(g_tdi, sql);
#ifdef DEBUG
	printf("prepareGetUserId status=%d\n", status);
#endif
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jint getUserId(JNIEnv *env, jobject thiz, jbyteArray jbuf)
{
	transjbArrToBuf(env, jbuf);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
#ifdef DEBUG
	printf("getUserId status=%d\n", status);
#endif
	CHECK_STATUS(status);
	if (status && status != 1064)
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
		return -1;

	int userId = -1;
	if (0 == tdi_prot_next(g_tdi))
	{
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
	}

	tdi_prot_st_close(g_tdi);
	return userId;
}

jbyteArray prepareGetAccounts(JNIEnv *env, jobject thiz)
{
	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *select = "select AccountID, AccountName from g_cfg_inoutaccountlist Where bBlockUp=0";
	status = tdi_prot_st_execute(g_tdi, select);
#ifdef DEBUG
	printf("prepareGetUserId status=%d\n", status);
#endif
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}
	return transTdiTojbArr(env);
}

jobjectArray getAccounts(JNIEnv *env, jobject thiz, jbyteArray jbuf)
{
	transjbArrToBuf(env, jbuf);

	int status;

	status = tdi_prot_st_result(g_tdi, g_recvBuf, g_recvLen);
#ifdef DEBUG
	printf("getAccounts status=%d\n", status);
#endif
	CHECK_STATUS(status);
	if (status && status != 1064)
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
		return NULL;
	jobjectArray accountArr = env->NewObjectArray(row, g_accountCache.cls, NULL);
	CHECK_NULL(accountArr);

	int i;
	row = 0;
	while (0 == tdi_prot_next(g_tdi))
	{
		jint accountId;
		jstring accountName;

		const char *str;
		const char *colName;

		colName = tdi_prot_st_field_name(g_tdi, 0);
		if (!tdi_prot_st_is_null(g_tdi, 0))
			accountId = tdi_prot_st_int_field(g_tdi, 0);
		else
		{
			accountId = -1;
			printError("%s is null in row%d", colName, row);
		}
#ifdef DEBUG
		printf("row = %d -- %s = %d",row, colName, accountId);
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
		accountName = env->NewStringUTF(str);
		CHECK_NULL(accountName);

		jobject account = env->NewObject(g_accountCache.cls, g_accountCache.initId, accountId, accountName);
		CHECK_NULL(account);
		env->SetObjectArrayElement(accountArr, row, account);
		CHECK_EXCEPTION(env->ExceptionOccurred());
		row++;
	}

	tdi_prot_st_close(g_tdi);
	return accountArr;
}

JNIEXPORT jbyteArray JNICALL Java_com_zsxj_pda_wdt_WDTQuery_test(JNIEnv *env, jobject, jobjectArray obj_stus) {

	printf("test");
	jclass stu_cls = env->GetObjectClass(env->GetObjectArrayElement(obj_stus, 0)); //或得Student类引用

	if (NULL == stu_cls) {
		printf("GetObjectClass failed \n");
	}

	// 下面这些函数操作，我们都见过的。O(∩_∩)O~
	jfieldID ageFieldID = env->GetFieldID(stu_cls, "age", "I"); //获得得Student类的属性id
	jfieldID nameFieldID = env->GetFieldID(stu_cls, "name", "Ljava/lang/String;"); //获得属性ID

	for (int i = 0; i < env->GetArrayLength(obj_stus); i++) {

		jobject obj_stu = env->GetObjectArrayElement(obj_stus, i);

		jint age = env->GetIntField(obj_stu, ageFieldID); //获得属性值
		jstring name = (jstring)env->GetObjectField(obj_stu, nameFieldID); //获得属性值

		const char *cName = env->GetStringUTFChars(name, 0);

		printf("stu %d age=%d", i, age);
		printf("stu %d name=%s", i, cName);
	}

	return NULL;
}

JNIEXPORT jboolean JNICALL isLogin(JNIEnv *, jobject) {

	if (NULL == g_tdi || !bLogin) {
		return false;
	} else {
		return true;
	}
}

JNIEXPORT void JNICALL logout(JNIEnv *, jobject) {

	bLogin = false;

	//
	tdi_prot_delete(g_tdi);
	tdi_prot_term();

	g_tdi = NULL;
}

jbyteArray preGetUser(JNIEnv *env, jobject thiz, jstring juserName) {

	int status;

	// 查询
	tdi_prot_st_prepare(g_tdi);

	const char *userName = env->GetStringUTFChars(juserName, NULL);
	const char *select = "SELECT Uid, UserNO, Username FROM g_sys_userlist WHERE Username = '%s'";
	char sql[strlen(select) + 10];
	sprintf(sql, select, userName);

	status = tdi_prot_st_execute(g_tdi, sql);
#ifdef DEBUG
	printf("----------------------preGetUser status=%d\n", status);
	printf("%s", sql);
#endif
	CHECK_STATUS(status);
	if (status)
	{
		throwEx(env, status, tdi_prot_data(g_tdi));
		return NULL;
	}

	return transTdiTojbArr(env);
}

jobject getUser(JNIEnv *env, jobject thiz, jbyteArray jbuf) {
	int status = getSqlResult(env, jbuf);
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

	jobject user = NULL;

	if (0 == tdi_prot_next(g_tdi))
	{
		jint userId;
		jstring userNo;
		jstring userName;
		const char *str;
		const char *colName;

		userId = getIntData(row, 0);
		userNo = getStrData(env, row, 1);
		userName = getStrData(env, row, 2);

		user = env->NewObject(g_userCache.cls, g_userCache.initId, userId, userNo, userName);
		CHECK_NULL(user);
	}

	tdi_prot_st_close(g_tdi);
	return user;
}
