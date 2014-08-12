package com.zsxj.pda.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public class ProviderContract {
	
	private ProviderContract() {}

	static final String AUTHORITY = "com.zsxj.pda.provider";

	private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	
	public static final class PdInfo implements BaseColumns {
		
		private PdInfo() {} 
		
		static final String PATH = "pd_info";
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
		
		private static String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + ".pd_info";
		static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
		static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
		
		public static final String COLUMN_NAME_PD_ID = "pd_id";
		public static final String COLUMN_NAME_REC_ID = "rec_id";
		public static final String COLUMN_NAME_SPEC_ID = "spec_id";
		public static final String COLUMN_NAME_SPEC_BARCODE = "spec_barcode";
		public static final String COLUMN_NAME_GOODS_NUM = "goods_num";
		public static final String COLUMN_NAME_GOODS_NAME = "goods_name";
		public static final String COLUMN_NAME_BARCODE = "barcode";
		public static final String COLUMN_NAME_SPEC_CODE = "spec_code";
		public static final String COLUMN_NAME_SPEC_NAME = "spec_name";
		public static final String COLUMN_NAME_POSITION_ID = "position_id";
		public static final String COLUMN_NAME_POSITION_NAME = "position_name";
		public static final String COLUMN_NAME_STOCK_OLD = "stock_old";
		public static final String COLUMN_NAME_STOCK_PD = "stock_pd";
		public static final String COLUMN_NAME_REMARK = "remark";
	}
	
	public static final class Positions implements BaseColumns {
		
		private Positions() {}
		
		static final String PATH = "positions";
		static final String TEMP_PATH = "temp_positions";
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
		public static final Uri TEMP_URI = Uri.withAppendedPath(AUTHORITY_URI, TEMP_PATH);
		
		private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + "." + PATH;
		static final String CONTENT_TYPE = 
				ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
		static final String CONTENT_ITEM_TYPE = 
				ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
		
		public static final String COLUMN_NAME_POSITION_ID = "position_id"; 
		public static final String COLUMN_NAME_POSITION_NAME = "position_name";
		public static final String COLUMN_NAME_WAREHOUSE_ID = "warehouse_id";
	}
	
	public static final class FastInExamineGoodsInfo implements BaseColumns {
		
		private FastInExamineGoodsInfo() {}
		
		static final String PATH = "fast_examine_goods";
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
		
		private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + "." + PATH;
		static final String CONTENT_TYPE = 
				ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
		static final String CONTENT_ITEM_TYPE = 
				ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
		
		public static final String COLUMN_NAME_SPEC_ID = "spec_id";
		public static final String COLUMN_NAME_SPEC_BARCODE = "spec_barcode";
		public static final String COLUMN_NAME_GOODS_NUM = "goods_num";
		public static final String COLUMN_NAME_GOODS_NAME = "goods_name";
		public static final String COLUMN_NAME_SPEC_CODE = "spec_code";
		public static final String COLUMN_NAME_SPEC_NAME = "spec_name";
		public static final String COLUMN_NAME_COUNT = "count";
		public static final String COLUMN_NAME_UNIT_PRICE = "unit_price";
		public static final String COLUMN_NAME_DISCOUNT = "discount";
	}
	
	public static final class TradeGoods implements BaseColumns {
		
		private TradeGoods() {}
		
		static final String PATH = "trade_goods";
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
		private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + "." + PATH;
		static final String CONTENT_TYPE = 
				ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
		static final String CONTENT_ITEM_TYPE = 
				ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
		
		public static final String COLUMN_NAME_REC_ID = "rec_id";
		public static final String COLUMN_NAME_BARCODE = "barcode";
		public static final String COLUMN_NAME_GOODS_NO = "goods_No";
		public static final String COLUMN_NAME_GOODS_NAME = "goods_name";
		public static final String COLUMN_NAME_SPEC_CODE = "spec_code";
		public static final String COLUMN_NAME_SPEC_NAME = "spec_name";
		public static final String COLUMN_NAME_POSITION_NAME = "position_name";
		public static final String COLUMN_NAME_SELL_COUNT = "sell_count";
		public static final String COLUMN_NAME_COUNT_CHECK = "count_check";
		public static final String COLUMN_NAME_EDITABLE = "editable";
	}
	
	public static final class CashSaleGoods implements BaseColumns {
		
		private CashSaleGoods() {}
		
		static final String PATH = "cash_sale_goods";
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
		
		private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + "." + PATH;
		static final String CONTENT_TYPE = 
				ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
		static final String CONTENT_ITEM_TYPE = 
				ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
		
		public static final String COLUMN_NAME_SPEC_ID = "spec_id";
		public static final String COLUMN_NAME_SPEC_BARCODE = "spec_barcode";
		public static final String COLUMN_NAME_GOODS_NUM = "goods_num";
		public static final String COLUMN_NAME_GOODS_NAME = "goods_name";
		public static final String COLUMN_NAME_SPEC_CODE = "spec_code";
		public static final String COLUMN_NAME_SPEC_NAME = "spec_name";
		public static final String COLUMN_NAME_CASH_SALE_STOCK = "cash_sale_stock";
		public static final String COLUMN_NAME_COUNT = "count";
		public static final String COLUMN_NAME_RETAILE_PRICE = "retail_price";
		public static final String COLUMN_NAME_WHOLESALE_PRICE = "wholesale_price";
		public static final String COLUMN_NAME_MEMBER_PRICE = "member_price";
		public static final String COLUMN_NAME_PURCHASE_PRICE = "purchase_price";
		public static final String COLUMN_NAME_PRICE_1 = "price_1";
		public static final String COLUMN_NAME_PRICE_2 = "price_2"; 
		public static final String COLUMN_NAME_PRICE_3 = "price_3";
		public static final String COLUMN_NAME_DISCOUNT = "discount";
		public static final String COLUMN_NAME_WAREHOUSE_ID = "warehouse_id";
		public static final String COLUMN_NAME_BARCODE = "barcode";
	}
}
