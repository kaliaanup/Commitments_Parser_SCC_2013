package comm.trust.libs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import comm.trust.config.ProjectConfig;

public class DatabaseConn {
	
	public static Connection conn=null;
	public static Statement stmt=null;
	public static ResultSet rs= null;
	
	public static void openMySqlConnection()
	{
		
		
		//open DB connection
		try {
			
			conn = DriverManager.getConnection("jdbc:mysql://"+ProjectConfig.mySQLDBServiceName+"/"
												+ProjectConfig.mySQLDBName+"?"
												+"user="+ProjectConfig.mySQLUsername
												+"&password="+ProjectConfig.mySQLPassword);
			System.out.println("-----------------------MySQL connection is opened now-----------------------");
			
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		

	}
	public static void closeMySqlConnection()
	{
		if (rs != null) {
	        try {
	            rs.close();
	        } catch (SQLException sqlEx) { } // ignore

	        rs = null;
	    }

	    if (stmt != null) {
	        try {
	            stmt.close();
	        } catch (SQLException sqlEx) { } // ignore

	        stmt = null;
	    }
	    
	    if (conn != null) {
	        try {
	            conn.close();
	        } catch (SQLException sqlEx) { } // ignore

	        conn = null;
	    }
	    
	    System.out.println("-----------------------MySQL connection is closed now-----------------------");

	}
	
	
	
	
}
