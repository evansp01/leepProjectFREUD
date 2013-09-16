package examScheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

//tree set

public class Scheduler {

    public static final int MAXEXAMSPERDAY = 2, LARGE = 50;
    public static final boolean AVAILABLE = false, TAKEN = true, BACKTOBACK = true;
    public int B2B1 = 1, B2B2 = 2, B2B3 = 3, NUMEXAMSPERDAY = 4, NUMBLOCKS = 4;

    StudentGraph<String, StudentEdge> swg;
    PriorityQueue<CourseVertex> pq;
    HashMap<String, CourseVertex> cm;
    HashMap<String, Student> sm;
    HashMap<String, Boolean> scheduled; //after removal from pq, checks if it has been scheduled already based on CRN   
    HashMap<String, String> crnToFac;
    HashMap<String, ArrayList<String>> facToCrn;
    private int days = 0, blocksPerDay = 0;

    /**
     * Constructs a scheduling object
     * 
     * @param g
     * @param sm
     * @param enroll
     * @param crnFac
     * @param facCrn
     */
    public Scheduler(StudentGraph<String, StudentEdge> g, HashMap<String, Student> sm, HashMap<String, Integer> enroll,
	    HashMap<String, String> crnFac, HashMap<String, ArrayList<String>> facCrn) {
	this.swg = g;
	this.sm = sm;
	this.crnToFac = crnFac;
	this.facToCrn = facCrn;
	Set<String> verticies = g.vertexSet();
	cm = new HashMap<>();
	pq = new PriorityQueue<>();
	scheduled = new HashMap<>();
	for (String s : verticies) {
	    if (s == "20170-21184" || s == "21184")
		System.out.println("here");
	    scheduled.put(s, false);
	    CourseVertex v = new CourseVertex(s, g, enroll.get(s));
	    cm.put(s, v);

	}
    }

    public Collection<CourseVertex> courseVertices() {
	return cm.values();
    }

    public Collection<Student> students() {
	return sm.values();
    }

    public StudentGraph<String, StudentEdge> getGraph() {
	return swg;
    }

    public HashMap<String, CourseVertex> getCourseMap() {
	return cm;
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

    /**
     * Gets the back to back blocks -- probably belongs in a different place
     * 
     * @return
     */
    public HashMap<Integer, ArrayList<Integer>> getB2BInput() {
	HashMap<Integer, ArrayList<Integer>> b2b = new HashMap<>();
	Scanner scanner = new Scanner(System.in);
	System.out.println("Final Exam Scheduler - 2013");
	System.out.println("Before we get started, we first need to know which slots are considered back to back.");
	String input = "no";
	while (!input.equals("yes")) {
	    System.out.println("Please type in first back to back block");
	    int block1 = scanner.nextInt() - 1; //-1 just reindexes
	    System.out.println("Please type in second back to back block");
	    int block2 = scanner.nextInt() - 1;

	    if (b2b.containsKey(block1))
		b2b.get(block1).add(block2);
	    else {
		ArrayList<Integer> conflicts = new ArrayList<>();
		conflicts.add(block2);
		b2b.put(block1, conflicts);
	    }

	    if (b2b.containsKey(block2))
		b2b.get(block1).add(block1);
	    else {
		ArrayList<Integer> conflicts = new ArrayList<>();
		conflicts.add(block1);
		b2b.put(block2, conflicts);
	    }

	    System.out.println("Are you done entering back to back blocks? (yes/no)");

	    //makes sure they type something legit
	    while (true) {
		input = scanner.next();
		if ((input.equals("yes") || input.equals("no")))
		    break;
		System.out.println("\nPlease type either \"yes\" or \"no\"");
	    }

	}
	scanner.close();

	return b2b;
    }

    public boolean Schedule(int days, int blocksPerDay, HashMap<Integer, ArrayList<Integer>> b2b, boolean useB2B) {
	this.days = days;
	this.blocksPerDay = blocksPerDay;
	for (CourseVertex cv : cm.values()) {
	    cv.getDayBlockInfo(days, blocksPerDay);
	}
	pq.clear();
	pq.addAll(cm.values());
	while (!pq.isEmpty()) {
	    CourseVertex current = pq.remove();
	    String CRN = current.name();
	    if (!scheduled.get(CRN)) { //course has been scheduled 
		if (!scheduleCourse(current, b2b, useB2B))
		    return false;
	    }
	}
	printSchedule();
	return true;

    }

    private boolean scheduleCourse(CourseVertex cv, HashMap<Integer, ArrayList<Integer>> b2b, boolean useB2B) {
	ArrayList<Pair> acceptableSlots;
	acceptableSlots = acceptableSlots(cv);
	int numAccSlots = acceptableSlots.size();

	if (numAccSlots == 0) {
	    System.out.println("Couldn't find a favorable slot");
	    return false;
	} else {
	    Random randomSlotGen = new Random();
	    int slot = randomSlotGen.nextInt(numAccSlots);
	    Pair dayblock = acceptableSlots.get(slot); //getting first slot for debugging purposes
	    int foundDay = dayblock.getFirst();
	    int foundBlock = dayblock.getSecond();
	    cv.setTime(foundBlock, foundDay);
	    scheduled.put(cv.name(), true);

	    //update based on instructor constraint - instructor cannot administer more than two exams in same block
	    String facName = crnToFac.get(cv.name()); /*
						       * AT LEAST THIS HASH
						       * TABLE SHOULD NOT EXIST
						       * (information could just
						       * be in the CourseVertex)
						       */
	    ArrayList<String> facDependents = facToCrn.get(facName);
	    for (String CRN : facDependents) {
		CourseVertex course = cm.get(CRN);
		if (!scheduled.get(CRN))
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
		    check3InADay(course, edge, foundDay, foundBlock);
		    if (course.isAvailable(foundDay) && useB2B)
			checkb2b(course, edge, foundDay, foundBlock, b2b);
		    course.updateAvailability();
		    //this is not neccesary -- the course has never been removed
		    //pq.add(course); //add back course to priority queue

		}
	    }
	    return true;
	}
    }

