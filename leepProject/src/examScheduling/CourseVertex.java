package examScheduling;

public class CourseVertex implements Comparable<CourseVertex> {
    private String name;
    private int wdegree;
    private int degree;
    private int block;
    private int day;

    public CourseVertex(String name, StudentGraph<String, StudentEdge> g) {
	this.name = name;
	wdegree = g.degreeOf(name);
	degree = g.edgesOf(name).size();
	block = -1;
	day = -1;
    }

    public void setTime(int block, int day) {
	this.block = block;
	this.day = day;
    }

    public int day() {
	return day;
    }

    public int block() {
	return block;
    }

    public String name() {
	return name;
    }

    public int getDegree() {
	return degree;
    }

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
