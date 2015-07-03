package org.openhds.test.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseStructure {

	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	private boolean verbose;
	
	public DatabaseStructure(){
		verbose = true;
		String sqlCountSelect = "SELECT COUNT(*) FROM _form_info_submission_association";
		String sqlSelect = "SELECT _CREATION_DATE, SUBMISSION_FORM_ID, URI_SUBMISSION_DATA_MODEL FROM _form_info_submission_association";
		
		printLog("Connecting to database...");
	
		try (Connection connection = DriverManager.getConnection(url, username, password); 
				Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlSelect);) {
			
		    printLog("Database connected!");
		    
		    getRowCountFormSQLSelect(connection, sqlCountSelect);
		    
//		    ResultSet rs = stmt.executeQuery(sqlSelect);
		    
		    while (rs.next()) {
		    	String c = rs.getString("_CREATION_DATE");
		        String form = rs.getString("SUBMISSION_FORM_ID");
		        String uri = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        
		        printLog(form + " / " + c + " (" + uri + ")");
		        
		        handleForm(uri, connection);
		    }
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}
	
	private void getRowCountFormSQLSelect(Connection connection, String sqlCommand){
		try(Statement stmt = connection.createStatement(); 
				ResultSet rs = stmt.executeQuery(sqlCommand);){
	        int rowCount = 0;
	        while(rs.next()) {
	            rowCount = Integer.parseInt(rs.getString("count(*)"));
	        }
	        printLog("Forms " + "(Available " + rowCount + "):");
		}
		catch(SQLException sqlE){
			System.err.println("Exception while trying to get rowCount: " + sqlE.getMessage());
		}
	}
	
	private void handleForm(String uri, Connection connection){	    
	    String sql = "SELECT _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PARENT_URI_FORM_DATA_MODEL FROM _form_data_model WHERE URI_SUBMISSION_DATA_MODEL = ?";
	    
		try(PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setString(1, uri);

			try(ResultSet rs = ps.executeQuery();){			    
			    while (rs.next()) {
			    	String elementType = rs.getString("ELEMENT_TYPE");
			    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			    	String element_uri = rs.getString("_URI");
			    	String parent_uri = rs.getString("PARENT_URI_FORM_DATA_MODEL");
			    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
			    	
			    	// Check for starting group 	
			    	if(parent_uri.equals(uri) && elementType.equalsIgnoreCase("GROUP") && (columnName == null)){
			    		printLog("Core table: " + tableName);
			    		printLog("-------------------- Found starting group, beginning to parse... -----------------------------");
			    		processUri(element_uri, connection);
			    		printLog("-------------------- End processing form. -----------------------------------------------------\n");
			    	}
			    }
			}  		    
		} catch (SQLException e) {
//			e.printStackTrace();
			System.err.println("Exception while trying to handle extraForm: " + e.getMessage());
		}
	}
	
	private void processUri(String _uri, Connection connection) throws SQLException{
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_COLUMN_NAME,ELEMENT_NAME, PERSIST_AS_TABLE_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    
	    try(PreparedStatement ps = connection.prepareStatement(sql); ){
			ps.setString(1, _uri);
			
			try(ResultSet rs = ps.executeQuery();){				
				while (rs.next()) {	
					String element_uri = rs.getString("_URI");		
					handleRow(element_uri, connection);
				}
			}
	    }
	}
	
	private void handleRow(String _uri, Connection conn) throws SQLException{
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_COLUMN_NAME,ELEMENT_NAME, PERSIST_AS_TABLE_NAME FROM _form_data_model WHERE _URI = ?";
	    
	    try(PreparedStatement ps = conn.prepareStatement(sql);){
			ps.setString(1, _uri);	
			
			try(ResultSet rs = ps.executeQuery();){				
				while (rs.next()) {
					String elementType = rs.getString("ELEMENT_TYPE");
					String element_uri = rs.getString("_URI");
					String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
					String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
					String elementName = rs.getString("ELEMENT_NAME");
					
					if(columnName != null){
						printLog(columnName + " " + tableName + " " + elementType + " " + elementName);
					}
					else{
						printLog("- Found structured element of type " + elementType + " with name " +elementName + " Persisted in table " + tableName + " -");
						
						if(elementType.equalsIgnoreCase("geopoint")){
							handleGeopoint(element_uri, conn);
						}
						else if(elementType.equalsIgnoreCase("GROUP")){
							handleGroup(element_uri, conn);
						}
						else if(elementType.equalsIgnoreCase("REPEAT")){
							handleRepeat(element_uri, conn);
						}
						else if(elementType.equalsIgnoreCase("BINARY")){
							handleBinary(element_uri, conn);
						}		 
						else if(elementType.equalsIgnoreCase("BINARY_CONTENT_REF_BLOB")){
							handleBinaryRef(element_uri, conn);
						}	
						else if(elementType.equalsIgnoreCase("REF_BLOB")){
							handleBlob(element_uri, conn);
						}	
						else if(elementType.equalsIgnoreCase("SELECTN")){
							System.out.println("SELECTN: " + tableName);
							handleSelectN(element_uri, conn);
						}	
						else{
							printLog("!!!!!!!!!!!!!UNKNOWN HANDLER!!!!!!!!!!!!!!!!!! " + elementType);
						}
					}
				}
			}
	    }
	}
	
	/* Geopoint contains 4 subelements */
	private void handleGeopoint(String element_uri, Connection conn) throws SQLException{
		printLog(">Handling Geopoint:");
		
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    
	    try(PreparedStatement ps = conn.prepareStatement(sql);){
			ps.setString(1, element_uri);

			try(ResultSet rs = ps.executeQuery();){	
			    while (rs.next()) {
			    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			    	
			    	printLog("Geopoint element: " + columnName);
			    }	
			}
	    }	    
	    printLog("End Handling Geopoint<");
	}
	
	/*Group can contain elements of any type*/
	private void handleGroup(String _uri, Connection conn) throws SQLException{
		printLog(">Handle Group (_uri: '" + _uri + "' )");
		
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = conn.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
	    while (rs.next()) {
	    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
	    	String elementType = rs.getString("ELEMENT_TYPE");
	    	String element_uri = rs.getString("_URI");
	    	
	    	printLog("Group element column name: " + columnName + " (type: " + elementType + ")");
	    	
	    	handleRow(element_uri, conn);
	    	
	//    	if(elementType.equalsIgnoreCase("GROUP"))
	//    	{
	//    		handleGroup(element_uri, conn);
	//    		System.out.println("GROUPINAGROUP");
	//    	}
	    }
	    
	    printLog("End Handle Group<");
	}
	
	/*Repeat references subtable*/
	private void handleRepeat(String _uri, Connection conn) throws SQLException{
		printLog(">Handle Repeat");
		
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = conn.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
	    while (rs.next()) {
	    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
	    	String elementType = rs.getString("ELEMENT_TYPE");
	    	String element_uri = rs.getString("_URI");
	//    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
	    	
	    	printLog("Repeat element: " + columnName);
	    	
	    	if(elementType.equalsIgnoreCase("GROUP"))
	    	{
	    		handleGroup(element_uri, conn);
	    	}
	    }
	    
	    printLog("End Handle Repeat<");
	}
	
	private void handleBinary(String element_uri, Connection conn) throws SQLException{
		printLog("Handle Binary!");
	}	
	
	private void handleBinaryRef(String element_uri, Connection conn) throws SQLException{
		printLog("Handle BinaryRef!");
	}	
	
	private void handleBlob(String element_uri, Connection conn) throws SQLException{
		printLog("Handle Blob!");
	}	
	
	private void handleSelectN(String _uri, Connection conn) throws SQLException{
		printLog(">Handle Selectn: " + _uri);
		
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, PARENT_URI_FORM_DATA_MODEL, ELEMENT_NAME FROM _form_data_model WHERE _URI = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = conn.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
	    while (rs.next()) {
	//    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
	    	String elementType = rs.getString("ELEMENT_TYPE");
	    	String element_uri = rs.getString("_URI");
	    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
	    	String elementName = rs.getString("ELEMENT_NAME");
	    	String parentUri = rs.getString("PARENT_URI_FORM_DATA_MODEL");
	    	
	    	printLog("Selectn element " + elementName + " references table: " + tableName);
	    	
	    	if(elementType.equalsIgnoreCase("SELECTN"))
	    	{
	    		System.out.println("Continue selectn");
	    		printSelectNValues(tableName, conn);
	    		
	    		String coreTable = getCoreTableName(parentUri, conn);
	    		System.out.println("CORE TABLE NAME: " + coreTable);
	    	}
	    }
	    
	    printLog("End Handle Selectn<");
	}	
	
	private String getCoreTableName(String _uri, Connection conn) throws SQLException{
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, PARENT_URI_FORM_DATA_MODEL, ELEMENT_NAME FROM _form_data_model WHERE _URI = ?";
	    
	    String table = "";
	    
	    try(PreparedStatement ps = conn.prepareStatement(sql);){
			ps.setString(1, _uri);
			
			try(ResultSet rs = ps.executeQuery();){

			    while (rs.next()) {
			//    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			    	String elementType = rs.getString("ELEMENT_TYPE");
			//    	String element_uri = rs.getString("_URI");
			    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
			    	String elementName = rs.getString("ELEMENT_NAME");
			    	String parentUri = rs.getString("PARENT_URI_FORM_DATA_MODEL");
			    	
			    	table = tableName;
			    	
			    	printLog("Selectn element " + elementName + " references table: " + tableName);
			    	
			    	if(elementType.equalsIgnoreCase("SELECTN"))
			    	{
			    		System.out.println("Continue selectn");
			    		printSelectNValues(tableName, conn);
			    	}
			    }
			}
	    }
		return table;
	}
	
	private void printSelectNValues(String tableName, Connection conn) throws SQLException{		
	    String sql = "SELECT VALUE FROM " + tableName;
	    
	    try(Statement stmt = conn.createStatement();ResultSet rs = stmt.executeQuery(sql);){
		    while (rs.next()) {
		    	String value = rs.getString("VALUE");
		    	
		    	printLog("----------------->>>>>>>>>>>>>>>>>>>>>>>>>>>> SELECTN VALUE: " + value);
		    }
	    }
	    
	    printLog("End Handle Selectn<");
	}
		
	private void printLog(String logEntry){
		if(verbose)
			System.out.println(logEntry);
	}
}
