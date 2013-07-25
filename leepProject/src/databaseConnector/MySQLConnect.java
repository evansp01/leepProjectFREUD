package databaseConnector;

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

public class MySQLConnect {

    //the course sheets
    final static String[] ColsForCourseList = { "CourseTerm", "PartofTerm", "CourseCRN", "Subject", "Sect",
	    "MaxEnroll", "ActualEnroll", "LinkIdentifier", "MCCode", "CourseTitle", "CrossListCode", "FacFirstName",
	    "FacLastName", "CrseNumb", "Days", "MeetBeginTime", "MeetEndTime", "CatalogDeptCode" };
    final static String[] TypesForCourseList = { "INT", "VARCHAR(5)", "INT", "VARCHAR(10)", "VARCHAR(5)", "INT", "INT",
	    "VARCHAR(5)", "VARCHAR(5)", "VARCHAR(40)", "VARCHAR(5)", "VARCHAR(20)", "VARCHAR(20)", "INT",
	    "VARCHAR(10)", "INT", "INT", "VARCHAR(10)" };
    final static String primKeyForCourseList = "CourseCRN,Days,MeetBeginTime,MeetEndTime,FacFirstName,FacLastName";

    //the students sheets
    final static String[] ColsForStudentList = { "CourseTerm", "PartofTerm", "CourseCRN", "Subject", "Sect",
	    "MaxEnroll", "ActualEnroll", "LinkIdentifier", "MCCode", "CourseTitle", "CrossListCode", "FacFirstName",
	    "FacLastName", "CrseNumb", "Days", "MeetBeginTime", "MeetEndTime", "CatalogDeptCode", "StudentIDNo" };
    final static String[] TypesForStudentList = { "INT", "VARCHAR(5)", "INT", "VARCHAR(10)", "VARCHAR(5)", "INT",
	    "INT", "VARCHAR(5)", "VARCHAR(5)", "VARCHAR(40)", "VARCHAR(5)", "VARCHAR(20)", "VARCHAR(20)", "INT",
	    "VARCHAR(10)", "INT", "INT", "VARCHAR(10)", "VARCHAR(12)" };
    final static String primKeyForStudentList = "CourseCRN,StudentIDNo,Days,MeetBeginTime,MeetEndTime,FacFirstName,FacLastName";
    //the finals sheets
    final static String[] ColsForFinalList = { "Customer", "1stContact", "EventName", "FirstBooking", "Semester",
	    "CourseCRN" };
    final static String[] TypesForFinalList = { "VARCHAR(20)", "VARCHAR(20)", "VARCHAR(20)", "VARCHAR(20)", "INT",
	    "INT" };
    final static String primKeyForFinalList = "CourseCRN";

    final static String[] ColsForFinalsList = { "pattern", "start", "end", "block", "day" };
    final static String[] TypesForFinalsList = { "VARCHAR(10)", "INT", "INT", "INT", "INT" };

    String url = null, user = null, password = null;
    Connection connect = null;
    boolean worked = false;

    public static void main(String[] args) {

	String url = "jdbc:mysql://localhost:3306/leep";
	String user = "javauser";
	String password = "testpass";
	MySQLConnect conn = new MySQLConnect(url, user, password);
	conn.connect();

	String[] Sems = { "201009", "201101", "201109", "201201", "201209", "201301", "201309" };
	String filePath = "home/evan/Documents/regleep/csvFiles/";
	if (true)
	    for (String semester : Sems) {
		String courseName = "courses" + semester;
		String studentName = "students" + semester;
		String finalName = "finals" + semester;
		String coursePath = filePath + courseName + ".csv";
		String studentPath = filePath + studentName + ".csv";
		String finalPath = filePath + finalName + ".csv";
		prl(coursePath);
		prl(studentPath);
		pr(conn.generalImporter(coursePath, ColsForCourseList, TypesForCourseList, courseName, "\\t", "\"",
			primKeyForCourseList));
		prl(conn.generalImporter(studentPath, ColsForStudentList, TypesForStudentList, studentName, "\\t",
			"\"", primKeyForStudentList));
		if (!semester.equals("201309"))
		    prl(conn.generalImporter(finalPath, ColsForFinalList, TypesForFinalList, finalName, "\\t", "\"",
			    primKeyForFinalList));
	    }
	String filePath2 = "/home/evan/Documents/regleep/finalSchedules/finalSchedule";
	for (int i = 1; i < Sems.length - 1; i++) {
	    prl(conn.loadFinalTables(filePath2 + Sems[i] + ".csv", "finalSchedule" + Sems[i], ColsForFinalsList,
		    TypesForFinalsList, "\\t"));
	}
	prl("Done!");
    }

