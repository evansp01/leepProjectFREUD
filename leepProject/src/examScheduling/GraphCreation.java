package examScheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.mysql.jdbc.exceptions.MySQLDataException;

import statistics.SchedulerCheck;
import databaseForMainProject.DatabaseConnection;

public class GraphCreation {
    public static final int ID = 1, CRN = 2, DISTINCTCRN = 1, ENROLL = 2, LASTNAME = 3, FIRSTNAME = 4, DAYS = 4,
	    BLOCKS = 4;

    StudentGraph<String, StudentEdge> g;
    HashMap<String, Student> sm;
    HashMap<String, Integer> enroll;
    HashMap<String, String> crnToFac;
    HashMap<String, ArrayList<String>> facToCrn;

    public GraphCreation(String dbname, String url, String usr, String pass) {
	try {
	    createGraph(dbname, url, usr, pass);
	} catch (SQLException e) {
	}
    }

    public StudentGraph<String, StudentEdge> getGraph() {
	return g;
    }

    public HashMap<String, Student> getStudentMap() {
	return sm;
    }

    public HashMap<String, Integer> getEnrollment() {
	return enroll;
    }

    public HashMap<String, String> getCrnToFac() {
	return crnToFac;
    }

    public HashMap<String, ArrayList<String>> getFacToCrn() {
	return facToCrn;
    }

    public void addToGraph(String CRN, String url, String usr, String pass, StudentGraph g, String dbname)
	    throws SQLException {
	DatabaseConnection connect = new DatabaseConnection(url, usr, pass);
	connect.connect();
	Statement st = connect.getStatement();
	Statement cross = connect.getStatement();

	g = new StudentGraph<String, StudentEdge>(StudentEdge.class);
	sm = new HashMap<>();
	enroll = new HashMap<>();
	crnToFac = new HashMap<>();
	facToCrn = new HashMap<>();

	HashMap<String, String> crnToCrossList = addCrossListedCourses(dbname, cross);
	//end of replaced thing
	String getCrosslisted = "SELECT DISTINCT CrossListCode FROM " + dbname + " WHERE CourseCRN = '"
		+ CRN + "'";
	ResultSet rs = st.executeQuery(getCrosslisted);
	LinkedList<String> clcs = new LinkedList<>();
	while(rs.next()){
	    clcs.add(rs.getString(1)); //get cross list code
	}
	//build query to search for anything
	//if the course crn is already there, then ignore it
	

	String getCourseCRNs = "SELECT DISTINCT CourseCRN, ActualEnroll, FacLastName, FacFirstName FROM " + dbname
		+ " WHERE CourseCRN = '" + CRN + "'";
	ResultSet courseCRNs = st.executeQuery(getCourseCRNs);

	while (courseCRNs.next()) {
	    //course is not cross listed
	    if (!crnToCrossList.containsKey(courseCRNs.getString(DISTINCTCRN))) {

		String facName = courseCRNs.getString(LASTNAME) + " " + courseCRNs.getString(FIRSTNAME);
		g.addVertex(courseCRNs.getString(DISTINCTCRN));
		enroll.put(courseCRNs.getString(DISTINCTCRN), courseCRNs.getInt(ENROLL));
		crnToFac.put(courseCRNs.getString(DISTINCTCRN), facName);

		if (facToCrn.containsKey(facName))
		    facToCrn.get(facName).add(courseCRNs.getString(DISTINCTCRN));
		else {
		    ArrayList<String> crns = new ArrayList<>();
		    crns.add(courseCRNs.getString(DISTINCTCRN));
		    facToCrn.put(facName, crns);
		}
	    }
	}
	courseCRNs.close();

    }

