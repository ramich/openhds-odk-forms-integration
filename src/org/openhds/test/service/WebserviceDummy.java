package org.openhds.test.service;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.omg.CORBA.portable.IndirectionException;
import org.openhds.test.model.ExtraForm;
import org.openhds.test.model.mirth.ColumnDummy;
import org.openhds.test.model.mirth.TableDummy;
import org.openhds.test.service.OpenHDSDatabaseService.Entity;
import org.openhds.test.service.interfaces.DirectoryWatcherListener;

import com.mysql.jdbc.NotImplemented;

/*---------------------------------------------------------------------------------------------------------------------------------------
 * 
 * DUMMY WEBSERVICE CLASS, CREATE TABLE, INSERT DATA
 * 
 * ---------------------------------------------------------------------------------------------------------------------------------------
 * */

public class WebserviceDummy implements DirectoryWatcherListener {

	final static String url = "jdbc:mysql://data-management.local:3306/openhds";
	final static String username = "data";
	final static String password = "data";
	
//	private String dataFilePath = "data/out.xml";
//	private String createTableFilePath = "data/createTableSAMPLE_CORE.xml";
	
	private OpenHDSDatabaseService databaseService;
	
	public WebserviceDummy(){
		databaseService = new OpenHDSDatabaseService();
	}
		
	/*---------------------------------------------------------------------------------------------------------------------------------------
	 * 
	 * INSERT DATA STUFF
	 * 
	 * ---------------------------------------------------------------------------------------------------------------------------------------
	 * */
	
	/* Pass ExtraForm Object, validate sthsth... 
	 * Used to insert new submitted data from ODK form to openHDS
	 * */
	private void handleExtraFormXmlInput(String dataFilePath){
		ExtraForm extraForm = parseExtraFormXml(dataFilePath);
		
		if(extraForm != null){
			String fieldWorkerId = extraForm.getFieldWorkerId();
			String locationId = extraForm.getLocationId();
			String roundNumber = extraForm.getRoundNumber();
			String visitId = extraForm.getVisitId();
			String individualId = extraForm.getIndividualId();
			String socialgrouplId = extraForm.getSocialGroupId();
			
			System.out.println("Validate object");
			System.out.println("individualId: " + individualId + " | fieldWorkerId: " + fieldWorkerId + " | locationId: " + locationId + " | roundNumber: " + roundNumber + " | visitId: " + visitId  + " | socialgroupId: " + socialgrouplId);
			
			if(validateIds(individualId, fieldWorkerId, locationId, roundNumber, visitId, socialgrouplId)){
				System.out.println("Seems valid. Continue");
				insertExtraFormDataIntoTable(extraForm);
			}
			else{
				System.err.println("Id validation failed! Could not insert dataset.");
			}
		}
		else{
			System.err.println("WebserviceDummy::handleExtraFormXmlInput::extraForm is null!");
		}
	}
	
	private ExtraForm parseExtraFormXml(String dataFilePath) {
		ExtraForm extraForm = null;
		 try {
				File file = new File(dataFilePath);
				JAXBContext jaxbContext = JAXBContext.newInstance(ExtraForm.class);
		 
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				extraForm = (ExtraForm) jaxbUnmarshaller.unmarshal(file);
				
//				System.out.println(extraForm);
				
				if(extraForm != null){
//					System.out.println("End unmarshalling ExtraForm!");
//					System.out.println("Found containing data elements: " + extraForm.getData().size());
				} 
		 
			  } catch (JAXBException e) {
				e.printStackTrace();
			  }
		 return extraForm;
	}
	
