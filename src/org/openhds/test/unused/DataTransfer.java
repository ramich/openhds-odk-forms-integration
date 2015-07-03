package org.openhds.test.unused;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openhds.test.model.ElementInterface;
import org.openhds.test.model.FormDataModel;
import org.openhds.test.model.PlainElement;
import org.openhds.test.model.StructuredElement;
import org.openhds.test.model.SubmittedEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DataTransfer {

	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	private boolean verbose = true;
	
	public DataTransfer(){
//		startProgram();
		getTableStructureForSpecificExtraForm();
	}
		
	/* Will just iterate over all extra forms */
	private void startProgram(){
		printLog("Connecting to database...");
		
		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    printLog("Database connected!");
		    
		    Statement stmt = connection.createStatement();
		    ResultSet rs;
		    
		    rs = stmt.executeQuery("SELECT COUNT(*) FROM _form_info_submission_association");
		    
	        int rowCount = 0;
	        while(rs.next()) {
	            rowCount = Integer.parseInt(rs.getString("count(*)"));
	        }
	        printLog("Forms " + "(Available " + rowCount + "):");
		    
		    rs = stmt.executeQuery("SELECT _CREATION_DATE, SUBMISSION_FORM_ID, URI_SUBMISSION_DATA_MODEL FROM _form_info_submission_association");
		    
		    while (rs.next()) {
		    	String c = rs.getString("_CREATION_DATE");
		        String form = rs.getString("SUBMISSION_FORM_ID");
		        String uri = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        
		        if(form.equalsIgnoreCase("visit_registration")){
		        	printLog(form + " / " + c + " (" + uri + ")");
		        	processForm(uri, connection);
		        }
		    }
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}		
	}
	
	/* Will just handle one specific extra forms */
	private void getTableStructureForSpecificExtraForm(){
		
		String extraFormName = "visit_registration";
		
		printLog("Connecting to database...");
		
		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    printLog("Database connected!");
		    
		    Statement stmt = connection.createStatement();
		    ResultSet rs;
		    
		    rs = stmt.executeQuery("SELECT _CREATION_DATE, SUBMISSION_FORM_ID, URI_SUBMISSION_DATA_MODEL FROM _form_info_submission_association");
		    
		    while (rs.next()) {
		    	String c = rs.getString("_CREATION_DATE");
		        String form = rs.getString("SUBMISSION_FORM_ID");
		        String uri = rs.getString("URI_SUBMISSION_DATA_MODEL");
		        
		        if(form.equalsIgnoreCase(extraFormName)){
		        	System.out.println("Found extra form to handle!");
		        	printLog(form + " / " + c + " (" + uri + ")");
		        	processForm(uri, connection);
		        }
		    }
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}
	
	private void processForm(String uri, Connection connection) throws SQLException{
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    String sql = "SELECT _URI, ELEMENT_TYPE, PERSIST_AS_TABLE_NAME, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PARENT_URI_FORM_DATA_MODEL FROM _form_data_model WHERE URI_SUBMISSION_DATA_MODEL = ?";
		ps = connection.prepareStatement(sql);
		ps.setString(1, uri);

	    rs = ps.executeQuery();
	    
	    FormDataModel model = null;
	    
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
	    		model = new FormDataModel();
	    		model.setELEMENT_TYPE(elementType);
	    		model.setELEMENT_NAME(elementName);
	    		model.setPERSIST_AS_TABLE_NAME(tableName);
	    		List<FormDataModel> children = processUri(element_uri, tableName, connection);
	    		for(FormDataModel child: children){
	    			model.addChild(child);
	    		}
//	    		printLog("-------------------- End parsing form. -------------------------------------------------------\n");
	    	}
	    }
	    
	    System.out.println("FormDataModel created for form >>" + model.getELEMENT_NAME() + "<<");
//	    System.out.println("\nNow printing datamodel:");
//	    printModel(model);
//	    System.out.println("\nNow printing data:");
	    getDataFromModel(model, connection);
//	    createXmlFromData(model, connection);
	}
	
