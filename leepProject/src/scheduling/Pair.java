package scheduling;
//Pair class for holding final exam schedule pairs
/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class Pair {

    public int v1;
    public int v2;

    /**
     * Construct a pair of integers
     * 
     * @param v1
     * @param v2
     */
    Pair(int v1, int v2) {
	this.v1 = v1;
	this.v2 = v2;

    }

    /**
     * get the first integer
     * 
     * @return the first int in the pair
     */
    public int day() {
	return v1;
    }

    /**
     * get the second integer
     * 
     * @return the second int in the pair
     */
    public int block() {
	return v2;
    }

}
