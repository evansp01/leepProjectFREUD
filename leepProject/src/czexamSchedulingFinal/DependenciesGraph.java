package czexamSchedulingFinal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * An extension of the SimpleWeightedGraph from the jgrapht library which is
 * used to represent the course and student dependencies
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class DependenciesGraph<V, E extends DependentEdge> extends SimpleWeightedGraph<V, E> {

    private static final long serialVersionUID = 8624829045211494116L;

    /**
     * adds a new edge to the dependencies graph
     * 
     * @param s
     *            start vertex
     * @param t
     *            end vertex
     * @param student
     *            a student in the edge
     * @param isStudent
     *            is the edge a student
     * @return the edge
     */
    public E addEdge(V s, V t, String student, boolean isStudent) {
	E ret = addEdge(s, t);
	if (isStudent)
	    ret.addStudent(student);

	return ret;
    }

    /**
     * constructs a dependencies graph
     * 
     * @param edgeClass
     */
    public DependenciesGraph(Class<E> edgeClass) {
	super(edgeClass);
    }

    /**
     * adds a student to the edge
     * 
     * @param edge
     * @param student
     */
    public void addEdgeMember(E edge, String student) {
	edge.addStudent(student);
	setEdgeWeight(edge, edge.numberOfStudents());
    }

    /**
     * Adds a group of students to the edge
     * 
     * @param edge
     * @param students
     */
    public void addStudents(E edge, Collection<String> students) {
	edge.addStudents(students);
	setEdgeWeight(edge, edge.numberOfStudents());
    }

    /**
     * returns an iterator over the students in this edge
     * 
     * @param edge
     * @return
     */
    public Iterator<String> getStudents(E edge) {
	return edge.getStudents();
    }

    /**
     * returns true if the edge contains at least one student
     * 
     * @param edge
     * @return
     */
    public boolean edgeIsStudent(E edge) {
	return edge.hasStudents();
    }

    /**
     * returns all courseVertices connected to this courseVertex
     * 
     * @param course
     * @return
     */
    public ArrayList<V> getDependencies(V course) { //accessor method to get all the linked courses relative to one course
	V course2;
	Set<E> edges = edgesOf(course);
	ArrayList<V> dependents = new ArrayList<>();
	for (E edge : edges) {
	    course2 = getEdgeSource(edge);
	    if (course2.equals(course))
		course2 = getEdgeTarget(edge);
	    dependents.add(course2);
	}

	return dependents;

    }

}
