package statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import databaseConnector.MySQLConnect;
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
    static final String URL = "jdbc:mysql://localhost:3306/leep", USR = "javauser", PASS = "testpass", SWF="studswfins";
    static final int DAY1 = 0, DAY2 = 1, DAY3 = 2, DAY4 = 3; 
    static MySQLConnect conn = null;
    static Connection con = null;  
    static BufferedWriter br = null;
    static final boolean toFile = true;
    static final boolean print = true;

    ArrayList<String> coursesDay1 = new ArrayList<String>(), coursesDay2 = new ArrayList<String>(),
	    coursesDay3 = new ArrayList<String>(), coursesDay4 = new ArrayList<String>();

    HashMap<Integer, ArrayList<String>> daysAndCourses = new HashMap<>();
    HashMap<String, String> crnToTime; 
    HashMap<String, Student> idToStud; 

    public SchedulerCheck() {
	daysAndCourses.put(DAY1, coursesDay1);
	daysAndCourses.put(DAY2, coursesDay2);
	daysAndCourses.put(DAY3, coursesDay3);
	daysAndCourses.put(DAY4, coursesDay4);
	crnToTime = new HashMap<>(); 
	idToStud = new HashMap<>();
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
    
    public void makeStudMap(Scheduler sched) { 
    	for (Student stud: sched.students()){ 
    		idToStud.put(stud.name(), stud);
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

  /*  public static void main(String[] args) {
    	int i = 0;
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
	

    }*/

    public void numB2BFinalsPerStud(String semester) throws SQLException {
	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
	connect.connect();
	Statement st = connect.getStatement();
	Statement st2 = connect.getStatement(); 
	String swf = SWF + semester;
	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);
	int[] studentsWb2b = new int[3];
	int[] studentsW3 = new int[3];
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT CourseCRN FROM " + swf + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query2);
	    boolean[][] exams = new boolean[4][4]; 
	    ArrayList<String> crns = new ArrayList<>();
	    while (rs2.next()) {
		String crn = rs2.getString(1); 
		crns.add(crn);
		String dt = crnToTime.get(crn); 
		if(dt==null)
			prl(dt); 
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
	    
	    for (int i=0; i<crns.size(); i++) { 
	    	for (int j=0; j<i; j++) { 
	    		if (sequential(crnToTime.get(crns.get(i)), crnToTime.get(crns.get(j)))) {
	    			System.out.println("Conflict for Student " + id ); 
	    			System.out.println(crns.get(i) + " " + crns.get(j)); 
	    			System.out.println(" ");
	    		}
	    	}
	    }  
	    
	   
	    rs2.close();

	} 
	
//	 System.out.println("crn: 27762 " + crnToTime.get("27762")); 
//	 System.out.println("crn: 27715 " + crnToTime.get("27715"));
//	 System.out.println("crn: 27723 " + crnToTime.get("27723"));

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
    
    private boolean sequential(String s, String t){ 
    	int day1 = Integer.parseInt("" + s.charAt(0));
		int time1 = Integer.parseInt("" + s.charAt(1)); 
		
		int day2 = Integer.parseInt("" + t.charAt(0));
		int time2 = Integer.parseInt("" + t.charAt(1)); 
		
		if(day1!=day2) 
			return false; 
		else if(Math.abs(time1-time2)==1)  
			if ((time1==0 && time2==1) || (time1==1 && time2==0)) 
				return false; 
			else 
				return true;
		
		return false;
    	
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
    
    public void numFinalDaysPerStud(String semester) throws SQLException {
    	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
    	connect.connect(); 
    	Statement st = connect.getStatement();
    	String swf = SWF + semester;
    	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
    	ResultSet rs1 = st.executeQuery(query1);
    	int[][] histogram = new int[4][5];
    	while (rs1.next()) {
    	    String id = rs1.getString(1); 
    	    Student stud = idToStud.get(id);   
    	    if (stud==null) 
    	    	continue;
    	    for (int i=0; i<4; i++) { 
    	    	int numFinals = stud.examsInDay(i); 
    	    	histogram[i][numFinals]+=1;
    	    }
    	}
    	prl("  Number of Finals per Final Day: ");
    	String[] cols = { "Finals", "Zero", "One", "Two", "Three", "Four" };
    	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
    	printArray2D(histogram, cols, rows);
    	prl();
    	rs1.close();
    	st.close();
        } 
    
    public void numExamsPerBlock(String semester) throws SQLException {//here
    	
    	int [][] blocks = new int [4][4];
    	for (int i=0; i<4; i++) { 
    		ArrayList<String> courses = daysAndCourses.get(i); 
    		for (String CRN: courses) { 
    			String dt = crnToTime.get(CRN);  
    			int time = Integer.parseInt("" + dt.charAt(1)); 
    			blocks[i][time]++;
    		}
    	}

    	prl("  Number of Exams in Each Block: ");
    	String[] cols = { "Exams", "Block 1", "Block 2", "Block 3", "Block 4" };
    	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
    	printArray2D(blocks, cols, rows);
    	prl();
        } 
    
    public void numStudsPerBlock(Scheduler sched) throws SQLException {  
    	int [][] blocks = new int [4][4];
    	for (CourseVertex course: sched.courseVertices()) { 
    		int day = course.day(); 
    		int block = course.block(); 
    		int enrollment = course.getEnrollment();
    		blocks[day][block]+=enrollment;
    	}
    	
    	prl("  Number of Students with Exams in Each Block: ");
    	String[] cols = { "Exams", "Block 1", "Block 2", "Block 3", "Block 4" };
    	String[] rows = { "Day 1", "Day 2", "Day 3", "Day 4" };
    	printArray2D(blocks, cols, rows);
    	prl();
    	
    } 
    
    public void miscStats(String semester) throws SQLException { 
    	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
    	connect.connect(); 
    	Statement st = connect.getStatement();
    	String swf = SWF + semester;
    	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
    	ResultSet rs1 = st.executeQuery(query1); 
    	int num3s, num4s, num5s; 
    	num3s=0; 
    	num4s=0; 
    	num5s=0;
    	while (rs1.next()) {
    	    String id = rs1.getString(1); 
    	    Student stud = idToStud.get(id);   
    	    if (stud==null) 
    	    	continue;
    	   int exams;
    	   if ((exams = stud.examsInDay(0) + stud.examsInDay(1))>2) { 
    		   if (exams==3) 
    			   num3s++; 
    		   else if (exams==4) 
    			   num4s++; 
    		   else if (exams==5) 
    			   num5s++;
    	   }
    		  
    	} 
    	prl("Number of students with 3 exams in 2 days: " + num3s); 
    	prl("Number of students with 4 exams in 2 days: " + num4s); 
    	prl("Number of students with 5 exams in 2 days: " + num5s);
    	prl();
    	rs1.close();
    	st.close();
    } 
    
    public void largeExamPlacement(Scheduler sched) { 
    	int firstDays=0; 
    	int lastDays=0;
    	for (CourseVertex exam: sched.courseVertices()) { 
    		if(exam.getEnrollment()>=50) { 
    			if (exam.day()==0 || exam.day()==1) 
    				firstDays++;  
    			else 
    				lastDays++;
    		}
    	}
    	
    	int totalDays = firstDays+lastDays; 
    	prl("Number of large exams in first two days: " + firstDays + " - " + (double) firstDays/totalDays); 
    	prl("Number of large exams in last two days: " + lastDays + " - " + (double) lastDays/totalDays);  
    	prl();
    } 
    
    public void printCRNSinDay(int day) {  
    	prl("------------------------------DAY " + (day+1) + "-------------------------------");
    	ArrayList<String> courses = daysAndCourses.get(day); 
    	
    	ArrayList<String> block1 = new ArrayList<>();  
    	ArrayList<String> block2 = new ArrayList<>(); 
    	ArrayList<String> block3 = new ArrayList<>(); 
    	ArrayList<String> block4 = new ArrayList<>();  
    	HashMap<Integer, ArrayList<String>> timeToCRNlist = new HashMap<>();  
    	
    	timeToCRNlist.put(0, block1); 
    	timeToCRNlist.put(1, block2); 
    	timeToCRNlist.put(2, block3); 
    	timeToCRNlist.put(3, block4);
    	
    	for (String CRN : courses) { 
    		String dt = crnToTime.get(CRN);  
    		int time = Integer.parseInt("" + dt.charAt(1)); 
    		timeToCRNlist.get(time).add(CRN);		
    	}  
    	
    	int max1 = Math.max(block1.size(), block2.size()); 
    	int max2 = Math.max(block3.size(), block4.size()); 
    	int maxBlockSize = Math.max(max1, max2); 
    	
    	prl("|\t\tBlock 1" + "\t\t | \t\t" + "Block 2" + "\t\t | \t\t" + "Block 3" + "\t\t | \t\t" + "Block 4" + "\t\t | \t\t"); 
    	for (int i=0; i<maxBlockSize; i++) { 
    		pr("|" + "\t\t"); 
    		if(block1.size()>i) 
    			pr(block1.get(i) + "\t\t | \t\t");   
    		else 
    			pr(" " + "\t\t | \t\t");  
    		
    		if(block2.size()>i) 
    			pr(block2.get(i) + "\t\t | \t\t");   
    		else 
    			pr(" " + "\t\t | \t\t");   
    		
    		if(block3.size()>i) 
    			pr(block3.get(i) + "\t\t | \t\t");   
    		else 
    			pr(" " + "\t\t | \t\t");  
    		
    		if(block4.size()>i) 
    			pr(block4.get(i) + "\t\t | \t\t");   
    		else 
    			pr(" " + "\t\t | \t\t");  
    		
    		prl();
    	} 
    	
    	prl();
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


    public static void prl(Object s) {
	System.out.println(s);
    }

    public static void prl() {
	System.out.println();
    }   
    
    public static void pr(Object s) {
    	System.out.print(s);
    
    } 
}
