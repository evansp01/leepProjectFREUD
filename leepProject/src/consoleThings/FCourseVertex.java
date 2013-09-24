package consoleThings;

/**
 * holds addition information for courses including crn, degree, weighted
 * degree, and final block and time
 * 
 * @author evan
 * 
 */

public class FCourseVertex implements Comparable<FCourseVertex> {
    private String name; //typically CourseCRN
    private int wdegree; //weighted degree
    private int degree; //unweighted degree
    private int block; //block time for exam
    private int day; //day of exam
    private int numDays, numBlocks;
    private int[][] degreeOfConflict;
    private int acceptableSlots;
    private int favorableSlots;
    private int enrollment;

    private static int B2B1 = 2, B2B2 = 3, B2B3 = 4;
    public static int THREE_IN_DAY = -1;

    /**
     * 
     * @param name
     *            crn associated with course vertex
     * @param g
     *            graph to create vertex from
     */
    public FCourseVertex(String name, FStudentGraph<String, FStudentEdge> g, int enrollment) {
	this.name = name;
	this.enrollment = enrollment;
	wdegree = g.degreeOf(name);
	degree = g.edgesOf(name).size();
	block = -1;
	day = -1;

	//the following info is changed in getDayBlockInfo function, it depends on the number of days and blocks 
	numDays = 0;
	numBlocks = 0;
	degreeOfConflict = null;
	acceptableSlots = 0;
	favorableSlots = 0;
    }

    public void getDayBlockInfo(int days, int blocks) {
	numDays = days;
	numBlocks = blocks;
	degreeOfConflict = new int[days][blocks];
	acceptableSlots = days * blocks;
	favorableSlots = days * blocks;
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

    public void setEnrollment(int enroll) {
	enrollment = enroll;
    }

    public static final int GREATER = 1, LESS = -1;

    @Override
    public int compareTo(FCourseVertex o) {
	if (o == null)
	    return GREATER;
	if (favorableSlots != o.favorableSlots) //first check favorable slots
	    return favorableSlots - o.favorableSlots;
	else if (acceptableSlots != o.acceptableSlots)
	    return acceptableSlots - o.acceptableSlots; //then acceptable
	else if (degree != o.degree)
	    return -(degree - o.degree); //then degree
	else if (wdegree != o.wdegree)
	    return -(wdegree - o.wdegree); //then weighted degree
	else
	    return LESS;
    }

    public void removeSlot(int day, int block) {
	degreeOfConflict[day][block] = THREE_IN_DAY;
    }

    public void removeDay(int day) {
	//set each block to -1 to denote the total unacceptability of the day
	for (int block = 0; block < numBlocks; block++) {
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

    public boolean isScheduled() {
	return !(day == -1 && block == -1);
    }

    public int[][] degreeOfConflict() {
	return degreeOfConflict;
    }

    public void addB2BConflict(int day, int block) {
	degreeOfConflict[day][block]++;
    }

    public void updateAvailability() {
	acceptableSlots = 0;
	favorableSlots = 0;
	for (int day = 0; day < numDays; day++) {
	    for (int block = 0; block < numBlocks; block++) {
		if (degreeOfConflict[day][block] != -1) { //-1 means totally unacceptable - meaning three exams in a row for some student 
		    acceptableSlots++;
		    if (block == B2B1 || block == B2B2 || block == B2B3) {
			if (degreeOfConflict[day][block] < 1) //as long as no more than 2 people have b2b exams, consider it favorable 
			    favorableSlots++;

		    } else
			favorableSlots++; //if not a back to back slot, just increase the number of favorable slots
		}
	    }
	}
    }

}
