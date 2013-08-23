package examScheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Scheduler {

    public static final int MAXEXAMSPERDAY = 2, LARGE=50;
    public static final boolean AVAILABLE = false, TAKEN = true, BACKTOBACK = true; 
    public int B2B1=1, B2B2=2, B2B3=3, NUMEXAMSPERDAY=4, NUMBLOCKS=4; 

    StudentGraph<String, StudentEdge> swg;
    PriorityQueue<CourseVertex> pq;
    HashMap<String, CourseVertex> cm;
    HashMap<String, Student> sm; 
    HashMap<String, Boolean> scheduled; //after removal from pq, checks if it has been scheduled already based on CRN   
    HashMap<String, String> crnToFac; 
    HashMap<String, ArrayList<String>> facToCrn;
    private int days = 0, blocksPerDay = 0;
    private boolean[][] backToBack = null;

    public Scheduler(StudentGraph<String, StudentEdge> g, HashMap<String, Student> sm, HashMap<String, Integer> enroll, HashMap<String, String> crnFac, HashMap<String, ArrayList<String>> facCrn) {
	this.swg = g;
	this.sm = sm; 
	this.crnToFac = crnFac; 
	this.facToCrn = facCrn;
	Set<String> verticies = g.vertexSet();
	cm = new HashMap<>();
	pq = new PriorityQueue<>(); 
	scheduled = new HashMap<>();
	for (String s : verticies) {  
		if (s=="20170-21184" || s=="21184") 
			System.out.println("here");
		scheduled.put(s, false);
	    CourseVertex v = new CourseVertex(s, g); 
	    v.enterEnrollment(enroll.get(s));
	    cm.put(s, v); 
	  
	}
    }

    private int getAverageDegree() {
	int sum = 0;
	int count = 0;
	for (CourseVertex c : cm.values()) {
	    sum += c.getDegree();
	    count++;
	}
	if (count > 0) {
	    return sum / count + 1;
	}
	return 1;

    }

    public Collection<CourseVertex> courseVertices() {
	return cm.values();
    } 
    
    public Collection<Student> students(){ 
    	return sm.values();
    } 
    
    public StudentGraph<String, StudentEdge> getGraph(){ 
    	return swg;
    } 
    
    public HashMap<String, CourseVertex> getCourseMap() { 
    	return cm;
    }

    public boolean[][] getPossibleTimes(String courseName, int b2blimit) {
	Set<StudentEdge> adj = swg.edgesOf(courseName);
	return getPossibleTimes(adj, courseName, b2blimit);
    }

    //shadow implementation
    //this method should return some format of the 
    //problems with the current schedule
    //probably doesn't belong here actually
    public Object reportProblems() {

	return null;
    }

    public void Schedule() {
	int[][] backToBack = { { 2, 3 }, { 3, 4 } };
	Schedule(4, 4, backToBack);

    }

    public void Schedule(int days, int blocksPerDay, int[][] backToBack) {
	this.days = days;
	this.blocksPerDay = blocksPerDay;
	this.backToBack = new boolean[blocksPerDay][blocksPerDay]; 
	//creates double array of booleans. backToBack[i] gives an array of back-to-back boolean values with respect to the ith block 
	//i.e. if blocks 2&3 are considered backToBack, then backToBack[2-1][3-1]=true and backToBack[3-1][2-1]=true
	for (int[] pair : backToBack) {
	    this.backToBack[pair[0] - 1][pair[1] - 1] = BACKTOBACK;
	    this.backToBack[pair[1] - 1][pair[0] - 1] = BACKTOBACK;
	}
	boolean success;
	int startAverage = getAverageDegree();
	int back = 0;
	int[] averageTry = { startAverage, (int) (startAverage * 0.5), (int) (startAverage * 1.5), Integer.MAX_VALUE };
	int currentTry = 0;
	int average = startAverage;
	do {
	    rebuildPQ();
	    success = true;
	    while (!pq.isEmpty()) {
		CourseVertex current = pq.remove();
		if (!attemptToSchedule(current, average, back)) {
		    success = false;
		    //		    System.out.println(average + " " + back + " failed");
		    break;
		}

	    }
	    if (++currentTry >= averageTry.length) {
		currentTry = 0;
		back++;
	    }
	    average = averageTry[currentTry];

	} while (!success);

	printSchedule();
    }

    private void rebuildPQ() {
	pq.clear();
	pq.addAll(cm.values());
	for (Student s : sm.values()) {
	    s.resetOccupancy();
	}

    }

    public void printSchedule() {
	for (int i = 0; i < days; i++) {
	    for (int j = 0; j < blocksPerDay; j++) {
		System.out.print(countAtTime(i, j) + "\t");
	    }
	    System.out.println();
	}
    }

    public int countAtTime(int day, int block) {
	int count = 0;
	for (CourseVertex v : cm.values()) { 
	    if (v.block() == block && v.day() == day)
	    	count++;
	}
	return count;
    }

    private boolean attemptToSchedule(CourseVertex cv, int limitThree, int bblimit) {
	Set<StudentEdge> adj = swg.edgesOf(cv.name());
	boolean[][] timeSlots = getPossibleTimes(adj, cv.name(), bblimit);
	for (int day = 0; day < days; day++)
	    for (int block = 0; block < blocksPerDay; block++) {
		if (timeSlots[day][block] == AVAILABLE) {
		    if (block != 2 || adj.size() < limitThree) {
			cv.setTime(block, day); //set course time
			for (StudentEdge e : adj) { //add this occupancy to all students
			    Iterator<String> studentItr = swg.getStudents(e);
			    while (studentItr.hasNext()) {
				String studentName = studentItr.next();
				Student student = sm.get(studentName);
				student.occupy(day, block);
			    }
			}
			return true;
		    }
		}
	    }
	return false;
    }

    private boolean[][] getPossibleTimes(Set<StudentEdge> adj, String name, int bblimit) {
	boolean[][] times = new boolean[days][blocksPerDay];

	for (StudentEdge e : adj) {
	    String other = null;
	    if ((other = swg.getEdgeTarget(e)).equals(name)) {
		other = swg.getEdgeSource(e);
	    } //set other to refer to the other course
	    Iterator<String> studentItr = swg.getStudents(e); 
	    
	    while (studentItr.hasNext()) {
		String studentName = studentItr.next();
		Student student = sm.get(studentName);
		for (int i = 0; i < days; i++) { 
			//check that student doesn't already have max # of exams per student on day i 
			//if so, make every slot unavailable on day i
		    if (student.gtNExamsInDay(MAXEXAMSPERDAY, i)) {
			for (int j = 0; j < blocksPerDay; j++) {
			    times[i][j] = TAKEN;
			}
		    }
		}
	    }

	    CourseVertex othr = cm.get(other);
	    if (othr.day() != -1 && othr.block() != -1) { 
	    //check that the number of students that would have back to back exams is less than a limit (bblimit) 
	    //if >limit, make sure no back to back scheduling occurs
		if (e.getWeight() > bblimit) {
		    for (int backToBackBlock : backToBackBlocks(othr.block())) {
			times[othr.day()][backToBackBlock] = TAKEN;
		    }
		} 
		//hard constraint - make sure no student scheduled at same time as other exam
		times[othr.day()][othr.block()] = TAKEN;
	    }
	}
	return times;
    }

    private Collection<Integer> backToBackBlocks(int blockIn) {
	LinkedList<Integer> backToBackList = new LinkedList<>();
	for (int block = 0; block < blocksPerDay; block++) {
	    if (backToBack[blockIn][block])
		backToBackList.add(block);

	}
	return backToBackList;
    } 
    
    //***********************************separating old approach from new implementation approach   
    
//    public void getOneGoodSchedule(int days, int blocksPerDay) {  
//    	HashMap<Integer, ArrayList<Integer>> b2b = getB2BInput();
//    	boolean success = Schedule(days, blocksPerDay, b2b);
//    	while(success!=true) { 
//    		success = Schedule(days, blocksPerDay, b2b);
//    	}
//    } 
    
    public HashMap<Integer, ArrayList<Integer>> getB2BInput() { 
    	HashMap<Integer, ArrayList<Integer>> b2b = new HashMap<>();
    	Scanner scanner = new Scanner( System.in );  
    	System.out.println("Final Exam Scheduler - 2013");
    	System.out.println("Before we get started, we first need to know which slots are considered back to back.");  
    	String input = "no";
    	while(!input.equals("yes")) { 
    		System.out.println("Please type in first back to back block"); 
    		int block1 = scanner.nextInt()-1; //-1 just reindexes
    		System.out.println("Please type in second back to back block"); 
    		int block2 = scanner.nextInt()-1;  
    		
    		if(b2b.containsKey(block1)) 
    			b2b.get(block1).add(block2); 
    		else { 
    			ArrayList<Integer> conflicts = new ArrayList<>(); 
    			conflicts.add(block2); 
    			b2b.put(block1, conflicts);
    		}  
    		
    		if(b2b.containsKey(block2)) 
    			b2b.get(block1).add(block1); 
    		else { 
    			ArrayList<Integer> conflicts = new ArrayList<>(); 
    			conflicts.add(block1); 
    			b2b.put(block2, conflicts);
    		}   
    		
    		System.out.println("Ready?(no/yes)"); 
    		input = scanner.next();
    			
    		
    	}  
    	
    	return b2b;
    }
    
    public boolean Schedule(int days, int blocksPerDay, HashMap<Integer, ArrayList<Integer>> b2b, boolean useB2B) { 
    	this.days = days; 
    	this.blocksPerDay=blocksPerDay;     
    	for (CourseVertex cv: cm.values()) { 
    		cv.getDayBlockInfo(days, blocksPerDay);
    	}
    	pq.clear(); 
    	pq.addAll(cm.values());   
    	while(!pq.isEmpty()) {  
    		CourseVertex current = pq.remove(); 
    		String CRN = current.name();
    		if (scheduled.get(CRN)) //course has been scheduled 
    			continue; 
    		else { 
    			if(!scheduleCourse(current, b2b, useB2B))  
    				return false;
    			
    		}
    	} 
    	printSchedule(); 
    	return true;

    	
    }
    
    private boolean scheduleCourse(CourseVertex cv, HashMap<Integer, ArrayList<Integer>> b2b, boolean useB2B) {
    	
    	int [][] unacc = cv.unacceptability(); //gives the chart of acceptable and favorable exam times  
    	int dayIt, blockIt, foundDay, foundBlock; //variables to iterate through days and blocks 
    	dayIt=0; 
    	blockIt=0;  
    	foundDay=-1; 
    	foundBlock=-1;  
    	ArrayList<Pair> accDaysBlocks = new ArrayList<Pair>();
    	while (dayIt<days) {  
    		blockIt=0;
    		while (blockIt<blocksPerDay) { 
    			if(unacc[dayIt][blockIt]!=-1 && unacc[dayIt][blockIt]<1) { //found an acceptable slot && a favorable slot   
    				//System.out.println("Found slot");
    				Pair dayblock = new Pair(dayIt, blockIt);  
    				accDaysBlocks.add(dayblock);
//    				if (cv.getEnrollment()>=LARGE)  {
//    					//if (dayIt==0 || dayIt==1) -------removing large constraint for now
//    						//accDaysBlocks.add(dayblock);  
//    				}
//    				else
//    					accDaysBlocks.add(dayblock);
    												//keeps track of what has been scheduled since we will have copies of 
    			}                                   //same course in priority queue
    			
    			blockIt++;
    		} 
    		
    		dayIt++;
    	}  
    	
    	int numAccSlots = accDaysBlocks.size();
    	
    	if (numAccSlots==0) {
    		System.out.println("Couldn't find a favorable slot");   
    		return false;
    	}
    	else {  
    		Random findRandomSlot = new Random(); 
    		int index = findRandomSlot.nextInt(numAccSlots); 
    		Pair dayblock = accDaysBlocks.get(index); //getting first slot for debugging purposes
    		foundDay = dayblock.getFirst(); 
    		foundBlock = dayblock.getSecond(); 
    		cv.setTime(foundBlock, foundDay);
			scheduled.put(cv.name(), true);    
			//update based on instructor constraint - instructor cannot administer more than two exams in same block
			String facName = crnToFac.get(cv.name()); 
			ArrayList<String> facDependents = facToCrn.get(facName); 
			for (String CRN: facDependents) {  
				CourseVertex course = cm.get(CRN); 
				if(!scheduled.get(CRN)) 
					course.removeSlot(foundDay, foundBlock);
				}
    		//change info of dependents based on where cv was placed 
    		ArrayList<String> dependents = swg.getDependencies(cv.name()); 
    		for (String CRN : dependents) { 
    			CourseVertex course = cm.get(CRN); 
    			
    			if (!scheduled.get(CRN)) { //if course has not been scheduled 
    				//function to make foundDay and foundBlock unavailable 
    				course.removeSlot(foundDay, foundBlock);
    				StudentEdge edge = swg.getEdge(cv.name(), course.name());  
    				check3InaRow(course, edge, foundDay, foundBlock);   
    				if (course.isAvailable(foundDay) && useB2B) 
    					checkb2b(course, edge, foundDay, foundBlock, b2b); 
    				course.updateAvailability(); 
    				pq.add(course); //add back course to priority queue
    				
    			}
    		}
    		
    		return true;
    	}
    }  
    
   
    private void check3InaRow(CourseVertex cv, StudentEdge e, int day, int block) { 
    	Iterator<String> studItr = e.getStudents();
    	while(studItr.hasNext()) {  
    		String studID = studItr.next();
    		Student stud = sm.get(studID);
    		stud.occupy(day, block);  
    		if(stud.gtNExamsInDay(MAXEXAMSPERDAY, day)){ //if student in class has MAXEXAMS already on day i
    				cv.removeDay(day); //then day becomes unavailable for this exam
    				//break; //no other slots can become unavailable - cannot break here, need all students to have block occupied
    		}
    	}
    } 
    
    //the following function can only occur if at least one slot of the day is available, otherwise, it may change a -1 (meaning 
    //completely unacceptable) to a 0
    private void checkb2b(CourseVertex cv, StudentEdge e, int day, int block, HashMap<Integer, ArrayList<Integer>> b2b) {  
    	Iterator<String> studItr = e.getStudents(); 
		while (studItr.hasNext()) {  
		studItr.next();
    	if (b2b.containsKey(block)) { 
    		ArrayList<Integer> badBlocks = b2b.get(block);
    		int [][] unacc = cv.unacceptability();  
    		for (int B2Bblock : badBlocks) { 
    			if(unacc[day][B2Bblock]!=-1) 
    				cv.addB2BConflict(day, B2Bblock);
    		} 
    	}
//			if (block == B2B1 || block == B2B2 || block == B2B3) { // if course
//																	// placed in
//																	// a b2b
//																	// slot 
//				int [][] unacc = cv.unacceptability();
//				if (block == B2B1 && unacc[day][B2B2]!=-1)
//					cv.addB2BConflict(day, B2B2); // adds unfavorability to B2B2
//				else if (block == B2B2) { 
//					if(unacc[day][B2B1]!=-1)
//						cv.addB2BConflict(day, B2B1); 
//					if(unacc[day][B2B3]!=-1)
//						cv.addB2BConflict(day, B2B3);
//				} else 
//					if(unacc[day][B2B2]!=-1)
//						cv.addB2BConflict(day, B2B2);
			}
//		}
    } 
    
    public void printLargeExams(int degree) {  
    	Set<String> vertices = swg.vertexSet();
    	for (String s: vertices) { 
    		if(cm.get(s).getDegree()>degree) 
    			System.out.println(s);
    	}
    }
    
    public boolean isEdgeBetween(CourseVertex cv1, CourseVertex cv2) { 
    	return swg.containsEdge(cv1.name(), cv2.name());
    }
}