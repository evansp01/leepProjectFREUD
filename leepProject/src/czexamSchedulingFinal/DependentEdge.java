package czexamSchedulingFinal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * 
 * @author evan
 * 
 */
public class DependentEdge extends DefaultWeightedEdge {

    private Set<String> students;

    /**
     * Constructor for StudentEdge
     */
    public DependentEdge() {
	super();
	students = new HashSet<String>();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapht.graph.DefaultWeightedEdge#getWeight()
     */
    protected double getWeight() {
	return super.getWeight();

    }

    /**
     * 
     * @param student
     */
    protected void addStudent(String student) {
	this.students.add(student);
    }

    protected boolean hasStudents() {
	return !students.isEmpty();
    }

    /**
     * 
     * @param students
     */
    protected void addStudents(Collection<String> students) {
	this.students.addAll(students);
    }

    /**
     * 
     * @param student
     * @return
     */
    protected boolean containsStudent(String student) {
	return students.contains(student);
    }

    /**
     * 
     * @return
     */
    protected int numberOfStudents() {
	return students.size();
    }

    /**
     * 
     * @return
     */
    protected Iterator<String> getStudents() {
	return students.iterator();
    }

    private static final long serialVersionUID = 2460690315192485866L;

}
