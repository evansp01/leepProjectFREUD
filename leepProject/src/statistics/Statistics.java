package statistics;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import databaseConnector.MySQLConnect;

public class Statistics {

    static final String[] Sems = { "201009", "201101", "201109", "201201", "201209", "201301", "201309" };
    static final String STUDS = "students", FINS = "finals", COURSES = "courses", SCHEDS = "finalSchedule", SWF = "studswfins";
    static MySQLConnect conn = null;
    static Connection con = null;

    //    String cond = join(stu, "CourseCRN") + " = " + join(cou, "CourseCRN") + " AND " + join(cou, "Days") + " = "
    //	    + join(sch, "pattern") + " AND " + join(cou, "MeetBeginTime") + " = " + join(sch, "start") + " AND "
    //	    + join(stu, "StudentIDNo") + " = '" + id + "'";

    //SELECT studswfins201209.CourseTitle, finalSchedule201209.* FROM studswfins201209, finalSchedule201209 WHERE studswfins201209.MeetBeginTime = finalSchedule201209.start AND studswfins201209.StudentIDNo = 'C70178809' AND studswfins201209.MeetEndTime = finalSchedule201209.end AND REPLACE(studswfins201209.Days,' ','') = REPLACE(finalSchedule201209.pattern,' ','');

    public static void doThings() throws SQLException {
	createConnection();

	Statement st = con.createStatement();
	Statement st2 = con.createStatement();

	int index = 4;
	String stu = STUDS + Sems[index];
	String fin = FINS + Sems[index];
	String cou = COURSES + Sems[index];
	String sch = SCHEDS + Sems[index];
	String swf = SWF + Sems[index];

	String coursesTimes = "SELECT " + join(stu, "CourseTitle") + ", " + join(sch, "*") + " FROM " + stu + ", "
		+ sch + " WHERE ";
	String condition = " studswfins201209.MeetBeginTime = finalSchedule201209.start AND studswfins201209.StudentIDNo = 'C70178809' "
		+ "AND studswfins201209.MeetEndTime = finalSchedule201209.end AND REPLACE(studswfins201209.Days,' ','') = "
		+ "REPLACE(finalSchedule201209.pattern,' ','');";

	String select = join(stu, "CourseTitle") + "," + join(stu, "StudentIDNo");
	String dbs = stu;
	ResultSet students = st.executeQuery("SELECT DISTINCT StudentIDNo FROM " + stu
		+ " WHERE CourseTitle = 'ALGORITHMS';");
	while (students.next()) {

	    String id = students.getString(1);
	    String cond = join(stu, "StudentIDNo") + " = '" + id + "'";
	    String query = "SELECT " + select + " FROM " + dbs + " WHERE " + cond + " ;";
	    //prl(query);
	    ResultSet rs = st2.executeQuery(query);
	    printResultSet(rs);
	    rs.close();
	    st2.close();
	}
	students.close();
	st.close();

    }

    //	for (int i = 0; i < Sems.length-1; i++) --was used to create all studswfins tables
    //    createStudWFins(Sems[i]);

    public static void createStudWFins(String semester) throws SQLException {
	Statement st = con.createStatement();
	String name = "studswfins";
	String drop = "DROP TABLE IF EXISTS " + name + semester + ";";
	String query = "CREATE TABLE " + name + semester + " AS (SELECT " + STUDS + semester + ".* " + "FROM " + STUDS
		+ semester + ", " + FINS + semester + " WHERE " + STUDS + semester + ".CourseCRN = " + FINS + semester
		+ ".CourseCRN);";
	prl(query);
	st.executeUpdate(drop);
	st.executeUpdate(query);
	st.close();
    }

    public static String join(String course, String join) {
	return course + "." + join;
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
