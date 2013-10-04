package czexamSchedulingFinal;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

import consoleThings.CurrentProject;
import consoleThings.Settings;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
//tree set

///Don't need to remove schedued from course vertex no longer in use because of pq -- also can remove all those hash tables

public class Scheduler {

    //these things are legit
    private DependenciesGraph<String, DependentEdge> swg;

    private HashMap<String, CourseVertex> cm;
    private HashMap<String, Student> sm;
    private PriorityQueue<CourseVertex> pq;
    private ArrayList<PreScheduledExam> as;

    //these things are not legit

    //this is legit
    private int days, blocks;
    private boolean largeCourse;
    private boolean[][] backToBack;
    private boolean fresh;

    public Scheduler(GraphCreation gc, CurrentProject project) {
	swg = gc.getGraph();
	sm = gc.getStudentMap();
	cm = gc.getCourseMap();
	pq = new PriorityQueue<>();
	as = gc.getAlreadyScheduled();
	days = project.settings.days;
	blocks = project.settings.blocks;
	largeCourse = project.settings.largeConstraint;
	backToBack = project.settings.backToBack;
	fresh = true;
	//at this point call the scheduling thing
    }

    private boolean updateAlreadyScheduled() {
	for (PreScheduledExam pe : as) {
	    if (!attemptToSchedule(cm.get(pe.name), pe.day, pe.block))
		return false;
	}
	return true;
    }

    public boolean attemptToSchedule(CourseVertex cv, int day, int block) {
	if (cv.degreeOfConflict()[day][block] != -1) {
	    scheduleCourse(cv, new Pair(day, block));
	} else
	    return false;
	return true;

    }

    public HashMap<String, CourseVertex> getCourseMap() {
	return cm;
    }

    private void clear() {
	for (CourseVertex cv : cm.values())
	    cv.clear();
	for (Student s : sm.values())
	    s.clear();
	fresh = true;

    }

    public boolean schedule() {
	boolean result = false;
	int i = 0;
	for (i = 0; i < 1000 && !result; i++) {
	    result = trySchedule();
	    if (!result)
		clear();
	}
	System.out.println("" + i + " " + result);
	return result;

    }

    private boolean trySchedule() {
	if (!fresh)
	    clear();
	fresh = false;
	updateAlreadyScheduled();
	pq.addAll(cm.values());
	while (!pq.isEmpty()) {
	    CourseVertex current = pq.remove();
	    if (!current.isScheduled()) { //course has been scheduled 
		if (!scheduleCourse(current)) {
		    System.out.println("Scheduling failed");

		    return false;
		}
	    }
	}
	return true;

    }

    private boolean scheduleCourse(CourseVertex cv) {
	ArrayList<Pair> acceptableSlots;
	acceptableSlots = findAvailableSlots(cv);

	if (acceptableSlots.isEmpty()) {
	    return false;
	} else {
	    Random randomSlotGen = new Random();
	    int slot = randomSlotGen.nextInt(acceptableSlots.size());
	    Pair dayblock = acceptableSlots.get(slot); //getting first slot for debugging purposes
	    scheduleCourse(cv, dayblock);
	    return true;
	}
    }

    private void scheduleCourse(CourseVertex cv, Pair time) {
	int foundDay = time.day();
	int foundBlock = time.block();
	cv.setTime(foundDay, foundBlock);
	ArrayList<String> dependents = swg.getDependencies(cv.name());
	for (String CRN : dependents) {
	    CourseVertex course = cm.get(CRN);
	    if (!course.isScheduled()) { //if course has not been scheduled 
		//function to make foundDay and foundBlock unavailable
		course.removeSlot(foundDay, foundBlock);
		DependentEdge edge = swg.getEdge(cv.name(), course.name());

		updateStudentsAndCourses(course, edge, foundDay, foundBlock);
		course.updateAvailability();

		pq.add(course); //add back course to priority queue
	    }
	}
    }

    private void updateStudentsAndCourses(CourseVertex cv, DependentEdge e, int day, int block) {
	Iterator<String> studentItr = e.getStudents();
	boolean dayRemoved = false;
	while (studentItr.hasNext()) {

	    Student student = sm.get(studentItr.next());
	    student.occupy(day, block);
	    if (!dayRemoved) { //if student in class has MAXEXAMS already on day i
		if (student.gtNExamsInDay(Settings.MAX_EXAM_PER_DAY, day)) {
		    cv.removeDay(day);
		    dayRemoved = true;
		} else {
		    for (int otherBlock = 0; otherBlock < blocks; otherBlock++) {
			if (backToBack[block][otherBlock]) {
			    cv.addBlockB2BConflict(day, otherBlock);
			}
		    }
		}
	    }
	}
    }

    /**
     * a function which returns acceptable slots for a given course vertex to be
     * scheduled in
     * 
     * @param cv
     * @return
     */
    public ArrayList<Pair> findAvailableSlots(CourseVertex cv) {
	int[][] degreeOfConflicts = cv.degreeOfConflict(); //gives the chart of acceptable and favorable exam times  
	int dayIt, blockIt; //variables to iterate through days and blocks 
	dayIt = 0;
	blockIt = 0;
	ArrayList<Pair> acceptableTimes = new ArrayList<Pair>();
	while (dayIt < days) {
	    blockIt = 0;
	    while (blockIt < blocks) {
		int conflict = degreeOfConflicts[dayIt][blockIt];
		//found an acceptable slot
		if (conflict != CourseVertex.THREE_IN_DAY && conflict < Settings.MAX_BACK_TO_BACK) {
		    if (largeCourse && cv.getEnrollment() >= Settings.LARGE) { //if large constraint is enabled
			if (dayIt <= days / 2)
			    acceptableTimes.add(new Pair(dayIt, blockIt));
		    } else {
			acceptableTimes.add(new Pair(dayIt, blockIt));
		    }
		}
		blockIt++;
	    }
	    dayIt++;
	}
	return acceptableTimes;
    }
}