//	private void createXmlFromData(FormDataModel model, Connection connection) throws SQLException{
//		System.out.println("Going to grab data from " + model.getPERSIST_AS_TABLE_NAME());
//		
//		String sql = "SELECT * FROM " + model.getPERSIST_AS_TABLE_NAME(); // + " WHERE _URI = ?";
//	    ResultSet rs = null;	    
//	    Statement stmt = connection.createStatement();
//		rs = stmt.executeQuery(sql);
//		ResultSetMetaData rsmd = rs.getMetaData();
//		
//		int count = 1;
//		
//		System.out.println("------------------------Print DATA:");
//		while(rs.next()){
//			if(count > 11)
//				return;
//						
//			SubmittedEntry entry = new SubmittedEntry(rsmd);
//			
//			for(int i = 1; i <= rsmd.getColumnCount(); i++){
//				String columnName = rsmd.getColumnName(i);
//				String value = rs.getString(columnName);
//				String columnTypeName = rsmd.getColumnTypeName(i);
//				int columnPrecision = rsmd.getPrecision(i);
////				System.out.println(columnName + ": " + value + " (" + columnTypeName + " " + columnPrecision +")");
//				
////				System.out.println("Add key " + columnName);
//				entry.addEntry(columnName, value);
//			}
//			count++;
//			
//			printSubmittedEntry(entry);
//		}
//		System.out.println("----------------------END Print DATA");
//		
//		if(count==1) System.out.println("NO RESULTS!");
//	}
//	
//	private void printSubmittedEntry(SubmittedEntry entry){	
//		entry.print();
//	}
	
	private void getDataFromModel(FormDataModel model, Connection connection) throws SQLException{
		System.out.println("Going to grab data from " + model.getPERSIST_AS_TABLE_NAME());
		
		String sql = "SELECT * FROM " + model.getPERSIST_AS_TABLE_NAME(); // + " WHERE _URI = ?";
	    ResultSet rs = null;	    
	    Statement stmt = connection.createStatement();
		rs = stmt.executeQuery(sql);
		
		int count = 1;
		while(rs.next()){
			if(count > 11)
				return;
			
			System.out.println(count + ": ------------------------START---------------------------");
			
			for(FormDataModel children : model.getChildren()){
				getResultSetDataWithFormDataModel(children, rs);
			}
			System.out.println(count + ": ----------------------END-------------------------------");
			count++;
		}
		
		if(count==1) System.out.println("NO RESULTS!");
			
	}
	
	private void getResultSetDataWithFormDataModel(FormDataModel model, ResultSet rs) throws SQLException{
		String columnName = model.getPERSIST_AS_COLUMN_NAME();
		String ELEMENT_NAME = model.getELEMENT_NAME();
		String ELEMENT_TYPE = model.getELEMENT_TYPE();
		
		if(columnName == null){
			System.out.println(ELEMENT_TYPE + ":[");
			for(FormDataModel children : model.getChildren()){
				getResultSetDataWithFormDataModel(children, rs);
			}
			System.out.println("]");
		}
		else{
	    	String value = rs.getString(columnName);
//	    	System.out.println(ELEMENT_NAME + ": " + value);
	    	System.out.format("%16s %16s %16s", ELEMENT_NAME, columnName, value);
		}
		System.out.println();
	}
	
	private void printModel(FormDataModel model){
	    if(model != null){
	    	System.out.println(model.getELEMENT_TYPE() + " " + model.getELEMENT_NAME() + " " + model.getPERSIST_AS_COLUMN_NAME());
	    	int childCount = model.getChildren().size();
	    	if(childCount > 0){
		    	System.out.println("Model has children: " + childCount);
		    	for(FormDataModel child: model.getChildren()){
		    		printModel(child);
		    	}
	    	}
	    }
	}
	
	private List<FormDataModel> processUri(String _uri, String coreTable, Connection connection) throws SQLException{
		
	    String sql = "SELECT _URI, _CREATOR_URI_USER, _CREATION_DATE, _LAST_UPDATE_URI_USER, _LAST_UPDATE_DATE, URI_SUBMISSION_DATA_MODEL, PARENT_URI_FORM_DATA_MODEL, ORDINAL_NUMBER, ELEMENT_TYPE, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PERSIST_AS_TABLE_NAME, PERSIST_AS_SCHEMA_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = connection.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
		List<FormDataModel> dataModel = new ArrayList<FormDataModel>();
		
		while (rs.next()) {
			String element_uri = rs.getString("_URI");
			
			String _CREATOR_URI_USER = rs.getString("_CREATOR_URI_USER");		
			String _CREATION_DATE = rs.getString("_CREATION_DATE");		
			String _LAST_UPDATE_URI_USER = rs.getString("_LAST_UPDATE_URI_USER");		
			String _LAST_UPDATE_DATE = rs.getString("_LAST_UPDATE_DATE");		
			String URI_SUBMISSION_DATA_MODEL = rs.getString("URI_SUBMISSION_DATA_MODEL");		
			String PARENT_URI_FORM_DATA_MODEL = rs.getString("PARENT_URI_FORM_DATA_MODEL");		
			String ORDINAL_NUMBER = rs.getString("ORDINAL_NUMBER");		
			String ELEMENT_TYPE = rs.getString("ELEMENT_TYPE");		
			String ELEMENT_NAME = rs.getString("ELEMENT_NAME");		
			String PERSIST_AS_COLUMN_NAME = rs.getString("PERSIST_AS_COLUMN_NAME");		
			String PERSIST_AS_TABLE_NAME = rs.getString("PERSIST_AS_TABLE_NAME");		
			String PERSIST_AS_SCHEMA_NAME = rs.getString("PERSIST_AS_SCHEMA_NAME");	

			FormDataModel formDataModel = new FormDataModel();
			formDataModel.set_URI(element_uri);
			formDataModel.set_CREATOR_URI_USER(_CREATOR_URI_USER);
			formDataModel.set_CREATION_DATE(_CREATION_DATE);
			formDataModel.set_LAST_UPDATE_URI_USER(_LAST_UPDATE_URI_USER);
			formDataModel.set_LAST_UPDATE_DATE(_LAST_UPDATE_DATE);
			formDataModel.setURI_SUBMISSION_DATA_MODEL(URI_SUBMISSION_DATA_MODEL);
			formDataModel.setPARENT_URI_FORM_DATA_MODEL(PARENT_URI_FORM_DATA_MODEL);
			formDataModel.setORDINAL_NUMBER(ORDINAL_NUMBER);
			formDataModel.setELEMENT_TYPE(ELEMENT_TYPE);
			formDataModel.setELEMENT_NAME(ELEMENT_NAME);
			formDataModel.setPERSIST_AS_COLUMN_NAME(PERSIST_AS_COLUMN_NAME);
			formDataModel.setPERSIST_AS_TABLE_NAME(PERSIST_AS_TABLE_NAME);
			formDataModel.setPERSIST_AS_SCHEMA_NAME(PERSIST_AS_SCHEMA_NAME);
			
//			handleFormDataEntry(element_uri, connection);
			
			ElementInterface.ElementType enumType = ElementInterface.ElementType.valueOf(ELEMENT_TYPE);	
			switch(enumType){
	          case STRING:
	          case JRDATETIME:
	          case JRDATE:
	          case JRTIME:
	          case INTEGER:
	          case DECIMAL:
//	        	  printLog(enumType.toString());
	        	  break;
				case GROUP:{
//					printLog("- GROUP:");
					List<FormDataModel> subElements = processUri(element_uri, coreTable, connection);
					formDataModel.setChildren(subElements);
//					printLog("- END GROUP:");
					break;
				}
				case GEOPOINT: {
//					printLog("- GEOPOINT: " + element_uri);
					List<FormDataModel> subElements = processUri(element_uri, coreTable, connection);
//					printLog("- END GEOPOINT. Found elements: " + subElements.size());
					
					formDataModel.setChildren(subElements);
					break;
				}
				default:{
					
				}
			}
			
			dataModel.add(formDataModel);
		}
		
		return dataModel;
		
//		handleFormDataEntry3(dataModel, coreTable, connection);
	}
	
