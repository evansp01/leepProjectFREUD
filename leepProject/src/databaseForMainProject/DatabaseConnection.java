package databaseForMainProject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//prepared statements
public class DatabaseConnection {

    //the course sheets
    final static String[] ColsForCourseList = { "CourseTerm", "PartofTerm", "CourseCRN", "Subject", "Sect",
	    "MaxEnroll", "ActualEnroll", "CourseTitle", "CrossListCode", "FacFirstName", "FacLastName", "CrseNumb",
	    "Days", "MeetBeginTime", "MeetEndTime", "CatalogDeptCode" };
    final static String[] TypesForCourseList = { "INT", "VARCHAR(5)", "INT", "VARCHAR(10)", "VARCHAR(5)", "INT", "INT",
	    "VARCHAR(40)", "VARCHAR(5)", "VARCHAR(20)", "VARCHAR(20)", "INT", "VARCHAR(10)", "INT", "INT",
	    "VARCHAR(10)" };
    final static String primKeyForCourseList = "CourseCRN,Days,MeetBeginTime,MeetEndTime,FacFirstName,FacLastName";

    //the students sheets
    final static String[] ColsForStudentList = { "CourseCRN", "StudentIDNo" };
    final static String[] TypesForStudentList = { "INT", "VARCHAR(12)" };
    final static String primKeyForStudentList = "CourseCRN,StudentIDNo";
    //the finals sheets
    final static String[] ColsForFinalList = { "CourseCRN" };
    final static String[] TypesForFinalList = { "INT" };

    String url = null, user = null, password = null;
    Connection connect = null;
    boolean worked = false;

    public static void main(String[] args) {

	String url = "jdbc:mysql://localhost:3306/leep";
	String user = "javauser";
	String password = "testpass";
	DatabaseConnection conn = new DatabaseConnection(url, user, password);
	conn.connect();

	String[] Sems = { "201009", "201101", "201109", "201201", "201209", "201301", "201309" };
	String filePath = "home/evan/Documents/regleep/csvFiles/";
	//	if (true)
	//	    for (String semester : Sems) {
	//		String courseName = "courses" + semester;
	//		String studentName = "students" + semester;
	//		String finalName = "finals" + semester;
	//		String coursePath = filePath + courseName + ".csv";
	//		String studentPath = filePath + studentName + ".csv";
	//		String finalPath = filePath + finalName + ".csv";
	//		prl(coursePath);
	//		prl(studentPath);
	//		pr(conn.generalImporter(coursePath, ColsForCourseList, TypesForCourseList, courseName, "\\t", "\"",
	//			primKeyForCourseList));
	//		prl(conn.generalImporter(studentPath, ColsForStudentList, TypesForStudentList, studentName, "\\t",
	//			"\"", primKeyForStudentList));
	//		if (!semester.equals("201309"))
	//		    prl(conn.generalImporter(finalPath, ColsForFinalList, TypesForFinalList, finalName, "\\t", "\"",
	//			    primKeyForFinalList));
	//	    }
	//	String filePath2 = "/home/evan/Documents/regleep/finalSchedules/finalSchedule";
	//	for (int i = 1; i < Sems.length - 1; i++) {
	//	    prl(conn.loadFinalTables(filePath2 + Sems[i] + ".csv", "finalSchedule" + Sems[i], ColsForFinalsList,
	//		    TypesForFinalsList, "\\t"));
	//	}
	prl("Done!");
    }

    public Statement getStatement() throws SQLException {
	return connect.createStatement();
    }

    public DatabaseConnection(String url, String user, String password) {
	this.url = url;
	this.user = user;
	this.password = password;
    }

