package com.zsxj.pda.wdt;

import java.util.ArrayList;
import java.util.List;

public class QueryResult {
	
	public static final int COLUMN_TYPE_INT = 1;
	public static final int COLUMN_TYPE_String = 2;

	private int mColumnCount;
	private int mRowCount;
	private String[] mColumnNames;
	private int[] mColumnTypes;
	private List<Object[]> mData;
	
	public QueryResult() {
		mColumnCount = 0;
		mRowCount = 0;
		mColumnNames = null;
		mColumnTypes = null;
		mData = new ArrayList<Object[]>();
	}
	
	public void setColumnCount(int columnCount) {
		mColumnCount = columnCount;
		mColumnNames = new String[columnCount];
		mColumnTypes = new int[columnCount];
	}

	public void setColumnName(int i, String name) {
		mColumnNames[i] = name;
	}

	public void setColumnType(int i, int type) {
		mColumnTypes[i] = type;
	}
	
	public void addNewRow() {
		mData.add(new Object[mColumnCount]);
		mRowCount++;
	}
	
	public void setInt(int row, int column, int value) {
		Object[] objects = mData.get(row);
		objects[column] = value;
	}
	
	public void setString(int row, int column, String value) {
		Object[] objects = mData.get(row);
		objects[column] = value;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("row = " + mRowCount + " column = " + mColumnCount + "\n");
		for (int i = 0; i < mRowCount; i++) {
			sb.append("row " + i + ": ");
			for (int j = 0; j < mColumnCount; j++) {
				sb.append("column" + j + "--");
				switch (mColumnTypes[j]) {
				case COLUMN_TYPE_INT:
					sb.append("(int)");
					break;
				case COLUMN_TYPE_String:
					sb.append("(String)");
					break;
				default:
					sb.append("(error)");
					break;
				}
				sb.append("(" + mColumnNames[j] + ")" + mData.get(i)[j] + " ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