	private void insertExtraFormDataIntoTable(ExtraForm extraForm) {

		String query = createInsertSqlStatement(extraForm);
		
		//Check for foreign key and insert FK reference data
		Map<String, String> foreignKeyData = getForeignKeyData(extraForm, query);
		
		StringBuilder sbColumn = new StringBuilder();
		StringBuilder sbData = new StringBuilder();
		
		Set<String> keys = foreignKeyData.keySet();
		for(String key: keys){
			sbColumn.append(", ");
			sbData.append(", ");
			
			
			sbColumn.append(key);
			sbData.append(foreignKeyData.get(key));
		}
		
		query = String.format(query, sbColumn.toString(), sbData.toString());
		
		System.out.println("INSERT DATA QUERY: " + query);
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement pstmt = connection.prepareStatement(query);) {		

			int pointer = 1;
			for (ExtraForm.Data d : extraForm.getData()) {
				if (d.data.length() == 0) {
					if (d.type.equalsIgnoreCase("INT")) {
						pstmt.setNull(pointer, java.sql.Types.INTEGER);
					} else if (d.type.equalsIgnoreCase("DECIMAL")) {
						pstmt.setNull(pointer, java.sql.Types.DECIMAL);
					} else if (d.type.equalsIgnoreCase("DATETIME")) {
						pstmt.setNull(pointer, java.sql.Types.DATE);
					} else if (d.type.equalsIgnoreCase("VARCHAR")) {
						pstmt.setNull(pointer, java.sql.Types.VARCHAR);
					} else {
						pstmt.setString(pointer, d.data);
					}
				} else {
					pstmt.setString(pointer, d.data);
				}
				pointer++;
			}
			

			int rowCount = pstmt.executeUpdate();
			if(rowCount > 0)
				System.out.println("Affected rows: " + rowCount);
			else
				System.out.println("Could not insert row.");
		}
		catch (SQLException e) {	
			if(e instanceof SQLIntegrityConstraintViolationException ){
				System.err.println("Insert failed! Contraint Exception: " + e.getMessage());
			}
			else{
				throw new IllegalStateException("Cannot connect the database!", e);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private Map<String, String> getForeignKeyData(ExtraForm extraForm, String query){
		Map<String, String> foreignKeyData = new HashMap<String, String>();
		
		if(query.contains("OPENHDS_VISIT_ID")){
			String visitUuid = databaseService.getUuIdForEntityExtId(extraForm.getVisitId(), Entity.VISIT);
			foreignKeyData.put("VISIT_UUID", "'" + visitUuid + "'");
		}
		
		if(query.contains("OPENHDS_INDIVIDUAL_ID") || query.contains("INDIVIDUAL_INFO_INDIVIDUAL_ID")){
			String individualUuid = databaseService.getUuIdForEntityExtId(extraForm.getIndividualId(), Entity.INDIVIDUAL);
			foreignKeyData.put("INDIVIDUAL_UUID", "'" + individualUuid + "'");
		}
		
		if(query.contains("OPENHDS_HOUSEHOLD_ID")){
			String socialGroupUuid = databaseService.getUuIdForEntityExtId(extraForm.getSocialGroupId(), Entity.SOCIALGROUP);
			foreignKeyData.put("HOUSEHOLD_UUID", "'" + socialGroupUuid + "'");
		}
		
		if(query.contains("OPENHDS_LOCATION_ID")){
			String locationUuid = databaseService.getUuIdForEntityExtId(extraForm.getLocationId(), Entity.LOCATION);
			foreignKeyData.put("LOCATION_UUID", "'" + locationUuid + "'");
		}
		
		return foreignKeyData;
	}
	
	private String createInsertSqlStatement(ExtraForm extraForm){
				
		String formName = extraForm.getFormName();
		String tableName = getCoreTableNameFromExtraForm(formName); //"VISIT_CORE_REGISTRATION";
		
//		System.out.println("formName: " + formName);
//		System.out.println("tableName: " + tableName);
		
		if(tableName == null){
			System.out.println("Could not find CORE_TABLE name. Aborting.");
			return null;
		}	
		
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		sb1.append("INSERT INTO test." + tableName);
		sb1.append(" ( ");
		sb2.append(" VALUES (");
		
		int count = 1;
		for(ExtraForm.Data d: extraForm.getData()){
			
//			if(count > 0){
//				sb1.append(", ");
//				sb2.append(", ");
//			}			
			sb1.append(d.columnName);
			sb2.append("?");
			
			if(count < extraForm.getData().size()){
				sb1.append(", ");
				sb2.append(", ");
			}
			
			count++;
		}		
		
		//ADD FOREIGN KEY DATA placeholders
		sb1.append(" %s");
		sb2.append(" %s");
		
		sb1.append(" )");
		sb2.append(" );");
		
		String query = sb1.toString() + " " + sb2.toString();
		System.out.println(query);
		
		return query.toString();
	}
	
	private String getCoreTableNameFromExtraForm(String formName){
		String coreTableName = null;
		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    
		    Statement stmt = connection.createStatement();
		    ResultSet rs;
		    
		    rs = stmt.executeQuery("SELECT formName, active, deleted, gender, insertDate, CORE_TABLE FROM form WHERE formName = '" + formName + "'");
		    
		    while (rs.next()) {
		        String CORE_TABLE = rs.getString("CORE_TABLE");
		        
		        if(CORE_TABLE != null)
		        	coreTableName = CORE_TABLE;
		    }
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
		
		return coreTableName;
	}	

	/*Dummy verification. Returns just true for now*/
	private boolean validateIds(String individualId, String fieldWorkerId, String locationId,
			String roundNumber, String visitId, String socialgrouplId) {

		if(individualId == null && fieldWorkerId==null && locationId==null && roundNumber == null && 
				visitId == null && socialgrouplId == null){
			System.out.println("Seems like it is not a openhds extra form");
			return true;
		}
		
		if(individualId == null || individualId.trim().length() == 0){
			return false;
		}
		
		String uuid = databaseService.getUuIdForEntityExtId(individualId, Entity.INDIVIDUAL);
		
		System.out.println("Individual uuid = " + uuid);
		
		return true;
	}
	

	
	/*---------------------------------------------------------------------------------------------------------------------------------------
	 * 
	 * CREATE TABLE STUFF
	 * 
	 * ---------------------------------------------------------------------------------------------------------------------------------------
	 * */
	
	/*
	 * Handle Create Table Xml
	 * TODO: Check key if valid, and also remove secret-key after success.
	 * TODO: Insert CORE_TABLE name into form table
	 * */
	public void handleCreateTableXml(String createTableFilePath){
		TableDummy table = readCreateTableXml(createTableFilePath);
		
		if(table == null)
		{
			System.err.println("Could not parse Create Table Xml.");
			return;
		}
		
		boolean keyIsValid = verifyKey(table.getKey());
		
		if(keyIsValid){
			String createSQLCommand = createTableSQLCommand(table);
			String alterCommand = alterTableForeignKey(createSQLCommand, table.getName());
			
			System.out.println(alterCommand);
			
			boolean successfullyCreatedTable = createTableWithSQLCommand(createSQLCommand);
			boolean successfullyAlteredTable = alterTableWithSQLCommand(alterCommand);
			
			System.out.println("successfullyAlteredTable: " + successfullyAlteredTable);
			
			//INSERT 
			if(successfullyCreatedTable){
				boolean insertCoreTableNameSuccessfull = insertCoreTableName(table.getKey(), table.getName());
				
				if(insertCoreTableNameSuccessfull){
					System.out.println("->Created new table. Everything went smoothly!");
				}
			}
			else{
				System.err.println("Core table could not be created.");
			}
		}
		else{
			System.err.println("Key validation failed.");
		}
	}
	
	private String alterTableForeignKey(String sql, String coreTable){
		String dbSchema = "test";
		String openHDSScheme = "openhds";
		StringBuilder builder = new StringBuilder();
		/*
		ALTER TABLE test.BASELINE_CORE 
		ADD FOREIGN KEY (INDIVIDUAL_UUID)
		REFERENCES openhds.individual(uuid);
		*/
		
		builder.append("ALTER TABLE " + dbSchema + "." + coreTable +" ");
		
		//ADD FOREIGN KEY CONSTRAINTS
		
		if(sql.contains("OPENHDS_VISIT_ID")){
			builder.append("ADD VISIT_UUID VARCHAR(32) NOT NULL, ");
			builder.append("ADD CONSTRAINT "+ coreTable + "_vuuidfk_1 FOREIGN KEY (VISIT_UUID) ");
			builder.append("REFERENCES " + openHDSScheme + ".visit(uuid), ");
		}
		
		if(sql.contains("OPENHDS_INDIVIDUAL_ID") || sql.contains("INDIVIDUAL_INFO_INDIVIDUAL_ID")){
			builder.append("ADD INDIVIDUAL_UUID VARCHAR(32) NOT NULL, ");
			builder.append("ADD CONSTRAINT " + coreTable + "_iuuidfk_1 FOREIGN KEY (INDIVIDUAL_UUID) ");
			builder.append("REFERENCES " + openHDSScheme + ".individual(uuid), ");
		}
		
		if(sql.contains("OPENHDS_HOUSEHOLD_ID")){
			builder.append("ADD HOUSEHOLD_UUID VARCHAR(32) NOT NULL, ");
			builder.append("ADD CONSTRAINT " + coreTable + "_hhuuidfk_1 FOREIGN KEY (HOUSEHOLD_UUID) ");
			builder.append("REFERENCES " + openHDSScheme + ".socialgroup(uuid), ");
		}
		
		if(sql.contains("OPENHDS_LOCATION_ID")){
			builder.append("ADD LOCATION_UUID VARCHAR(32) NOT NULL, ");
			builder.append("ADD CONSTRAINT " + coreTable + "_luuidfk_1 FOREIGN KEY (LOCATION_UUID) ");
			builder.append("REFERENCES " + openHDSScheme + ".location(uuid), ");
		}
		
		if(builder.toString().contains(",")){
			builder.deleteCharAt(builder.lastIndexOf(","));
		}
		
		builder.append(";");		
		builder.append("\n");
		
		return builder.toString();
	}
	
	public boolean verifyKey(String key){
		
		String sql = "SELECT CORE_TABLE FROM openhds.form WHERE uuid = ?";
		
		boolean isValid = false;
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement ps = connection.prepareStatement(sql)){
			
			ps.setString(1, key);
	
			try(ResultSet stmt = ps.executeQuery();){
				if(stmt.next()){
					String coreTable = stmt.getString("CORE_TABLE");
					
					if(coreTable == null || coreTable.trim().length() == 0){
						System.out.println("KEY SEEMS VALID!");
						isValid = true;
					}
					else{
						System.err.println("ATTRIBUTE CORE_TABLE IS ALREADY SET.");
					}
				}
			}
		}
		catch(SQLException sqlException){
			sqlException.printStackTrace();	
		}
		return isValid;
	}
		
	private TableDummy readCreateTableXml(String createTableFilePath){
		TableDummy table = null;
		File file = null;
		try {
			//File file = new File("data/createTable.xml");
			file = new File(createTableFilePath);
			JAXBContext jaxbContext = JAXBContext.newInstance(TableDummy.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			table = (TableDummy) jaxbUnmarshaller.unmarshal(file);
			System.out.println("Successfully unmarshalled file!");

		} catch (JAXBException e) {
			System.err.println("Could not read in file: " + file!=null?file.getName(): "?");
			e.printStackTrace();
		}
		 return table;
	}
	
	/* Creates SQL Statement to create new Table for ExtraForm received via MirthDummy
	 * */
	private String createTableSQLCommand(TableDummy table){
		StringBuilder builder = new StringBuilder();
		
		String schemaName = "test";
		
		String tableName = table.getName();
		
		builder.append("CREATE TABLE `" + schemaName + "`.`" + tableName + "` ( ");
		
		for(ColumnDummy column: table.getColumns()){
			
			String columnName = column.getName();
			String defaultValue = column.getDefault_value();
			String typeName = column.getType();
			String nullable = column.getAllow_null();
			String columnSize = column.getSize();
			
			builder.append("`" + columnName + "` " );
			builder.append(typeName + " ");
			
			if(!typeName.equalsIgnoreCase("DATETIME"))
				builder.append("(" + columnSize + ") ");
			
			if(nullable != null && nullable.equalsIgnoreCase("false")){
				builder.append("NOT NULL ");
			}
			
			if(defaultValue != null && defaultValue.trim().length() > 0 && defaultValue!= "null"){
				builder.append("DEFAULT '" + defaultValue + "' ");
			}
			
			builder.append(", \n");
		}
		
//		builder.append("\n");
		
		//ADD FOREIGN KEY FIELDS
//		if(builder.toString().contains(","))
//			builder.deleteCharAt(builder.lastIndexOf(","));
//		builder.append("INDIVIDUAL_UUID VARCHAR(32) NOT NULL, \n\n");
		
		String primaryKey = table.getPrimaryKey();
		if(primaryKey != null && primaryKey.length() > 0){
			builder.append("PRIMARY KEY ( ");
			builder.append("`" + primaryKey + "` ");
//			if(builder.toString().contains(","))
//				builder.deleteCharAt(builder.lastIndexOf(","));
			builder.append(")\n");
		}
		
		builder.append(");");
		
		System.out.println(builder.toString());
		return builder.toString();
	}
	
	private boolean createTableWithSQLCommand(String sql){
		boolean success = false;
		try (Connection connection = DriverManager.getConnection(url, username, password);
				Statement stmt = connection.createStatement();){
			
			int resultCount = stmt.executeUpdate(sql); //Will return 0
//			System.out.println("Result count: " + resultCount);
			
			success = true;
		}
		catch(SQLException sqlException){
			if (sqlException.getErrorCode() == 1050 ) {
		        // Database already exists error
		        System.err.println("Duplicate: " + sqlException.getMessage());
		    } 
			else if(sqlException instanceof SQLSyntaxErrorException){
				System.err.println("SQLSyntaxErrorException: " + sqlException.getMessage());
			} 
			else{
				sqlException.printStackTrace();	
			}
		}
		return success;
	}
	
	private boolean alterTableWithSQLCommand(String sql){
		boolean success = false;
		try (Connection connection = DriverManager.getConnection(url, username, password);
				Statement stmt = connection.createStatement();){
			
			int resultCount = stmt.executeUpdate(sql); //Will return 0
//			System.out.println("Result count: " + resultCount);
			
			success = true;
		}
		catch(SQLException sqlException){
			if (sqlException.getErrorCode() == 1050 ) {
		        // Database already exists error
		        System.err.println("Duplicate: " + sqlException.getMessage());
		    } 
			else if(sqlException instanceof SQLSyntaxErrorException){
				System.err.println("SQLSyntaxErrorException: " + sqlException.getMessage());
			} 
			else{
				sqlException.printStackTrace();	
			}
		}
		return success;
	}	
	
	private boolean insertCoreTableName(String key, String tableName){
		String sql = "UPDATE openhds.form SET CORE_TABLE = ? WHERE uuid = ?";
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement ps = connection.prepareStatement(sql)){
			
			ps.setString(1, tableName);
			ps.setString(2, key);
	
			int rowCount = ps.executeUpdate();
			
			if(rowCount > 0){
				return true;
			}
			else{
				System.err.println("COULD NOT INSERT CORE_TABLE DATA");
			}
		}
		catch(SQLException sqlException){
			sqlException.printStackTrace();	
		}
		return false;
	}

	@Override
	public void onDirectoryChanged(WatchEvent.Kind<?> kind, Path fileName, Path dir) {	
		Path child = dir.resolve(fileName);
//		System.out.println("WebserviceDummy::onDirectoryChanged: " + fileName + " | " + child.toString());
		
		if(Files.isRegularFile(child)){
			System.out.println("new File found: " + fileName.getFileName());
			
			if(kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.toString().startsWith("createTable_")){
				System.out.println("Filename startswith createTable_, handling as CREATE TABLE INPUT");
//				this.createTableFilePath = child.toString();
				handleCreateTableXml(child.toString());
				
				boolean deleteCreateFile = false;
				
				if(deleteCreateFile){
					try {
						System.out.println("NOW REMOVE CREATE FILE AFTER CREATING TABLE");
						Files.delete(child);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.toString().startsWith("submission_")){
				System.out.println("Filename starts with submission_, handling as DATA INPUT: " + fileName.toString());
//				this.dataFilePath = child.toString();
				handleExtraFormXmlInput(child.toString());

				boolean deleteSubmissions = true;
				
				if(deleteSubmissions){
					try {
						System.out.println("NOW REMOVE FILE AFTER SUBMITTING DATA");
						Files.delete(child);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else{
				System.err.println("New file has no known filename pattern. Skipping.");
			}
		}
	}
}