    public Connection getConnect() {
	return connect;
    }

    public boolean loadFinalTables(String path, String tableName, String[] cols, String[] types, String reg) {
	BufferedReader buff = null;
	String[][] table = null;
	Statement st = null;
	try {
	    buff = new BufferedReader(new FileReader(path));
	    String line = buff.readLine();
	    table = new String[5][];
	    for (int i = 0; i < 5; i++) {
		if ((line = buff.readLine()) == null)
		    return false;
		table[i] = line.split(reg, -1);
	    }
	    createTable(cols, types, null, tableName);
	    st = connect.createStatement();
	    for (int j = 1; j < 5; j++) {
		for (int i = 2; i < 7; i++) {
		    String query = null;
		    String[] rows = table[j][i].split(",", -1);
		    if (rows.length == 1) {
			query = "INSERT INTO " + tableName + " (pattern,start,end,block,day) VALUES("
				+ "'OPEN','0','0','" + table[0][i] + "','" + table[j][1] + "');";
		    } else if (rows.length % 2 == 1) {
			//urggggg
		    } else {
			for (int k = 0; k < rows.length; k += 2) {
			    //split off days and times and things
			    String dayString = rows[k].replaceAll("TH", "R");
			    dayString = dayString.replace("Th", "R");
			    dayString = dayString.replaceAll("-", " ");
			    String timeString = rows[k + 1].replaceAll(":", "");
			    String[] times = timeString.split("-", -1);
			    int t1 = Integer.parseInt(times[0]);
			    int t2 = Integer.parseInt(times[1]);
			    if (t1 < 800) {
				t1 += 1200;
				t2 += 1200;
			    }
			    if(t2 < 800){
				t2+=1200;
			    }
			    query = "INSERT INTO " + tableName + " (pattern,start,end,block,day) VALUES('" + dayString
				    + "','" + t1 + "','" + t2 + "','" + (i-1) + "','" + j + "');";
			    prl(query);
			    st.executeUpdate(query);
			}
		    }

		}
	    }

	} catch (FileNotFoundException e) {
	    prl(e.getMessage());
	    return false;

	} catch (IOException e) {
	    prl(e.getMessage());
	    return false;
	    // TODO Auto-generated catch block
	} catch (SQLException e) {
	    prl(e.getMessage());
	    return false;
	    // TODO Auto-generated catch block
	} finally {
	    if (buff != null)
		try {
		    buff.close();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}

	}
	return true;

    }

    public MySQLConnect(String url, String user, String password) {
	this.url = url;
	this.user = user;
	this.password = password;
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

    public boolean createTable(String[] cols, String[] types, String primKey, String name) {
	if (types.length != cols.length) {
	    System.out.println("urg");
	    return false;
	}
	StringBuilder query = new StringBuilder();
	query.append("CREATE TABLE " + name + " (");
	for (int i = 0; i < cols.length; i++) {
	    query.append(cols[i] + " " + types[i]);
	    if (i != cols.length - 1)
		query.append(",");
	}
	if (primKey != null)
	    query.append(", PRIMARY KEY( " + primKey + ")");
	query.append(");");

	String drop = "DROP TABLE IF EXISTS " + name;
	Statement st = null;
	try {
	    st = connect.createStatement();
	    System.out.println(query.toString());
	    st.executeUpdate(drop);
	    st.executeUpdate(query.toString());
	} catch (SQLException e) {
	    System.out.println("bad sql");
	    System.out.println(e.getMessage());
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

    public static void loadStudentScheudle(String fileName) {

    }

    public static void loadFinalExams(String fileName) {

    }

    public static void loadCourseOfferings(String filename) {

    }

    public static void prl(String s) {
	System.out.println(s);
    }

    public static void pr(String s) {
	System.out.print(s);
    }

    public static void prl(boolean s) {
	System.out.println(s);
    }

    public static void pr(boolean s) {
	System.out.print(s);
    }

}
