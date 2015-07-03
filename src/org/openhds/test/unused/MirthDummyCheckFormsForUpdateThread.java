package org.openhds.test.unused;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.openhds.test.service.MirthDummy;

public class MirthDummyCheckFormsForUpdateThread extends Thread{
	
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";
	
	private MirthDummy mirth;
	
	public MirthDummyCheckFormsForUpdateThread(MirthDummy mirth){
		this.mirth = mirth;
	}
	
	@Override
	public void run() {
		while(true){
			
			check();
			
			try{
				Thread.sleep(2000);
			}
			catch(InterruptedException ie){
				
			}
		}
		
	}
	
	private void check(){
		String sql = "SELECT uuid, formName, active, deleted, CORE_TABLE from openhds.form";
		
		try (Connection connection = DriverManager.getConnection(url, username, password);
				Statement statement = connection.createStatement(); ) 
		{	
			ResultSet result = statement.executeQuery(sql);
			while(result.next()){
				String uuid = result.getString("uuid");
				String coreTable = result.getString("CORE_TABLE");
				String formName = result.getString("formName");
				String active = result.getString("active");
				int deleted = result.getInt("deleted");
				
				if((coreTable == null || coreTable.trim().length() == 0) && deleted == 0 
						&& (active != null && active.equalsIgnoreCase("yes"))){
					
					System.out.println("Found new entry for extra form: " + formName + " (uuid: " + uuid + ")");
					
					update(formName, uuid);
				}
			}
			
		}
		catch(SQLException sqlException){
			sqlException.printStackTrace();
		}
	}
	
	private void update(String formName, String uuid){
		System.out.println("Notifying mirth dummy");
//		mirth.foundNewExtraForm(formName, uuid);
	}
}
