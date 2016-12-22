package mariadbtxbench;

import java.util.GregorianCalendar;

/**
 *
 * @author Ann-Kathrin Hillig, Benjamin Laws, Tristan Simon
 */
public class UpperClass {

    static boolean measure = true;          // controls while-loop in threads
    static boolean timeToCount = false;     // de-/activates tx-counting for 5 min measurement
    
    static int[] countArr = new int[5];     // count result array for 5 threads
    static int[] failArr = new int[5];      // fail count Array for 5 threads

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // init and start threads
        Thread arr[] = new Thread[5];

        for (int i = 0; i < 5; i++) {
            
            // constructor call including thread-id parameter
            arr[i] = new Thread(new WorkingClass(i));
            
            // start threads
            arr[i].start();
            System.out.println("Thread " + i + " started...");
        }
        
        // timing control with timestamp outputs
        try {
            System.out.println("starting threads, warming up..." +
                                new GregorianCalendar().getTime());
            Thread.sleep(240_000);          // 4 min wait
            timeToCount = !timeToCount;     // toggle measurement (on)
            
            System.out.println("beginning measurement: " +
                                new GregorianCalendar().getTime());
            Thread.sleep(300_000);          // 5 min wait

            timeToCount = !timeToCount;     // toggle measurement (off)
            System.out.println("stopping measurement: " +
                                new GregorianCalendar().getTime());
            System.out.println("cooling down...");

            Thread.sleep(60_000);   // 1 min wait
            measure = false;        // "close" threads -> end while loop
            
            Thread.sleep(5_000);    // wait 5 seconds for threads to write into arrays
            System.out.println("finished." + new GregorianCalendar().getTime());

        }
        catch (Exception e) {            
            System.out.println("timer error");
        }
        
        int txCount = 0, failCount = 0;
        
        // add up transaction-counts from all 5 threads
        for (int i = 0; i < 5; i++)
            txCount += countArr[i];
        
        // add up transaction-fails from all 5 threads
        for (int i = 0; i < 5; i++)
            failCount += failArr[i];
        
        System.out.println("\nResult (overall during 5 mins): " + txCount +
                            " transactions INCLUDING fails");
        
        System.out.println("Result (Tx/s): " + txCount / 300);
        System.out.println("Failed transactions (overall during 5 mins): "
                            + failCount + "\n");

    }

}
