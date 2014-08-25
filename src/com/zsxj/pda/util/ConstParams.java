package com.zsxj.pda.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;

public class ConstParams {
	
	public static final class PrefKeys {
		
		private PrefKeys() {};
		
		public static final String SELLER_NICK = "seller_nick";
		public static final String USER_NAME = "user_name";
		public static final String PASSWORD = "password";
		public static final String MEM_PSD = "memorize_password";
		public static final String AUTO_LOGIN = "auto_login";
		public static final String DOWNLOAD_ID = "download_id";
		
		public static final String WHICH_ACCOUNT = "account_position";
		public static final String WAREHOUSE_ID = "warehouse_id";
		public static final String WAREHOUSE_NAME = "warehouse_name";
		
		public static final String SYNC_POSITION_TIME = "sync_position_time";
		
		public static final String DEFAULT_SHOP = "default_shop";
		public static final String DEFAULT_PRICE = "defualt_price";
	}
	
	public static final class Extras {
		
		private Extras() {}
		
		public static final String WAREHOUSE_ID = "warehouse_id";
		public static final String SPEC_ID = "spec_id";
		public static final String SPEC_BARCODE = "spec_barcode";
		public static final String GOODS_NUM = "goods_num";
		public static final String GOODS_NAME = "goods_name";
		public static final String SPEC_CODE = "spec_code";
		public static final String SPEC_NAME = "spec_name";
		public static final String POSITION_ID = "position_id";
		public static final String POSITION_IDS = "position_ids";
		public static final String POSITION_NAME = "position_name";
		public static final String POSITION_NAMES = "position_names";
		public static final String STOCKS = "stocks";
		public static final String STOCKS_OLD = "stocks_old";
		public static final String STOCKS_PD = "stocks_pd";
		public static final String PD_ID = "pd_id";
		public static final String SCAN_TYPE = "scan_type";
		public static final String BARCODE = "barcode";
		public static final String RESET_WAREHOUSE = "reset_warehouse";
		public static final String POSITION_SYNC_RESPONSE_PERCENT = "position_sync_response_percent";
		public static final String POSITION_SYNC_RESPONSE_TIME = 
				"position_sync_response_time";
		public static final String SYNC_ERROR_MSG = "sync_error_msg";
		public static final String RECORD_IDS = "record_id";
		public static final String WHICH_SPEC = "spec_index";
		public static final String GOODS_TOTAL = "goods_total";
		public static final String COUNT = "count";
		public static final String UNIT_PRICE = "unit_price";
		public static final String DISCOUNT = "discount";
		public static final String TRADE_ID = "trade_id";
		public static final String PICKER_ID = "picker_id";
		public static final String POST_ID = "post_id";
		public static final String SELL_COUNT = "sell_count";
		public static final String COUNT_CHECK = "count_check";
		public static final String _ID = "_id";
		public static final String CASH_SALE_STOCK = "cash_sale_stock";
		public static final String ERROR_MESSAGE = "error_message";
		public static final String RETAIL_PRICE = "retail_price";
		public static final String WHOLESALE_PRICE = "whole_price";
		public static final String MEMBER_PRICE = "member_price";
		public static final String PURCHASE_PRICE = "purchase_price";
		public static final String PRICE_1 = "price_1";
		public static final String PRICE_2 = "price_2";
		public static final String PRICE_3 = "price_3";
		
		public static final String CUSTOMER_NAME = "customer_name";
		public static final String TEL = "tel";
		public static final String NICK_NAME = "nick_name";
		public static final String ZIP_CODE = "zip_code";
		public static final String PROVINCE = "province";
		public static final String CITY = "city";
		public static final String DISTRICT = "district";
		public static final String STREET_ADDRESS = "street_address";
        public static final String EMAIL = "email";
        public static final String NO_DISCOUNT_TOTAL = "no_discount_total";
        public static final String DISCOUNT_TOTAL = "discount_total";
        
        public static final String SEARCH_TERM = "search_term";
	}
	
	// Purpose of scanning barcode
	public static final class ScanType {
		
		private ScanType(){}
		
		public static final int TYPE_FAST_PD = 0;
		public static final int TYPE_QUERY_SPECS = 1;
		public static final int TYPE_PD = 2;
		public static final int TYPE_FAST_IN_EXAMINE_GOODS = 3;
		public static final int TYPE_OUT_EXAMINE_GOODS = 4;
		public static final int TYPE_PICK_GOODS = 5;
		public static final int TYPE_CASH_SALE = 6;
		public static final int TYPE_CASH_SALE_BY_TERM = 7;
	}
	
	// Handler cases
	public static final class HandlerCases {
		
		private HandlerCases(){}
		
		public static final int NO_CONN = 0;
		
		public static final int PREPARE_QUERY_ERROR = 10;
		public static final int EMPTY_RESULT = 11;
		public static final int QUERY_SUCCESS = 12;
		public static final int GET_COUNT = 13;
		public static final int GET_COUNT_ERROR = 14;
		public static final int QUERY_FAIL = 15;
		public static final int QUERY_2_SUCCESS = 16;
		public static final int QUERY_2_FAIL = 17;
		public static final int OUT_EXAMINE_MUTI_ROWS = 18;
		public static final int TIME_OUT = 19;
		
