package cStatistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import statistics.Utilities;

import databaseForMainProject.DatabaseConnection;

public class SchedulerChecking {

    public static void check(DatabaseConnection conn, String dbname, String otherdb) throws SQLException {
	Statement st1 = conn.getStatement();
	Statement st2 = conn.getStatement();
	int day = 4, block = 4;
	for (int i = 0; i < day; i++) {
	    for (int j = 0; j < block; j++) {
		String query = "SELECT Distinct CourseCRN FROM " + dbname + " WHERE FinalDay = " + i
			+ " AND FinalBlock = " + j;
		System.out.println(query);
		ResultSet rs = st1.executeQuery(query);
		System.out.println("Day " + (i + 1) + " Block " + (j + 1));
		Utilities.print(rs);

	    }
	}
    }
}
