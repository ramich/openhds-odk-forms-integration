package org.openhds.test.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateDatabaseDummy {
	
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	final static String tableName = "SAMPLE_CORE";
	
	final static String extraFormName = "visit_registration";
	
	private boolean verbose = true;
	
	public CreateDatabaseDummy(){
		getCoreDatabaseFromFormName();
		
		String schemaName = "test";
		String command = createDatabaseCreateStatementFromMetaData();
		String fullCommand = String.format(command, schemaName);
		System.out.println(fullCommand);
	}
	
	private void getCoreDatabaseFromFormName(){		
		printLog("Connecting to database...");
		boolean found = false;
		String sql = "SELECT _CREATION_DATE, SUBMISSION_FORM_ID, URI_SUBMISSION_DATA_MODEL FROM _form_info_submission_association";
		
		try (Connection connection = DriverManager.getConnection(url, username, password); 
				Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql);) {
		    
			printLog("Database connected!");
  
		    while (rs.next()) {
		    	String c = rs.getString("_CREATION_DATE");
		        String form = rs.getString("SUBMISSION_FORM_ID");
		        String uri = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        
		        if(form.equalsIgnoreCase(extraFormName)){
		        	found = true;
		        		
		        	System.out.println("Found extra form to handle: ");
		        	printLog(form + " / " + c + " (" + uri + ")");
		        	processForm(uri, connection);
		        }
		    }
		    
		    if(found){
		    	System.out.println("Found extraForm");
		    }
		    else{
		    	System.out.println("Couldn't find extraForm with name " + extraFormName);
		    }
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}
	
	private void processForm(String uri, Connection connection) throws SQLException{
		String sql = "SELECT _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PARENT_URI_FORM_DATA_MODEL FROM _form_data_model WHERE URI_SUBMISSION_DATA_MODEL = ?";
		
		try(PreparedStatement ps = connection.prepareStatement(sql);){
			ps.setString(1, uri);
			
			try(ResultSet rs = ps.executeQuery();){
			    while (rs.next()) {
			    	String elementType = rs.getString("ELEMENT_TYPE");
			    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			    	String element_uri = rs.getString("_URI");
			    	String parent_uri = rs.getString("PARENT_URI_FORM_DATA_MODEL");
			    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
			    	String elementName = rs.getString("ELEMENT_NAME");
			    	
		//	    	System.out.println("columnName: " + columnName + " " + (parent_uri.equals(uri)) + " " + element_uri);
			    	
			    	// Check for starting group 	
			    	if(parent_uri.equals(uri) && elementType.equalsIgnoreCase("GROUP") && (columnName == null)){
			    		printLog("Core table: " + tableName);
		//	    		printLog("-------------------- Found starting group, beginning to parse... -----------------------------");
		//	    		printLog("-------------------- End parsing form. -------------------------------------------------------\n");
			    	}
			    }
			}
		}
	}
	
	private String createDatabaseCreateStatementFromMetaData(){
		String sqlCommand = null;
		
		String sqlSelect = "SELECT * FROM " + tableName;
		
		try (Connection connection = DriverManager.getConnection(url, username, password); 
				Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery( sqlSelect );) {
			
		    printLog("Database connected!");
		    
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
//			String tableName = null;
			StringBuilder sb = new StringBuilder( 1024 );
			if ( columnCount > 0 ) { 
			    sb.append( "CREATE TABLE %s." ).append( rsmd.getTableName( 1 ) ).append( " ( " );
			}
			for ( int i = 1; i <= columnCount; i ++ ) {
			    if ( i > 1 ) sb.append( ", " );
			    String columnName = rsmd.getColumnLabel( i );
			    String columnType = rsmd.getColumnTypeName( i );
			    	
			    sb.append( columnName ).append( " " ).append( columnType );
	
			    int precision = rsmd.getPrecision( i );
			    if ( precision != 0 && !columnType.equalsIgnoreCase("DATETIME")) {
			        sb.append( "( " ).append( precision ).append( " )" );
			    }
			    
			    int nullability = rsmd.isNullable(i);
			    
		        //
		        // Check the nullability status of a column (ID)
		        //		
		        if (nullability == ResultSetMetaData.columnNullable) {
//		        	System.out.println("Columns ID can have a null value");
		        	sb.append(" NULL ");
		        } else if (nullability == ResultSetMetaData.columnNoNulls) {
//		        	System.out.println("Columns ID does not allowed to have a null value");
		        	sb.append(" NOT NULL ");
		        } else if (nullability == ResultSetMetaData.columnNullableUnknown) {
//		        	System.out.println("Nullability unknown");        
		        }
		        
		        //Set PRIMARY KEY
		        if(rsmd.getColumnLabel( i ).equalsIgnoreCase("_URI")){
		        	sb.append("PRIMARY KEY");
		        }
			} // for columns
			sb.append( " ) " );
	
			sqlCommand = sb.toString();
//			System.out.println( sqlCommand );
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
		
		return sqlCommand;
	}
	
	private void printLog(String logEntry){
		if(verbose)
			System.out.println(logEntry);
	}
}