		public static final int PREPARE_UPDATE_FAIL = 20;
		public static final int UPDATE_SUCCESS = 21;
		public static final int UPDATE_FAIL = 22;
		
		public static final int SOCKET_CONN_FAIL = 30;
		public static final int SHAKE_1_FAIL = 31;
		public static final int SHAKE_2_FAIL = 32;
		public static final int SHAKE_3_FAIL = 33;
		public static final int SHAKE_4_FAIL = 34;
		public static final int PREPARE_GET_USER_ID_FAIL = 35;
		public static final int GET_USER_ID_FAIL = 36;
		public static final int PREPARE_GET_ACCOUNTS_FAIL = 37;
		public static final int GET_ACCOUNTS_FAIL = 38;
		public static final int LOGIN_SUCCESS = 39;
		public static final int UPDATE_FAIL_NO_CONN = 310;
		public static final int INVALID_PWD = 311;
		public static final int PREPARE_GET_USER_FAIL = 312;
		public static final int GET_USER_FAIL = 313;
		
		public static final int INSERT_OVER = 40;
		
		public static final int SCAN_RESULT = 50;
		
		public static final int UPDATE_CHECK = 60;
	}
	
	public static final class Actions {
		
		private Actions() {}
		
		public static final String POSITION_SYNC_RESPONSE_PERCENT_ACTION =
				"com.zsxj.pda.POSITION_SYNC_RESPONSE_PERCENT";
		public static final String POSITION_SYNC_RESPONSE_COMPLETION_ACTION =
				"com.zsxj.pda.POSITION_SYNC_RESPONSE_COMPLETION";
		public static final String POSITION_SYNC_RESPONSE_INTERRUPT_ACTION = 
				"com.zsxj.pda.POSITION_SYNC_RESPONSE_INTERRUPT";
	}
	
	public static final class Events {
		private Events() {}
		
		public static final String TIME_OUT = "query_time_out"; 
		public static final String DBE_INVALID_PACKET = "dbe_invalid_packet";
	}
	
	public static final class TdiErrorCode {
		private TdiErrorCode() {}
		
		public static final int DBE_NO_ERROR = 0;
				
		/*1-20,致命错误，无需重新尝试*/
		public static final int DBE_DRIVER_NOT_FOUND = 1;
		public static final int DBE_INVALID_DRIVER = 2;
		public static final int DBE_INVALID_CONNECT_PARAMS = 3;
		public static final int DBE_INVALID_VERSION = 4;
		public static final int DBE_REDIRECT_TOO_TIMES = 5;
		public static final int DBE_ALREADY_CONNECTED = 6;
		public static final int DBE_INVALID_LICENSE = 7;
		public static final int DBE_SIGN_EXPIRED = 8;

		public static final int DBE_INVALID_PWD = 10;		//用户名或密码错误
		public static final int DBE_REACH_LIMIT = 11;		//连接数达到上限
		public static final int DBE_DB_CONFIG = 12;			//server数据库配置有误
		public static final int DBE_CLIENT_MUST_UPDATE = 13;//客户端版本太低
		public static final int DBE_DB_MUST_UPDATE = 14;	//数据库版本太低
		public static final int DBE_DB_ACCOUNT_DISABLED = 15; //帐户禁用
		//DBE_SUB_ACCOUNT_NOT_BIND = 16, //用子帐号登录时，子帐号未启用
				
		public static final int DBE_NEED_EXTRA_AUTH = 17;
		public static final int DBE_OTHER_ERROR = 19;
		public static final int DBE_ERR_REDIRECT = 20;		//连接重定向
		
		/*21-40严重错误，需要重新建立连接*/
		public static final int DBE_DISCONNECTED = 21;
		public static final int DBE_OS_RESOURCE = 22;
		public static final int DBE_NETWORK = 23;			//CR_SERVER_LOST
		public static final int DBE_INVALID_PACKET = 24;
		public static final int DBE_FULL_OF_BUFFER = 25;		//write to buffer
		public static final int DBE_CMD_PROCESS = 26;			//CR_COMMANDS_OUT_OF_SYNC

		public static final int DBE_NEED_RECONNECT_LAST = 40;	//需要重连的最大错误编号
		
		/*41-99, 无需重连*/
		public static final int DBE_INVALID_PARAMS = 41;
		public static final int DBE_NO_RESULT = 42;
		public static final int DBE_COLUMN_NOT_FOUND = 43;
		public static final int DBE_NULL_FIELD = 44;
		public static final int DBE_OUT_OF_RANGE = 45;
		
		/*100以上,数据库系统错误*/
		public static final int DEB_DATABASE = 100;
		public static final int DBE_DUPLICATE_KEY = 101;
	}
	
	public static class Interface {
		private Interface () {}
		
