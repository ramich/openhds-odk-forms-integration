package org.openhds.test.model;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmittedEntry{
	private ResultSetMetaData rsmt;
	public Map<String, String> dataTuple;
	public int columnCount;
	private List<String> catalogNames;
	private List<String> columnClassNames;
	private List<String> columnLabels;
	public List<String> columnNames;
	public List<String> columnTypeNames;
	private List<String> schemaNames;
	private List<String> tableNames;
	private List<Integer> columnDisplaySizes;
	private List<Integer> columnTypes;
	private List<Integer> precisions;
	private List<Integer> scales;
	
	public SubmittedEntry(ResultSetMetaData metaData){
		
		init();
		
		this.rsmt = metaData;
		
		try{
			saveMetaData(this.rsmt);
		}
		catch(SQLException sqlE){
			sqlE.printStackTrace();
		}
	}
	
	private void init(){
		dataTuple = new HashMap<String, String>();
		catalogNames = new ArrayList<String>();
		columnClassNames = new ArrayList<String>();
		columnLabels = new ArrayList<String>();
		columnNames = new ArrayList<String>();
		columnTypeNames = new ArrayList<String>();
		schemaNames = new ArrayList<String>();
		tableNames = new ArrayList<String>();
		columnDisplaySizes = new ArrayList<Integer>();
		columnTypes = new ArrayList<Integer>();
		precisions = new ArrayList<Integer>();
		scales = new ArrayList<Integer>();
	}
	
	private void saveMetaData(ResultSetMetaData metaData) throws SQLException{
		this.columnCount = metaData.getColumnCount();
		
		for(int i = 1; i <= metaData.getColumnCount(); i++){
			
			String catalogName = metaData.getCatalogName(i);
			String columnClassName = metaData.getColumnClassName(i);
			String columnLabel = metaData.getColumnLabel(i);
			String columnName = metaData.getColumnName(i);
			String columnTypeName = metaData.getColumnTypeName(i);
			String schemaName = metaData.getSchemaName(i);
			String tableName = metaData.getTableName(i);
			
			int columnDisplaySize = metaData.getColumnDisplaySize(i);
			int columnType = metaData.getColumnType(i);
			int precision = metaData.getPrecision(i);
			int scale = metaData.getScale(i);
			
			catalogNames.add(catalogName);
			columnClassNames.add(columnClassName);
			columnLabels.add(columnLabel);
			columnNames.add(columnName);
			columnTypeNames.add(columnTypeName);
			schemaNames.add(schemaName);
			tableNames.add(tableName);
			columnDisplaySizes.add(columnDisplaySize);
			columnTypes.add(columnType);
			precisions.add(precision);
			scales.add(scale);
			
//			System.out.println();
//			System.out.println("catalogName: " + catalogName);
//			System.out.println("columnClassName: " + columnClassName);
//			System.out.println("columnDisplaySize: " + columnDisplaySize);
//			System.out.println("columnLabel: " + columnLabel);
//			System.out.println("columnName: " + columnName);
//			System.out.println("columnTypeName: " + columnTypeName);
//			System.out.println("schemaName: " + schemaName);
//			System.out.println("tableName: " + tableName);
//			System.out.println("columnType: " + columnType);
//			System.out.println("precision: " + precision);
//			System.out.println("scale: " + scale);
		}
	}
	
	public void addEntry(String columnName, String value){
		if(!dataTuple.containsKey(columnName)){
			dataTuple.put(columnName, value);
		}
		else{
			System.err.println("Key already inserted! " + columnName);
		}
	}
	
	public void print(){
		
		boolean printMetaData = false;
		
		for(int i = 0; i < columnCount; i++ ){
			
			String catalogName = catalogNames.get(i);
			String columnClassName = columnClassNames.get(i);
			String columnLabel = columnLabels.get(i);
			String columnName = columnNames.get(i);
			String columnTypeName = columnTypeNames.get(i);
			String schemaName = schemaNames.get(i);
			String tableName = tableNames.get(i);
			
			int columnDisplaySize = columnDisplaySizes.get(i);
			int columnType = columnTypes.get(i);
			int precision = precisions.get(i);
			int scale = scales.get(i);
							
			if(printMetaData){
				System.out.println("catalogName: " + catalogName);
				System.out.println("columnClassName: " + columnClassName);
				System.out.println("columnDisplaySize: " + columnDisplaySize);
				System.out.println("columnLabel: " + columnLabel);
				System.out.println("columnName: " + columnName);
				System.out.println("columnTypeName: " + columnTypeName);
				System.out.println("schemaName: " + schemaName);
				System.out.println("tableName: " + tableName);
				System.out.println("columnType: " + columnType);
				System.out.println("precision: " + precision);
				System.out.println("scale: " + scale);
			}
			
			if(dataTuple.containsKey(columnName)){
				String value = dataTuple.get(columnName);
				System.out.println(columnName + ": " + value);
			}
			else{
				System.out.println("Could not find key " + columnName);
			}
		}
	}
	
}
