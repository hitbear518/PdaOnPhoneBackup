package com.zsxj.pda.wdt;

import java.util.ArrayList;
import java.util.List;

import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.util.Util;

import android.database.Cursor;
import android.util.SparseArray;


public class MergedSpec {

	public final int[] recIds;
	public final int specId;
	public final String goodsNum;
	public final String goodsName;
	public final String specCode;
	public final String specName;
	public final String specBarcode;
	public final int[] positionIds;
	public final String[] positionNames;
	public final String[] stocks;
	public final String[] stockOlds;
	public final String[] stockPds;
	
	public MergedSpec(int specId, String goodsNum, String goodsName,
			String specCode, String specName, String specBarcode,
			int[] positionIds, String[] positionNames, String[] stocks) {
		super();
		this.specId = specId;
		this.goodsNum = goodsNum;
		this.goodsName = goodsName;
		this.specCode = specCode;
		this.specName = specName;
		this.specBarcode = specBarcode;
		this.positionIds = positionIds;
		this.positionNames = positionNames;
		this.stocks = stocks;
		
		recIds = null;
		stockOlds = null;
		stockPds = null;
	}
	
	
	
	public MergedSpec(int[] recIds, int specId, String goodsNum, String goodsName,
			String specCode, String specName, String specBarcode,
			int[] positionIds, String[] positionNames, String[] stockOlds,
			String[] stockPds) {
		super();
		this.recIds = recIds;
		this.specId = specId;
		this.goodsNum = goodsNum;
		this.goodsName = goodsName;
		this.specCode = specCode;
		this.specName = specName;
		this.specBarcode = specBarcode;
		this.positionIds = positionIds;
		this.positionNames = positionNames;
		this.stockOlds = stockOlds;
		this.stockPds = stockPds;
		
		stocks = null;
	}

	public static MergedSpec[] mergeSpecs(Spec[] specs) {
		SparseArray<List<Spec>> sparseArr = new SparseArray<List<Spec>>();
		for (int i = 0; i < specs.length; i++) {
			List<Spec> specList = sparseArr.get(specs[i].specId);
			if (null == specList) {
				specList = new ArrayList<Spec>();
				sparseArr.put(specs[i].specId, specList);
			}
			specList.add(specs[i]);
		}
		
		MergedSpec[] mergedSpecs = new MergedSpec[sparseArr.size()];
		for (int i = 0; i < mergedSpecs.length; i++) {
			List<Spec> specList = sparseArr.valueAt(i);
			int size = specList.size();
			final int[] positionIds = new int[size];
			final String[] positionNames = new String[size];
			final String[] stocks = new String[size];
			for(int j = 0; j < size; j++) {
				positionIds[j] = specList.get(j).positionId;
				positionNames[j] = specList.get(j).positionName;
				stocks[j] = Util.trimDouble(specList.get(j).stock);
			}
			Spec spec = specList.get(0);
			MergedSpec mergedSpec = new MergedSpec(spec.specId, spec.goodsNum, 
					spec.goodsName, spec.specCode, spec.specName, spec.specBarcode, 
					positionIds, positionNames, stocks);
			mergedSpecs[i] = mergedSpec;
		}
		
		return mergedSpecs;
	}
	
	public static MergedSpec[] mergePdSpecs(Cursor specs) {
		SparseArray<List<Spec>> sparseArr = new SparseArray<List<Spec>>();
		int recIdIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_REC_ID);
		int specIdIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_SPEC_ID);
		int specBarcodeIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_SPEC_BARCODE);
		int goodsNumIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_GOODS_NUM);
		int goodsNameIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_GOODS_NAME);
		int barcodeIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_BARCODE);
		int specCodeIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_SPEC_CODE);
		int specNameIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_SPEC_NAME);
		int positionIdIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_POSITION_ID);
		int positionNameIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_POSITION_NAME);
		int stockOldIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_STOCK_OLD);
		int stockPdIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_STOCK_PD);
		int remarkIdx = specs.getColumnIndex(PdInfo.COLUMN_NAME_REMARK);
		if (specs.getCount() <= 0) {
			return null;
		}
		while (specs.moveToNext()) {
			List<Spec> specList = sparseArr.get(specs.getInt(specIdIdx));
			if (null == specList) {
				specList = new ArrayList<Spec>();
				sparseArr.put(specs.getInt(specIdIdx), specList);
			}
			Spec spec = new Spec(specs.getInt(recIdIdx), 
					specs.getInt(specIdIdx),
					specs.getString(specBarcodeIdx), 
					specs.getString(goodsNumIdx), 
					specs.getString(goodsNameIdx), 
					specs.getString(barcodeIdx), 
					specs.getString(specCodeIdx), 
					specs.getString(specNameIdx), 
					specs.getInt(positionIdIdx), 
					specs.getString(positionNameIdx), 
					specs.getString(stockOldIdx), 
					specs.getString(stockPdIdx), 
					specs.getString(remarkIdx));
			specList.add(spec);
		}
		MergedSpec[] mergedSpecs = new MergedSpec[sparseArr.size()];
		for (int i = 0; i < mergedSpecs.length; i++) {
			List<Spec> specList = sparseArr.valueAt(i);
			int size = specList.size();
			final int[] recIds = new int[size];
			final int[] positionIds = new int[size];
			final String[] positionNames = new String[size];
			final String[] stockOlds = new String[size];
			final String[] stockPds = new String[size];
			for(int j = 0; j < size; j++) {
				recIds[j] = specList.get(j).recId;
				positionIds[j] = specList.get(j).positionId;
				positionNames[j] = specList.get(j).positionName;
				stockOlds[j] = specList.get(j).stockOld;
				stockPds[j] = specList.get(j).stockPd;
			}
			Spec spec = specList.get(0);
			MergedSpec mergedSpec = new MergedSpec(recIds, spec.specId, spec.goodsNum, 
					spec.goodsName, spec.specCode, spec.specName, spec.specBarcode, 
					positionIds, positionNames, stockOlds, stockPds);
			mergedSpecs[i] = mergedSpec;
		}
		
		return mergedSpecs;
	}
	
	public static MergedSpec[] noMerge(Spec[] specs) {
		MergedSpec[] mergedSpecs = new MergedSpec[specs.length];
		for (int i = 0; i < specs.length; i++) {
			mergedSpecs[i] = new MergedSpec(specs[i].specId, specs[i].goodsNum, specs[i].goodsName, 
				specs[i].specCode, specs[i].specName, specs[i].specBarcode, null, null, null);
		}
		return mergedSpecs;
	}
}