		private static final String HAOXU_IP = "121.196.139.2";
		private static final String DUODUOTEST_IP = "121.199.38.85";
		private static final String DEMO_IP = DUODUOTEST_IP;
		private static final String QIQU_IP = "121.196.130.67";
		private static final String JOYVIO_IP = "121.196.130.67";
		private static final String JIULONG_IP = JOYVIO_IP;
		
		public static final String getPrefix(String sellerNick) {
			String prefix = null;
			if (sellerNick.equals("haoxu")) {
                prefix = "http://" + HAOXU_IP + "/StockAPI/interface.php";
			} else if (sellerNick.equals("duoduotest")) {
				prefix = "http://" + DUODUOTEST_IP + "/openapi/interface.php";
			} else if (sellerNick.equals("demo")) {
				prefix = "http://" + DEMO_IP + "/openapi/interface.php";
			} else if (sellerNick.equals("qiqu")) {
				prefix = "http://" + QIQU_IP + "/stockapi/interface.php";
			} else if (sellerNick.equals("joyvio")) {
				prefix = "http://" + JOYVIO_IP + "/stockapi/interface.php";
			} else if (sellerNick.equals("jiulong")) {
				prefix = "http://" + JIULONG_IP + "/stockapi/interface.php";
			}
			return prefix;
		}
		
		public static final String SEP = "&";
		public static final String METHOD = "Method=";
		private static final String SELLER_ID = "SellerID=";
		public static String getSellerIdSeg() {
			return SELLER_ID + Globals.getSellerNick() + SEP;
		}
		public static final String INTERFACE_ID = "InterfaceID=";
		
		public static final String SIGN = "Sign=";
		public static final String CONTENT = "Content=";
		
		public static final String HAOXU_KEY = "8ba20e39e9d49b59522c82a4b32c2598";
		public static final String DUODUOTEST_KEY = "12345";
		public static final String DEMO_KEY = "12345";
		public static final String QIQU_KEY = "87ec48c80143ea83c01876b119dea475";
		public static final String JOYVIO_KEY = "116963eb965eb8cd3705f583633d65d4";
		public static final String JIULONG_KEY = "116963eb965eb8cd3705f583633d65d4";
		
		public static final String getKey(String sellerNick) {
			if (sellerNick.equals("haoxu")) {
				return HAOXU_KEY;
			} else if (sellerNick.equals("duoduotest")) {
				return DUODUOTEST_KEY;
			} else if (sellerNick.equals("demo")) {
				return DEMO_KEY;
			} else if (sellerNick.equals("qiqu")) {
				return QIQU_KEY;
			} else if (sellerNick.equals("joyvio")) {
				return JOYVIO_KEY;
			} else if (sellerNick.equals("jiulong")) {
				return JIULONG_KEY;
			}
			return null;
		}
		
		public static final String METHOD_NEW_ORDER = METHOD + "NewOrder" + SEP;
		
		// 一级结点与数据
		public static final String OUT_IN_FLAG = "OutInFlag";
		public static final int FLAG_SALE_OUT = 3;
		public static final String REG_OPERATOR_NO = "RegOperatorNO";

		public static final String IF_ORDER_CODE = "IF_OrderCode";
		public static String getPdaTradeOrderCode(Context context) {
			String orderCode = "PJY";
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssSSS", Locale.US);
			Calendar cal = Calendar.getInstance();
			String today = sdf.format(cal.getTime());
			orderCode += today;

			return orderCode;
		}
		
		public static String getInterfaceId() {
			String sellerNick = Globals.getSellerNick();
			if (sellerNick.equals("joyvio") || sellerNick.equals("jiulong")) {
				return "POP";
			} else {
				return "pda";
			}
		}
		
		public static final String WAREHOUSE_NO = "WarehouseNO";
		public static final String GOODS_TOTAL = "GoodsTotal";
		public static final String COD_FLAG = "COD_Flag";
		public static final String ORDER_PAY = "OrderPay";
		public static final String LOGISTICS_PAY = "LogisticsPay";
		public static final String SHOP_NAME = "ShopName";
		public static final String BUYER_NAME = "BuyerName";
		public static final String BUYER_POST_CODE = "BuyerPostCode";
		public static final String BUYER_TEL = "BuyerTel";
		public static final String NICK_NAME = "NickName";
		public static final String BUYER_PROVINCE = "BuyerProvince";
		public static final String BUYER_CITY = "BuyerCity";
		public static final String BUYER_DISTRICT = "BuyerDistrict";
		public static final String BUYER_ADDR = "BuyerAdr";
		public static final String ITEM_COUNT = "ItemCount";
		public static final String ITEM_LIST = "ItemList";
		public static final String BUYER_EMAIL = "BuyerEmaill";
		public static final String FAVOURABLE_TOTAL = "FavourableTotal";
		public static final String LOGISTICS_CODE = "LogisticsCode";
		public static final String REMARK = "Remark";
		
		// 二级结点
		public static final String ITEM = "Item";
		public static final String SKU_CODE = "Sku_Code";
		public static final String SKU_NAME = "Sku_Name";
		public static final String SKU_PRICE = "Sku_Price";
		public static final String DISCOUNT = "Discount";
		public static final String TOTAL = "Total";
		public static final String QTY = "Qty";
	}
}
