package org.openhds.test.unused;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.openhds.test.service.MirthProperties;

public class Testing {
	
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String url2 = "jdbc:mysql://data-management.local:3306/openhds";
	final static String username = "data";
	final static String password = "data";

	private boolean verbose = true;
	
	public Testing(){
//		getSthSth();
	}
	
	private void getSthSth(){
		String uuid = "uuid:6914659d-13b1-4ec4-a5a4-e320d10c8f87";
		
		printLog("Connecting to database...");
		
		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    printLog("Database connected!");
		    
		    Statement stmt = connection.createStatement();
		    ResultSet rs;
		    		    
		    rs = stmt.executeQuery("SELECT PERSIST_AS_TABLE_NAME, PERSIST_AS_COLUMN_NAME, URI_SUBMISSION_DATA_MODEL, ORDINAL_NUMBER FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = '" +  uuid +"'");
		    
		    while (rs.next()) {
		    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
		        String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
		        String uriSubmissionDataModel = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        int ordinalNumber = rs.getInt("ORDINAL_NUMBER");
		        
		        if(ordinalNumber == 1 && columnName == null && uriSubmissionDataModel.equalsIgnoreCase(uuid)){
		        	System.out.println("Found form entry point!");
		        	System.out.println(tableName + " " + columnName + " " + uriSubmissionDataModel);
		        }
		    }
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}
	
//	createVisit("1", "socialgroup", "CAC000002", "socialGroup", "CAC00000201");
//	createVisit("1", "socialgroup", "CAC000002", null, "CAC00000201");
	/* Create visitId in openhds*/
    public void createVisit(String roundNumber, String visitLevel, String locationExtId, String socialgroup, String socialgroupExtId) {
        String suffix= roundNumber;
    	while(suffix.length() < 3){
    		suffix="0"+suffix;
    	}
    	String generatedId;
    	if (visitLevel.equalsIgnoreCase("location")) {
    		generatedId = locationExtId + suffix ;
    	} else {
    		if (socialgroup!=null) {
    			System.out.println("Taking socialgroupExtId to create visitId. Socialgroup extId length: " + socialgroupExtId.length());
    			generatedId = socialgroupExtId + suffix ;
    		} else {
    			generatedId = locationExtId +"00" + suffix ;
    		}
    	}

        System.out.println("Generated visitId: " + generatedId + "(length: " + generatedId.length() + ")");
    }
    
    public void verifyLocationId(){
    	String locationExtId = "ISE000020";
    	String sql = "SELECT count(*) FROM openhds.location WHERE extId = ?";
    	
		try (Connection connection = DriverManager.getConnection(url2, username, password);
				PreparedStatement ps = connection.prepareStatement(sql);) {
			
			ps.setString(1, locationExtId);
			
			try(ResultSet rs = ps.executeQuery()){
				if(rs.next()){
					int count = rs.getInt("count(*)");
					System.out.println("ResultSet count: " + count);
					if(count > 0){
						System.out.println("Found entry matching extId " + locationExtId);
					}
					else{
						System.err.println("Found no entry matching entry " + locationExtId);
					}
				}
				else{
					System.out.println("ResultSet is empty.");
				}
			}
		}
		catch(SQLException sqlE){
			sqlE.printStackTrace();
		}
    }
	
	private void printLog(String logEntry){
		if(verbose)
			System.out.println(logEntry);
	}
	
	
	private void replaceStringTestCreate(){
		String sql = "CREATE TABLE `test`.`BASELINE_CORE` ( `_URI` VARCHAR (80) NOT NULL ," + 
					"`_CREATOR_URI_USER` VARCHAR (80) NOT NULL , " +
					"`_CREATION_DATE` DATETIME NOT NULL ," + 
					"`END` DATETIME ," + 
					"`OPENHDS_FIELD_WORKER_ID` VARCHAR (255) ," + 
					"`DEVICE_ID` VARCHAR (255) ," + 
					"`INDIVIDUAL_INFO_GENDER` VARCHAR (255) ," + 
					"`MAJO4MO` VARCHAR (255) ," + 
					"PRIMARY KEY ( `_URI` ), \n" +
					"%s " +
					");";
		
		String replacer = "`INDIVIDUAL_UUID` VARCHAR(32) NOT NULL";
		
		sql = String.format(sql, replacer);
		
		System.out.println(sql);
	}
	
	private void replaceStringTestInsert(){
		String sql = "INSERT INTO TABLE `test`.`BASELINE_CORE` \n( " +
					"`_URI`," + 
					"`_CREATOR_URI_USER`, " +
					"`_CREATION_DATE`," + 
					"`END`," + 
					"`OPENHDS_FIELD_WORKER_ID`," + 
					"`DEVICE_ID`," + 
					"`INDIVIDUAL_INFO_GENDER`," + 
					"`MAJO4MO` " +
					"\n" +
					"%s " +
					")" + 
					"\n" +
					"VALUES \n("+
					"1, " +
					"2, " +
					"3, " +
					"4, " +
					"5, " +
					"6, " +
					"7, " +
					"8 " +
					"%s " +
					")\n"+
					");";
		
		String replacer1 = ", `-COLUMN-`";
		String replacer2 = ", -DATA-";
		
		sql = String.format(sql, replacer1, replacer2 );
		
		System.out.println(sql);
	}
	
	public void testPropertiesFile(){
		CrunchifyGetPropertyValues properties = new CrunchifyGetPropertyValues();
		try {
			properties.getPropValues();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Testing t = new Testing();
//		t.verifyLocationId();
//		t.replaceStringTestInsert();
//		t.testPropertiesFile();
		
		System.out.println(MirthProperties.getTimeout());
	}
	
}