//	private void handleFormDataEntry2(List<FormDataModel> dataModel, String coreTable, Connection conn) throws SQLException{
//		
//		String sql = "SELECT * FROM " + coreTable; // + " WHERE _URI = ?";
//	    ResultSet rs = null;
//	    
//	    Statement stmt = conn.createStatement();
//		
//		rs = stmt.executeQuery(sql);
//		
//		int count = 1;
//		while(rs.next()){
//			Map<String, String> row = new HashMap<String, String>();
//			
//			for(FormDataModel formDataModel: dataModel){
//				
//				String elementType = formDataModel.getELEMENT_TYPE();
//				String element_uri = formDataModel.get_URI();
//				String columnName = formDataModel.getPERSIST_AS_COLUMN_NAME();
//				String tableName = formDataModel.getPERSIST_AS_TABLE_NAME();
//				String elementName = formDataModel.getELEMENT_NAME();
//				
////				System.out.println("columnName: " + columnName);
//				
//				if(columnName != null){
//			    	String value = rs.getString(columnName);
//	//				printLog(columnName + " " + tableName + " " + elementType + " " + elementName);
//					
//	//				printSubmittedFormData(formDataModel, coreTable, conn);
////					System.out.print(columnName + " : " + value + " | ");
//			    	row.put(columnName, value);
//				}
//				else{
//					printLog("- Found structured element of type " + elementType + " with name " +elementName + " Persisted in table " + tableName + " -");
//					handleStructuredElement(element_uri, conn);
//				}
//			}
//			
//			printRow(row, count);
//			count++;
//		}
//	}
	