    //just seemed more elegant than commenting the code out
    private static final boolean LARGE_CONSTRAINT = false;

    /**
     * a function which returns acceptable slots for a given course vertex to be
     * scheduled in
     * 
     * @param cv
     * @return
     */
    //I separated this because it could be useful for finding places to add and remove courses
    public ArrayList<Pair> acceptableSlots(CourseVertex cv) {
	int[][] degreeOfConflicts = cv.degreeOfConflict(); //gives the chart of acceptable and favorable exam times  
	int dayIt, blockIt; //variables to iterate through days and blocks 
	dayIt = 0;
	blockIt = 0;
	ArrayList<Pair> acceptableTimes = new ArrayList<Pair>();
	while (dayIt < days) {
	    blockIt = 0;
	    while (blockIt < blocksPerDay) {
		//found an acceptable slot && a favorable slot 
		if (degreeOfConflicts[dayIt][blockIt] != CourseVertex.THREE_IN_DAY
			&& degreeOfConflicts[dayIt][blockIt] < 1) {

		    Pair acceptableTime = new Pair(dayIt, blockIt);
		    acceptableTimes.add(acceptableTime);
		    if (LARGE_CONSTRAINT) { //if large constraint is enabled
			if (cv.getEnrollment() >= LARGE) {
			    if (dayIt == 0 || dayIt == 1) //-------removing large constraint for now
				acceptableTimes.add(acceptableTime);
			} else
			    acceptableTimes.add(acceptableTime);
		    }
		    //keeps track of what has been scheduled since we will have copies of 
		} //same course in priority queue
		blockIt++;
	    }
	    dayIt++;
	}
	return acceptableTimes;
    }

    private void check3InADay(CourseVertex cv, StudentEdge e, int day, int block) {
	Iterator<String> studItr = e.getStudents();
	while (studItr.hasNext()) {
	    String studID = studItr.next();
	    Student stud = sm.get(studID);
	    stud.occupy(day, block);
	    if (stud.gtNExamsInDay(MAXEXAMSPERDAY, day)) { //if student in class has MAXEXAMS already on day i
		cv.removeDay(day); //then day becomes unavailable for this exam
		//no other slots can become unavailable - cannot break here, need all students to have block occupied
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
		ArrayList<Integer> b2bBlocks = b2b.get(block);
		int[][] degreeOfConflict = cv.degreeOfConflict();
		for (int b2bBlock : b2bBlocks) {
		    if (degreeOfConflict[day][b2bBlock] != CourseVertex.THREE_IN_DAY)
			cv.addB2BConflict(day, b2bBlock);
		}
	    }
	}
    }

    public void printLargeExams(int degree) {
	Set<String> vertices = swg.vertexSet();
	for (String s : vertices) {
	    if (cm.get(s).getDegree() > degree)
		System.out.println(s);
	}
    }

    public boolean isEdgeBetween(CourseVertex cv1, CourseVertex cv2) {
	return swg.containsEdge(cv1.name(), cv2.name());
    }
}
