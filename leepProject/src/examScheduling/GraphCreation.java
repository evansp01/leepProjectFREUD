package examScheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Set;

import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import databaseForMainProject.DatabaseConnection;

public class GraphCreation {
    public static final int ID = 1, CRN = 2, DISTINCTCRN = 1;

    public static SimpleWeightedGraph<String, DefaultWeightedEdge> createGraph(String dbname, String url, String usr,
	    String pass) throws SQLException {

	DatabaseConnection connect = new DatabaseConnection(url, usr, pass);
	connect.connect();
	Statement st = connect.getStatement();

	SimpleWeightedGraph<String, DefaultWeightedEdge> g = new SimpleWeightedGraph<String, DefaultWeightedEdge>(
		DefaultWeightedEdge.class);

	String getCourseCRNs = "SELECT DISTINCT CourseCRN FROM " + dbname;
	ResultSet courseCRNs = st.executeQuery(getCourseCRNs);

	while (courseCRNs.next()) {
	    g.addVertex(courseCRNs.getString(DISTINCTCRN));
	}
	courseCRNs.close();

	String getIDCRNPairs = "SELECT DISTINCT StudentIDNo, CourseCRN FROM " + dbname + " ORDER BY StudentIDNo";
	ResultSet orderedIDs = st.executeQuery(getIDCRNPairs);

	String currentStudent = null;
	String nextStudent = null;

	ArrayList<String> crns = null;

	while (orderedIDs.next()) {
	    if (!(nextStudent = orderedIDs.getString(ID)).equals(currentStudent)) {
		if (crns != null && crns.size() > 1) {
		    DefaultWeightedEdge e = null;
		    String s = null;
		    String t = null;
		    for (int i = 0; i < crns.size(); i++)
			for (int j = 0; j < i; j++) {
			    s = crns.get(i);
			    t = crns.get(j);
			    if (g.containsEdge(s, t)) {
				e = g.getEdge(s, t);
				g.setEdgeWeight(e, g.getEdgeWeight(e) + 1);
			    } else
				g.addEdge(crns.get(i), crns.get(j));
			}
		}
		currentStudent = nextStudent;
		crns = new ArrayList<String>();
		crns.add(orderedIDs.getString(CRN));

	    } else {
		crns.add(orderedIDs.getString(CRN));
	    }

	}
	orderedIDs.close();
	st.close();
	connect.close();
	return g;
    }

    public static void main(String[] args) {
	String url = "jdbc:mysql://localhost:3306/leep";
	String usr = "javauser";
	String pass = "testpass";
	SimpleWeightedGraph<String, DefaultWeightedEdge> g = null;
	try {
	    g = createGraph("studswfins201101", url, usr, pass);
	} catch (SQLException e) {
	    e.printStackTrace();
	}

	Scheduler schedule = new Scheduler(g);
	schedule.Schedule();

	//	BronKerboschCliqueFinder<String, DefaultWeightedEdge> f = new BronKerboschCliqueFinder<String, DefaultWeightedEdge>(
	//		g);
	//	Collection<Set<String>> s = f.getBiggestMaximalCliques();
	//	@SuppressWarnings("unchecked")
	//	Set<String>[] q = s.toArray((Set<String>[]) new Set[1]);
	//
	//	System.out.println(q.length);
	//	System.out.println(q[0].size() + " " + q[1].size());
	//
	//	System.out.println(g.toString());
    }
}
