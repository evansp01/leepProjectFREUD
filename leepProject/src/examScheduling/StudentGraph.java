package examScheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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
    
    public ArrayList<V> getDependencies(V course) { //accessor method to get all the linked courses relative to one course
    	V course2;
    	Set<E> edges = edgesOf(course); 
    	ArrayList<V> dependents = new ArrayList<>();
    	for (E edge : edges) {  
    		course2=getEdgeSource(edge);
    		if(course2.equals(course)) 
    			course2=getEdgeTarget(edge); 
    		dependents.add(course2); 
    	}
    	
		return dependents; 
    	
    }

}
