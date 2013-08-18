package statistics;

import java.util.ArrayList;
import java.util.Collection;

import examScheduling.CourseVertex;
import examScheduling.Scheduler;
import examScheduling.Student; 
import examScheduling.StudentEdge;
import examScheduling.StudentGraph;



public class NewestSchedulingStats {   
	
	public static final int NUMEXAMDAYS=4, NUMBLOCKS=4; 
	
	
	public static void backToBackPerStud(Scheduler sched) { 
		int numB2Bs=0;
		for(Student stud : sched.students()) { 
			for (int day=0; day<NUMEXAMDAYS; day++) { 
				if(stud.examsInDay(day)>=2) { 
					if(stud.isOccupied(day, sched.B2B1) && stud.isOccupied(day, sched.B2B2)) {
						numB2Bs++;   
						System.out.println("Student: " + stud.name());
					}
					else if(stud.isOccupied(day, sched.B2B2) && stud.isOccupied(day, sched.B2B3)) { 
						numB2Bs++;  
						System.out.println("Student: " + stud.name());
					}
				}
			}
		} 
		
		System.out.println("Number of back to back exams: " + numB2Bs++);
	} 
	
	public static void examsInADay(Scheduler sched){  
		int numOf3s=0;
		for(Student stud : sched.students()) { 
			for (int day=0; day<NUMEXAMDAYS; day++) { 
				if(stud.examsInDay(day)>2) 
					numOf3s++;
			}
		} 
		
		System.out.println("Number of 3 in a day conflicts: " + numOf3s);
		
	} 
	
	public static void examsDepCheck(Scheduler sched){ 
		for (CourseVertex cv: sched.courseVertices()) {   
			if (cv.block() == 1 || cv.block() == 2 || cv.block() == 3) {
				StudentGraph<String, StudentEdge> swg = sched.getGraph();
				ArrayList<String> dependents = swg.getDependencies(cv.name());
				for (String CRN : dependents) {
					CourseVertex dep = sched.getCourseMap().get(CRN); 
					if (dep.day()==cv.day()) { 
						if(cv.block()==1 && dep.block()==2) 
							System.out.println(cv.name() + " " + dep.name()); 
						else if(cv.block()==2 && dep.block()==1)
							System.out.println(cv.name() + " " + dep.name());   
						else if (cv.block()==2 && dep.block()==3) 
							System.out.println(cv.name() + " " + dep.name());  
						else if (cv.block()==3 && dep.block()==2) 
							System.out.println(cv.name() + " " + dep.name()); 
					}
				}
			}
		}
	}
	

}
