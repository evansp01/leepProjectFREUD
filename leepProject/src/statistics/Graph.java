package statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Graph {

    Map<String, Integer> idToLabel;
    int numEdges = 0;
    int[][] edges = null;
    boolean edgeGen = false;

    public Graph() {
	idToLabel = new HashMap<String, Integer>();
	numEdges = 0;
	edgeGen = false;
	edges = null;
    }

    public void insertNode(String crn) {
	if (edgeGen)
	    throw new IllegalStateException("edges already generated");
	idToLabel.put(crn, numEdges++);
    }

    public void genEdges() {
	edges = new int[numEdges][numEdges];
	edgeGen = true;
    }

    public void addEdge(String ve1, String ve2) {
	if (!edgeGen)
	    throw new IllegalStateException("edges not generated");

	int v1 = idToLabel.get(ve1);
	int v2 = idToLabel.get(ve2);
	edges[v1][v2]++;
    }

    /*
     * graph [ node [ id A ] node [ id B ] node [ id C ] edge [ source B target
     * A ] edge [ source C target A ] ]
     */

    public void print() {
	if (!edgeGen)
	    throw new IllegalStateException("edges not generated");

	prl("graph \n[");
	prl("directed 0");
	for (String s : idToLabel.keySet()) {
	    prl("node \n[");
	    prl("id " + idToLabel.get(s));
	    prl("label \"" + s + "\"");
	    prl("]");
	}
	for (int j = 0; j < numEdges; j++) {
	    for (int i = 0; i < j; i++) {
		if (edges[i][j] > 0) {
		    prl("edge \n[");
		    prl("source " + i);
		    prl("target " + j);
		    prl("weight " + edges[i][j]);
		    prl("]");
		}
	    }
	}
	prl("]");
    }

    public void printToFile(String fn) {
	if (!edgeGen)
	    throw new IllegalStateException("edges not generated");
	BufferedWriter br = null;
	try {
	    File f = new File(fn);
	    br = new BufferedWriter(new FileWriter(f.getAbsolutePath()));
	    br.write("graph \n[\ndirected 0\n");
	    for (String s : idToLabel.keySet()) {
		br.write("node \n[\nid " + idToLabel.get(s) + "\nlabel \"" + s + "\"\n]\n");
	    }
	    for (int j = 0; j < numEdges; j++) {
		for (int i = 0; i < j; i++) {
		    if (edges[i][j] > 0) {
			br.write("edge \n[\nsource " + i + "\ntarget " + j + "\nweight " + edges[i][j] + "\n]\n");
		    }
		}
	    }
	    br.write("]\n");

	} catch (IOException e) {
	    prl(e.getMessage());
	    e.printStackTrace();
	} finally {
	    if (br != null)
		try {
		    br.close();
		} catch (IOException e) {
		    prl(e.getMessage());
		    e.printStackTrace();
		}

	}

    }

    public static void prl(String s) {
	System.out.println(s);
    }
}
