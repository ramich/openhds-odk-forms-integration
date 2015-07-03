package org.openhds.test.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class OpenHDSDatabaseService {

	final static String url = "jdbc:mysql://data-management.local:3306/openhds";
	final static String username = "data";
	final static String password = "data";
	
	public enum Entity { FIELDWORKER, INDIVIDUAL, VISIT, LOCATION, ROUND, SOCIALGROUP };
	
	
	public String getUuIdForEntityExtId(String extId, Entity entity){
		String return_uuid = null;
		
		String schema = "openhds";
		String sql = "SELECT uuid FROM " + schema + ".%s WHERE extId = ?";
		
		if(entity == Entity.INDIVIDUAL){
			sql = String.format(sql, "individual");
		}
		else if(entity == Entity.FIELDWORKER){
			sql = String.format(sql, "fieldworker");
		}
		else if(entity == Entity.LOCATION){
			sql = String.format(sql, "location");
		}
		else if(entity == Entity.ROUND){
			sql = String.format(sql, "round");
		}		
		else if(entity == Entity.VISIT){
			sql = String.format(sql, "visit");
		}
		else if(entity == Entity.SOCIALGROUP){
			sql = String.format(sql, "socialgroup");
		}
		
		try(Connection connection = DriverManager.getConnection(url, username, password);
				PreparedStatement ps = connection.prepareStatement(sql); ){

			ps.setString(1, extId);
			
			try(ResultSet rs = ps.executeQuery();){
				if(rs.next()){
					String uuid = rs.getString("uuid");
					return_uuid = uuid;
				}
			}
		}
		catch(SQLException sqlE){
			sqlE.printStackTrace();
		}
		
		return return_uuid;
	}
}
