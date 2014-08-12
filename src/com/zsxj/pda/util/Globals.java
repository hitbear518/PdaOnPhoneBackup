package com.zsxj.pda.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.Customer;
import com.zsxj.pda.wdt.User;

public class Globals {
	
//	private static int sUserId;
	private static String sSellerNick;
//	private static String sUserName;
	private static Account[] sAccounts;
	private static int sSupplierId = -1;
	private static int sWhichPrice = -1;
	private static int sWhichWarehouse = -1;
	private static int sWhichSupplier = -1;
	private static int sModuleUseWarehouseId = -1;
	private static String sCashSaleWarehouseNO = null;
	private static int sWhichShop = -1;
	private static String sShopName = null;
	private static boolean sIsScanning = false;
	private static Customer sCustomer = null;
	private static User sUser = null;
	
	public static void setUser(User user) {
		sUser = user;
	}
	
	public static int getUserId() {
		return sUser.userId;
	}
	
	public static String getUserName() {
		return sUser.userName;
	}
	
	public static String getUserNo() {
		return sUser.userNo;
	}

//	public static void setUserId(int userId) {
//		sUserId = userId;
//	}
	
	public static String getUserPrefsName() {
		return sSellerNick + "_" + sUser.userName + "_prefs";
	}
	
	public static String getSellerNick() {
		return sSellerNick;
	}
	
//	public static void setDataPrefix(String sellerNick, String uerName) {
//		sSellerNick = sellerNick;
//		sUserName = uerName;
//	}
	
	public static void setSellerNick(String sellerNick) {
		sSellerNick = sellerNick;
	}
	
	public static Account[] getAccounts() {
		return sAccounts;
	}
	
	public static void setAccounts(Account[] accounts) {
		sAccounts = accounts;
	}
	
	public static String getDbName() {
		return sSellerNick + ".db";
	}
	
	public static String getWarehousePrefsName(int warehouseId) {
		return sSellerNick + "_" + warehouseId + "_prefs";
	}
	
	public static int getWarehouseId(Context ctx) {
		SharedPreferences prefs = 
			ctx.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE);
		return prefs.getInt(PrefKeys.WAREHOUSE_ID, -1);
	}
	
	public static void setSupplierId(int providerId) {
		sSupplierId = providerId;
	}
	
	public static int getSupplierId() {
		return sSupplierId;
	}
	
	public static void setWhichSupplier(int whichSupplier) {
		sWhichSupplier = whichSupplier;
	}
	
	public static int getWhichSupplier() {
		return sWhichSupplier;
	}
	
	public static void setWhichWarehouse(int whichWarehouse) {
		sWhichWarehouse = whichWarehouse;
	}
	
	public static int getWhichWarehouse() {
		return sWhichWarehouse;
	}
	
	public static void setModuleUseWarehouseId(int warehouseId) {
		sModuleUseWarehouseId = warehouseId;
	}
	
	public static int getModuleUseWarehouseId() {
		return sModuleUseWarehouseId;
	}
	
	public static void setWhichPrice(int whichPrice) {
		sWhichPrice = whichPrice;
	}
	
	public static int getWhichPrice() {
		return sWhichPrice;
	}
	
	public static void setWhichShop(int whichShop) {
		sWhichShop = whichShop;
	}
	
	public static int getWhichShop() {
		return sWhichShop;
	}
	
	public static void setShopName(String shopName) {
		sShopName = shopName;
	}
	
	public static String getShopName() {
		return sShopName;
	}
	
	public static void setCashSaleWarehouseNO(String warehouseNO) {
		sCashSaleWarehouseNO = warehouseNO;
	}
	
	public static String getCashSaleWarehouseNO() {
		return sCashSaleWarehouseNO;
	}
	
	public static int[] getModules(String sellerNick) {
		if (sellerNick.equals("duoduoyun") || sellerNick.equals("yinpai") || sellerNick.equals("jingu")) {
			return new int[] {
				0, 1, 2, 3, 4, 99
			};
		} else if (sellerNick.equals("haoxu") || sellerNick.equals("qiqu")) {
			return new int[] {
				0, 1, 2, 3, 4, 5, 99
			};
		} else if (sellerNick.equals("demo") || sellerNick.equals("duoduotest") || sellerNick.equals("joyvio") || sellerNick.equals("jiulong")) {
			return new int[] {
				5, 99
			};
		} else {
			return new int[] {99};
		}
	}
	
	public static void setIsScanning(boolean isScanning) {
		sIsScanning = isScanning;
	}
	
	public static boolean getIsScanning() {
		return sIsScanning;
	}
	
	public static void setCustomer(Customer customer) {
		sCustomer = customer;
	}
	
	public static Customer getCustomer() {
		return sCustomer;
	}
}