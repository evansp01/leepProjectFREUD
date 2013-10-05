package database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import scheduling.CourseVertex;

import console.CurrentProject;


/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class CreateFinalTable {
    /**
     * Updates the final exams based on the the values in the supplied
     * courseVertex mapF
     * 
     * @param conn
     * @param map
     */
    public static void updateExams(DatabaseConnection conn, HashMap<String, CourseVertex> map) {
	String dbname = CurrentProject.studentsWithInfo;
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

    /**
     * sets up the desired tables after they have been imported creates the
     * studentsWithInfo table which is used throughout the program as the main
     * table, and adds final days and blocks. Also deals with cross listed
     * courses
     * 
     * @param conn
     */
    public static boolean maintainTables(DatabaseConnection conn) {
	String studentsTable = CurrentProject.students;
	String coursesTable = CurrentProject.courses;

	String tableToCreate = CurrentProject.studentsWithInfo;
	Statement st = null;
	try {

	    st = conn.getStatement();
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
	    return false;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return true;

    }

    public static int CROSSLISTCODE = 2, COURSECRN = 1, ACTUALENROLL = 3;

    /**
     * deal with cross listed courses by hyphenating crns
     * 
     * @param dbname
     * @param conn
     * @throws SQLException
     */
    private static boolean crossListCodeThings(String dbname, DatabaseConnection conn) {

	Statement findCrossList = null;
	Statement update = null;
	try {
	    findCrossList = conn.getStatement();
	    update = conn.getStatement();
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

	} catch (SQLException e) {
	    return false;
	} finally {
	    if (findCrossList != null)
		try {
		    findCrossList.close();
		} catch (SQLException e) {

		}
	    if (update != null)
		try {
		    update.close();
		} catch (SQLException e) {

		}
	}
	return true;
    }
}
