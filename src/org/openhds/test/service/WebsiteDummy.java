package org.openhds.test.service;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.openhds.test.model.SubmittedEntry;

/*
 * Simulate display and selection of extra forms for merging from ODK Db into OpenHDS Db
 * */
public class WebsiteDummy {
	
	final static String url = "jdbc:mysql://data-management.local:3306/openhds";
	final static String url2 = "jdbc:mysql://data-management.local:3306/test";
	final static String username = "data";
	final static String password = "data";
	private boolean verbose = true;
	private List<String> uuids;
	
	public WebsiteDummy(){
		System.out.println("WebsiteDummy");
	}
	
	private void connect(){
		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    printLog("Database connected!");
		    
		    Statement stmt = connection.createStatement();
		    ResultSet rs;
		    
		    rs = stmt.executeQuery("SELECT COUNT(*) FROM form");
		    
	        int rowCount = 0;
	        while(rs.next()) {
	            rowCount = Integer.parseInt(rs.getString("count(*)"));
	        }
	        printLog("Extra Forms " + "(Available " + rowCount + "):");
		    
		    rs = stmt.executeQuery("SELECT formName, active, deleted, gender, insertDate, uuid FROM form");
		    
		    List<String> extraForms = new ArrayList<String>();
		    uuids = new ArrayList<String>();
		    
		    int count = 1;
		    while (rs.next()) {
		    	String formName = rs.getString("formName");
		        String active = rs.getString("active");
		        String deleted = rs.getString("deleted");
		        String gender = rs.getString("gender");
		        String insertDate = rs.getString("insertDate");
		        String uuid = rs.getString("uuid");
		        
		        printLog("[" +count + "] Name: " + formName + " | Active: " + active + " | Deleted? " + deleted + " | Gender: " + gender + "  | Inserted on: " + insertDate + "  | uuid: " + uuid);
		        
		        extraForms.add(formName);
		        uuids.add(uuid);
//		        processForm(uri, connection);
		        count++;
		    }
		    System.out.println("[0] Exit program");

		    int selection = getSelection();
		    handleSelection(selection, extraForms);
		    
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
	}
	
	private int getSelection(){
		int returnValue = -1;
		
		System.out.print("Please input selection: ");
		Scanner s = new Scanner(System.in);
		if(s.hasNextInt()){	
			int selection = s.nextInt();
			returnValue = selection;
			System.out.println("Found input: " + selection);
		}
		else{
			System.err.println("Please input an integer!");
		}
		
		s.close();
		return returnValue;
	}
	
	private void handleSelection(int selection, List<String> extraForms){
	    if(selection > 0 && selection <= extraForms.size()){
	    	System.out.println("Continue");
	    	
	    	String selectedForm = extraForms.get(selection-1);
	    	String uuid = uuids.get(selection-1);
	    	
	    	System.out.println("Selected form: " + selectedForm + " " + uuid);
	    	
	    	getFormData(uuid);
	    }
	    else{
	    	System.out.println("Unknown form # or Exit program. Bye");
	    }
	}
		
	private void printLog(String logEntry){
		if(verbose)
			System.out.println(logEntry);
	}
	
	private void getFormData(String uuid){
		System.out.println("Grabbing data for uuid: " + uuid);
		
		String sql = "SELECT CORE_TABLE FROM openhds.form WHERE uuid = ?";
		try (Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement ps = connection.prepareStatement(sql)) {
			
			ps.setString(1, uuid);
			
			try(ResultSet rs = ps.executeQuery();){
				if(rs.next()){
					String coreTable = rs.getString("CORE_TABLE");
					System.out.println("Core Table: " + coreTable);
					
					if(coreTable != null){
						String data = readData(coreTable);
						writeHtml(coreTable, data);
						System.out.println("Done!");
					}
					else{
						System.err.println("Could not find CORE_TABLE in openhds.form");
					}
				}
			}
		}
		catch(SQLException sqle){
			sqle.printStackTrace();
		}
	}
	
	private String readData(String tableName){
		
//		String tableName = "SAMPLE_CORE"; //"VISIT_REGISTRATION_CORE";
		String sql = "SELECT * FROM " + tableName; // + " WHERE _URI = ?";
		StringBuilder sb = new StringBuilder();

		System.out.println("Going to grab data from " + tableName);
		
		try (Connection connection = DriverManager.getConnection(url2, username, password);
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(sql);) {

			ResultSetMetaData rsmd = rs.getMetaData();
			
			int count = 1;
			
//			System.out.println("------------------------Print DATA:");
			
			sb.append("<h1>" + tableName + "</h1>");
			sb.append("<h3>Updated on " + new Date() + "</h3>");
			sb.append("<table border='1'>");
			if(rs.next()){
				sb.append("<tr>");
				sb.append("<th>#</th>");
				for(int i = 1; i <= rsmd.getColumnCount(); i++){
					String columnName = rsmd.getColumnName(i);
					sb.append("<th>"+ columnName + "</th>");
				}
				sb.append("</tr>");
				rs.beforeFirst();
			}
			while(rs.next()){
//				if(count > 11)
//					return;
				sb.append("<tr>");
				SubmittedEntry entry = new SubmittedEntry(rsmd);
//				System.out.println(count + ":");
				sb.append("<td>" + count + "</td>");
				for(int i = 1; i <= rsmd.getColumnCount(); i++){
					String columnName = rsmd.getColumnName(i);
					String value = rs.getString(columnName);
					String columnTypeName = rsmd.getColumnTypeName(i);
					int columnPrecision = rsmd.getPrecision(i);
//					System.out.print(columnName + ": " + value + " (" + columnTypeName + " " + columnPrecision +")" + " | ");
					
//					sb.append("<td>" + value + " (" + columnTypeName + " " + columnPrecision +")" + "</td>");
					sb.append("<td>" + value + "</td>");
	//				System.out.println("Add key " + columnName);
					entry.addEntry(columnName, value);
				}
				count++;
				
//				System.out.println();
//				printSubmittedEntry(entry);
//				createXml(entry, tableName);
				sb.append("</tr>");
			}
			sb.append("</table>");
			sb.append("Entries: " + (count-1));
//			System.out.println("----------------------END Print DATA");
			
			if(count==1) System.out.println("NO RESULTS!");
		
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}
		return sb.toString();
	}	
	
	private void writeHtml(String title, String body){
		try{
			File htmlTemplateFile = new File("data/template.html");
			String htmlString = FileUtils.readFileToString(htmlTemplateFile);
//			String title = "New Page";
//			String body = "This is Body";
			htmlString = htmlString.replace("$title", title);
			htmlString = htmlString.replace("$body", body);
			String outputFile = "data/" + title + ".html";
			File newHtmlFile = new File(outputFile);
			FileUtils.writeStringToFile(newHtmlFile, htmlString);
			System.out.println("Saved to " + outputFile);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		new WebsiteDummy().connect();
//		new WebsiteDummy().writeHtml();
	}

}
