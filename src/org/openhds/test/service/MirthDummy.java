package org.openhds.test.service;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openhds.test.model.SubmittedEntry;
import org.openhds.test.model.mirth.ColumnDummy;
import org.openhds.test.model.mirth.TableDummy;
import org.openhds.test.service.interfaces.DirectoryWatcherListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Class to simulate Mirth Channel process
 * ---------------------------------------
 * 
 * Will start a Thread that will pull the openhds "form" table and read the CORE_FORM attribute. 
 * If its value is null or empty, this Mirth-Dummy-channel will start reading the odk_aggregate database for the corresponding 
 * core form table structure. Will then create a mapped XML element out of the table structure and "send" this to   
 * @TODO: Insert flag into DB to signal Mirth Channel simulation when it is okay to start. 
 * 
 * */
public class MirthDummy implements DirectoryWatcherListener{
	
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	public enum EVENT { NEW_FILE, NEW_FORM };
	
	private List<String> queue;
	
//	private String tableName = "SAMPLE_CORE"; //"VISIT_REGISTRATION_CORE";
	
	public MirthDummy(){
		queue = new ArrayList<String>();
//		readDataXml();
//		checkIfUpdatesAvailable();
		
//		TableDummy table = replicateOdkTableStructure();
//		convertToXml(table);
		
		Thread channel1 = new Thread(new MirthDummyChannel1());
		channel1.start();
		
//		getFormNameFromCoreTable("SAMPLE_CORE");
		
//		Thread channel2 = new Thread(new MirthDummyChannel2());
//		channel2.start();	
	}
	
	/*Checks openhds.form table for new extra form entries */
	private class MirthDummyChannel1 implements Runnable{
		
		public MirthDummyChannel1(){
			System.out.println("Mirth Dummy Channel1 started up");
		}
		
		@Override
		public void run() {
			while(true){

				
				try{
					Thread.sleep(MirthProperties.getTimeout());
				}
				catch(InterruptedException ie){
					ie.printStackTrace();
				}
				
				checkForChanges();
			}
		}
	}
	
	/* Checks openhds.form table for ready statusflag */
//	private class MirthDummyChannel2 implements Runnable{
//		@Override
//		public void run() {
//			while(true){
//				System.out.println("Channel2");
//
//				try{
//					Thread.sleep(2000);
//				}
//				catch(InterruptedException ie){
//					ie.printStackTrace();
//				}
//				
//				sendData();
//			}
//		}
//	}
	
	private void sendData(String tableName){
		System.out.println("Send data from table: " + tableName);
		readDataXml(tableName);
	}
	
	private void checkForChanges(){		
		String sql = "SELECT uuid, formName, active, deleted, CORE_TABLE from openhds.form";
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				Statement statement = connection.createStatement(); ) 
		{	
			boolean found = false;
			ResultSet result = statement.executeQuery(sql);
			while(result.next()){
				String uuid = result.getString("uuid");
				String coreTable = result.getString("CORE_TABLE");
				String formName = result.getString("formName");
				String active = result.getString("active");
				int deleted = result.getInt("deleted");
				
				if((coreTable == null || coreTable.trim().length() == 0) && deleted == 0 
						&& (active != null && active.equalsIgnoreCase("yes"))
						&& !queue.contains(uuid)){
					
					System.out.println("Found new entry for extra form: " + formName + " (uuid: " + uuid + ")");
					
					found = true;
					queue.add(uuid);
					notify(formName, uuid);
				}
			}
			
			if(!found){
				System.out.println("[" +new Date() + "]: No new extra form entry found in db. Waiting for next retry...");
			}
			
		}
		catch(SQLException sqlException){
			System.out.println("CAUSE: " + sqlException.getCause());
			sqlException.printStackTrace();
			System.out.println(sqlException.getClass());
		}
	}
	
	/**/
	private void notify(String formName, String uuid){
		String tableName = getCoreTableFromFormName(formName);
		System.out.println("TableName for form: " + tableName);
		
		if(tableName != null){
//			setTableName(tableName);
			
			TableDummy table = replicateOdkTableStructure(tableName);
			if(table != null){
				// We set a secret-key that we then can identify on the server if it matches. 
				// only then we will create the table
				table.setKey(uuid);
				convertToXml(table);
			}
		}
		else{
			System.err.println("Could not find CORE TABLE FOR FORM: " + formName);
		}
	}	
	
