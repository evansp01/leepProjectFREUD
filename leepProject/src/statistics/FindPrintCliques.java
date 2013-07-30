package statistics;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import databaseForMainProject.DatabaseConnection;

public class FindPrintCliques { 
	
	static final String[] Sems = { "201009", "201101", "201109", "201201",
		"201209", "201301" };

    public static SimpleWeightedGraph<String, DefaultWeightedEdge> createGraph(String dbname, String url, String usr,
	    String pass) throws SQLException {

	DatabaseConnection connect = new DatabaseConnection(url, usr, pass);
	connect.connect();
	Statement st = connect.getStatement();
	Statement st2 = connect.getStatement();

	SimpleWeightedGraph<String, DefaultWeightedEdge> g = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

	String query1 = "SELECT DISTINCT CourseCRN, CourseTitle  FROM " + dbname;
	ResultSet rs1 = st.executeQuery(query1);
	while (rs1.next()) {
	    g.addVertex(rs1.getString(2) + " " + rs1.getString(1));
	}
	rs1.close();
	String query2 = "SELECT DISTINCT StudentIDNo FROM " + dbname;
	rs1 = st.executeQuery(query2);
	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query3 = "SELECT DISTINCT CourseCRN, CourseTitle FROM " + dbname + " WHERE StudentIDNo = '" + id + "';";
	    ResultSet rs2 = st2.executeQuery(query3);
	    ArrayList<String> crns = new ArrayList<String>();
	    while (rs2.next()) {
		crns.add(rs2.getString(2) + " " + rs2.getString(1));
	    }
	    DefaultWeightedEdge e;
	    for (int i = 0; i < crns.size(); i++)
		for (int j = 0; j < i; j++) {
		    String s = crns.get(i);
		    String t = crns.get(j);
		    if (g.containsEdge(s, t)) {
			e = g.getEdge(s, t);
			g.setEdgeWeight(e, g.getEdgeWeight(e) + 1);
		    } 
		    else {
		    g.addEdge(crns.get(i), crns.get(j)); 
		    }
		}
	    rs2.close();
	}

	rs1.close();
	st.close();
	st2.close();
	connect.close();
	return g;

    }

    public static void main(String[] args) { 
    	
	try {
		PrintStream out = new PrintStream(new FileOutputStream("Cliques.txt"));
		System.setOut(out);
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	}
	String url = "jdbc:mysql://localhost:3306/leep";
	String usr = "javauser";
	String pass = "testpass";
	SimpleWeightedGraph<String,DefaultWeightedEdge> g = null;
	try { 
		for (int k=0; k<Sems.length; k++) {
	    
		g=createGraph("studswfins" + Sems[k], url, usr, pass);   
		System.out.println("studswfins " + Sems[k]);
	   
		BronKerboschCliqueFinder<String, DefaultWeightedEdge> f = new BronKerboschCliqueFinder<String, DefaultWeightedEdge>(g);
		Collection<Set<String>> s = f.getBiggestMaximalCliques();
		@SuppressWarnings("unchecked")
		Set<String>[] q = s.toArray((Set<String>[]) new Set[1]);
		
		System.out.println(q.length);
		System.out.println(q[0].size()+" "+q[1].size()); 
		
		for (int i=0; i<q.length; i++){ 
			System.out.println("Clique number " + (i+1)); 
			for(String cour : q[i]){   
				System.out.println(cour);
			} 
			
			System.out.println(" "); 
			System.out.println("*******************************"); 
			System.out.println(" ");
		} 
		
		}
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    } 
	
    
}

