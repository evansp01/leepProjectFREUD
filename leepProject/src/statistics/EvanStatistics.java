package statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import databaseConnector.MySQLConnect;

public class EvanStatistics {

    static final String[] Sems = { "201009", "201101", "201109", "201201", "201209", "201301", "201309" };
    static final String STUDS = "students", FINS = "finals", COURSES = "courses", SCHEDS = "finalSchedule",
	    SWF = "studswfins", CWF = "courseswfins";
    static MySQLConnect conn = null;
    static Connection con = null;
    static BufferedWriter br = null;
    static final boolean toFile = true;
    static final boolean print = true;

    public static void doThings() throws SQLException {
	createConnection();

	try {

	    File file = new File("/home/evan/Documents/regleep/statistics.txt");
	    if (toFile)
		br = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
	    for (int i = 1; i < Sems.length - 1; i++) {
		String semester = Sems[i];
		prl("********* CURRENT SEMESTER: " + semester + " *************");
		numStudsWFins(semester);
		numFinalsPerStud(semester);
		numDaysPerStud(semester);
		numExamsPerBlock(semester);
		numCoursesPerBlock(semester);
		numExamsPerFac(semester);
		numExamsPerDept(semester, "CatalogDeptCode");
		numBigCoursePerBlock(semester, 30);
		numFinalDaysPerStud(semester);
		numB2BFinalsPerStud(semester);
		//buildStudentEdgeGraph(semester);
	    }
	    if (toFile) {
		br.flush();
		br.close();
	    }
	} catch (IOException e) {
	    prl("fail");
	}
    }

