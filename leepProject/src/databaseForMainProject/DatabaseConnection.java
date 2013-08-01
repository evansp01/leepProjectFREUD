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

/**
 * 
 * @author Evan Palmer
 * 
 *         Database connector -- abstracts database connection also includes
 *         methods for loading the csv files required for the program to run
 * 
 */
//prepared statements
public class DatabaseConnection {

    //the course sheets
    public static final String CourseTable = "FREUDcourses";
    public static final String[] ColsForCourseList = { "CourseTerm", "PartofTerm", "CourseCRN", "Subject", "Sect",
	    "MaxEnroll", "ActualEnroll", "CourseTitle", "FacFirstName", "FacLastName", "CrseNumb", "Days",
	    "MeetBeginTime", "MeetEndTime", "CatalogDeptCode" };
    public static final String[] TypesForCourseList = { "INT", "VARCHAR(5)", "INT", "VARCHAR(10)", "VARCHAR(5)", "INT",
	    "INT", "VARCHAR(40)", "VARCHAR(20)", "VARCHAR(20)", "INT", "VARCHAR(10)", "INT", "INT", "VARCHAR(10)" };
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

    //ERROR CODES
    public static final int SUCCESS = 0, WRONG_NUMBER_OF_COLUMNS = 1, UNEXPECTED_COLUMN_NAME = 2,
	    COULD_NOT_CREATE_TABLE = 3, ROW_LENGTH_MISMATCH = 4, UNRECOGNIZED_TYPE = 5, FILE_NOT_FOUND = 6,
	    IO_ERROR = 7, SQL_ERROR = 8;

    //types
    public static final int UNRECOGNIZED = 0, INT = 1, STRING = 2;

    private String errorString = null;

    /**
     * 
     * @param url
     *            the url of the database to connect to
     * @param user
     *            the user of the database
     * @param password
     *            the password of the user
     */
    public DatabaseConnection(String url, String user, String password) {
	this.url = url;
	this.user = user;
	this.password = password;
	this.DELIM = "\t";

    }

    /**
     * Connects to a database with the parameters given in the constructor.
     * 
     * @return returns true if successful, false if fails. On failure, the error
     *         string will be set to the sql message.
     */
    public boolean connect() {
	try {
	    connect = DriverManager.getConnection(url, user, password);
	    return true;
	} catch (SQLException ex) {
	    errorString = ex.getMessage();
	    return false;
	}
    }

    /**
     * Attempts to close the sql connection
     */
    public void close() {
	try {
	    connect.close();
	} catch (SQLException e) {
	}
    }

    /**
     * 
     * @return returns a statement from the connection.
     * @throws SQLException
     *             thrown if the connection is not active
     */
    public Statement getStatement() throws SQLException {
	return connect.createStatement();
    }

    /**
     * 
     * @return returls the expected delimiter of csv files loaded.
     */
    public String getDelim() {
	return DELIM;
    }

    /**
     * 
     * @param delim
     *            sets the delimiter for loading csv files.
     */
    public void setDelim(String delim) {
	DELIM = delim;
    }

    /**
     * 
     * @return returns a string describing the most recent error.
     */
    public String getErrorString() {
	return errorString;
    }

    /**
     * 
     * @param fileName
     *            path to the student schedule
     * @return returns an integer describing success or failure. A value of 0
     *         indicates success. Otherwise the error string will describe the
     *         error
     */
    public int loadStudentScheudle(String fileName) {
	int result = 0;
	result = generalLoader(fileName, StudentTable, ColsForStudentList, TypesForStudentList, DELIM,
		primKeyForStudentList);
	return result;
    }

    /**
     * 
     * @param fileName
     *            path to the final schedule
     * @return returns an integer describing success or failure. A value of 0
     *         indicates success. Otherwise the error string will describe the
     *         error
     */
    public int loadFinalExams(String fileName) {
	int result = 0;
	result = generalLoader(fileName, FinalTable, ColsForFinalList, TypesForFinalList, DELIM, null);
	return result;
    }

    /**
     * 
     * @param fileName
     *            path to the course schedule
     * @return returns an integer describing success or failure. A value of 0
     *         indicates success. Otherwise the error string will describe the
     *         error
     */
    public int loadCourseOfferings(String fileName) {
	int result = 0;
	result = generalLoader(fileName, CourseTable, ColsForCourseList, TypesForCourseList, DELIM,
		primKeyForCourseList);
	return result;
    }

    /**
     * 
     * @param cols
     *            the column names
     * @param types
     *            the types corresponding to the column names
     * @param primKey
     *            the primary key of the table
     * @param name
     *            the name of the table
     * @return returns a boolean describing success
     */
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
     *            filename
     * @param name
     *            name of table
     * @param cols
     *            name of column headers
     * @param types
     *            types corresponding to column headers
     * @param delim
     *            the delimiter to be used
     * @param pk
     *            the primary key for the table
     * @return returns an integer describing success or failure
     */
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
		if (row.length != cols.length) {
		    errorString = "Row length mismatch " + row.length + " != " + cols.length;
		    return WRONG_NUMBER_OF_COLUMNS;
		}
		for (int i = 0; i < cols.length; i++) {
		    if (!row[i].replace(" ", "").equalsIgnoreCase(cols[i])) {
			errorString = "Unexpected column name: " + row[i];
			return UNEXPECTED_COLUMN_NAME;
		    }

		}
	    }

	    //ensure the table is successfully created
	    if (!createTable(cols, types, pk, name)) {
		errorString = "Could not create table";
		return COULD_NOT_CREATE_TABLE;
	    }

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
			errorString = "row " + rowIndex + " does not have the expected length";
			return ROW_LENGTH_MISMATCH;
		    }

		    for (int colIndex = 0; colIndex < cols.length; colIndex++) {
			switch (switchTypes[colIndex]) {
			case INT:
			    pst.setInt(colIndex + 1, Integer.parseInt(row[colIndex]));
			    break;
			case STRING:
			    pst.setString(colIndex + 1, row[colIndex]);
			    break;
			default:
			    errorString = "Unexpected type found (this shouldn't happen)";
			    return UNRECOGNIZED_TYPE;
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
	    errorString = "File not found: " + e.getMessage();
	    return FILE_NOT_FOUND;
	} catch (IOException e) {
	    errorString = "IO Exception: " + e.getMessage();
	    return IO_ERROR;
	} catch (SQLException e) {
	    errorString = "SQL error: " + e.getMessage();
	    return SQL_ERROR;
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
	errorString = null;
	return SUCCESS;

    }

    /**
     * 
     * @param type
     *            - the sql type
     * @return returns a integer describing the java type
     */
    private static int translateType(String type) {
	if (type.toLowerCase().contains("int"))
	    return INT;
	if (type.toLowerCase().contains("varchar"))
	    return STRING;
	return UNRECOGNIZED;
    }

    /**
     * a testing method
     * 
     * @param args
     *            parameters are ignored
     */
    public static void main(String[] args) {

	String url = "jdbc:mysql://localhost:3306/leep";
	String user = "javauser";
	String password = "testpass";
	DatabaseConnection conn = new DatabaseConnection(url, user, password);
	conn.connect();
	conn.loadCourseOfferings("/home/evan/Documents/regleep/realTest/courses201209.csv");
	conn.loadFinalExams("/home/evan/Documents/regleep/realTest/finals201209.csv");
	conn.loadStudentScheudle("/home/evan/Documents/regleep/realTest/students201209.csv");
    }

}