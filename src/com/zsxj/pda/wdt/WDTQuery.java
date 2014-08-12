package com.zsxj.pda.wdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.util.Log;

import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.SocketService;
import com.zsxj.pda.util.ConstParams.Events;
import com.zsxj.pda.util.ConstParams.HandlerCases;

public class WDTQuery {
	
	static {
		System.loadLibrary("wdt_tdi");
	}
	
	public interface QueryCallBack {
		public void onQuerySuccess(Object qr);
		public void onQueryFail(int type, WDTException wdtEx);
	}
	
	private native byte[] preGetWarehouses() throws WDTException;
	private native Warehouse[] getWarehouses(byte[] buf) throws WDTException;
	private native byte[] preGetPdEntries(int warehouseId) throws WDTException;
	private native PdEntry[] getPdEntries(byte[] buf) throws WDTException;
	private native byte[] preGetSpecs(int warehouseId, String barcode) throws WDTException;
	private native Spec[] getSpecs(byte[] buf) throws WDTException;
	private native byte[] preGetPositionCount(int warehouseId) throws WDTException;
	private native int getPositionCount(byte[] buf) throws WDTException;
	private native byte[] preGetPositions(int warehouseId, int pos, int pageSize) throws WDTException;
	private native Position[] getPositions(byte[] bytes) throws WDTException;
	private native byte[] preGetPdDetails(int pdId, int pos, int pageSize) throws WDTException;
	private native Spec[] getPdDetails(byte[] buf) throws WDTException;
	private native byte[] preGetPdDetailCount(int pdId) throws WDTException;
	private native int getPdDetailCount(byte[] buf) throws WDTException;
	private native byte[] preGetPdSpecs(int pdId, String barcode) throws WDTException;
	private native Spec[] getPdSpecs(byte[] buf) throws WDTException;
	private native byte[] preGetSuppliers(int pos, int pageSize) throws WDTException;
	private native Supplier[] getSuppliers(byte[] buf) throws WDTException;
	private native byte[] preGetPrice(int specId, int providerId) throws WDTException;
	private native Price getPrice(byte[] buf) throws WDTException;
	private native byte[] preGetLogistics() throws WDTException;
	private native Logistics[] getLogistics(byte[] buf) throws WDTException;
	private native byte[] preGetTradeInfo(String tradeOrPost) throws WDTException;
	private native TradeInfo getTradeInfo(byte[] buf) throws WDTException;
	private native byte[] preGetPickers() throws WDTException;
	private native User[] getPickers(byte[] buf) throws WDTException;
	private native byte[] preGetTradeGoods(int tradeId, int warehouseId) throws WDTException;
	private native TradeGoods[] getTradeGoods(byte[] buf) throws WDTException;
	private native byte[] preGetInExamSpecs(String barcode) throws WDTException;
	private native Spec[] getInExamSpecs(byte[] buf) throws WDTException;
	private native byte[] preGetSupplierCount() throws WDTException;
	private native int getSupplierCount(byte[] buf) throws WDTException;
	private native byte[] preGetShops() throws WDTException;
	private native Shop[] getShops(byte[] buf) throws WDTException;
	private native byte[] preGetCashSaleSpecs(int warehouseId, String barcode) throws WDTException;
	private native CashSaleSpec[] getCashSaleSpecs(byte[] buf) throws WDTException;
	private native byte[] preGetInterfaceWarehouses(String interfaceId) throws WDTException;
	private native byte[] preGetCashSaleSpecStock(int specId, int warehouseId);
	private native String getCashSaleSpecStock(byte[] buf);
	private native byte[] preGetCustomers(String tel);
	private native Customer[] getCustomers(byte[] buf);
	private native byte[] preGetCashSaleSpecsByTerm(int warehouseId, String searchTerm);
	