//	public void setTableName(String tableName){
//		this.tableName = tableName;
//	}
	
	/* Start 
	 * */
//	private void checkIfUpdatesAvailable(){
//		MirthDummyCheckFormsForUpdateThread t = new MirthDummyCheckFormsForUpdateThread(this);
//		t.start();
//	}
	
	private TableDummy replicateOdkTableStructure(String tableName){
		//String sql = "SELECT * FROM " + tableName; // + " WHERE _URI = ?";
		TableDummy table = null;
		
		try (Connection connection = DriverManager.getConnection(url, username, password);) 
		{	
			DatabaseMetaData metaData = connection.getMetaData();			
			table = new TableDummy();
			
			List<String> primKeys = getPrimaryKeyList(connection, metaData, tableName);
//			table.setKey("secret-key");
			//table.setName("SAMPLE_CORE");
			table.setName(tableName);
			if(primKeys.size() > 0){
				StringBuilder sb = new StringBuilder();
				int primKeyCount = 1;
				for(String primKey: primKeys){
					if(primKeyCount > 1){
						sb.append(", ");
					}
					sb.append(primKey);
					primKeyCount++;
				}
				table.setPrimaryKey(sb.toString());
			}
			
			List<ColumnDummy> columns = createColumns(connection, metaData, tableName);
			table.setColumns(columns);			
		}
		catch (SQLException e) {
			    throw new IllegalStateException("Cannot connect the database!", e);
		}
		
		return table;
	}
	
	private void convertToXml(TableDummy table){
		
		try {
			String outFile = String.format("data/createTable_%s.xml", table.getName());
			System.out.println("Saving to: " + outFile);
			File file = new File(outFile);
			JAXBContext jaxbContext = JAXBContext.newInstance(TableDummy.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(table, file);
//			jaxbMarshaller.marshal(table, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	public List<ColumnDummy> createColumns(Connection con, DatabaseMetaData metaData, String tableName){	
		List<ColumnDummy> columns = new ArrayList<ColumnDummy>();
		
		int count = 0;
		
		try(ResultSet result = metaData.getColumns(null, null, tableName, null); ){
			while (result.next()) {		

				String columnName = result.getString("COLUMN_NAME");
				String defaultValue = result.getString("COLUMN_DEF");
				int dataType = result.getInt("DATA_TYPE");
				String typeName = result.getString("TYPE_NAME");
				int nullable = result.getInt("NULLABLE");
				int columnSize = result.getInt("COLUMN_SIZE");
				
				ColumnDummy column = new ColumnDummy();
				
				column.setName(columnName);
				column.setDefault_value(defaultValue);
				column.setType(typeName);
				column.setSize(Integer.toString(columnSize));
				column.setAllow_null(nullable==ResultSetMetaData.columnNoNulls?"false":"true");
				
				columns.add(column);
				
				count++;
			}					
		}		
		catch(SQLException sqlE){
			System.err.println("Exception: " + sqlE.getMessage());
		}
		
		return columns;
	}
	
//	/* Parse MetaData and create JSON like String from Data. 
//	 * <<NOT USED ANYMORE, TEST ONLY !! >>
//	 * 
//	 * */
//	public void parseDatabaseMetaData(Connection con, DatabaseMetaData metaData, String tableName){		
//		StringBuilder builder = new StringBuilder();
//		
//		builder.append("table: \n");
//		builder.append("\tname: " + tableName);
//		builder.append("\n\tkey: " + "random-generated-key");
//		builder.append("\n");
//		
//		int count = 0;
//		
//		try(ResultSet result = metaData.getColumns(null, null, tableName, null); ){
//			builder.append("field: [\n");
//			
//			while (result.next()) {		
//				
//				builder.append("{\n");
//				
//				String columnName = result.getString("COLUMN_NAME");
//				String defaultValue = result.getString("COLUMN_DEF");
//				int dataType = result.getInt("DATA_TYPE");
//				String typeName = result.getString("TYPE_NAME");
//				int nullable = result.getInt("NULLABLE");
//				int columnSize = result.getInt("COLUMN_SIZE");
//				
//				
//				builder.append("\tname: " + columnName);
//				builder.append("\n\ttype: " + typeName);
//				builder.append("\n\tsize: " + columnSize);
//				builder.append("\n\tallow_null: " + (nullable==ResultSetMetaData.columnNoNulls?"false":"true"));
//				builder.append("\n\tdefault_value: " + defaultValue);
//				
//				builder.append("\n}");
//				builder.append("\n");
//				
//				count++;
//			}		
//			
//			builder.append(" ]");
//			
//		}		
//		catch(SQLException sqlE){
//			System.err.println("Exception: " + sqlE.getMessage());
//		}
//		
//		System.out.println(builder.toString());
//	}	
	
	
	/* Connects to Core table and reads out all values line by line, 
	 * converting them into XML.
	 * TODO: Read flag, indicating that it's okay to send new data.
	 * */
	private void readDataXml(String tableName){
		
//		String tableName = "SAMPLE_CORE"; //"VISIT_REGISTRATION_CORE";
		String sql = "SELECT * FROM " + tableName; // + " WHERE _URI = ?";

		System.out.println("Going to grab data from " + tableName);
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(sql);) {

			ResultSetMetaData rsmd = rs.getMetaData();
			
			int count = 1;
			
			System.out.println("------------------------Print DATA:");
			while(rs.next()){
//				if(count > 11)
//					return;
							
				SubmittedEntry entry = new SubmittedEntry(rsmd);
				
				for(int i = 1; i <= rsmd.getColumnCount(); i++){
					String columnName = rsmd.getColumnName(i);
					String value = rs.getString(columnName);
					String columnTypeName = rsmd.getColumnTypeName(i);
					int columnPrecision = rsmd.getPrecision(i);
	//				System.out.println(columnName + ": " + value + " (" + columnTypeName + " " + columnPrecision +")");
					
	//				System.out.println("Add key " + columnName);
					entry.addEntry(columnName, value);
				}
				count++;
				
//				printSubmittedEntry(entry);
				createXml(entry, tableName);
			}
			System.out.println("----------------------END Print DATA");
			
			if(count==1) System.out.println("NO RESULTS!");
		
		} catch (SQLException e) {
			System.err.println("Error-Code: " + e.getErrorCode());
			if(e.getErrorCode() == 1146){
				System.err.println(String.format("Table '%s' doesn't exist. Could not extract data.", tableName));
			}
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}	
	
	/* Create Dummy XML to test XML creation functionality */
	private void createXml(SubmittedEntry entry, String coreTableName){
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        try {
            icBuilder = icFactory.newDocumentBuilder();
            Document doc = icBuilder.newDocument();
            Element mainRootElement = doc.createElement("extraform");
            
            //Get formName from CORE_TABLE
            String formName = getFormNameFromCoreTable(coreTableName);
            mainRootElement.setAttribute("formName", formName);
            doc.appendChild(mainRootElement);
 
            // append child elements to root element
//            addDataElement(doc, mainRootElement, "_URI", "string", "354-asfs-2345235-asfasf-76");
//            addDataElement(doc, mainRootElement, "active", "boolean", "1");
            
//            Set<String> keySet = entry.dataTuple.keySet();
//            for(String s: keySet)
//            	System.out.println(s);
            for(int i = 0; i < entry.columnCount; i++){
            	String data = entry.dataTuple.get(entry.columnNames.get(i));
//            	System.out.println("Data: " + data);
            	addDataElement(doc, mainRootElement, entry.columnNames.get(i), entry.columnTypeNames.get(i), data);
            }
 
            // output DOM XML to file/console 
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
//            StreamResult console = new StreamResult(System.out);
            
//            String dataOutputFile = "data/out.xml";
            String out = new SimpleDateFormat("'data/submissions/submission_" + coreTableName + "_'yyyy-MM-d'_'hh-mm-ss-SSS'.xml'").format(new Date());
            StreamResult console = new StreamResult(new File(out));
            
            transformer.transform(source, console);
 
            System.out.println("\nXML DOM Created Successfully..");
 
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void addDataElement(Document doc, Element element, String columnName, String type, String data){
        Element dataElement = doc.createElement("data");
        dataElement.setAttribute("columnName", columnName);
        dataElement.setAttribute("type", type);
        dataElement.setTextContent(data);
        element.appendChild(dataElement);
	}	
	
	private String getCoreTableFromFormName(String extraFormName){		
		String tableName = null;
		String sql = "SELECT _CREATION_DATE, SUBMISSION_FORM_ID, URI_SUBMISSION_DATA_MODEL FROM _form_info_submission_association";
		
		try (Connection connection = DriverManager.getConnection(url, username, password); 
				Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql);) {
  
		    while (rs.next()) {
		    	String c = rs.getString("_CREATION_DATE");
		        String form = rs.getString("SUBMISSION_FORM_ID");
		        String uri = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        
		        if(form.equalsIgnoreCase(extraFormName)){		        		
//		        	System.out.println("Found extra form to handle: ");
		        	tableName = processForm(uri, connection);
		        }
		    }		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
		
		return tableName;
	}
	
	private String processForm(String uri, Connection connection) throws SQLException{
		String tableNameResult = null; 
		String sql = "SELECT _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PARENT_URI_FORM_DATA_MODEL FROM _form_data_model WHERE URI_SUBMISSION_DATA_MODEL = ?";
		
		try(PreparedStatement ps = connection.prepareStatement(sql);){
			ps.setString(1, uri);
			
			try(ResultSet rs = ps.executeQuery();){
			    if (rs.next()) {
			    	String elementType = rs.getString("ELEMENT_TYPE");
			    	String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			    	String element_uri = rs.getString("_URI");
			    	String parent_uri = rs.getString("PARENT_URI_FORM_DATA_MODEL");
			    	String tableName = rs.getString("PERSIST_AS_TABLE_NAME");
			    	String elementName = rs.getString("ELEMENT_NAME");
			    	
		//	    	System.out.println("columnName: " + columnName + " " + (parent_uri.equals(uri)) + " " + element_uri);
			    	
			    	// Check for starting group 	
			    	if(parent_uri.equals(uri) && elementType.equalsIgnoreCase("GROUP") && (columnName == null)){
//			    		System.out.println("Core table: " + tableName);
			    		tableNameResult = tableName;
		//	    		printLog("-------------------- Found starting group, beginning to parse... -----------------------------");
		//	    		printLog("-------------------- End parsing form. -------------------------------------------------------\n");
			    	}
			    }
			}
		}
		return tableNameResult;
	}
	
	private List<String> getPrimaryKeyList(Connection con, DatabaseMetaData metaData, String tableName){
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
		
	private String getFormNameFromCoreTable(String coreTableName){
		
		String sql = "SELECT SUBMISSION_FORM_ID FROM _form_data_model " + 
		"LEFT JOIN _form_info_submission_association ON _form_info_submission_association.URI_SUBMISSION_DATA_MODEL = _form_data_model.PARENT_URI_FORM_DATA_MODEL " + 
		"WHERE _form_data_model.PERSIST_AS_TABLE_NAME = ? AND _form_data_model.PERSIST_AS_COLUMN_NAME is null AND _form_data_model.ORDINAL_NUMBER = 1;";
		
		String returnFormName = null;
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement p = connection.prepareStatement(sql);) 
		{				
			p.setString(1, coreTableName);
			
			try(ResultSet result = p.executeQuery();){
				if(result.next()){
					String formId = result.getString("SUBMISSION_FORM_ID");
					returnFormName = formId;
					System.out.println("Grabbed FormId for CORETABLE: " + formId + " |  " + coreTableName);
				}
			}
		}
		catch(SQLException sqlException){
			System.out.println("CAUSE: " + sqlException.getCause());
			sqlException.printStackTrace();
			System.out.println(sqlException.getClass());
		}
		
		return returnFormName;
	}
	
	@Override
	public void onDirectoryChanged(WatchEvent.Kind<?> kind, Path fileName, Path dir) {	
		Path child = dir.resolve(fileName);
		System.out.println("MirthDummy::onDirectoryChanged: " + fileName + " | " + child.toString());
			
		if(kind == StandardWatchEventKinds.ENTRY_DELETE && fileName.toString().startsWith("createTable")){				
			System.out.println("Filename startswith createTable, handling as DATA INPUT");
			try{
				String tableName = fileName.toString().substring(12);
				if(tableName.length() > 4 && child.toString().toLowerCase().endsWith(".xml")){
					tableName = tableName.substring(0, tableName.length()-4);
					sendData(tableName);
				}
				else{
					System.err.println("Could not get table name from file: " + fileName);
				}
			}
			catch(IndexOutOfBoundsException oobe){
				oobe.printStackTrace();
			}
		}
	}
}