    public static void buildStudentEdgeGraph(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT CourseCRN FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	Graph g = new Graph();
	while (rs1.next()) {
	    g.insertNode(rs1.getString(1));
	}
	g.genEdges();
	rs1.close();
	String query2 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	rs1 = st.executeQuery(query2);
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query3 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query3);
	    ArrayList<String> crns = new ArrayList<String>();
	    while (rs2.next()) {
		crns.add(rs2.getString(1));
	    }
	    for (int i = 0; i < crns.size(); i++)
		//horrible horrible inefficiency
		for (int j = 0; j < crns.size(); j++) {
		    g.addEdge(crns.get(i), crns.get(j));
		}
	    rs2.close();

	}
	g.printToFile("/home/evan/Documents/regleep/graphs/graph" + semester + ".gml");

	rs1.close();

	st.close();
	st2.close();
    }

    public static void numB2BFinalsPerStud(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] studentsWb2b = new int[3];
	int[] studentsW3 = new int[3];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT FinalDay, FinalTime FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    boolean[][] exams = new boolean[4][5];
	    while (rs2.next()) {
		int day = rs2.getInt(1);
		int time = rs2.getInt(2);
		if (time != -1)
		    exams[day - 1][time - 1] = true;
	    }
	    int temp = 0;
	    if ((temp = backToBack(exams)) != 0)
		studentsWb2b[temp - 1]++;
	    if ((temp = triple(exams)) != 0)
		studentsW3[temp - 1]++;
	    rs2.close();

	}

	studentsWb2b[1] -= studentsW3[0];
	studentsWb2b[2] -= studentsW3[1];

	prl("Students With One Set of Back to Back Finals:\t" + studentsWb2b[0]);
	prl("Students With Two Sets of Back to Back Finals:\t" + studentsWb2b[1]);
	prl("Students With Three Finals in a Row:\t\t" + studentsW3[0]);
	prl("Students With Four Finals in a Row:\t\t" + studentsW3[1]);
	prl();

	rs1.close();

	st.close();
	st2.close();
    }

    public static int backToBack(boolean[][] exams) {
	boolean past;
	int b2b = 0;
	for (int j = 0; j < 4; j++) {
	    past = false;
	    for (int i = 0; i < 5; i++) {
		if (exams[j][i]) {
		    if (past)
			b2b++;
		    past = true;
		} else {
		    past = false;
		}

	    }
	}
	return b2b;
    }

    public static int triple(boolean[][] exams) {
	boolean past;
	boolean doublePast;
	int trip = 0;
	for (int j = 0; j < 4; j++) {
	    past = false;
	    doublePast = false;
	    for (int i = 0; i < 5; i++) {
		if (exams[j][i]) {
		    if (past) {
			if (doublePast)
			    trip++;
			else
			    doublePast = true;
		    }
		    past = true;
		} else {
		    past = false;
		    doublePast = false;
		}

	    }
	}
	return trip;

    }

    public static void numFinalDaysPerStud(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[][] histogram = new int[4][5];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    for (int i = 1; i <= 4; i++) {
		String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE StudentIDNo = '" + id
			+ "' AND FinalDay ='" + i + "';";

		ResultSet rs2 = st2.executeQuery(query2);
		int numFinals = rs2.last() ? rs2.getRow() : 0;
		histogram[i - 1][numFinals] += 1;
		rs2.close();
	    }

	}

	prl("  Number of Finals per Final Day: ");
	String[] cols = { "Finals", "Zero", "One", "Two", "Three", "Four" };
	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
	printArray2D(histogram, cols, rows);
	prl();
	rs1.close();
	st.close();
	st2.close();
    }

    public static void numBigCoursePerBlock(String semester, int enroll) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE ActualEnroll > '" + enroll + "'";
	ResultSet rs1 = st.executeQuery(query1);
	int[][] blocks = new int[4][5];
	int oddBlock = 0;
	while (rs1.next()) {
	    String crn = rs1.getString(1);
	    String query2 = "SELECT DISTINCT FinalDay, FinalTime FROM " + swf + " WHERE CourseCRN = '" + crn + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    while (rs2.next()) {
		int day = rs2.getInt(1);
		int time = rs2.getInt(2);
		if (time != -1)
		    blocks[day - 1][time - 1] += 1;
		else
		    oddBlock++;
	    }
	    rs2.close();
	}
	prl("  Number of Large (Size > " + enroll + ") Courses per Block: ");
	String[] cols = { "Courses", "Block 1", "Block 2", "Block 3", "Block 4", "Block 5" };
	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
	printArray2D(blocks, cols, rows);
	prl();

	rs1.close();
	st.close();
	st2.close();
    }

    //print
    public static void numExamsPerDept(String semester, String cdcorsubj) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT " + cdcorsubj + " FROM " + swf + " ORDER BY " + cdcorsubj;
	ResultSet rs1 = st.executeQuery(query1);
	prl("Department Exam Distribution: ");
	while (rs1.next()) {
	    String cdc = rs1.getString(1);
	    String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE " + cdcorsubj + " = '" + cdc + "';";
	    //prl(query2);
	    ResultSet rs2 = st2.executeQuery(query2);
	    int numExams = rs2.last() ? rs2.getRow() : 0;
	    prl(cdc + ": " + numExams);
	    rs2.close();
	}
	prl();
	rs1.close();
	st.close();
	st2.close();
    }

    //print
    public static void numExamsPerFac(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT FacFirstName,FacLastName FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] histogram = new int[4];
	while (rs1.next()) {
	    String fn = rs1.getString(1);
	    String ln = rs1.getString(2);
	    fn = fn.replace("'", "\\'");
	    ln = ln.replace("'", "\\'");
	    String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE FacFirstName = '" + fn
		    + "' AND FacLastName ='" + ln + "';";
	    //prl(query2);
	    ResultSet rs2 = st2.executeQuery(query2);
	    int numExams = rs2.last() ? rs2.getRow() : 0;
	    histogram[numExams - 1] += 1;
	    rs2.close();
	}

	prl("  Number of Exams per Faculty: ");
	String[] cols = { "Exams", "One", "Two", "Three", "Four" };
	String row = "Freq";
	printArray1D(histogram, cols, row);
	prl();
	rs1.close();
	st.close();
	st2.close();
    }

    //print
    public static void numExamsPerBlock(String semester) throws SQLException {//here
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[][] blocks = new int[4][5];
	int oddBlock = 0;
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT FinalDay, FinalTime FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    while (rs2.next()) {
		int day = rs2.getInt(1);
		int time = rs2.getInt(2);
		if (time != -1)
		    blocks[day - 1][time - 1] += 1;
		else
		    oddBlock++;
	    }

	    rs2.close();
	}

	prl("  Number of Students With Exams in Each Block: ");
	String[] cols = { "Exams", "Block 1", "Block 2", "Block 3", "Block 4", "Block 5" };
	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
	printArray2D(blocks, cols, rows);
	prl();
	prl("Student exams in non-standard exam blocks: " + oddBlock);
	prl();
	rs1.close();

	st.close();
	st2.close();
    }

    //print
    public static void numCoursesPerBlock(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT CourseCRN FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[][] blocks = new int[4][5];
	int oddBlock = 0;
	while (rs1.next()) {
	    String crn = rs1.getString(1);
	    String query2 = "SELECT DISTINCT FinalDay, FinalTime FROM " + swf + " WHERE CourseCRN = '" + crn + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    while (rs2.next()) {
		int day = rs2.getInt(1);
		int time = rs2.getInt(2);
		if (time != -1)
		    blocks[day - 1][time - 1] += 1;
		else
		    oddBlock++;
	    }

	    rs2.close();
	}

	prl("  Number of Courses With Exams in Each Block: ");
	String[] cols = { "Courses", "Block 1", "Block 2", "Block 3", "Block 4", "Block 5" };
	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
	printArray2D(blocks, cols, rows);
	prl();
	prl("Course exams in non-standard exam blocks: " + oddBlock);
	prl();
	rs1.close();

	st.close();
	st2.close();
    }

    //print
    public static void numDaysPerStud(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] histogram = new int[4];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT FinalDay FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    int numDays = rs2.last() ? rs2.getRow() : 0;
	    histogram[numDays - 1] += 1;
	    rs2.close();
	}

	prl("  Number of Final Days per Student: ");
	String[] cols = { "Days", "One", "Two", "Three", "Four" };
	String row = "Freq";
	printArray1D(histogram, cols, row);
	prl();
	rs1.close();

	st.close();
	st2.close();
    }

    //print
    public static void numStudsWFins(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String students = STUDS + semester;
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + students;
	String query2 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	ResultSet rs2 = st2.executeQuery(query2);
	int size1 = rs1.last() ? rs1.getRow() : 0;
	int size2 = rs2.last() ? rs2.getRow() : 0;
	prl();
	prl("Out of " + size1 + " students, " + size2 + " have finals");
	prl();
	rs1.close();
	rs2.close();
	st.close();
	st2.close();
    }

    //added print
    public static void numFinalsPerStud(String semester) throws SQLException {
	Statement st = con.createStatement();
	Statement st2 = con.createStatement();
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] histogram = new int[5];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    int numFinals = rs2.last() ? rs2.getRow() : 0;
	    histogram[numFinals - 1] += 1;
	    rs2.close();
	}

	prl("  Number of Finals per Student: ");
	String[] cols = { "Finals", "One", "Two", "Three", "Four", "Five" };
	String row = "Freq ";
	printArray1D(histogram, cols, row);
	prl();
	rs1.close();

	st.close();
	st2.close();
    }

    public static void main(String[] args) {
	try {
	    doThings();
	} catch (SQLException e) {
	    prl(e.getMessage());
	}
    }

    public static void printResultSet(ResultSet rs) {
	try {
	    ResultSetMetaData meta = rs.getMetaData();
	    for (int i = 0; i < meta.getColumnCount(); ++i) {
		pr(meta.getColumnLabel(i + 1) + "    ");
	    }
	    prl("");
	    while (rs.next()) {
		for (int i = 0; i < meta.getColumnCount(); ++i) {
		    pr(rs.getString(i + 1) + "    ");
		}
		prl("");
	    }
	} catch (SQLException e) {
	    prl(e.getMessage());
	}
    }

    public static void createConnection() {
	String url = "jdbc:mysql://localhost:3306/leep";
	String user = "javauser";
	String password = "testpass";
	conn = new MySQLConnect(url, user, password);
	conn.connect();
	con = conn.getConnect();
    }

    public static void printArray1D(int[] array, Object[] cols, Object row) {
	int[][] temp = new int[1][];
	temp[0] = array;
	Object[] temp2 = new Object[1];
	temp2[0] = row;
	printArray2D(temp, cols, temp2);
    }

    public static void printArray2D(int[][] array, Object[] cols, Object[] rows) {
	for (Object c : cols) {
	    pr(c.toString() + "\t");
	}
	prl();
	for (int i = 0; i < array.length; i++) {
	    pr(rows[i].toString() + "\t");
	    for (Object o : array[i])
		pr(o.toString() + "\t");
	    prl();
	}
    }

    public static void printArray1D(Object[] array, Object[] cols, Object row) {
	Object[][] temp = new Object[1][];
	temp[0] = array;
	Object[] temp2 = new Object[1];
	temp2[0] = row;
	printArray2D(temp, cols, temp2);
    }

    public static void printArray2D(Object[][] array, Object[] cols, Object[] rows) {
	for (Object c : cols) {
	    pr(c.toString() + "\t");
	}
	prl();
	for (int i = 0; i < array.length; i++) {
	    pr(rows[i].toString() + "\t");
	    for (Object o : array[i])
		pr(o.toString() + "\t");
	    prl();
	}

    }

    public static void prl() {
	prl("");
    }

    public static void prl(Object s) {
	if (toFile) {
	    try {
		br.write(s.toString());
		br.newLine();
	    } catch (IOException e) {
		System.out.println("can't print");
	    }
	}
	if (print)
	    System.out.println(s);
    }

    public static void pr(Object s) {
	if (toFile) {
	    try {
		br.write(s.toString());
	    } catch (IOException e) {
		System.out.println("can't print");
	    }
	}
	if (print)
	    System.out.print(s);
    }

}