	private static int TYPE_WAREHOUSES = 0;
	private static int TYPE_PD_ENTRIES = 1;
	private static int TYPE_SPECS = 2;
	private static int TYPE_TAKE_STOCK_RAPIDLY = 3;
	private static int TYPE_POSITION_COUNT = 8;
	private static int TYPE_POSITIONS = 4;
	private static int TYPE_PD_DETAILS = 5;
	private static int TYPE_PD_DETAIL_COUNT = 6;
	private static int TYPE_PD_SPECS = 7;
	private static int TYPE_SUPPLIERS = 9;
	private static int TYPE_PRICE = 10;
	private static int TYPE_LOGISTICS = 11;
	private static int TYPE_TRADE_INFO = 12;
	private static int TYPE_PICKERS = 13;
	private static int TYPE_TRADE_GOODS = 14;
	private static int TYPE_IN_EXAM_SPECS = 15;
	private static int TYPE_SUPPLIER_COUNT = 16;
	private static int TYPE_SHOP = 17;
	private static int TYPE_CASH_SALE_SPEC = 18;
	private static int TYPE_INTERFACE_WAREHOUSES = 19;
	private static int TYPE_CASH_SALE_SPEC_STOCK = 20;
	private static int TYPE_CUSTOMER = 21;
	private static int TYPE_CASH_SALE_SPECS_BY_TERM = 22;
 	
	protected static WDTQuery instance = null;
	
	private ExecutorService mCachedThreadPool;
	
	public WDTQuery() {
		mCachedThreadPool = Executors.newCachedThreadPool();
	}
	
	public static WDTQuery getinstance() {
		
		if(null == instance)
			instance = new WDTQuery();
		
		return instance;
	}
	
	public void queryWarehouses(Context ctx, QueryCallBack cb) {
		
		Params p = new Params();
		p.type = WDTQuery.TYPE_WAREHOUSES;

		Thread queryThread = new Thread(new QueryRunnable(p, cb));
		queryThread.start();
	}
	
	public void queryPdEntries(Context ctx, QueryCallBack cb, int warehouseId) {
		
		Params p = new Params();
		p.type = WDTQuery.TYPE_PD_ENTRIES;
		p.warehouseId = warehouseId;
		
		Thread queryThread = new Thread(new QueryRunnable(p, cb));
		queryThread.start();
	}
	
	public void querySpecs(Context ctx, QueryCallBack cb, int warehouseId, String barcode) {
		Params p = new Params();
		p.type = WDTQuery.TYPE_SPECS;
		p.warehouseId = warehouseId;
		p.barcode = barcode;
		
		Thread queryThread = new Thread(new QueryRunnable(p, cb));
		queryThread.start();
	}
	
	public void queryTakeStockRapidly(Context ctx, QueryCallBack cb, int warehouseId, String barcode) {
		Params p = new Params();
		p.type = WDTQuery.TYPE_TAKE_STOCK_RAPIDLY;
		p.warehouseId = warehouseId;
		p.barcode = barcode;
		
		Thread queryThread = new Thread(new QueryRunnable(p, cb));
		queryThread.start();
	}
	
	public void queryPositionCount(Context ctx, QueryCallBack callBack, int warehouseId) {
		Params params = new Params();
		params.type = TYPE_POSITION_COUNT;
		params.warehouseId = warehouseId;
		
		Thread queryThread = new Thread(new QueryRunnable(params, callBack));
		queryThread.start();
	}
	
	public void queryPositions(Context ctx, QueryCallBack callBack, int warehouseId, int pos, int pageSize) {
		Params params = new Params();
		params.type = TYPE_POSITIONS;
		params.pos = pos;
		params.pageSize = pageSize;
		params.warehouseId = warehouseId;
		
		Thread queryThread = new Thread(new QueryRunnable(params, callBack));
		queryThread.start();
	}
	
	public void queryPdDetails(Context ctx, QueryCallBack callBack, int pdId, 
			int pos, int pageSize) {
		Params params = new Params();
		params.type = TYPE_PD_DETAILS;
		params.pdId = pdId;
		params.pos = pos;
		params.pageSize = pageSize;
		
		mCachedThreadPool.execute(new QueryRunnable(params, callBack));
	}
	
