package examScheduling;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class CourseVertex implements Comparable<CourseVertex> {
    String name;
    int wdegree;
    int degree;
    int block;
    int day;

    public CourseVertex(String name, SimpleWeightedGraph<String, DefaultWeightedEdge> g) {
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

    public int getDay() {
	return day;
    }

    public int getBlock() {
	return block;
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