    public boolean createTable(String[] cols, String[] types, String primKey, String name) {
	if (types.length != cols.length) {
	    return false;
	}
	//build table creation string
	StringBuilder tableCreator = new StringBuilder();
	tableCreator.append("CREATE TABLE " + name + " (");
	for (int i = 0; i < cols.length; i++) {
	    tableCreator.append(cols[i] + " " + types[i]);
	    if (i != cols.length - 1)
		tableCreator.append(",");
	}
	if (primKey != null)
	    tableCreator.append(", PRIMARY KEY( " + primKey + ")");
	tableCreator.append(");");
	//drop table if exists string
	String dropTable = "DROP TABLE IF EXISTS " + name;
	Statement st = null;
	try {
	    //execute queries
	    st = connect.createStatement();
	    st.executeUpdate(dropTable);
	    st.executeUpdate(tableCreator.toString());
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

    private String[] splitLine(String line, String r1, String r2) {
	String[] s1 = line.split(r2, -1);
	String[][] s2 = new String[s1.length][];
	for (int i = 0; i < s1.length; i++) {
	    s2[i] = s1[i].split(r1, -1);
	}
	int length = 0;
	for (String[] s : s2)
	    length += s.length;
	String[] retVal = new String[length];
	int index = 0;
	for (String[] s : s2) {
	    for (String ss : s) {
		retVal[index] = ss;
		index++;
	    }
	}
	for (int i = 0; i < retVal.length; i++) {
	    retVal[i] = retVal[i].replace("'", "\\'");
	}
	return retVal;

    }

    /**
     * 
     * @param fn
     *            --name of the file
     * @param cols
     *            --name of columns that we are interested
     * @param types
     *            --types associated with the columns (for creating the table)
     * @param name
     *            --name of the table to create
     * @param delim
     *            --delimiter of csv file
     * @return --returns whether or not the creation was successful
     */
    public boolean generalImporter(String fn, String[] cols, String[] types, String name, String delim, String delim2,
	    String primKey) {
	BufferedReader br = null;
	String line = "";
	String[] row = null;
	int rowLength = 0;
	int colsInTable = cols.length;
	Map<Integer, Integer> keys = new HashMap<Integer, Integer>();
	Statement st = null;

	try {

	    br = new BufferedReader(new FileReader(fn));

	    if ((line = br.readLine()) != null) { // determine what indicies we are interested in
		row = splitLine(line, delim, delim2);
		rowLength = row.length;
		for (int i = 0; i < cols.length; i++) {
		    for (int j = 0; j < rowLength; j++) {
			row[j] = row[j].replaceAll(" ", "");
			if (row[j].compareToIgnoreCase(cols[i]) == 0) {
			    if (keys.containsKey(i) || keys.containsValue(j)) //assert no duplicates
				return false;
			    keys.put(i, j);
			}
		    }
		}
	    }
	    //assert that all the required columns were found in the table
	    if (keys.size() != colsInTable) { //make sure all keys were found
		System.out.println("keys not found");
		return false;
	    }
	    System.out.println("keys found");

	    //create the table here

	    createTable(cols, types, primKey, name);

	    connect.setAutoCommit(false);
	    st = connect.createStatement();

	    StringBuilder colNames = new StringBuilder();
	    for (int i = 0; i < cols.length; i++) {
		colNames.append(cols[i]);
		if (i < cols.length - 1)
		    colNames.append(",");
	    }
	    while (true) {
		int i = 0;
		for (i = 0; (line = br.readLine()) != null && i < 100; i++) {
		    row = splitLine(line, delim, delim2);
		    if (row.length < rowLength) {
			System.out.println("bad row length " + row.length + " " + rowLength);
			System.out.println(Arrays.toString(row));
			return false;
		    }
		    StringBuilder str = new StringBuilder();
		    str.append("INSERT IGNORE INTO " + name + " (" + colNames.toString() + ") VALUES(");
		    for (int i1 = 0; i1 < colsInTable; i1++) {
			if (row[keys.get(i1)] == null || row[keys.get(i1)].equals(""))
			    str.append("\\N");
			else
			    str.append("'" + row[keys.get(i1)] + "'");
			if (i1 < colsInTable - 1)
			    str.append(",");
		    }
		    str.append(");");
		    //System.out.println(str.toString());
		    st.addBatch(str.toString());
		}
		st.executeBatch();
		connect.commit();
		if (i < 100)
		    break;
	    }

	} catch (FileNotFoundException e) {
	    pr("file not found");
	    return false;
	} catch (IOException e) {
	    // failed to correctly read file
	    return false;
	} catch (SQLException e) {
	    System.out.println(e.getMessage());
	    return false;
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		    e.printStackTrace();
		}
	}
	return true;
    }

    //should rename errors -- also this is a general picky loader
    public int loadStudentScheudle(String fn, String name, String[] cols, String[] types, String delim) {
	BufferedReader br = null;
	Statement st = null;
	String line = null;
	String[] row = null;
	try {

	    br = new BufferedReader(new FileReader(fn));
	    //make sure the column headers line up with the first row of the data
	    if ((line = br.readLine()) != null) {
		row = line.split(delim, -1);
		if (row.length != cols.length)
		    return 1;
		for (int i = 0; i < cols.length; i++) {
		    if (!row[i].equalsIgnoreCase(cols[i]))
			return 2;
		}
	    }
	    //ensure the table is successfully created
	    if (!createTable(cols, types, null, name))
		return 3;
	    connect.setAutoCommit(false);
	    st = connect.createStatement();

	    StringBuilder columnNamesString = new StringBuilder();
	    for (int i = 0; i < cols.length; i++) {
		columnNamesString.append(cols[i]);
		if (i < cols.length - 1)
		    columnNamesString.append(",");
	    }
	    //batch committ
	    while (true) {
		int rowIndex;
		for (rowIndex = 0; (line = br.readLine()) != null && rowIndex < 100; rowIndex++) {
		    row = line.split(delim, -1);
		    //rows must be cols length
		    if (row.length != cols.length) {
			return 4;
		    }
		    StringBuilder dbInsertStatement = new StringBuilder();
		    dbInsertStatement.append("INSERT IGNORE INTO " + name + " (" + columnNamesString.toString()
			    + ") VALUES(");
		    for (int colIndex = 0; colIndex < cols.length; colIndex++) {
			//no entry can be null
			if (row[rowIndex] == null || row[rowIndex].replaceAll("[ \t\r\n]", "").equals(""))
			    return 6;
			//replace statement sanetizes
			dbInsertStatement.append("'" + row[colIndex].replaceAll("'", "\\'") + "'");
			if (colIndex < cols.length - 1)
			    dbInsertStatement.append(",");
		    }
		    dbInsertStatement.append(");");
		    st.addBatch(dbInsertStatement.toString());
		}
		st.executeBatch();
		connect.commit();
		if (rowIndex < 100)
		    break;
	    }
	    connect.setAutoCommit(true);
	} catch (FileNotFoundException e) {
	    return 7;
	} catch (IOException e) {
	    return 8;
	} catch (SQLException e) {
	    return 9;
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		}
	    }
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return 0;

    }

    public int loadStudentScheudle() {
	return 0;
    }

    public static void loadFinalExams(String fileName) {

    }

    public static void loadCourseOfferings(String filename) {

    }

    public boolean connect() {
	try {
	    connect = DriverManager.getConnection(url, user, password);
	    worked = true;
	    return true;
	} catch (SQLException ex) {
	    prl(ex.getMessage());
	    worked = false;
	    return false;
	}
    }

    public void close() {
	try {
	    connect.close();
	} catch (SQLException e) {
	}
    }

    public static void prl(Object s) {
	System.out.println(s.toString());
    }

    public static void pr(Object s) {
	System.out.print(s.toString());
    }

}
