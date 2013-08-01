package statistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import databaseForMainProject.DatabaseConnection;
import examScheduling.CourseVertex;
import examScheduling.GraphCreation;
import examScheduling.Scheduler;
import examScheduling.Student;
import examScheduling.StudentEdge;
import examScheduling.StudentGraph;

public class SchedulerCheck {

    static final String[] Sems = { "studswfins201009", "studswfins201101", "studswfins201109", "studswfins201201",
	    "studswfins201209", "studswfins201301" };
    static final String URL = "jdbc:mysql://localhost:3306/leep", USR = "javauser", PASS = "testpass";
    static final int DAY1 = 0, DAY2 = 1, DAY3 = 2, DAY4 = 3;

    ArrayList<String> coursesDay1 = new ArrayList<String>(), coursesDay2 = new ArrayList<String>(),
	    coursesDay3 = new ArrayList<String>(), coursesDay4 = new ArrayList<String>();

    HashMap<Integer, ArrayList<String>> daysAndCourses = new HashMap<>();
    HashMap<String, String> crnToTime;

    public SchedulerCheck() {
	daysAndCourses.put(DAY1, coursesDay1);
	daysAndCourses.put(DAY2, coursesDay2);
	daysAndCourses.put(DAY3, coursesDay3);
	daysAndCourses.put(DAY4, coursesDay4);
	crnToTime = new HashMap<>();
    }

    public int numberOfDays() {
	return daysAndCourses.size();
    }

    public ArrayList<String> coursesOnDay(int k) {
	return daysAndCourses.get(k);
    }

    public void organizeCourses(Scheduler sched) {
	for (CourseVertex v : sched.courseVertices()) {
	    int day = v.day();
	    ArrayList<String> courseList = daysAndCourses.get(day);
	    courseList.add(v.name());
	    crnToTime.put(v.name(), "" + v.day() + v.block());
	}
    }

    public void createQuery(ArrayList<String> courses, String sem) throws SQLException {
	String condition = " ";
	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
	connect.connect();
	Statement st = connect.getStatement();

	for (int i = 0; i < courses.size(); i++) {
	    condition = condition + "t1.CourseCRN= " + courses.get(i);
	    if (i != courses.size() - 1)
		condition = condition + " OR ";
	    else
		condition = condition + " ";
	}

	String query = "SELECT t1.StudentIDNo, count(t1.StudentIDNo) FROM (SELECT DISTINCT CourseCRN, StudentIDNo FROM "
		+ sem + ") AS t1  WHERE " + condition + "GROUP BY t1.StudentIDNo HAVING count(t1.StudentIDNo)>=3";
	ResultSet result = null;
	try {
	    result = st.executeQuery(query);
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	while (result.next()) {
	    System.out.println(result.getString(1) + " " + result.getInt(2));
	}
	System.out.println("*******************");
	System.out.println(" ");
	result.close();
	st.close();
	connect.close();

    }

    public static void main(String[] args) {
	for (int i = 0; i < Sems.length; i++) {
	    StudentGraph<String, StudentEdge> g = null;
	    HashMap<String, Student> sm = null;
	    SchedulerCheck initCheck = new SchedulerCheck();

	    GraphCreation grc = new GraphCreation(Sems[i], URL, USR, PASS);
	    g = grc.getGraph();
	    sm = grc.getStudentMap();

	    Scheduler sched = new Scheduler(g, sm);
	    sched.Schedule();
	    System.out.println(Sems[i]);
	    initCheck.organizeCourses(sched);

	    for (int j = 0; j < initCheck.numberOfDays(); j++) {
		try {
		    System.out.println("Day " + (j + 1));
		    initCheck.createQuery(initCheck.coursesOnDay(j), Sems[i]);
		} catch (SQLException e) {
		    e.printStackTrace();
		}
	    }
	    try {
		initCheck.numB2BFinalsPerStud(Sems[i]);
	    } catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}

    }

    public void numB2BFinalsPerStud(String swf) throws SQLException {
	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
	connect.connect();
	Statement st = connect.getStatement();
	Statement st2 = connect.getStatement();
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] studentsWb2b = new int[3];
	int[] studentsW3 = new int[3];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    boolean[][] exams = new boolean[4][4];
	    while (rs2.next()) {
		String crn = rs2.getString(1);
		String dt = crnToTime.get(crn);

		int day = Integer.parseInt("" + dt.charAt(0));
		int time = Integer.parseInt("" + dt.charAt(1));
		if (time != -1)
		    exams[day][time] = true;
	    }
	    int temp = 0;
	    if ((temp = backToBack(exams)) != 0)
		studentsWb2b[temp - 1]++;
	    if ((temp = triple(exams)) != 0)
		studentsW3[temp - 1]++;
	    rs2.close();

	}

	//studentsWb2b[1] -= studentsW3[0];
	//studentsWb2b[2] -= studentsW3[1];

	prl("Students With One Set of Back to Back Finals:\t" + studentsWb2b[0]);
	prl("Students With Two Sets of Back to Back Finals:\t" + studentsWb2b[1]);
	prl("Students With Three Finals in a Row:\t\t" + studentsW3[0]);
	prl("Students With Four Finals in a Row:\t\t" + studentsW3[1]);
	prl();

	rs1.close();

	st.close();
	st2.close();
    }

    public int backToBack(boolean[][] exams) {
	boolean past;
	int b2b = 0;
	for (int j = 0; j < 4; j++) {
	    past = false;
	    for (int i = 0; i < 4; i++) {
		if (exams[j][i]) {
		    if (i == 1)
			past = false;
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

    public int triple(boolean[][] exams) {
	boolean past;
	boolean doublePast;
	int trip = 0;
	for (int j = 0; j < 4; j++) {
	    past = false;
	    doublePast = false;
	    for (int i = 0; i < 4; i++) {
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

    public static void prl(Object s) {
	System.out.println(s);
    }

    public static void prl() {
	System.out.println();
    }
}
