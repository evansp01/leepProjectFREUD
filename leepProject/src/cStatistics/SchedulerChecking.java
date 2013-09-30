package cStatistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cutilities.Utilities;

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

    public static void printSchedule(DatabaseConnection conn, String dbname) throws SQLException {
	int day = 4, block = 4;
	Statement sts[] = new Statement[block];
	ResultSet[] dayRS = new ResultSet[block];
	for (int i = 0; i < block; i++)
	    sts[i] = conn.getStatement();

	System.out.println("This is a printout of the schedule.\nThe numbers "
		+ "in parenthesis signify that\nthe crns are cross listed (within blocks)\n");
	for (int i = 0; i < day; i++) {
	    for (int j = 0; j < block; j++) {
		String query = "SELECT Distinct CourseCRN FROM " + dbname + " WHERE FinalDay = " + i
			+ " AND FinalBlock = " + j;
		dayRS[j] = sts[j].executeQuery(query);
	    }
	    Utilities.printDay(dayRS, i);
	    System.out.println();

	}
	for (int i = 0; i < block; i++)
	    sts[i].close();

    }

}
