package databaseForMainProject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import czexamSchedulingFinal.CourseVertex;

public class CreateFinalTable {

    public static void updateExams(DatabaseConnection conn, HashMap<String, CourseVertex> map) {
	String dbname = "FREUDstudswfins";
	try {
	    Statement st = conn.getStatement();
	    for (CourseVertex cv : map.values()) {
		String query = "UPDATE " + dbname + " SET FinalDay = '" + cv.day() + "', FinalBlock = '" + cv.block()
			+ "' WHERE CHARINDEX ('" + cv.name() + "', CourseCRN) > 0";
		st.executeUpdate(query);
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}

    }

    public static void maintainTables(DatabaseConnection conn) {
	try {
	    String studentsTable = "FREUDstudents";
	    String coursesTable = "FREUDcourses";
	    String finalsTable = "FREUDfinals";
	    Statement st = conn.getStatement();
	    String tableToCreate = "FREUDstudswfins";
	    String thingsToSelect = "t1.CourseCRN, t1.StudentIDNo, t2.FacFirstName, t2.FacLastName, t2.CrossListCode, t2.ActualEnroll";
	    String query0 = "DROP TABLE IF EXISTS " + tableToCreate;
	    String query1 = "CREATE TABLE " + tableToCreate + " AS SELECT " + thingsToSelect + " FROM " + studentsTable
		    + " AS t1 LEFT JOIN " + coursesTable + " AS t2 ON t1.CourseCRN = t2.CourseCRN";
	    String query2 = "ALTER TABLE " + tableToCreate + " ADD COLUMN finalDay INT NOT NULL DEFAULT '-1';";
	    String query3 = "ALTER TABLE " + tableToCreate + " ADD COLUMN finalBlock INT NOT NULL DEFAULT '-1';";
	    String query4 = "DROP TABLE IF EXISTS " + studentsTable;
	    st.executeUpdate(query0);
	    st.executeUpdate(query1);
	    st.executeUpdate(query2);
	    st.executeUpdate(query3);
	    st.executeUpdate(query4);

	    crossListCodeThings(tableToCreate, conn);

	} catch (SQLException e) {
	}

    }

    public static int CROSSLISTCODE = 2, COURSECRN = 1, ACTUALENROLL = 3;

    private static void crossListCodeThings(String dbname, DatabaseConnection conn) throws SQLException {
	Statement findCrossList = conn.getStatement();
	Statement update = conn.getStatement();
	String thingsToSelect = "CourseCRN, CrossListCode, ActualEnroll";
	String getCrossListed = "SELECT DISTINCT " + thingsToSelect + " FROM " + dbname
		+ " WHERE CrossListCode IS NOT NULL ORDER BY CrossListCode";
	ResultSet crossListed = findCrossList.executeQuery(getCrossListed);

	Set<String> crnsOfCurrentCLC = new HashSet<>();
	String currentCLC = null, previousCLC = null, currentCRN = null;
	int totalEnroll = 0;

	while (crossListed.next()) {
	    currentCLC = crossListed.getString(CROSSLISTCODE);
	    currentCRN = crossListed.getString(COURSECRN);
	    int currentEnroll = crossListed.getInt(ACTUALENROLL);
	    //if no previous or if the previous is the same as the current
	    if (previousCLC == null || previousCLC.equals(currentCLC)) {
		int size = crnsOfCurrentCLC.size();
		crnsOfCurrentCLC.add(currentCRN);
		if (crnsOfCurrentCLC.size() > size) //if this isn't a repeat crn
		    totalEnroll += currentEnroll;
		previousCLC = currentCLC;
	    } else { //there was a new cross list code
		StringBuilder hyphenatedCRN = new StringBuilder();
		StringBuilder query = new StringBuilder();
		for (String crn : crnsOfCurrentCLC) {
		    hyphenatedCRN.append(crn + "-");
		    query.append("CourseCRN = '" + crn + "' OR ");
		}
		String hyphenatedCRNString = hyphenatedCRN.substring(0, hyphenatedCRN.length() - 1);
		String queryString = query.substring(0, query.length() - 4);
		String queryFin = "UPDATE " + dbname + " SET CourseCRN = '" + hyphenatedCRNString
			+ "', ActualEnroll = '" + totalEnroll + "' WHERE " + queryString;
		update.executeUpdate(queryFin);

		//change to new crn
		previousCLC = currentCLC;
		totalEnroll = currentEnroll;
		crnsOfCurrentCLC.clear();
		crnsOfCurrentCLC.add(currentCRN);

	    }

	}

    }
}
