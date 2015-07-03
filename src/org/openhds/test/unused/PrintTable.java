package org.openhds.test.unused;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * @author Abhishek Somani
 * http://www.javaroots.com/2013/09/print-tables-details-in-schema-jdbc.html
 */
public class PrintTable {

	public static String SCHEMA_NAME = "${YOUR_SCHEMA_NAME}";
	final static String url = "jdbc:mysql://data-management.local:3306/odk_prod";
	final static String username = "data";
	final static String password = "data";

	public static void main(String[] args) {

		// create and setup your database and get db connection
//		DataBase db = new DataBase();
//		db.init();

		try {
			Connection con = DriverManager.getConnection(url, username, password); //db.getConnection();
			DatabaseMetaData metaData = con.getMetaData();

			String tableType[] = { "TABLE" };

			StringBuilder builder = new StringBuilder();

			ResultSet result = metaData.getTables(null, SCHEMA_NAME, null,
					tableType); 
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
				builder.deleteCharAt(builder.lastIndexOf(","));
				builder.append(" )");
				builder.append("\n");
				builder.append("----------------");
				builder.append("\n");
			}

			System.out.println(builder.toString());

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