//	private void handleStructuredElement(String _uri, Connection conn) throws SQLException{
//		
//		printLog("Handle Structured element: " + _uri);
//	    String sql = "SELECT _URI, _CREATOR_URI_USER, _CREATION_DATE, _LAST_UPDATE_URI_USER, _LAST_UPDATE_DATE, URI_SUBMISSION_DATA_MODEL, PARENT_URI_FORM_DATA_MODEL, ORDINAL_NUMBER, ELEMENT_TYPE, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PERSIST_AS_TABLE_NAME, PERSIST_AS_SCHEMA_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
//	    ResultSet rs = null;
//	    PreparedStatement ps = null;
//	    
//		ps = conn.prepareStatement(sql);
//		ps.setString(1, _uri);
//		
//		rs = ps.executeQuery();
//		
//		while(rs.next()){
//			String value = rs.getString("ELEMENT_NAME");
//			String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
//			printLog(value + " " + columnName);
//		}
//	}
	
//	private void printRow(Map<String, String> row, int count){
//	    Iterator it = row.entrySet().iterator();
//	    printLog("Row " + count + ": ");
//	    while (it.hasNext()) {
//	        Map.Entry pair = (Map.Entry)it.next();
////	        System.out.print(columnName + " : " + value + " | ");
//	        printLog(pair.getKey() + " : " + pair.getValue() + " | ");
//	        it.remove(); // avoids a ConcurrentModificationException	
//	    }
//
//		System.out.println();
//	}
	
	private void printSubmittedFormData(FormDataModel formDataModel, String coreTable, Connection conn) throws SQLException{
		
		String sql = "SELECT * FROM " + coreTable; // + " WHERE _URI = ?";
	    ResultSet rs = null;
	    
	    Statement stmt = conn.createStatement();
		
		rs = stmt.executeQuery(sql);
		
		
		
	    while (rs.next()) {
	    	String columnName = formDataModel.getPERSIST_AS_COLUMN_NAME();
	    	String value = rs.getString(columnName);
	    	
	    	printLog(">" + columnName + " : " + value);
	    }
	}
	
	private void handleFormDataEntry(String _uri, Connection conn) throws SQLException{
	    String sql = "SELECT PERSIST_AS_COLUMN_NAME, _URI, ELEMENT_TYPE, PERSIST_AS_COLUMN_NAME,ELEMENT_NAME, PERSIST_AS_TABLE_NAME FROM _form_data_model WHERE _URI = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = conn.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
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
				
//				if(elementType.equalsIgnoreCase("geopoint")){
//					handleGeopoint(element_uri, conn);
//				}
//				else if(elementType.equalsIgnoreCase("GROUP")){
//					handleGroup(element_uri, conn);
//				}
//				else if(elementType.equalsIgnoreCase("REPEAT")){
//					handleRepeat(element_uri, conn);
//				}
//				else if(elementType.equalsIgnoreCase("BINARY")){
//					handleBinary(element_uri, conn);
//				}		 
//				else if(elementType.equalsIgnoreCase("BINARY_CONTENT_REF_BLOB")){
//					handleBinaryRef(element_uri, conn);
//				}	
//				else if(elementType.equalsIgnoreCase("REF_BLOB")){
//					handleBlob(element_uri, conn);
//				}	
//				else if(elementType.equalsIgnoreCase("SELECTN")){
//					System.out.println("SELECTN: " + tableName);
//					handleSelectN(element_uri, conn);
//				}	
//				else{
//					printLog("!!!!!!!!!!!!!UNKNOWN HANDLER!!!!!!!!!!!!!!!!!! " + elementType);
//				}
			}
		}
	}
	
