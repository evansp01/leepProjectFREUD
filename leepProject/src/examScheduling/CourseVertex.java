package examScheduling;

/**
 * holds addition information for courses including crn, degree, weighted
 * degree, and final block and time
 * 
 * @author evan
 * 
 */

public class CourseVertex implements Comparable<CourseVertex> {
    private String name; //typically CourseCRN
    private int wdegree; //weighted degree
    private int degree; //unweighted degree
    private int block; //block time for exam
    private int day; //day of exam
    private boolean scheduled; //returns true if course has been scheduled 
    private int [][] unacceptability;  
    private int acceptableSlots; 
    private int favorableSlots;  
    private int enrollment;
    
    private static int B2B1=2, B2B2=3, B2B3=4;
    
    private static final int BLOCKSPERDAY=4, DAYS=4;

    /**
     * 
     * @param name
     *            crn associated with course vertex
     * @param g
     *            graph to create vertex from
     */
    public CourseVertex(String name, StudentGraph<String, StudentEdge> g) {
	this.name = name;
	wdegree = g.degreeOf(name);
	degree = g.edgesOf(name).size();
	block = -1;
	day = -1; 
	scheduled=false; 
	unacceptability = new int [DAYS][BLOCKSPERDAY]; 
	acceptableSlots=DAYS*BLOCKSPERDAY; 
	favorableSlots=DAYS*BLOCKSPERDAY; 
	enrollment=0;
    }

    /**
     * sets the final block and day of this course
     * 
     * @param block
     *            final block
     * @param day
     *            final day
     */

    public void setTime(int block, int day) {
	this.block = block;
	this.day = day;
    }

    /**
     * 
     * @return returns the final day of this course -1 indicates not yet
     *         scheduled
     */
    public int day() {
	return day;
    }

    /**
     * 
     * @return returns the final block of this course -1 indicates not yet
     *         scheduled
     */
    public int block() {
	return block;
    }

    /**
     * 
     * @return returns the crn associated with this vertex
     */
    public String name() {
	return name;
    }

    /**
     * 
     * @return return the unweighted degree of this course vertex
     */
    public int getDegree() {
	return degree;
    }

    /**
     * 
     * @return returns the weighted degree of this course vertex
     */
    public int getWeightedDegree() {
	return wdegree;
    } 
    
    public int getAccSlots() { 
    	return acceptableSlots;
    }  
    
    public int getEnrollment() { 
    	return enrollment;
    }
    
    public void enterEnrollment(int enroll) { 
    	enrollment=enroll;
    }

//    @Override
//    public int compareTo(CourseVertex o) {
//	if (o == null)
//	    return 1;
//	if (o.degree == degree) {
//	    if (o.wdegree == wdegree) {
//		return name.compareTo(o.name);
//	    }
//	    return -(wdegree - o.wdegree);
//	}
//	return -(degree - o.degree);
//    }  
    
    public int compareTo(CourseVertex o) { 
    	if (o==null) 
    		return 1; 
    	if (favorableSlots!=o.favorableSlots) //first check favorable slots
    		return favorableSlots-o.favorableSlots;   
    	else if(acceptableSlots!=o.acceptableSlots) 
    		return acceptableSlots-o.acceptableSlots; //then acceptable
    	else  if (degree!=o.degree)
    		return -(degree-o.degree); //then degree
    	else if (wdegree!=o.wdegree) 
    		return -(wdegree-o.wdegree); //then weighted degree
    	else 
    		return -1;
    } 
    
    public void removeSlot(int day, int block) { 
    	unacceptability[day][block]=-1;
    }
    
    public void removeDay(int day) { 
    	//set each block to -1 to denote the total unacceptability of the day
    	for (int block=0; block<BLOCKSPERDAY; block++){ 
    		unacceptability[day][block]=-1;
    	}
    }   
    
    public boolean isAvailable(int day) {  
    	for (int block : unacceptability[day]) {//returns true if at least one slot is acceptable 
    		if (block!=-1) //day is unacceptable if all of its slots are -1
    			return true; 
    	} 
    	
    	return false;
    }
    
    public boolean isScheduled(){ 
    	return scheduled;
    } 
    
    public void schedule() { 
    	
    	scheduled=true;
    }  
   
    public int [][] unacceptability() { 
    	return unacceptability;
    } 
    
    public void addB2BConflict(int day, int block) { 
    	unacceptability[day][block]++;
    }
    
    public void updateAvailability(){  
    	acceptableSlots=0; 
    	favorableSlots=0;
    	for (int day=0; day<DAYS; day++) { 
    		for (int block=0; block<BLOCKSPERDAY; block++) { 
    			if(unacceptability[day][block]!=-1) { //-1 means totally unacceptable - meaning three exams in a row for some student 
    				acceptableSlots++;  
    				if (block==B2B1 || block==B2B2 || block==B2B3){  
    					if(unacceptability[day][block]<1) //as long as no more than 2 people have b2b exams, consider it favorable 
    						favorableSlots++; 
    				
    				} 
    				else 
    					favorableSlots++; //if not a back to back slot, just increase the number of favorable slots
    			}
    		}
    	}
    }  
    
    
    
    
    
}
