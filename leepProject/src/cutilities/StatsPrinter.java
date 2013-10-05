package cutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;

/**
 * A rather large number of formatted printing methods which are used to display
 * the things found in schedulerChecking
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class StatsPrinter {
    Formatter form;
    boolean toFile;

    public StatsPrinter() {
	toFile = false;

    }

    public StatsPrinter(File f) throws IOException {
	toFile = true;
	form = new Formatter(new BufferedWriter(new FileWriter(f)));

    }

    /**
     * closes the formatter if it is not closed
     */
    public void close() {
	if (form != null)
	    form.close();
    }

    public void printNumSomething(int num, String something) {
	prl("Number of " + something + ": " + num);
    }

    public void printSectionHeader(String header) {
	prl();
	prl(header);
    }

    public void printList(String list, int inRow) {
	if (list.length() == 0) {
	    switch (inRow) {
	    case 1:
		prl("No students with same time conflicts");
		break;
	    case 2:
		prl("No students with back to back finals");
		break;
	    case 3:
		prl("No students with three finals in a row");
		break;
	    }
	} else
	    pr(list);
    }

    public void printConflicts(int[] conflicts) {
	prl("Total back to back conflicts:   " + conflicts[0]);
	prl("Total three in a row conflicts: " + conflicts[1]);

    }

    public void printDayExams2DIntArray(int[][] arrayOfInts, int day, int maxExams) {
	String[] cols = new String[maxExams + 1];
	String[] rows = new String[day];
	cols[0] = "Day";
	for (int i = 0; i < maxExams; i++)
	    if (i == 1)
		cols[i + 1] = "" + i + " Exam";
	    else
		cols[i + 1] = "" + i + " Exams";
	for (int i = 0; i < day; i++)
	    rows[i] = "Day " + (i + 1);
	printArray2D(arrayOfInts, cols, rows, 5, 7);

    }

    public void printDayBlock2DIntArray(int[][] arrayOfInts, int day, int block) {
	String[] cols = new String[block + 1];
	String[] rows = new String[day];
	cols[0] = "Day";
	for (int i = 0; i < block; i++)
	    cols[i + 1] = "Block " + (i + 1);
	for (int i = 0; i < day; i++)
	    rows[i] = "Day " + (i + 1);
	printArray2D(arrayOfInts, cols, rows, 5, 7);

    }

    public void printB2BPerStudent(HashMap<Integer, Integer> b2bps) {
	int maxValues = 0;
	if (b2bps.keySet().size() != 0)
	    maxValues = Collections.max(b2bps.keySet());
	else {
	    prl("No Finals");
	    return;
	}
	String[] cols = { "Number Of Back To Back Finals", "Number Students" };
	String[] rows = new String[maxValues + 1];
	int[][] array = new int[maxValues + 1][1];
	for (int i = 0; i < maxValues + 1; i++) {
	    if (i != 1)
		rows[i] = Integer.toString(i) + " Finals";
	    else
		rows[i] = Integer.toString(i) + " Final";
	    array[i][0] = b2bps.get(i);
	}
	printArray2D(array, cols, rows, 29, 15);

    }

    public void printNExamsInMDays(HashMap<Integer, Integer>[] nInm) {
	int days = nInm.length;
	int maxValue = 0;
	for (int i = 0; i < days; i++) {
	    int newMax = 0;
	    if (nInm[i].keySet().size() != 0)
		newMax = Collections.max(nInm[i].keySet());
	    if (newMax > maxValue)
		maxValue = newMax;
	}
	if (maxValue == 0) {
	    prl("No Finals");
	    return;
	}
	String[] cols = new String[maxValue + 2];
	String[] rows = new String[days];
	cols[0] = "Number Days";
	for (int i = 0; i < maxValue + 1; i++) {
	    if (i == 1)
		cols[i + 1] = Integer.toString(i) + " Exam";
	    else
		cols[i + 1] = Integer.toString(i) + " Exams";
	}
	for (int i = 0; i < days; i++) {
	    rows[i] = "" + (i + 2) + " Days";
	}
	int[][] array = new int[days][maxValue + 1];
	for (int i = 0; i < days; i++)
	    for (int j = 0; j < maxValue + 1; j++)
		if (nInm[i].containsKey(j))
		    array[i][j] = nInm[i].get(j);
		else
		    array[i][j] = 0;
	printArray2D(array, cols, rows, 12, 9);
    }

    //cols is top row
    //rows is 2nd to last row
    private void printArray2D(int[][] array, String[] cols, String[] rows, int first, int others) {
	StringBuilder headers = new StringBuilder("|%-" + first + "s |");
	for (int i = 0; i < cols.length - 1; i++)
	    headers.append("%" + others + "s|");
	headers.append("%n");
	String template = headers.toString();
	format(template, cols);
	Object[] values = new Object[cols.length];
	for (int i = 0; i < array.length; i++) {
	    values[0] = rows[i].toString();
	    for (int j = 0; j < array[0].length; j++)
		values[j + 1] = Integer.toString(array[i][j]);
	    format(template, values);
	}

    }

    private void format(String s, Object[] o) {
	if (!toFile)
	    System.out.format(s, o);
	else
	    form.format(s, o);
    }

    private void prl() {
	prl("");
    }

    private void prl(Object s) {
	if (!toFile)
	    System.out.println(s);
	else
	    form.format("%s%n", s);
    }

    private void pr(Object s) {
	if (!toFile)
	    System.out.print(s);
	else
	    form.format("%s", s);
    }

}
