#ifndef TDI_PROT
#define TDI_PROT

#ifdef __cplusplus
extern "C"
{
#endif

typedef short INT16;

typedef int INT32;
typedef unsigned int UINT32;
typedef long long INT64;
typedef unsigned long long UINT64;


#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "DEBUG", __VA_ARGS__);
#define printError(...) __android_log_print(ANDROID_LOG_ERROR, "ERROR", __VA_ARGS__);

enum TDI_ERROR_CODE
{
	DBE_NO_ERROR = 0,
	
	/*1-20,致命错误，无需重新尝试*/
	DBE_DRIVER_NOT_FOUND = 1,
	DBE_INVALID_DRIVER = 2,
	DBE_INVALID_CONNECT_PARAMS = 3,
	DBE_INVALID_VERSION = 4,
	DBE_REDIRECT_TOO_TIMES = 5,
	DBE_ALREADY_CONNECTED = 6,
	DBE_INVALID_LICENSE = 7,
	DBE_SIGN_EXPIRED = 8,

	DBE_INVALID_PWD = 10,		//用户名或密码错误
	DBE_REACH_LIMIT = 11,		//连接数达到上限
	DBE_DB_CONFIG = 12,			//server数据库配置有误
	DBE_CLIENT_MUST_UPDATE = 13, //客户端版本太低
	DBE_DB_MUST_UPDATE = 14,	//数据库版本太低
	DBE_DB_ACCOUNT_DISABLED = 15, //帐户禁用
	//DBE_SUB_ACCOUNT_NOT_BIND = 16, //用子帐号登录时，子帐号未启用
			
	DBE_NEED_EXTRA_AUTH = 17,
	DBE_OTHER_ERROR = 19,
	DBE_ERR_REDIRECT = 20,		//连接重定向
	
	/*21-40严重错误，需要重新建立连接*/
	DBE_DISCONNECTED = 21,
	DBE_OS_RESOURCE = 22,
	DBE_NETWORK = 23,			//CR_SERVER_LOST
	DBE_INVALID_PACKET = 24,
	DBE_FULL_OF_BUFFER = 25,		//write to buffer
	DBE_CMD_PROCESS = 26,			//CR_COMMANDS_OUT_OF_SYNC

	DBE_NEED_RECONNECT_LAST = 40,	//需要重连的最大错误编号
	
	/*41-99, 无需重连*/
	DBE_INVALID_PARAMS = 41,
	DBE_NO_RESULT = 42,
	DBE_COLUMN_NOT_FOUND = 43,
	DBE_NULL_FIELD = 44,
	DBE_OUT_OF_RANGE = 45,
	
	/*100以上,数据库系统错误*/
	DEB_DATABASE = 100,
	DBE_DUPLICATE_KEY = 101,
};
	
//字段类型
//整数
#define COLUMN_TYPE_INT 1
//字符串
#define COLUMN_TYPE_STRING 2

//初始化库
void tdi_prot_init();

void tdi_prot_term();

//分配一个协议缓冲区
void * tdi_prot_new();

void tdi_prot_delete(void * pCtx);

//取得要数据
char * tdi_prot_data(void * pCtx);
//取得数据长度
int tdi_prot_len(void * pCtx);

//握手第一步
//pszIP服务器的IP地址
//uVersion客户端版本号
int tdi_prot_sh_1(void * pCtx, const char * pszIP, UINT32 uVersion);
//握手第二步
//pszSID卖家ID
//pData服务器返回的数据
int tdi_prot_sh_2(void * pCtx, const char * pszSID, char * pData, int nLen);

INT32 tdi_prot_parse_status(void * pCtx, char * pData, int nLen);

int tdi_prot_login(void * pCtx, const char * pszUser, const char * pszPwd, int bMD5, int bReconn);

//分配命令
void tdi_prot_st_prepare(void * pCtx);
//释放一个命名
void tdi_prot_st_close(void * pCtx);
//绑定一个整数参数
int tdi_prot_st_bind_int(void * pCtx, const char * pszName, INT32 nValue, int bNull);
//绑定一个字符串参数
int tdi_prot_st_bind_str(void * pCtx, const char * pszName, const char * pszValue);
//构造执行命令
int tdi_prot_st_execute(void * pCtx, const char * pszSQL);
//分析执行结果
int tdi_prot_st_result(void * pCtx, char * pData, int nLen);

//取行数
int tdi_prot_st_rows(void * pCtx);

//取列个数
int tdi_prot_st_fields(void * pCtx);
//取列名
const char * tdi_prot_st_field_name(void * pCtx, int nIndex);
//取列值类型
int tdi_prot_st_field_type(void * pCtx, int nIndex);

//œ¬“∆“ª––
int tdi_prot_next(void * pCtx);

//下移一行
int tdi_prot_st_int_field(void * pCtx, int nIndex);
//取整数值
//如果字段值为空，返回NULL
const char* tdi_prot_st_str_field(void * pSt, int nIndex);
//判断字段是否为空
//如果为NULL返回1否则返回0
int tdi_prot_st_is_null(void * pCtx, int nIndex);

int tdi_prot_print(void * pCtx, char *str);

#ifdef __cplusplus
}
#endif

#endif /* !TDI_PROT */
