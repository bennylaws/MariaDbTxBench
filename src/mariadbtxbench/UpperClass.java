package mariadbtxbench;

import java.util.GregorianCalendar;

/**
 *
 * @author Ann-Kathrin Hillig, Benjamin Laws, Tristan Simon
 */
public class UpperClass {

    static boolean measure = true;          // controls while-loop in threads
    static boolean timeToCount = false;     // de-/activates tx-counting for 5min measurement
    
    static int[] countArr = new int[5];     // count result array for 5 threads
    static int[] failArr = new int[5];      // fail count Array for 5 threads

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
            System.out.println("starting threads, warming up..." +
                                new GregorianCalendar().getTime());
            Thread.sleep(20_000);
            timeToCount = !timeToCount;     // toggle measurement (on)
            
            System.out.println("beginning measurement: " +
                                new GregorianCalendar().getTime());
            Thread.sleep(300_000);

            timeToCount = !timeToCount;     // toggle measurement (off)
            System.out.println("stopping measurement: " +
                                new GregorianCalendar().getTime());
            System.out.println("cooling down...");

            Thread.sleep(6_000);
            measure = false;                // "close" threads -> end while loop
            
            Thread.sleep(5_000);           // wait for threads to write into arrays
            System.out.println("finished." + new GregorianCalendar().getTime());

        }
        catch (Exception e) {
            
            System.out.println("Timer-Errör");
            
        }
        
        int txCount = 0, failCount = 0;
        
        // add up transaction-counts from all 5 threads
        for (int i = 0; i < 5; i++)
            txCount += countArr[i];
        
        // add up transaction-fails from all 5 threads
        for (int i = 0; i < 5; i++)
            failCount += failArr[i];
        
        System.out.println("\nResult (overall during 0.5 mins): " + txCount +
                            " transactions INCLUDING fails");
        
        System.out.println("Result (Tx/s): " + txCount / 300);
        System.out.println("Failed transactions (overall during 5 mins): " + failCount + "\n");

    }

}
