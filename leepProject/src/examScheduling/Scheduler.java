package examScheduling;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class Scheduler {

    public static final int DAYS = 4, BLOCKSPERDAY = 4;
    public static final boolean AVAILABLE = false, TAKEN = true;

    SimpleWeightedGraph<String, DefaultWeightedEdge> swg;
    PriorityQueue<CourseVertex> pq;
    HashMap<String, CourseVertex> hm;

    public Scheduler(SimpleWeightedGraph<String, DefaultWeightedEdge> g) {
	this.swg = g;
	Set<String> verticies = g.vertexSet();
	pq = new PriorityQueue<>();
	hm = new HashMap<>();
	for (String s : verticies) {
	    CourseVertex v = new CourseVertex(s, g);
	    pq.add(v);
	    hm.put(s, v);
	}
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
	for (CourseVertex v : hm.values()) {
	    if (v.block == block && v.day == day)
		count++;
	}
	return count;

    }

    private boolean attemptToSchedule(CourseVertex cv) {
	boolean[][] timeSlots = getPossibleTimes(cv);
	for (int day = 0; day < DAYS; day++)
	    for (int block = 0; block < BLOCKSPERDAY; block++) {
		if (timeSlots[day][block] == AVAILABLE) {
		    cv.setTime(block, day);
		    return true;
		}
	    }
	return false;
    }

    private boolean[][] getPossibleTimes(CourseVertex cv) {
	boolean[][] times = new boolean[DAYS][BLOCKSPERDAY];
	Set<DefaultWeightedEdge> adj = swg.edgesOf(cv.name);
	for (DefaultWeightedEdge e : adj) {
	    String other = null;
	    if ((other = swg.getEdgeTarget(e)).equals(cv.name)) {
		other = swg.getEdgeSource(e);
	    }
	    CourseVertex othr = hm.get(other);
	    if (othr.day != -1 && othr.block != -1) {
		if (othr.block == 1 || othr.block == 3) {
		    times[othr.day][2] = TAKEN;
		    times[othr.day][othr.block] = TAKEN;
		} else if (othr.block == 2) {
		    times[othr.day][1] = TAKEN;
		    times[othr.day][2] = TAKEN;
		    times[othr.day][3] = TAKEN;

		} else {
		    times[othr.day][othr.block] = TAKEN;
		}
	    }

	}
	return times;
    }
}