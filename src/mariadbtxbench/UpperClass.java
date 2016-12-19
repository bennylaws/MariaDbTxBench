package mariadbtxbench;

/**
 *
 * @author Ann-Kathrin Hillig, Benjamin Laws, Tristan Simon
 */
public class UpperClass {

    static boolean measure = true;          // controls while-loop in threads
    static boolean timeToCount = false;     // de-/activates tx-counting for 5min measurement
    
    static long[] countArr = new long[5];   // count result array for 5 threads

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // init and start threads
        Thread arr[] = new Thread[5];

        for (int i = 0; i < 5; i++) {
            arr[i] = new Thread(new WorkingClass(i));   // constructer call including thread-id param
            arr[i].start();
            System.out.println("Thread " + i + " started...");
        }
        
        // timing control
        try {
            Thread.sleep(240_000);
            timeToCount = !timeToCount;     // toggle measurement (on)
            
            Thread.sleep(300_000);
            timeToCount = !timeToCount;     // toggle measurement (off)
            
            Thread.sleep(60_000);
            measure = false;                // "close" threads -> end while loop
            
            Thread.sleep(10_000);           // wait for threads to write into array
            
        } catch (Exception e) {
            System.out.println("Timer-Errör");
        }
        
        int txCount = 0;
        
        // add up transaction-counts from all 5 threads
        for (int i = 0; i < 5; i++)
            txCount += countArr[i];
        
        System.out.println("Finished. Result: " + txCount / 300 + " Tx/s");

    }

}