	public void queryPdDetailCount(Context ctx, QueryCallBack callBack, int pdId) {
		Params params = new Params();
		params.type = TYPE_PD_DETAIL_COUNT;
		params.pdId = pdId;
		
		mCachedThreadPool.execute(new QueryRunnable(params, callBack));
	}
	
	public void queryPdSpecs(Context ctx, QueryCallBack callBack, int pdId, 
			String barcode) {
		Params params = new Params();
		params.type = TYPE_PD_SPECS;
		params.pdId = pdId;
		params.barcode = barcode;
		
		Thread queryThread = new Thread(new QueryRunnable(params, callBack));
		queryThread.start();
	}
	
	public void querySuppliers(Context context, QueryCallBack callBack, int pos, int pageSize) {
		Params params = new Params();
		params.type = TYPE_SUPPLIERS;
		params.pos = pos;
		params.pageSize = pageSize;
		
		Thread queryThread = new Thread(new QueryRunnable(params, callBack));
		queryThread.start();
	}
	
	public void queryPrice(Context context, QueryCallBack callBack, int specId, int providerId) {
		Params params = new Params();
		params.type = TYPE_PRICE;
		params.specId = specId;
		params.providerId = providerId;
		
		Thread queryThread = new Thread(new QueryRunnable(params, callBack));
		queryThread.start();
	}
	
