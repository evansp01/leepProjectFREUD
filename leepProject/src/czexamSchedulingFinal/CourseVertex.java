package czexamSchedulingFinal;

import java.util.Arrays;

import consoleThings.Settings;

/**
 * holds addition information for courses including crn, degree, weighted
 * degree, and final block and time
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */

public class CourseVertex implements Comparable<CourseVertex> {

    private int block; //block time for exam
    private int day; //day of exam
    private String name; //typically CourseCRN
    private int wdegree; //weighted degree
    private int degree; //unweighted degree
    private int[][] degreeOfConflict;
    private int acceptableSlots;
    private int favorableSlots;
    private int enrollment;

    public static int THREE_IN_DAY = -1;

    /**
     * 
     * @param name
     *            crn associated with course vertex
     * @param g
     *            graph to create vertex from
     */
    public CourseVertex(String name, int enrollment, int days, int blocks) {
	this.name = name;
	this.enrollment = enrollment;
	day = -1;
	block = -1;

	this.degreeOfConflict = new int[days][blocks];
	//not sure if I need all of these
	acceptableSlots = days * blocks;
	favorableSlots = days * blocks;
    }

    public void clear() {
	for (int[] d : degreeOfConflict)
	    Arrays.fill(d, 0);
	day = -1;
	block = -1;
	updateAvailability();
    }

    public void setDegrees(DependenciesGraph<String, DependentEdge> g) {
	wdegree = g.degreeOf(name);
	degree = g.edgesOf(name).size();
    }

    /**
     * sets the final block and day of this course
     * 
     * @param block
     *            final block
     * @param day
     *            final day
     */

    public void setTime(int day, int block) {
	this.day = day;
	this.block = block;

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

    public int getEnrollment() {
	return enrollment;
    }

    public boolean isScheduled() {
	return !(day == -1 && block == -1);
    }

    public static final int GREATER = 1, LESS = -1;

    @Override
    public int compareTo(CourseVertex o) {
	if (o == null)
	    return GREATER;
	if (acceptableSlots != o.acceptableSlots)
	    return acceptableSlots - o.acceptableSlots; //then acceptable
	if (favorableSlots != o.favorableSlots) //first check favorable slots
	    return favorableSlots - o.favorableSlots;
	if (degree != o.degree)
	    return -(degree - o.degree); //then degree
	if (wdegree != o.wdegree)
	    return -(wdegree - o.wdegree); //then weighted degree

	return LESS;
    }

    //things I am unsure about

    public void removeSlot(int day, int block) {
	degreeOfConflict[day][block] = THREE_IN_DAY;
    }

    public void removeDay(int day) {
	//set each block to -1 to denote the total unacceptability of the day
	for (int block = 0; block < degreeOfConflict.length; block++) {
	    degreeOfConflict[day][block] = THREE_IN_DAY;
	}
    }

    public boolean isAvailable(int day) {
	for (int block : degreeOfConflict[day]) {//returns true if at least one slot is acceptable 
	    if (block != THREE_IN_DAY) //day is unacceptable if all of its slots are -1
		return true;
	}
	return false;
    }

    public int[][] degreeOfConflict() {
	return degreeOfConflict;
    }

    public void addBlockB2BConflict(int day, int block) {
	//add unless the block is already impossible
	if (degreeOfConflict[day][block] != THREE_IN_DAY)
	    degreeOfConflict[day][block]++;
    }

    //needs freaking to be generalized
    public void updateAvailability() {
	acceptableSlots = 0;
	favorableSlots = 0;
	for (int day = 0; day < degreeOfConflict.length; day++) {
	    for (int block = 0; block < degreeOfConflict[0].length; block++) {
		if (degreeOfConflict[day][block] != THREE_IN_DAY) { //-1 means totally unacceptable - meaning three exams in a row for some student 
		    acceptableSlots++;
		    if (degreeOfConflict[day][block] < Settings.MAX_BACK_TO_BACK) //as long as no more than 2 people have b2b exams, consider it favorable 
			favorableSlots++;
		}
	    }
	}
    }

}