    //this method is far too large
    private void createGraph(String dbname, String url, String usr, String pass) throws SQLException {

	DatabaseConnection connect = new DatabaseConnection(url, usr, pass);
	connect.connect();
	Statement st = connect.getStatement();
	Statement cross = connect.getStatement();
	g = new StudentGraph<String, StudentEdge>(StudentEdge.class);
	sm = new HashMap<>();
	enroll = new HashMap<>();
	crnToFac = new HashMap<>();
	facToCrn = new HashMap<>();

	HashMap<String, String> crnToCrossList = addCrossListedCourses(dbname, cross);
	//end of replaced thing

	String getCourseCRNs = "SELECT DISTINCT CourseCRN, ActualEnroll, FacLastName, FacFirstName FROM " + dbname;
	ResultSet courseCRNs = st.executeQuery(getCourseCRNs);

	while (courseCRNs.next()) {
	    //course is not cross listed
	    if (!crnToCrossList.containsKey(courseCRNs.getString(DISTINCTCRN))) {

		String facName = courseCRNs.getString(LASTNAME) + " " + courseCRNs.getString(FIRSTNAME);
		g.addVertex(courseCRNs.getString(DISTINCTCRN));
		enroll.put(courseCRNs.getString(DISTINCTCRN), courseCRNs.getInt(ENROLL));
		crnToFac.put(courseCRNs.getString(DISTINCTCRN), facName);

		if (facToCrn.containsKey(facName))
		    facToCrn.get(facName).add(courseCRNs.getString(DISTINCTCRN));
		else {
		    ArrayList<String> crns = new ArrayList<>();
		    crns.add(courseCRNs.getString(DISTINCTCRN));
		    facToCrn.put(facName, crns);
		}
	    }
	}
	courseCRNs.close();

	String getIDCRNPairs = "SELECT DISTINCT StudentIDNo, CourseCRN FROM " + dbname + " ORDER BY StudentIDNo";
	ResultSet orderedIDs = st.executeQuery(getIDCRNPairs);

	String currentStudent = null;
	String nextStudent = null;

	ArrayList<String> crns = null;

	while (orderedIDs.next()) {
	    if (!(nextStudent = orderedIDs.getString(ID)).equals(currentStudent)) {
		if (crns != null && crns.size() > 1) {
		    // only count students with more than one exam; the others
		    // don't matter
		    sm.put(currentStudent, new Student(currentStudent, DAYS, BLOCKS));
		    StudentEdge e = null;
		    String s = null;
		    String t = null;
		    for (int i = 0; i < crns.size(); i++)
			for (int j = 0; j < i; j++) {
			    s = crns.get(i);
			    t = crns.get(j);
			    if (crnToCrossList.containsKey(s)) {
				s = crnToCrossList.get(s);
			    }
			    if (crnToCrossList.containsKey(t)) {
				t = crnToCrossList.get(t);
			    }

			    if (g.containsEdge(s, t)) {
				e = g.getEdge(s, t);
				g.addStudent(e, currentStudent);
			    } else {
				g.addEdge(s, t, currentStudent);
			    }
			}
		}
		currentStudent = nextStudent;
		crns = new ArrayList<String>();
		crns.add(orderedIDs.getString(CRN));

	    } else {
		crns.add(orderedIDs.getString(CRN));
	    }

	}

	// could add faculty
	orderedIDs.close();
	st.close();
	connect.close();
    }

    public HashMap<String, String> addCrossListedCourses(String dbname, Statement cross) throws SQLException {
	HashMap<String, String> crnToCrossList = new HashMap<>();
	//I think this query should not include the requirement that the coursetitles be the same as this is not true for all classes
	String getCrossListed = "SELECT DISTINCT a.CourseTitle, a.CrossListCode, a.CourseCRN, a.ActualEnroll, a.FacLastName, "
		+ "a.FacFirstName FROM " + dbname + " a JOIN " + dbname + " b ON "
		+ "(a.CrossListCode IS NOT NULL AND a.CourseTitle=b.CourseTitle AND a.CourseCRN!=b.CourseCRN)";

	//get the query
	ResultSet crossListed = cross.executeQuery(getCrossListed);

	String courseTitle = null;
	String hyphenatedCRN = null;
	int enrollment = 0;
	ArrayList<String> crossListedCourses = new ArrayList<>();
	String facname = null;

	//HMM I'm PRETTY SURE COURSETITLE_CL should actually be crosslist code
	//I'll have to ask dana
	while (crossListed.next()) {
	    //get the new title
	    String newTitle = crossListed.getString(COURSETITLE_CL);
	    //if there is no previous title
	    if (courseTitle == null) {
		crossListedCourses.add(crossListed.getString(COURSECRN_CL));
		hyphenatedCRN = crossListed.getString(COURSECRN_CL);
		enrollment = crossListed.getInt(ENROLL_CL);
		facname = crossListed.getString(FACFIRST_CL) + " " + crossListed.getString(FACLAST_CL);
		courseTitle = newTitle;
	    } else {
		//same course continue appending
		if (newTitle.equals(courseTitle)) {
		    crossListedCourses.add(crossListed.getString(COURSECRN_CL));
		    hyphenatedCRN = hyphenatedCRN + "-" + crossListed.getString(COURSECRN_CL);
		    enrollment += crossListed.getInt(ENROLL_CL);
		} else {
		    //new course, add the previously looked at cross-listed vertex to graph  
		    addHyphenatedCourse(hyphenatedCRN, enrollment, facname);
		    //adds key so a course can be checked if it's cross-listed with the method 'containsKey()'
		    for (String CRN : crossListedCourses) {
			crnToCrossList.put(CRN, hyphenatedCRN);
		    }

		    crossListedCourses.clear();

		    facname = crossListed.getString(FACFIRST_CL) + " " + crossListed.getString(FACLAST_CL);
		    hyphenatedCRN = crossListed.getString(COURSECRN_CL);
		    enrollment = crossListed.getInt(ENROLL_CL);
		    crossListedCourses.add(hyphenatedCRN);
		    courseTitle = newTitle;
		}
	    }

	}
	//add the course to the graph
	addHyphenatedCourse(hyphenatedCRN, enrollment, facname);
	//adds key so a course can be checked if it's cross-listed with the method 'containsKey()'
	for (String CRN : crossListedCourses) {
	    crnToCrossList.put(CRN, hyphenatedCRN);
	}
	return crnToCrossList;

    }

