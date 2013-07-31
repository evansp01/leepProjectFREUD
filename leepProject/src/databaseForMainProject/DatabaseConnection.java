package databaseForMainProject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

//prepared statements
public class DatabaseConnection {

    //the course sheets
    public static final String CourseTable = "FREUDcourses";
    public static final String[] ColsForCourseList = { "CourseTerm", "PartofTerm", "CourseCRN", "Subject", "Sect",
	    "MaxEnroll", "ActualEnroll", "CourseTitle", "CrossListCode", "FacFirstName", "FacLastName", "CrseNumb",
	    "Days", "MeetBeginTime", "MeetEndTime", "CatalogDeptCode" };
    public static final String[] TypesForCourseList = { "INT", "VARCHAR(5)", "INT", "VARCHAR(10)", "VARCHAR(5)", "INT",
	    "INT", "VARCHAR(40)", "VARCHAR(5)", "VARCHAR(20)", "VARCHAR(20)", "INT", "VARCHAR(10)", "INT", "INT",
	    "VARCHAR(10)" };
    public static final String primKeyForCourseList = "CourseCRN,Days,MeetBeginTime,MeetEndTime,FacFirstName,FacLastName";
    //the students sheets
    public static final String StudentTable = "FREUDstudents";
    public static final String[] ColsForStudentList = { "CourseCRN", "StudentIDNo" };
    public static final String[] TypesForStudentList = { "INT", "VARCHAR(12)" };
    public static final String primKeyForStudentList = "CourseCRN,StudentIDNo";
    //the finals sheets
    public static final String FinalTable = "FREUDfinals";
    public static final String[] ColsForFinalList = { "CourseCRN" };
    public static final String[] TypesForFinalList = { "INT" };

    private String url = null, user = null, password = null;
    private Connection connect = null;
    private String DELIM = null;

    public DatabaseConnection(String url, String user, String password) {
	this.url = url;
	this.user = user;
	this.password = password;
	this.DELIM = "\t";

    }

    public boolean connect() {
	try {
	    connect = DriverManager.getConnection(url, user, password);
	    return true;
	} catch (SQLException ex) {
	    prl(ex.getMessage());
	    return false;
	}
    }

    public void close() {
	try {
	    connect.close();
	} catch (SQLException e) {
	}
    }

    public Statement getStatement() throws SQLException {
	return connect.createStatement();
    }

    public String getDelim() {
	return DELIM;
    }

    public void setDelim(String delim) {
	DELIM = delim;
    }

    public int loadStudentScheudle(String fileName) {
	int result = 0;
	result = generalLoader(fileName, StudentTable, ColsForStudentList, TypesForStudentList, DELIM,
		primKeyForStudentList);
	return result;
    }

    public int loadFinalExams(String fileName) {
	int result = 0;
	result = generalLoader(fileName, FinalTable, ColsForFinalList, TypesForFinalList, DELIM, null);
	return result;
    }

    public int loadCourseOfferings(String fileName) {
	int result = 0;
	result = generalLoader(fileName, CourseTable, ColsForFinalList, TypesForFinalList, DELIM, primKeyForCourseList);
	return result;
    }

    //change this to a prepared statement
    private boolean createTable(String[] cols, String[] types, String primKey, String name) {
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

    //should rename errors -- also this is a general picky loader
    private int generalLoader(String fn, String name, String[] cols, String[] types, String delim, String pk) {
	BufferedReader br = null;
	PreparedStatement pst = null;
	String line = null;
	String[] row = null;
	int[] switchTypes = new int[cols.length];
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
	    if (!createTable(cols, types, pk, name))
		return 3;

	    //building the prepared statement
	    StringBuilder prepStatement = new StringBuilder();
	    prepStatement.append("INSERT IGNORE INTO ");
	    prepStatement.append(name);
	    prepStatement.append("(");
	    for (int i = 0; i < cols.length; i++) {
		switchTypes[i] = translateType(types[i]);
		prepStatement.append(cols[i]);
		if (i < cols.length - 1)
		    prepStatement.append(",");
	    }
	    prepStatement.append(") VALUES(");
	    for (int colIndex = 0; colIndex < cols.length - 1; colIndex++) {
		prepStatement.append("?,");
	    }
	    prepStatement.append("?)");
	    pst = connect.prepareStatement(prepStatement.toString());

	    //batch committ
	    connect.setAutoCommit(false);
	    while (true) {
		int rowIndex;
		for (rowIndex = 0; (line = br.readLine()) != null && rowIndex < 100; rowIndex++) {
		    row = line.split(delim, -1);
		    //rows must be cols length
		    if (row.length != cols.length) {
			return 4;
		    }

		    for (int colIndex = 0; colIndex < cols.length; colIndex++) {
			switch (switchTypes[colIndex]) {
			case 1:
			    pst.setInt(colIndex, Integer.parseInt(row[colIndex]));
			    break;
			case 2:
			    pst.setString(colIndex, row[colIndex]);
			}
		    }
		    pst.addBatch();

		}
		pst.executeBatch();
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
	    if (pst != null)
		try {
		    pst.close();
		} catch (SQLException e) {
		}
	}
	return 0;

    }

    public static int translateType(String type) {
	if (type.toLowerCase().contains("int"))
	    return 1;
	if (type.toLowerCase().contains("varchar"))
	    return 2;
	return 0;
    }

    public static void main(String[] args) {

	String url = "jdbc:mysql://localhost:3306/leep";
	String user = "javauser";
	String password = "testpass";
	DatabaseConnection conn = new DatabaseConnection(url, user, password);
	conn.connect();

	String[] Sems = { "201009", "201101", "201109", "201201", "201209", "201301", "201309" };
	String filePath = "home/evan/Documents/regleep/csvFiles/";
	prl("Done!");
    }

    public static void prl(Object s) {
	System.out.println(s.toString());
    }

    public static void pr(Object s) {
	System.out.print(s.toString());
    }

}