//	private void processSubmittedFormEntry(){
//		
//	}
	
	private void handleFormDataEntry3(List<FormDataModel> dataModel, String coreTable, Connection conn) throws SQLException{
		
		String sql = "SELECT * FROM " + coreTable; // + " WHERE _URI = ?";
	    ResultSet rs = null;
	    
	    Statement stmt = conn.createStatement();
		
		rs = stmt.executeQuery(sql);
		
		int count = 1;
		while(rs.next()){
			
			getData(dataModel, rs, conn);
			
//			printRow(row, count);
			count++;
		}
	}	
	
	private List<ElementInterface> getData(List<FormDataModel> dataModel, ResultSet rs, Connection conn) throws SQLException{
		List<ElementInterface> elements = new ArrayList<ElementInterface>();
		
		for(FormDataModel formDataModel: dataModel){
			
			String elementType = formDataModel.getELEMENT_TYPE();
			String element_uri = formDataModel.get_URI();
			String columnName = formDataModel.getPERSIST_AS_COLUMN_NAME();
			String tableName = formDataModel.getPERSIST_AS_TABLE_NAME();
			String elementName = formDataModel.getELEMENT_NAME();
			
//			System.out.println("columnName: " + columnName);
			
			if(columnName != null){
		    	String value = rs.getString(columnName);
//				printLog(columnName + " " + tableName + " " + elementType + " " + elementName);
				
//				printSubmittedFormData(formDataModel, coreTable, conn);
		    	printLog(columnName + " : " + value + " | ");
		    	PlainElement e = new PlainElement(ElementInterface.ElementType.STRING);
		    	e.setValue(value);
		    	
		    	elements.add(e);
			}
			else{
				printLog("- Found structured element of type " + elementType + " with name " + elementName + " Persisted in table " + tableName + " -");
				ElementInterface.ElementType enumType = ElementInterface.ElementType.valueOf(elementType);
				
				StructuredElement struEl = new StructuredElement(enumType);	
				
				switch(enumType){
					case GROUP:
					case GEOPOINT:
						printLog("Handling structured element of type " + enumType);		
						List<ElementInterface> childElements = handleStructuredElement2(element_uri, conn);
						struEl.setChildElements(childElements);
						break;
					case REPEAT:	
					case BINARY:
						break;
					default:{
						printLog("No handled structure type found: " + enumType);
						System.exit(0);
					}
				}
				
				elements.add(struEl);
				
			}
		}	
		
		return elements;
	}
	
	private List<ElementInterface> handleStructuredElement2(String _uri, Connection conn) throws SQLException{
		
		List<ElementInterface> childElements = new ArrayList<ElementInterface>();
		
		printLog("Handle Structured element: " + _uri);
	    String sql = "SELECT _URI, _CREATOR_URI_USER, _CREATION_DATE, _LAST_UPDATE_URI_USER, _LAST_UPDATE_DATE, URI_SUBMISSION_DATA_MODEL, PARENT_URI_FORM_DATA_MODEL, ORDINAL_NUMBER, ELEMENT_TYPE, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PERSIST_AS_TABLE_NAME, PERSIST_AS_SCHEMA_NAME FROM _form_data_model WHERE PARENT_URI_FORM_DATA_MODEL = ?";
	    ResultSet rs = null;
	    PreparedStatement ps = null;
	    
		ps = conn.prepareStatement(sql);
		ps.setString(1, _uri);
		
		rs = ps.executeQuery();
		
		while(rs.next()){
			String value = rs.getString("ELEMENT_NAME");
			String columnName = rs.getString("PERSIST_AS_COLUMN_NAME");
			String ELEMENT_TYPE = rs.getString("ELEMENT_TYPE");
			
			printLog("------------------------> " + value + " " + columnName + " " + ELEMENT_TYPE);
			
			if(ElementInterface.ElementType.valueOf(ELEMENT_TYPE) == ElementInterface.ElementType.GROUP){
				printLog("CHILD ELEMENT OF TYPE GROUP FOUND !");
				System.exit(0);
			}
			StructuredElement e = new StructuredElement(ElementInterface.ElementType.valueOf(ELEMENT_TYPE));
			
			childElements.add(e);
			
//            switch ( ElementInterface.ElementType.valueOf(ELEMENT_TYPE) ) {
//            // xform tag types
//            case STRING:
//            case JRDATETIME:
//            case JRDATE:
//            case JRTIME:
//            case INTEGER:
//            case DECIMAL:
//            case GEOPOINT:
//            case BINARY:  // identifies BinaryContent table
//            case BOOLEAN:
//            case SELECT1: // identifies SelectChoice table
//            case SELECTN: // identifies SelectChoice table
//            case REPEAT:
//            case GROUP:
//                    return null;
//            case PHANTOM: // if a relation needs to be divided in order to fit
//                    return null;
//            case BINARY_CONTENT_REF_BLOB: // association between VERSIONED_BINARY and REF_BLOB
//            case REF_BLOB: // the table of the actual byte[] data (xxxBLOB)
//            case LONG_STRING_REF_TEXT: // association between any field and REF_TEXT
//            case REF_TEXT: // the table of extended string values (xxxTEXT)
//            default:
//                    throw new IllegalStateException("unexpected request for unreferencable element type");
//            }
		}
		
		return childElements;
	}	
	
	private void printLog(String logEntry){
		if(verbose)
			System.out.println(logEntry);
	}
}
