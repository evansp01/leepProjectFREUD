package czexamSchedulingFinal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import consoleThings.CurrentProject;
import databaseForMainProject.DatabaseConnection;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class GraphCreation {

    private CurrentProject project;
    private DependenciesGraph<String, DependentEdge> g;
    private HashMap<String, Student> sm;
    private HashMap<String, CourseVertex> cm;
    private ArrayList<PreScheduledExam> scheduledCourses;

    /**
     * attempts to create a graph from a given project constructor takes a
     * project argument which provides databases and settings and throws a sql
     * exception if something goes wrong
     * 
     * @param project
     * @throws SQLException
     */

    public GraphCreation(CurrentProject project) throws SQLException {
	this.project = project;
	g = new DependenciesGraph<String, DependentEdge>(DependentEdge.class);
	sm = new HashMap<>();
	cm = new HashMap<>();
	scheduledCourses = new ArrayList<>();

	DatabaseConnection connect = project.connection;
	Statement st = null;
	try {
	    st = connect.getStatement();

	    String finalsTable = CurrentProject.finals;
	    String studswfins = CurrentProject.studentsWithInfo;
	    String tableName = "graphcreateTemp";

	    String query = "CREATE TEMP TABLE " + tableName + " AS (SELECT t1.* FROM " + studswfins + " AS t1, "
		    + finalsTable + " AS t2 WHERE CHARINDEX (t2.CourseCRN, t1.CourseCRN) > 0)";
	    st.executeUpdate(query);

	    addVerts(st, tableName);
	    addStudentsDeps(st, tableName);
	    if (project.settings.facultyConstraint)
		addFacultyDeps(st, tableName);

	    //update course verts to include degrees

	    st.executeUpdate("DROP TABLE " + tableName);
	} catch (SQLException e) {
	    throw new SQLException();
	} finally {
	    if (st != null)
		st.close();
	}
	for (CourseVertex cv : cm.values())
	    cv.setDegrees(g);
    }

    /**
     * Getter for the dependencies graph
     * 
     * @return graph the dependencies graph
     */
    public DependenciesGraph<String, DependentEdge> getGraph() {
	return g;
    }

    /**
     * getter for the students map
     * 
     * @return studentmap a mapping of id numbers to student objects
     */
    public HashMap<String, Student> getStudentMap() {
	return sm;
    }

    /**
     * getter for the courses map
     * 
     * @return coursemap a mapping of course crns to course objects
     */
    public HashMap<String, CourseVertex> getCourseMap() {
	return cm;
    }

    /**
     * getter for prescheduled exams
     * 
     * @return a list of all already scheduled exams
     */
    public ArrayList<PreScheduledExam> getAlreadyScheduled() {
	return scheduledCourses;
    }

    public static final int COURSECRN = 1, ENROLLMENT = 2, FDAY = 3, FBLOCK = 4;

    /**
     * Adds the course verticies to the graph
     * 
     * @param st
     * @param tableName
     * @throws SQLException
     */

    private void addVerts(Statement st, String tableName) throws SQLException {
	String getCourseCRNs = "SELECT DISTINCT CourseCRN, ActualEnroll, FinalDay, FinalBlock FROM " + tableName;
	ResultSet courseCRNs = st.executeQuery(getCourseCRNs);
	int days = project.settings.days;
	int blocks = project.settings.blocks;
	boolean large = project.settings.largeConstraint;

	while (courseCRNs.next()) {
	    //course is not cross listed
	    String courseName = courseCRNs.getString(COURSECRN);
	    int fday = courseCRNs.getInt(FDAY);
	    int fblock = courseCRNs.getInt(FBLOCK);
	    //only include enrollment if large exams is a constraintF
	    int enroll = large ? courseCRNs.getInt(ENROLLMENT) : 1;
	    g.addVertex(courseName);
	    CourseVertex vert = new CourseVertex(courseName, enroll, days, blocks);
	    if (fday >= 0 && fblock >= 0) {
		//note that the exam is scheduled and put it in the 'scheduled exams' list so it can be iterated over later
		scheduledCourses.add(new PreScheduledExam(courseName, fday, fblock));
	    }
	    cm.put(courseName, vert);

	}
	courseCRNs.close();
    }

    public static final int ID_STUDENT = 1, CRN_STUDENT = 2;

    /**
     * Add the student dependencies to the graph
     * 
     * @param st
     * @param tableName
     * @throws SQLException
     */
    private void addStudentsDeps(Statement st, String tableName) throws SQLException {
	String getIDCRNPairs = "SELECT DISTINCT StudentIDNo, CourseCRN FROM " + tableName + " ORDER BY StudentIDNo";
	ResultSet orderedIDs = st.executeQuery(getIDCRNPairs);

	int days = project.settings.days;
	int blocks = project.settings.blocks;
	String currentStudent = null;
	String nextStudent = null;
	ArrayList<String> crns = new ArrayList<String>();

	while (orderedIDs.next()) {
	    if (!(nextStudent = orderedIDs.getString(ID_STUDENT)).equals(currentStudent)) {
		if (crns != null && crns.size() > 1) {
		    if (!sm.containsKey(currentStudent))
			sm.put(currentStudent, new Student(currentStudent, days, blocks));
		    DependentEdge e = null;
		    String s = null;
		    String t = null;
		    for (int i = 0; i < crns.size(); i++)
			for (int j = 0; j < i; j++) {
			    s = crns.get(i);
			    t = crns.get(j);
			    if (g.containsEdge(s, t)) {
				e = g.getEdge(s, t);
				g.addEdgeMember(e, currentStudent);
			    } else {
				g.addEdge(s, t, currentStudent, true);
			    }
			}
		}
		currentStudent = nextStudent;
		crns.clear();
		crns.add(orderedIDs.getString(CRN_STUDENT));

	    } else {
		crns.add(orderedIDs.getString(CRN_STUDENT));
	    }

	}
	orderedIDs.close();

    }

    public static final int FIRST_FAC = 1, LAST_FAC = 2, CRN_FAC = 3;

    /**
     * add faculty dependencies to the graph
     * 
     * @param st
     * @param tableName
     * @throws SQLException
     */
    private void addFacultyDeps(Statement st, String tableName) throws SQLException {
	String getIDCRNPairs = "SELECT DISTINCT FacFirstName, FacLastName, CourseCRN FROM " + tableName
		+ " ORDER BY FacFirstName, FacLastName";
	ResultSet orderedIDs = st.executeQuery(getIDCRNPairs);

	String currentFac = null;
	String nextFac = null;
	ArrayList<String> crns = new ArrayList<String>();
	while (orderedIDs.next()) {
	    nextFac = orderedIDs.getString(FIRST_FAC) + "$" + orderedIDs.getString(LAST_FAC);
	    if (!nextFac.equals(currentFac)) {
		if (crns != null && crns.size() > 1) {
		    String s = null;
		    String t = null;
		    for (int i = 0; i < crns.size(); i++)
			for (int j = 0; j < i; j++) {
			    s = crns.get(i);
			    t = crns.get(j);
			    //in the case of faculty, only add the edge if it does not exist, since they
			    //do not count for the 3 in day or the back to back constraints, only the direct impossible
			    //constraints
			    if (!g.containsEdge(s, t)) {
				g.addEdge(s, t, currentFac, false);
			    }
			}
		}
		currentFac = nextFac;
		crns.clear();
		crns.add(orderedIDs.getString(CRN_FAC));

	    } else {
		crns.add(orderedIDs.getString(CRN_FAC));
	    }

	}
	orderedIDs.close();
    }

}