	public void queryLogistics(Context context, QueryCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_LOGISTICS;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryTradeInfo(Context context, QueryCallBack callBack, String tradeOrPost) {
		Params params = new Params();
		params.type = TYPE_TRADE_INFO;
		params.tradeOrPost = tradeOrPost;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryPickers(Context context, QueryCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_PICKERS;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryTradeGoods(Context context, QueryCallBack callBack, int tradeId, int warehouseId) {
		Params params = new Params();
		params.type = TYPE_TRADE_GOODS;
		params.tradeId = tradeId;
		params.warehouseId = warehouseId;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryInExamSpecs(Context context, QueryCallBack callBack, String barcode) {
		Params params = new Params();
		params.type = TYPE_IN_EXAM_SPECS;
		params.barcode = barcode;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void querySupplierCount(Context context, QueryCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_SUPPLIER_COUNT;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryShops(Context context, QueryCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_SHOP;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryCashSaleSpecs(Context context, QueryCallBack callBack, int warehouseId, String barcode) {
		Params params = new Params();
		params.type = TYPE_CASH_SALE_SPEC;
		params.warehouseId = warehouseId;
		params.barcode = barcode;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryInterfaceWarehouses(Context context, QueryCallBack callback, String interfaceId) {
    	Params params = new Params();
    	params.type = TYPE_INTERFACE_WAREHOUSES;
    	params.interfaceId = interfaceId;
    	
    	new Thread(new QueryRunnable(params, callback)).start();
	}
	
	public void queryCashSaleSpecStock(QueryCallBack callBack, int specId, int warehouseId) {
		Params params = new Params();
		params.type = TYPE_CASH_SALE_SPEC_STOCK;
		params.specId = specId;
		params.warehouseId = warehouseId;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryCustomers(QueryCallBack callBack, String tel) {
		Params params = new Params();
		params.type = TYPE_CUSTOMER;
		params.tel = tel;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public void queryCashSaleSpecsByTerm(QueryCallBack callBack, int warehouseId, String searchTerm) {
		Params params = new Params();
		params.type = TYPE_CASH_SALE_SPECS_BY_TERM;
		params.warehouseId = warehouseId;
		params.searchTerm = searchTerm;
		
		new Thread(new QueryRunnable(params, callBack)).start();
	}
	
	public class QueryRunnable implements Runnable {

		private Params mParams;
		private QueryCallBack mCallBack;
		
		OutputStream ops = SocketService.ops;
		InputStream ips = SocketService.ips;
		int recvLen;
		byte[] recvBuf = new byte[8192];
		
		public QueryRunnable(Params p, QueryCallBack cb) {
			mParams = p;
			mCallBack = cb;
		}
		
		public void run() {
		
			if (!SocketService.isConnected()) {
				mCallBack.onQueryFail(HandlerCases.NO_CONN, null);
				return;
			}
			
			if (ops == null || ips == null) {
				ServicePool.getinstance().getEventCenter().fireEvent(this, Events.TIME_OUT);
				return;
			}
			
			try {
				// Query, prepare
				byte[] bytes1 = null;
				if (TYPE_WAREHOUSES == mParams.type) {
					bytes1 = preGetWarehouses();
				} else if (TYPE_PD_ENTRIES == mParams.type) {
					bytes1 = preGetPdEntries(mParams.warehouseId);
				} else if (TYPE_SPECS == mParams.type) {
					bytes1 = preGetSpecs(mParams.warehouseId, mParams.barcode);
				} else if (TYPE_POSITION_COUNT == mParams.type) {
					bytes1 = preGetPositionCount(mParams.warehouseId);
				} else if (TYPE_POSITIONS == mParams.type) {
					bytes1 = preGetPositions(mParams.warehouseId, mParams.pos, mParams.pageSize);
				} else if (TYPE_PD_DETAILS == mParams.type) {
					bytes1 = preGetPdDetails(mParams.pdId, mParams.pos, mParams.pageSize);
				} else if (TYPE_PD_SPECS == mParams.type) {
					bytes1 = preGetPdSpecs(mParams.pdId, mParams.barcode);
				} else if (TYPE_PD_DETAIL_COUNT == mParams.type) {
					bytes1 = preGetPdDetailCount(mParams.pdId);
				} else if (TYPE_SUPPLIERS == mParams.type) {
					bytes1 = preGetSuppliers(mParams.pos, mParams.pageSize);
				} else if (TYPE_PRICE == mParams.type) {
					bytes1 = preGetPrice(mParams.specId, mParams.providerId);
				} else if (TYPE_LOGISTICS == mParams.type) {
					bytes1 = preGetLogistics();
				} else if (TYPE_TRADE_INFO == mParams.type) {
					bytes1 = preGetTradeInfo(mParams.tradeOrPost);
				} else if (TYPE_PICKERS == mParams.type) {
					bytes1 = preGetPickers();
				} else if (TYPE_TRADE_GOODS == mParams.type) {
					bytes1 = preGetTradeGoods(mParams.tradeId, mParams.warehouseId);
				} else if (TYPE_IN_EXAM_SPECS == mParams.type) {
					bytes1 = preGetInExamSpecs(mParams.barcode);
				} else if (TYPE_SUPPLIER_COUNT == mParams.type) {
					bytes1 = preGetSupplierCount();
				} else if (TYPE_SHOP == mParams.type) {
					bytes1 = preGetShops();
				} else if (TYPE_CASH_SALE_SPEC == mParams.type) {
					bytes1 = preGetCashSaleSpecs(mParams.warehouseId, mParams.barcode);
				} else if (TYPE_INTERFACE_WAREHOUSES == mParams.type) {
			    	bytes1 = preGetInterfaceWarehouses(mParams.interfaceId);
				} else if (TYPE_CASH_SALE_SPEC_STOCK == mParams.type) {
					bytes1 = preGetCashSaleSpecStock(mParams.specId, mParams.warehouseId);
				} else if (TYPE_CUSTOMER == mParams.type) {
					bytes1 = preGetCustomers(mParams.tel);
				} else if (TYPE_CASH_SALE_SPECS_BY_TERM == mParams.type) {
					bytes1 = preGetCashSaleSpecsByTerm(mParams.warehouseId, mParams.searchTerm);
				}
				
				// Send
				if (null == bytes1) {
					Log.e("Query", "prepare query return null");
					mCallBack.onQueryFail(HandlerCases.PREPARE_QUERY_ERROR, null);
					return;
				}
				ops.write(bytes1);
				
				// Receive
				Log.d("Time out", "Before read");
				recvLen = ips.read(recvBuf);
				Log.d("Time out", "Read success");
				
				// Query, get value
				Object result = null;
				if (TYPE_WAREHOUSES == mParams.type) {
					result = getWarehouses(recvBuf);
				} else if (TYPE_PD_ENTRIES == mParams.type) {
					result = getPdEntries(recvBuf);
				} else if (TYPE_SPECS == mParams.type) {
					result = getSpecs(recvBuf);
				} else if (TYPE_POSITION_COUNT == mParams.type) {
					int count = getPositionCount(recvBuf);
					result = Integer.valueOf(count);
				} else if (TYPE_POSITIONS == mParams.type) {
					result = getPositions(recvBuf);
				} else if (TYPE_PD_DETAIL_COUNT == mParams.type) {
					int count = getPdDetailCount(recvBuf);
					result = Integer.valueOf(count);
				} else if (TYPE_PD_DETAILS == mParams.type) {
					result = getPdDetails(recvBuf);
				} else if (TYPE_PD_SPECS == mParams.type) {
					result = getPdSpecs(recvBuf);
				} else if (TYPE_SUPPLIERS == mParams.type) {
					result = getSuppliers(recvBuf);
				} else if (TYPE_PRICE == mParams.type) {
					result = getPrice(recvBuf);
				} else if (TYPE_LOGISTICS == mParams.type) {
					result = getLogistics(recvBuf);
				} else if (TYPE_TRADE_INFO == mParams.type) {
					result = getTradeInfo(recvBuf);
				} else if (TYPE_PICKERS == mParams.type) {
					result = getPickers(recvBuf);
				} else if (TYPE_TRADE_GOODS == mParams.type) {
					result = getTradeGoods(recvBuf);
				} else if (TYPE_IN_EXAM_SPECS == mParams.type) {
					result = getInExamSpecs(recvBuf);
				} else if (TYPE_SUPPLIER_COUNT == mParams.type) {
					int count = getSupplierCount(recvBuf);
					result = Integer.valueOf(count);
				} else if (TYPE_SHOP == mParams.type) {
					result = getShops(recvBuf);
				} else if (TYPE_CASH_SALE_SPEC == mParams.type || TYPE_CASH_SALE_SPECS_BY_TERM == mParams.type) {
					result = getCashSaleSpecs(recvBuf);
				} else if (TYPE_INTERFACE_WAREHOUSES == mParams.type) {
                    result = getWarehouses(recvBuf);
				} else if (TYPE_CASH_SALE_SPEC_STOCK == mParams.type) {
					result = getCashSaleSpecStock(recvBuf);
				} else if (TYPE_CUSTOMER == mParams.type) {
					result = getCustomers(recvBuf);
				}
				
				mCallBack.onQuerySuccess(result);
			} catch (IOException e) {
				System.out.println("IO Exception");
				System.out.println("fireEvents");
				if (null == ServicePool.getinstance().getEventCenter()) {
					System.out.println("null == ServicePool.getinstance().getEventCenter()");
				} else {
					System.out.println("null != ServicePool.getinstance().getEventCenter()");
				}
				if (!SocketService.isConnected()) {
					mCallBack.onQueryFail(HandlerCases.NO_CONN, null);
					return;
				}
				ServicePool.getinstance().getEventCenter().fireEvent(this, Events.TIME_OUT);
			} catch (WDTException wdtEx) {
				if (wdtEx.getStatus() > 20 || wdtEx.getStatus() < 0) {
					System.out.println("fireEvents");
					if (null == ServicePool.getinstance().getEventCenter()) {
						System.out.println("null == ServicePool.getinstance().getEventCenter()");
					} else {
						System.out.println("null != ServicePool.getinstance().getEventCenter()");
					}
					ServicePool.getinstance().getEventCenter().fireEvent(this, Events.DBE_INVALID_PACKET);
					return;
				}
				Log.e("WDTException", "status = " + wdtEx.getStatus() + ": " + 
					wdtEx.getMessage());
				mCallBack.onQueryFail(-1, wdtEx);
			}
		}
	}
}
