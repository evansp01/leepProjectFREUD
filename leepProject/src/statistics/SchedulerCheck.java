package statistics;  


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import databaseForMainProject.DatabaseConnection;
import examScheduling.*;  


public class SchedulerCheck {  
	
	static final String [] Sems = {"studswfins201009", "studswfins201101", "studswfins201109", "studswfins201201", "studswfins201209", "studswfins201301"};
    static final String URL= "jdbc:mysql://localhost:3306/leep", USR="javauser", PASS= "testpass";     
    static final int DAY1=0, DAY2=1, DAY3=2, DAY4=3; 
    
    ArrayList<String> coursesDay1=new ArrayList<String>(), coursesDay2=new ArrayList<String>(), 
    		coursesDay3=new ArrayList<String>(), coursesDay4=new ArrayList<String>();
    
    HashMap<Integer, ArrayList<String>> daysAndCourses = new HashMap<>();  
    
    public SchedulerCheck(){
    	daysAndCourses.put(DAY1, coursesDay1);  
    	daysAndCourses.put(DAY2, coursesDay2); 
    	daysAndCourses.put(DAY3, coursesDay3); 
    	daysAndCourses.put(DAY4, coursesDay4);
    } 
    
    public int numberOfDays(){ 
    	return daysAndCourses.size();
    } 
    
    public ArrayList<String> coursesOnDay(int k){ 
    	return daysAndCourses.get(k);
    }
    
    
    public void organizeCourses(Scheduler sched) {  
    	for (CourseVertex v: sched.courseVertices()) {  
    		int day = v.day(); 
    		ArrayList<String> courseList = daysAndCourses.get(day);  
    		courseList.add(v.name());  
    		}
    } 
    
    public void createQuery(ArrayList<String> courses, String sem) throws SQLException {  
    	String condition = " "; 
    	DatabaseConnection connect = new DatabaseConnection(URL, USR, PASS);
    	connect.connect(); 
    	Statement st = connect.getStatement(); 
    	
    	for (int i=0; i<courses.size(); i++) { 
    		condition = condition + "t1.CourseCRN= " + courses.get(i);  
    		if (i!=courses.size()-1)  
    			condition = condition + " OR "; 
    		else 
    			condition = condition + " ";  
    	}  
    	
    	String query = "SELECT t1.StudentIDNo, count(t1.StudentIDNo) FROM (SELECT DISTINCT CourseCRN, StudentIDNo FROM " + sem + ") AS t1  WHERE " + condition + 
    			"GROUP BY t1.StudentIDNo HAVING count(t1.StudentIDNo)>=3"; 
    	ResultSet result = null;
		try {
			result = st.executeQuery(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		
    	while(result.next()){ 
    		System.out.println(result.getString(1)+ " " + result.getInt(2));
    	} 
    	System.out.println("*******************"); 
    	System.out.println(" ");
    	result.close(); 
    	st.close(); 
    	connect.close();
    	
    }
    
    public static void main(String [] args) { 
    	for (int i=0; i<Sems.length; i++){
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
		
		for (int j=0; j<initCheck.numberOfDays(); j++){ 
			try { 
				System.out.println("Day " + (j+1));
				initCheck.createQuery(initCheck.coursesOnDay(j), Sems[i]);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} 
    	}
    	
		
		
		
		
    }
}

