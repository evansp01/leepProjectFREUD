package czexamSchedulingFinal;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

import consoleThings.CurrentProject;
import consoleThings.Settings;

/**
 * Attempts to generate a schedule matching the settings given in the input
 * project settings file from the graph in the input graph creation
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */

public class Scheduler {

    public static final int FAILURE = -1;

    private DependenciesGraph<String, DependentEdge> swg;

    private HashMap<String, CourseVertex> cm;
    private HashMap<String, Student> sm;
    private PriorityQueue<CourseVertex> pq;
    private ArrayList<PreScheduledExam> as;

    private int tolerance;
    private int largeCourseValue;
    private int days, blocks;
    private boolean largeCourse;
    private boolean[][] backToBack;
    private boolean fresh;

    /**
     * Creates a scheduler object based on the graph and values from graph
     * creation
     * 
     * @param gc
     *            the graph
     * @param project
     *            the project which provides the settings fileF
     */
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
	tolerance = project.settings.tolerance;
	largeCourseValue = project.settings.largeCourse;
	fresh = true;
	updateAlreadyScheduled();
	//at this point call the scheduling thing
    }

    /**
     * updates all courses which have already been scheduled
     * 
     * @return
     */
    private boolean updateAlreadyScheduled() {
	for (PreScheduledExam pe : as) {
	    if (!attemptToSchedule(cm.get(pe.name), pe.day, pe.block))
		return false;
	}
	return true;
    }

    /**
     * attempts to schedule a certain course for a certain time
     * 
     * @param cv
     *            the course to schedule
     * @param day
     *            the day to schedule on
     * @param block
     *            the block to schedule on
     * @return success or failure
     */
    public boolean attemptToSchedule(CourseVertex cv, int day, int block) {
	if (cv.degreeOfConflict()[day][block] != -1) {
	    scheduleCourse(cv, new Pair(day, block));
	} else
	    return false;
	return true;

    }

    /**
     * gets a hashmap which maps crn numbers to course vertices
     * 
     * @return
     */
    public HashMap<String, CourseVertex> getCourseMap() {
	return cm;
    }

    /**
     * private method to clear all students and course vertices in preparation
     * for rescheduling
     */
    private void clear() {
	for (CourseVertex cv : cm.values())
	    cv.clear(tolerance);
	for (Student s : sm.values())
	    s.clear();
	updateAlreadyScheduled();
	fresh = true;

    }

    /**
     * attempt to schedule with a custom number of retries
     * 
     * @param limit
     *            number of retries
     * @return result the number of times it took to find a successful schedule
     *         (result<0 indicates failure)
     */
    public int schedule(int limit) {
	boolean result = false;
	int i = 0;
	for (i = 0; i < limit && !result; i++) {
	    result = trySchedule();
	    if (!result)
		clear();
	}
	if (result)
	    return i;
	else
	    return FAILURE;

    }

    /**
     * Attempts to find a successful schedule
     * 
     * @return boolean whether or not the scheduling was successful
     */
    private boolean trySchedule() {
	if (!fresh)
	    clear();
	fresh = false;

	pq.addAll(cm.values());
	while (!pq.isEmpty()) {
	    CourseVertex current = pq.remove();
	    if (!current.isScheduled()) { //course has been scheduled 
		if (!scheduleCourse(current)) {
		    return false;
		}
	    }
	}
	return true;

    }

    /**
     * Attempts to schedule a course by randomly picking an available time and
     * assigning the course to that time
     * 
     * @param cv
     * @return
     */
    private boolean scheduleCourse(CourseVertex cv) {
	ArrayList<Pair> acceptableSlots;
	//get available slots
	acceptableSlots = findAvailableSlots(cv, tolerance, largeCourse);

	if (acceptableSlots.isEmpty()) {
	    return false;
	} else {
	    //randomly pick a slot
	    Random randomSlotGen = new Random();
	    int slot = randomSlotGen.nextInt(acceptableSlots.size());
	    Pair dayblock = acceptableSlots.get(slot);
	    //schedule the course for that time
	    scheduleCourse(cv, dayblock);
	    return true;
	}
    }

    /**
     * Schedules a course for a particular time and updates other courses and
     * students
     * 
     * @param cv
     *            the course to schedule
     * @param time
     *            the pair holding the day and block
     */
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
		course.updateAvailability(tolerance);

		pq.add(course); //add back course to priority queue
	    }
	}
    }

    /**
     * Updates the availability of students and courses after scheduling a
     * particular course
     * 
     * @param cv
     *            the scheduled course
     * @param e
     *            the edge currently being updated
     * @param day
     *            final day
     * @param block
     *            final block
     */
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
    public ArrayList<Pair> findAvailableSlots(CourseVertex cv, int backToBack, boolean largeCourse) {
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
		if (conflict != CourseVertex.THREE_IN_DAY && conflict < backToBack) {
		    if (largeCourse && cv.getEnrollment() >= largeCourseValue) { //if large constraint is enabled
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
