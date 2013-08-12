package examScheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import statistics.NewestSchedulingStats;
import statistics.SchedulerCheck;

import databaseForMainProject.DatabaseConnection;

public class GraphCreation {
    public static final int ID = 1, CRN = 2, DISTINCTCRN = 1, ENROLL=2, LASTNAME=3, FIRSTNAME=4;

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
    

    private void createGraph(String dbname, String url, String usr, String pass) throws SQLException {

	DatabaseConnection connect = new DatabaseConnection(url, usr, pass);
	connect.connect();
	Statement st = connect.getStatement();

	g = new StudentGraph<String, StudentEdge>(StudentEdge.class);
	sm = new HashMap<>(); 
	enroll = new HashMap<>(); 
	crnToFac = new HashMap<>(); 
	facToCrn = new HashMap<>();

	String getCourseCRNs = "SELECT DISTINCT CourseCRN, ActualEnroll, FacLastName, FacFirstName FROM " + dbname;
	ResultSet courseCRNs = st.executeQuery(getCourseCRNs);

	while (courseCRNs.next()) { 
		String facName = courseCRNs.getString(LASTNAME) + " " + courseCRNs.getString(FIRSTNAME);
	    g.addVertex(courseCRNs.getString(DISTINCTCRN)); 
	    enroll.put(courseCRNs.getString(DISTINCTCRN), courseCRNs.getInt(ENROLL)); 
	    crnToFac.put(courseCRNs.getString(DISTINCTCRN), facName); 
	    
	    if(facToCrn.containsKey(facName)) 
	    	facToCrn.get(facName).add(courseCRNs.getString(DISTINCTCRN)); 
	    else { 
	    	ArrayList<String> crns = new ArrayList<>(); 
	    	crns.add(courseCRNs.getString(DISTINCTCRN));  
	    	facToCrn.put(facName, crns );
	    }
	}
	courseCRNs.close();

	String getIDCRNPairs = "SELECT DISTINCT StudentIDNo, CourseCRN FROM " + dbname + " ORDER BY StudentIDNo";
	ResultSet orderedIDs = st.executeQuery(getIDCRNPairs);

	String currentStudent = null;
	String nextStudent = null;

	ArrayList<String> crns = null;

		while (orderedIDs.next()) {
			if (!(nextStudent = orderedIDs.getString(ID)) 
					.equals(currentStudent)) {
				if (crns != null && crns.size() > 1) {
					// only count students with more than one exam; the others
					// don't matter
					sm.put(currentStudent, new Student(currentStudent, 4, 4));
					StudentEdge e = null;
					String s = null;
					String t = null;
					for (int i = 0; i < crns.size(); i++)
						for (int j = 0; j < i; j++) {
							s = crns.get(i);
							t = crns.get(j);
							if (g.containsEdge(s, t)) {
								e = g.getEdge(s, t);
								g.addStudent(e, currentStudent);
							} else
								g.addEdge(crns.get(i), crns.get(j),
										currentStudent);
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
    

    public static void main(String[] args) throws SQLException {
	String url = "jdbc:mysql://localhost:3306/leep";
	String usr = "javauser";
	String pass = "testpass";

	GraphCreation f = new GraphCreation("studswfins201109", url, usr, pass);

	Scheduler schedule = new Scheduler(f.getGraph(), f.getStudentMap(), f.getEnrollment(), f.getCrnToFac(), f.getFacToCrn());  
	//for (int i=0; i<200; i++) { 
	if(schedule.Schedule(4, 4)) { 
		//statistics using our own data stored in the graph, edges, etc.
		NewestSchedulingStats.backToBackPerStud(schedule); 
		NewestSchedulingStats.examsInADay(schedule); 
		NewestSchedulingStats.examsDepCheck(schedule); 
		
		//statistics using mysql data
		SchedulerCheck schedch = new SchedulerCheck(); 
		schedch.organizeCourses(schedule); 
		schedch.numB2BFinalsPerStud("studswfins201109");
	}
	//}
	//schedule.getOneGoodSchedule(4, 4);  
	
    }
}
