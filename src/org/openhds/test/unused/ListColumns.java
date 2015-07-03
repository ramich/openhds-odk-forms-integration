package org.openhds.test.unused;

/**
 * ListColumns.java
 * Copyright (c) 2007 by Dr. Herong Yang. All rights reserved.
 * Copied from http://www.herongyang.com/JDBC/sqljdbc-jar-Column-List.html
 */
import java.sql.*;
public class ListColumns {
  public static void main(String [] args) {
    Connection con = null;
    try {

      Class.forName("com.mysql.jdbc.Driver");
      con = DriverManager.getConnection(
          "jdbc:mysql://data-management.local:3306/odk_prod?"
        + "user=data&password=data");

      String tabletName = "_form_info_submission_association";
      
      DatabaseMetaData meta = con.getMetaData();
      ResultSet res = meta.getColumns(null, null, tabletName, null);
      System.out.println("List of columns: "); 
      
      while (res.next()) {
         System.out.println(
           "  "+res.getString("TABLE_SCHEM")
           + ", "+res.getString("TABLE_NAME")
           + ", "+res.getString("COLUMN_NAME")
           + ", "+res.getString("TYPE_NAME")
           + ", "+res.getInt("COLUMN_SIZE")
           + ", "+res.getString("NULLABLE")); 
      }
      res.close();

      con.close();
    } catch (java.lang.ClassNotFoundException e) {
      System.err.println("ClassNotFoundException: "
        +e.getMessage());
    } catch (SQLException e) {
      System.err.println("SQLException: "
        +e.getMessage());
    }
  }
}
