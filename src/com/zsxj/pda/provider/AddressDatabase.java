package com.zsxj.pda.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;



public class AddressDatabase extends SQLiteAssetHelper {
	
	public static final String PROVINCE_ID = "ProvinceID";
	public static final String PROVINCE_NAME = "ProvinceName";
	public static final String CITY_ID = "CityID";
	public static final String CITY_NAME = "CityName";
	public static final String DISTRICT_ID = "DistrictID";
	public static final String DISTRICT_NAME = "DistrictName";
	
	private static final String DATABASE_NAME = "address.db";
	private static final int DATABASE_VERSION = 1;

	public AddressDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public Cursor getProvinces() {
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String[] projectionIn = {
			"0 _id",
			PROVINCE_ID,
			PROVINCE_NAME	
		};
		
		qb.setTables("province"); 
		Cursor c = qb.query(db, projectionIn, null, null, null, null, null);		
		c.moveToFirst();
		return c;
	}
	
	public Cursor getCities(int provinceId) {
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String[] projectionIn = {
			"0 _id",
			CITY_ID,
			CITY_NAME	
		};
		String selection = PROVINCE_ID + "=?";
		String[] selectionArgs = {
			provinceId + ""
		};
		
		qb.setTables("city"); 
		Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, null);		
		c.moveToFirst();
		return c;
	}
	
	public Cursor getDistricts(int cityId) {
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String[] projectionIn = {
			"0 _id",
			DISTRICT_ID,
			DISTRICT_NAME
		};
		String selection = CITY_ID + "=?";
		String[] selectionArgs = {
			cityId + ""
		};
		
		qb.setTables("district"); 
		Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, null);		
		c.moveToFirst();
		return c;
	}
}
