package examScheduling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

//tree set

public class Scheduler {

    public static final int MAXEXAMSPERDAY = 2;
    public static final boolean AVAILABLE = false, TAKEN = true, BACKTOBACK = true;

    StudentGraph<String, StudentEdge> swg;
    PriorityQueue<CourseVertex> pq;
    HashMap<String, CourseVertex> cm;
    HashMap<String, Student> sm;
    private int days = 0, blocksPerDay = 0;
    private boolean[][] backToBack = null;

    public Scheduler(StudentGraph<String, StudentEdge> g, HashMap<String, Student> sm) {
	this.swg = g;
	this.sm = sm;
	Set<String> verticies = g.vertexSet();
	cm = new HashMap<>();
	pq = new PriorityQueue<>();
	for (String s : verticies) {
	    CourseVertex v = new CourseVertex(s, g);
	    cm.put(s, v);
	}
    }

    private int getAverageDegree() {
	int sum = 0;
	int count = 0;
	for (CourseVertex c : cm.values()) {
	    sum += c.getDegree();
	    count++;
	}
	if (count > 0) {
	    return sum / count + 1;
	}
	return 1;

    }

    public Collection<CourseVertex> courseVertices() {
	return cm.values();
    }

    public boolean[][] getPossibleTimes(String courseName, int b2blimit) {
	Set<StudentEdge> adj = swg.edgesOf(courseName);
	return getPossibleTimes(adj, courseName, b2blimit);
    }

    //shadow implementation
    //this method should return some format of the 
    //problems with the current schedule
    //probably doesn't belong here actually
    public Object reportProblems() {

	return null;
    }

    public void Schedule() {
	int[][] backToBack = { { 2, 3 }, { 3, 4 } };
	ScheduleA(4, 4, backToBack);

    }

    public void ScheduleA(int days, int blocksPerDay, int[][] backToBack) {
	this.days = days;
	this.blocksPerDay = blocksPerDay;
	this.backToBack = new boolean[blocksPerDay][blocksPerDay];
	for (int[] pair : backToBack) {
	    this.backToBack[pair[0] - 1][pair[1] - 1] = BACKTOBACK;
	    this.backToBack[pair[1] - 1][pair[0] - 1] = BACKTOBACK;
	}
	boolean success = true;
	rebuildPQ();
	PriorityQueue<CourseVertex> pq2 = new PriorityQueue<>();
	while (!pq.isEmpty()) {
	    CourseVertex current = pq.remove();
	    if (!attemptToSchedule(current, Integer.MAX_VALUE, 0)) {
		pq2.add(current);
		success = false;
	    }
	}
	printSchedule();
	success = true;
	System.out.println(pq2.size());
	while (!pq2.isEmpty()) {
	    CourseVertex current = pq2.remove();
	    if (!attemptToSchedule(current, Integer.MAX_VALUE, Integer.MAX_VALUE)) {
		success = false;
		System.out.println("breaking");
		break;
	    }
	}
	if (success == false) {
	    System.out.println("sadness " + pq2.size());
	}

	printSchedule();
    }

    public void Schedule(int days, int blocksPerDay, int[][] backToBack) {
	this.days = days;
	this.blocksPerDay = blocksPerDay;
	this.backToBack = new boolean[blocksPerDay][blocksPerDay];
	for (int[] pair : backToBack) {
	    this.backToBack[pair[0] - 1][pair[1] - 1] = BACKTOBACK;
	    this.backToBack[pair[1] - 1][pair[0] - 1] = BACKTOBACK;
	}
	boolean success;
	int startAverage = getAverageDegree();
	int back = 0;
	int[] averageTry = { startAverage, (int) (startAverage * 0.5), (int) (startAverage * 1.5), Integer.MAX_VALUE };
	int currentTry = 0;
	int average = startAverage;
	do {
	    rebuildPQ();
	    success = true;
	    while (!pq.isEmpty()) {
		CourseVertex current = pq.remove();
		if (!attemptToSchedule(current, average, back)) {
		    success = false;
		    //		    System.out.println(average + " " + back + " failed");
		    break;
		}

	    }
	    if (++currentTry >= averageTry.length) {
		currentTry = 0;
		back++;
	    }
	    average = averageTry[currentTry];

	} while (!success);

	printSchedule();
    }

    private void rebuildPQ() {
	pq.clear();
	pq.addAll(cm.values());
	for (Student s : sm.values()) {
	    s.resetOccupancy();
	}

    }

    public void printSchedule() {
	for (int i = 0; i < days; i++) {
	    for (int j = 0; j < blocksPerDay; j++) {
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

    private boolean attemptToSchedule(CourseVertex cv, int limitThree, int bblimit) {
	Set<StudentEdge> adj = swg.edgesOf(cv.name());
	boolean[][] timeSlots = getPossibleTimes(adj, cv.name(), bblimit);
	for (int day = 0; day < days; day++)
	    for (int block = 0; block < blocksPerDay; block++) {
		if (timeSlots[day][block] == AVAILABLE) {
		    if (block != 2 || adj.size() < limitThree) {
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
	    }
	return false;
    }

    private boolean[][] getPossibleTimes(Set<StudentEdge> adj, String name, int bblimit) {
	boolean[][] times = new boolean[days][blocksPerDay];

	for (StudentEdge e : adj) {
	    String other = null;
	    if ((other = swg.getEdgeTarget(e)).equals(name)) {
		other = swg.getEdgeSource(e);
	    } //set other to refer to the other course
	    Iterator<String> studentItr = swg.getStudents(e);
	    while (studentItr.hasNext()) {
		String studentName = studentItr.next();
		Student student = sm.get(studentName);
		for (int i = 0; i < days; i++) {
		    if (student.gtNExamsInDay(MAXEXAMSPERDAY, i)) {
			System.out.println("unfortunate " + i + "  " + name);
			for (int j = 0; j < blocksPerDay; j++) {
			    times[i][j] = TAKEN;
			}
		    }
		}
	    }

	    CourseVertex othr = cm.get(other);
	    if (othr.day() != -1 && othr.block() != -1) {
		if (e.getWeight() > bblimit) {
		    for (int backToBackBlock : backToBackBlocks(othr.block())) {
			times[othr.day()][backToBackBlock] = TAKEN;
		    }
		}
		times[othr.day()][othr.block()] = TAKEN;
	    }
	}
	return times;
    }

    private Collection<Integer> backToBackBlocks(int blockIn) {
	LinkedList<Integer> backToBackList = new LinkedList<>();
	for (int block = 0; block < blocksPerDay; block++) {
	    if (backToBack[blockIn][block])
		backToBackList.add(block);

	}
	return backToBackList;
    }
}