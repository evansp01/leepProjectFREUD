package examScheduling;

/**
 * holds addition information for courses including crn, degree, weighted
 * degree, and final block and time
 * 
 * @author evan
 * 
 */

public class CourseVertex implements Comparable<CourseVertex> {
    private String name;
    private int wdegree;
    private int degree;
    private int block;
    private int day;

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

    @Override
    public int compareTo(CourseVertex o) {
	if (o == null)
	    return 1;
	if (o.degree == degree) {
	    if (o.wdegree == wdegree) {
		return name.compareTo(o.name);
	    }
	    return -(wdegree - o.wdegree);
	}
	return -(degree - o.degree);
    }
}
