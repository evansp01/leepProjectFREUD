package examScheduling;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

public class StudentEdge extends DefaultWeightedEdge {

    private Set<String> students;

    public StudentEdge() {
	super();
	students = new HashSet<String>();

    }

    protected double getWeight() {
	return super.getWeight();

    }

    protected void addStudent(String student) {
	this.students.add(student);
    }

    protected void addStudents(Collection<String> students) {
	this.students.addAll(students);
    }

    protected boolean containsStudent(String student) {
	return students.contains(student);
    }

    protected int numberOfStudents() {
	return students.size();
    }

    protected Iterator<String> getStudents() {
	return students.iterator();
    }

    private static final long serialVersionUID = 2460690315192485866L;

}
