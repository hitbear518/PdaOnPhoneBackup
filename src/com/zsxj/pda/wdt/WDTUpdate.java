package com.zsxj.pda.wdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.SocketService;
import com.zsxj.pda.util.ConstParams.Events;
import com.zsxj.pda.util.ConstParams.HandlerCases;

public class WDTUpdate {

	static {
		System.loadLibrary("wdt_tdi");
	}
	
	public interface UpdateCallBack {
		public void onUpdateSuccess();
		public void onUpdateFail(int type, WDTException wdtEx); 
	}
	
	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
	
	private native byte[] preFastPd(int userId, int warehouseId, String inforStr, int accountId) throws WDTException;
	private native byte[] preSetStock(int warehouseId, int specId, int positionId, String stock) throws WDTException;
	private native byte[] preNewPosition(int warehouseId, int specId, int positionId) throws WDTException;
	private native byte[] prePdSubmit(int userId, int pdId, int accountId, 
			int bLast, String infoStr) throws WDTException;
	private native byte[] preFastInExamineGoodsSubmit(int userId, int warehouseId,
		int supplierId, int logisticId, int accountId, String otherFee,
		String goodsTotal, String allTotal, String postageFee, String cashFee,
		String postId, int bLast, String infoStr) throws WDTException;
	private native byte[] preStockOut(int tradeId, int pickerId, int scannerId);
	private native byte[] preUpdateTradeBscan(int tradeId);
	private native byte[] prePickError(int tradeId);
	private native void completeUpdate(byte[] buf) throws WDTException;
	
	
	private static final int TYPE_FAST_PD = 0;
	private static final int TYPE_STOCK_TRANSFER = 1;
	private static final int TYPE_NEW_POSITION = 2;
	private static final int TYPE_PD_SUBMIT = 3;
	private static final int TYPE_FAST_IN_EXAMINE_GOODS_PD = 4;
	private static final int TYPE_STOCK_OUT = 5;
	private static final int TYPE_PICK_ERROR = 6;
	
	public void fastPd(int userId, int warehouseId, String infoStr, int accountId, UpdateCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_FAST_PD;
		params.userId = userId;
		params.warehouseId = warehouseId;
		params.infoStr = infoStr;
		params.accountId = accountId;
		
		Thread thread = new Thread(new Thread(new UpdateRunnable(params, callBack)));
		thread.start();
	}
	
	public void stockTransfer(int warehouseId, int specId, int outPositionId, 
			int inPositionId, String outStock, String inStock, 
			UpdateCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_STOCK_TRANSFER;
		params.warehouseId = warehouseId;
		params.specId = specId;
		params.outPositionId = outPositionId;
		params.inPositionId = inPositionId;
		params.outStock = outStock;
		params.inStock = inStock;
		
		Thread thread = new Thread(new Thread(new UpdateRunnable(params, callBack)));
		thread.start();
	}
	
	public void newPosition(int warehouseId, int specId, int positionId,
			UpdateCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_NEW_POSITION;
		params.warehouseId = warehouseId;
		params.specId = specId;
		params.positionId = positionId;
		
		Thread thread = new Thread(new Thread(new UpdateRunnable(params, callBack)));
		thread.start();
	}
	
	public void pdSubmit(int userId, int pdId, int accountId, 
			List<String> infoStrList, UpdateCallBack callBack) {
		Params params = new Params();
		params.type = TYPE_PD_SUBMIT;
		params.userId = userId;
		params.pdId = pdId;
		params.accountId = accountId;
		params.infoStrList = infoStrList;
		
		singleThreadExecutor.execute(new UpdateRunnable(params, callBack));
	}
	
	public void fastInExamineGoodsSubmit(int userId, int warehouseId,
		int supplierId, int logisticId, int accountId, String otherFee,
		String goodsTotal, String postageFee, String cashFee, String postId,
		List<String> infoStrList, UpdateCallBack callBack) {
		
		Params params = new Params();
		params.type = TYPE_FAST_IN_EXAMINE_GOODS_PD;
		params.userId = userId;
		params.warehouseId = warehouseId;
		params.supplierId = supplierId;
		params.logisticId = logisticId;
		params.accountId = accountId;
		params.otherFee = otherFee;
		params.goodsTotal = goodsTotal;
		params.postageFee = postageFee;
		params.cashFee = cashFee;
		params.postId = postId;
		params.infoStrList = infoStrList;
		
		singleThreadExecutor.execute(new UpdateRunnable(params, callBack));
	}
	
	public void stockOut(UpdateCallBack callBack, int tradeId, int pickerId, 
		int scannerId) {
		Params params = new Params();
		params.type = TYPE_STOCK_OUT;
		params.tradeId = tradeId;
		params.pickerId = pickerId;
		params.userId = scannerId;
		
		singleThreadExecutor.execute(new UpdateRunnable(params, callBack));
	}
	
	public void pickError(UpdateCallBack callBack, int tradeId) {
		Params params = new Params();
		params.type = TYPE_PICK_ERROR;
		params.tradeId = tradeId;
		
		singleThreadExecutor.execute(new UpdateRunnable(params, callBack));
	}
	
	public class UpdateRunnable implements Runnable {
		
		private Params mParams;
		private UpdateCallBack mCallBack;
		
		OutputStream ops = SocketService.ops;
		InputStream ips = SocketService.ips;
		int recvLen;
		byte[] recvBuf = new byte[8192];
		
		public UpdateRunnable(Params p, UpdateCallBack cb) {
			mParams = p;
			mCallBack = cb;
		}

