package examScheduling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

public class Scheduler {

    public static final int DAYS = 4, BLOCKSPERDAY = 4, MAXEXAMSPERDAY = 2, BACKTOBACKLIMIT = 1;
    public static final boolean AVAILABLE = false, TAKEN = true;

    StudentGraph<String, StudentEdge> swg;
    PriorityQueue<CourseVertex> pq;
    HashMap<String, CourseVertex> cm;
    HashMap<String, Student> sm;

    public Scheduler(StudentGraph<String, StudentEdge> g, HashMap<String, Student> sm) {
	this.swg = g;
	this.sm = sm;
	Set<String> verticies = g.vertexSet();
	pq = new PriorityQueue<>();
	cm = new HashMap<>();
	for (String s : verticies) {
	    CourseVertex v = new CourseVertex(s, g);
	    pq.add(v);
	    cm.put(s, v);
	}
    }

    public Collection<CourseVertex> courseVertices() {
	return cm.values();
    }

    public void Schedule() {
	while (!pq.isEmpty()) {
	    CourseVertex current = pq.remove();
	    if (!attemptToSchedule(current)) {
		System.out.println("OOPS");
		//backtrack!!!
	    }

	}
	printSchedule();
    }

    public void printSchedule() {
	for (int i = 0; i < DAYS; i++) {
	    for (int j = 0; j < BLOCKSPERDAY; j++) {
		System.out.print(countAtTime(i, j) + "\t");
	    }
	    System.out.println();
	}
    }

    public int countAtTime(int day, int block) {
	int count = 0;
	for (CourseVertex v : cm.values()) {
	    if (v.block() == block && v.day() == day)
		count++;
	}
	return count;
    }

    private boolean attemptToSchedule(CourseVertex cv) {
	Set<StudentEdge> adj = swg.edgesOf(cv.name());
	boolean[][] timeSlots = getPossibleTimes(adj, cv.name());
	for (int day = 0; day < DAYS; day++)
	    for (int block = 0; block < BLOCKSPERDAY; block++) {
		if (timeSlots[day][block] == AVAILABLE) {
		    cv.setTime(block, day); //set course time
		    for (StudentEdge e : adj) { //add this occupancy to all students
			Iterator<String> studentItr = swg.getStudents(e);
			while (studentItr.hasNext()) {
			    String studentName = studentItr.next();
			    Student student = sm.get(studentName);
			    student.occupy(day, block);
			}
		    }
		    return true;
		}
	    }
	return false;
    }

    private boolean[][] getPossibleTimes(Set<StudentEdge> adj, String name) {
	boolean[][] times = new boolean[DAYS][BLOCKSPERDAY];

	for (StudentEdge e : adj) {
	    String other = null;
	    if ((other = swg.getEdgeTarget(e)).equals(name)) {
		other = swg.getEdgeSource(e);
	    } //set other to refer to the other course
	    Iterator<String> studentItr = swg.getStudents(e);
	    while (studentItr.hasNext()) {
		String studentName = studentItr.next();
		Student student = sm.get(studentName);
		for (int i = 0; i < DAYS; i++) {
		    if (student.gtNExamsInDay(MAXEXAMSPERDAY, i)) {
			for (int j = 0; j < BLOCKSPERDAY; j++) {
			    times[i][j] = TAKEN;
			}
		    }
		}
	    }

	    CourseVertex othr = cm.get(other);
	    if (othr.day() != -1 && othr.block() != -1) {
		if (e.getWeight() > BACKTOBACKLIMIT) {
		    if (othr.block() == 1 || othr.block() == 3) {
			times[othr.day()][2] = TAKEN;
		    } else if (othr.block() == 2) {
			times[othr.day()][1] = TAKEN;
			times[othr.day()][3] = TAKEN;

		    }

		}
		times[othr.day()][othr.block()] = TAKEN;
	    }
	}
	return times;
    }
}