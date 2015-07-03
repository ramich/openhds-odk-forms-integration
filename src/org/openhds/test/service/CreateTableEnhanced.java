package org.openhds.test.service;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * @author Abhishek Somani
 * http://www.javaroots.com/2013/09/print-tables-details-in-schema-jdbc.html
 */
public class CreateTableEnhanced {

//	public static String SCHEMA_NAME = "test"; //"odk_prod";
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	final static String schemaName = "test";
	final static String tableName = "VA_WHO_2014_FINAL10_CORE"; //"SAMPLE_CORE"; //"MY_CORE"; 

	public CreateTableEnhanced(){

		try {
			Connection con = DriverManager.getConnection(url, username, password); //db.getConnection();
			DatabaseMetaData metaData = con.getMetaData();

//			printAllTables(con, metaData);
//			printColumnProperties(con, metaData);
			
			System.out.println("CREATE TABLE `" + schemaName + "`.`" + tableName + "` ( ");
			createEntry(con, metaData);
			System.out.println(");");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void printAllTables(Connection con, DatabaseMetaData metaData){
		System.out.println("PrintTableEnhanced::printAllTables");
		
		String tableType[] = { "TABLE" };
		
		StringBuilder builder = new StringBuilder();

		try(ResultSet result = metaData.getTables(null, null, null, tableType); ){
			while (result.next()) {
				String tableName = result.getString(3);
	
				builder.append(tableName + "( ");
				ResultSet columns = metaData.getColumns(null, null, tableName,
						null);
	
				while (columns.next()) {
					String columnName = columns.getString(4);
					builder.append(columnName);
					builder.append(",");
				}
				if(builder.toString().contains(","))
					builder.deleteCharAt(builder.lastIndexOf(","));
				builder.append(" )");
				builder.append("\n");
				builder.append("----------------");
				builder.append("\n");
			}
		}
		catch(SQLException sqlE){
			System.err.println("Exception: " + sqlE.getMessage());
		}
		
		System.out.println(builder.toString());
	}
	
	private void printTableTypes(Connection con, DatabaseMetaData metaData){
		try(ResultSet tableTypes = metaData.getTableTypes(); ){
			
			while(tableTypes.next()){
				String tp = tableTypes.getString(1);
				System.out.println(tp);
			}
		}
		catch(SQLException sqle){
			
		}
	}
	
	/* Refer to http://docs.oracle.com/javase/6/docs/api/java/sql/DatabaseMetaData.html
	 * getTables to see what each column of the ResultSet means. 
	 * */
	public void printColumnProperties(Connection con, DatabaseMetaData metaData){
		
		System.out.println("PrintTableEnhanced::printColumnProperties");
		
		StringBuilder builder = new StringBuilder();
		
		try(ResultSet result = metaData.getColumns(null, null, tableName, null); ){
			while (result.next()) {
				String columnName = result.getString("COLUMN_NAME");
				String defaultValue = result.getString("COLUMN_DEF");
				int dataType = result.getInt("DATA_TYPE");
				String typeName = result.getString("TYPE_NAME");
				System.out.println("Default value for column " + columnName + " is " + defaultValue + " (type: " + typeName + ")");
				
				if(defaultValue != null && defaultValue.startsWith("'")){
					System.out.println("String default value");
				}
								
				System.out.println("");
			}
		}
		catch(SQLException sqlE){
			System.err.println("Exception: " + sqlE.getMessage());
		}
		
		System.out.println(builder.toString());
	}
	
	public String getJdbcTypeName(int jdbcType) {
		String type = "";
		Map map = new HashMap();

		// Get all field in java.sql.Types
		Field[] fields = java.sql.Types.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			try {
				String name = fields[i].getName();
				Integer value = (Integer) fields[i].get(null);
				map.put(value, name);
				type = name;
			} catch (IllegalAccessException e) {
			}
		}
		return type;
	}
	
	
	
	/* Refer to http://docs.oracle.com/javase/6/docs/api/java/sql/DatabaseMetaData.html
	 * getTables to see what each column of the ResultSet means. 
	 * */
	public void createEntry(Connection con, DatabaseMetaData metaData){		
		StringBuilder builder = new StringBuilder();
		
		int count = 0;
		try(ResultSet result = metaData.getColumns(null, null, tableName, null); ){
			while (result.next()) {								
				String columnName = result.getString("COLUMN_NAME");
				String defaultValue = result.getString("COLUMN_DEF");
				int dataType = result.getInt("DATA_TYPE");
				String typeName = result.getString("TYPE_NAME");
				int nullable = result.getInt("NULLABLE");
				int columnSize = result.getInt("COLUMN_SIZE");
				
//				System.out.println("Default value for column " + columnName + " is " + defaultValue + " (type: " + typeName + ")");
//				System.out.println("Nullable: " + nullable + " | columnSize: " + columnSize);
				
				builder.append("`" + columnName + "` " );
				
				if(typeName.equalsIgnoreCase("CHAR") || typeName.equalsIgnoreCase("VARCHAR") || typeName.equalsIgnoreCase("LONGVARCHAR")){
					builder.append(typeName);
					builder.append("(" + columnSize + ") ");
				}
				else if(typeName.equalsIgnoreCase("DECIMAL") || typeName.equalsIgnoreCase("NUMERIC") || typeName.equalsIgnoreCase("DOUBLE") 
						|| typeName.equalsIgnoreCase("REAL") || typeName.equalsIgnoreCase("FLOAT") || typeName.equalsIgnoreCase("BIGINT")
						|| typeName.equalsIgnoreCase("INTEGER") || typeName.equalsIgnoreCase("SMALLINT") || typeName.equalsIgnoreCase("TINYINT")
						|| typeName.equalsIgnoreCase("BIT") || typeName.equalsIgnoreCase("INT")){
					builder.append(typeName);
					builder.append("(" + columnSize + ") ");
				}
				else if(typeName.equalsIgnoreCase("DATETIME")){
//					builder.append(typeName);
//					builder.append("(" + columnSize + ") ");
					builder.append(typeName + " ");
				}
				else{
					builder.append(typeName + " ");
				}
				
				if(nullable == ResultSetMetaData.columnNoNulls){
					builder.append("NOT NULL ");
				}
					
				if(defaultValue!=null){
					if(typeName.equalsIgnoreCase("CHAR") || typeName.equalsIgnoreCase("VARCHAR") || typeName.equalsIgnoreCase("LONGVARCHAR")){
						builder.append("DEFAULT '" + defaultValue + "' ");
					}
					else if(typeName.equalsIgnoreCase("DECIMAL") || typeName.equalsIgnoreCase("NUMERIC") || typeName.equalsIgnoreCase("DOUBLE") 
							|| typeName.equalsIgnoreCase("REAL") || typeName.equalsIgnoreCase("FLOAT") || typeName.equalsIgnoreCase("BIGINT")
							|| typeName.equalsIgnoreCase("INTEGER") || typeName.equalsIgnoreCase("SMALLINT") || typeName.equalsIgnoreCase("TINYINT")
							|| typeName.equalsIgnoreCase("BIT") || typeName.equalsIgnoreCase("INT")){
						try{
							int defaultInt = Integer.parseInt(defaultValue);
							builder.append("DEFAULT " + defaultInt + " ");
						}
						catch(NumberFormatException nfe){
							System.err.println("Could not parse " + defaultValue + " as number.");
						}
					}
				}
				
				if(defaultValue != null && defaultValue.startsWith("'")){
					System.out.println("String default value");
				}
				
				builder.append(", \n");
				
				count++;
			}		
			
			//PRIMARY KEY
			List<String> primaryKeyList = getPrimaryKeyList(con, metaData);
			if(primaryKeyList.size() > 0){
				builder.append("PRIMARY KEY ( ");
				for(String s : primaryKeyList){
					builder.append("`" + s + "`, ");
				}
				if(builder.toString().contains(","))
				builder.deleteCharAt(builder.lastIndexOf(","));
				builder.append(")");
			}
		}		
		catch(SQLException sqlE){
			System.err.println("Exception: " + sqlE.getMessage());
		}
		
		System.out.println(builder.toString());
	}
	
	
	private List<String> getPrimaryKeyList(Connection con, DatabaseMetaData metaData){
		StringBuilder builder = new StringBuilder();
		List<String> list = new ArrayList<String>();
		
		try(ResultSet rs = metaData.getPrimaryKeys(null, null, tableName);){
			while(rs.next()){
			    String primaryKey = rs.getString("COLUMN_NAME");
			    builder.append(primaryKey);
			    list.add(primaryKey);
			}
		}
		catch(SQLException sqlE){
			
		}

		return list;
	}
	  
}