		@Override
		public void run() {
			if (!SocketService.isConnected()) {
				mCallBack.onUpdateFail(HandlerCases.NO_CONN, null);
				return;
			}
			
			if (ops == null || ips == null) {
				ServicePool.getinstance().getEventCenter().fireEvent(this, Events.TIME_OUT);
				return;
			}
			
			try {
				byte[] bytes1 = null;
				if (mParams.type == TYPE_FAST_PD) {
					bytes1 = preFastPd(mParams.userId, mParams.warehouseId, mParams.infoStr, mParams.accountId);
				} else if (mParams.type == TYPE_STOCK_TRANSFER) {
					bytes1 = preSetStock(mParams.warehouseId, mParams.specId, 
							mParams.outPositionId, mParams.outStock);
				} else if (TYPE_NEW_POSITION == mParams.type) {
					bytes1 = preNewPosition(
							mParams.warehouseId, mParams.specId, mParams.positionId);
				} else if (TYPE_PD_SUBMIT == mParams.type) {
					int size = mParams.infoStrList.size();
					for (int i = 0; i < size - 1; i++) {
						bytes1 = prePdSubmit(mParams.userId, mParams.pdId, 
								mParams.accountId, 0, mParams.infoStrList.get(i));
						if (null == bytes1) {
							mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
							return;
						}
						ops.write(bytes1);
						recvLen = ips.read(recvBuf);
						completeUpdate(recvBuf);
					}
					bytes1 = prePdSubmit(mParams.userId, mParams.pdId, 
							mParams.accountId, 1, mParams.infoStrList.get(size - 1));
					if (null == bytes1) {
						mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
						return;
					}
					ops.write(bytes1);
					recvLen = ips.read(recvBuf);
					completeUpdate(recvBuf);
					
					mCallBack.onUpdateSuccess();
					return;
				} else if (TYPE_FAST_IN_EXAMINE_GOODS_PD == mParams.type) {
					int size = mParams.infoStrList.size();
					Double otherFee = Double.valueOf(mParams.otherFee);
					Double goodsTotal = Double.valueOf(mParams.goodsTotal);
					Double allTotal = otherFee + goodsTotal;
					for (int i = 0; i < size - 1; i++) { 
						bytes1 = preFastInExamineGoodsSubmit(mParams.userId, 
							mParams.warehouseId, mParams.supplierId, mParams.logisticId, 
							mParams.accountId, mParams.otherFee, mParams.goodsTotal, 
							allTotal.toString(), mParams.postageFee, mParams.cashFee, 
							mParams.postId, 0, mParams.infoStrList.get(i));
						if (null == bytes1) {
							mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
							return;
						}
						ops.write(bytes1);
						recvLen = ips.read(recvBuf);
						completeUpdate(recvBuf);
					}
					bytes1 = preFastInExamineGoodsSubmit(mParams.userId, 
						mParams.warehouseId, mParams.supplierId, mParams.logisticId, 
						mParams.accountId, mParams.otherFee, mParams.goodsTotal, 
						allTotal.toString(), mParams.postageFee, mParams.cashFee, 
						mParams.postId, 1, mParams.infoStrList.get(size - 1));
					
					if (null == bytes1) {
						mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
						return;
					}
					ops.write(bytes1);
					recvLen = ips.read(recvBuf);
					completeUpdate(recvBuf);
					
					mCallBack.onUpdateSuccess();
					return;
				} else if (TYPE_STOCK_OUT == mParams.type) {
					bytes1 = preStockOut(mParams.tradeId, mParams.pickerId, 
						mParams.userId);
				} else if (TYPE_PICK_ERROR == mParams.type) {
					bytes1 = prePickError(mParams.tradeId);
				}
				
				// Send
				if (null == bytes1) {
					mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
					return;
				}
				ops.write(bytes1);
				recvLen = ips.read(recvBuf);	
				completeUpdate(recvBuf);
				
				if (mParams.type == TYPE_FAST_PD) {
					mCallBack.onUpdateSuccess();
					return;
				} else if (mParams.type == TYPE_STOCK_TRANSFER) {
					bytes1 = preSetStock(mParams.warehouseId, mParams.specId, 
							mParams.inPositionId, mParams.inStock);
				} else if (TYPE_NEW_POSITION == mParams.type) {
					mCallBack.onUpdateSuccess();
					return;
				} else if (TYPE_STOCK_OUT == mParams.type) {
					bytes1 = preUpdateTradeBscan(mParams.tradeId);
				} else if (TYPE_PICK_ERROR == mParams.type) {
					mCallBack.onUpdateSuccess();
					return;
				}
				
				// Send
				if (null == bytes1) {
					mCallBack.onUpdateFail(HandlerCases.PREPARE_UPDATE_FAIL, null);
					return;
				}
				ops.write(bytes1);
				recvLen = ips.read(recvBuf);	
				completeUpdate(recvBuf);
				
				mCallBack.onUpdateSuccess();
			} catch (IOException e) {
				System.out.println("IO Exception");
				System.out.println("fireEvents");
				if (null == ServicePool.getinstance().getEventCenter()) {
					System.out.println("null == ServicePool.getinstance().getEventCenter()");
				} else {
					System.out.println("null != ServicePool.getinstance().getEventCenter()");
				}
				ServicePool.getinstance().getEventCenter().fireEvent(this, Events.TIME_OUT);
			} catch (WDTException wdtEx) {
				if (wdtEx.getStatus() == 24) {
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
				mCallBack.onUpdateFail(-1, wdtEx);
			}
		}
	}
}
