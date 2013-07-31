package examScheduling;

import java.util.Collection;
import java.util.Iterator;

import org.jgrapht.graph.SimpleWeightedGraph;

public class StudentGraph<V, E extends StudentEdge> extends SimpleWeightedGraph<V, E> {

    private static final long serialVersionUID = 8624829045211494116L;

    public E addEdge(V s, V t, String student) {
	E ret = addEdge(s, t);
	ret.addStudent(student);
	return ret;
    }

    public StudentGraph(Class<E> edgeClass) {
	super(edgeClass);
    }

    public void addStudent(E edge, String student) {
	edge.addStudent(student);
	setEdgeWeight(edge, edge.numberOfStudents());
    }

    public void addStudents(E edge, Collection<String> students) {
	edge.addStudents(students);
	setEdgeWeight(edge, edge.numberOfStudents());
    }

    public Iterator<String> getStudents(E edge) {
	return edge.getStudents();
    }

}