    public void addHyphenatedCourse(String hyphenatedCRN, int enrollment, String facname) {
	g.addVertex(hyphenatedCRN);

	//THIS SHOULD ALSO BE IN COURSE VERTEX
	enroll.put(hyphenatedCRN, enrollment);

	//THIS SHOULD BE IN COURSE VERTEX -- one less hash table
	crnToFac.put(hyphenatedCRN, facname);

	if (facToCrn.containsKey(facname))
	    facToCrn.get(facname).add(hyphenatedCRN);
	else {
	    ArrayList<String> crns = new ArrayList<>();
	    crns.add(hyphenatedCRN);
	    facToCrn.put(facname, crns);
	}
    }

    public static void main(String[] args) throws SQLException {
	String url = "jdbc:mysql://localhost:3306/leep";
	String usr = "javauser";
	String pass = "testpass";
	String sem = "201209";
	GraphCreation f = new GraphCreation("studswfins201209", url, usr, pass);
	boolean useB2B = true;

	Scheduler schedule = new Scheduler(f.getGraph(), f.getStudentMap(), f.getEnrollment(), f.getCrnToFac(),
		f.getFacToCrn());
	//for (int i=0; i<200; i++) { 
	//if(schedule.Schedule(4, 4)) {   
	HashMap<Integer, ArrayList<Integer>> b2b = schedule.getB2BInput();
	while (!schedule.Schedule(DAYS, BLOCKS, b2b, useB2B)) {
	    f = new GraphCreation("studswfins201209", url, usr, pass);
	    schedule = new Scheduler(f.getGraph(), f.getStudentMap(), f.getEnrollment(), f.getCrnToFac(),
		    f.getFacToCrn());
	}
	//statistics using our own data stored in the graph, edges, etc.
	//		NewestSchedulingStats.backToBackPerStud(schedule); 
	//		NewestSchedulingStats.examsInADay(schedule); 
	//		NewestSchedulingStats.examsDepCheck(schedule); 
	System.out.println(" ");
	//statistics using mysql data
	SchedulerCheck schedch = new SchedulerCheck();
	schedch.organizeCourses(schedule);
	schedch.makeStudMap(schedule);

	schedch.numB2BFinalsPerStud(sem, DAYS, BLOCKS, b2b);
	schedch.numFinalDaysPerStud(sem, DAYS);
	schedch.numExamsPerBlock(sem, DAYS);
	schedch.numStudsPerBlock(schedule);
	schedch.miscStats(sem, DAYS);
	schedch.largeExamPlacement(schedule);

	schedch.printCRNSinDay(0);
	schedch.printCRNSinDay(1);
	schedch.printCRNSinDay(2);
	schedch.printCRNSinDay(3);
	//}
	//}
	//schedule.getOneGoodSchedule(4, 4);  

    }

    private static final int COURSETITLE_CL = 1, CROSSLISTCODE_CL = 2, COURSECRN_CL = 3, ENROLL_CL = 4, FACLAST_CL = 5,
	    FACFIRST_CL = 6;


}
