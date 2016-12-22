package mariadbtxbench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.lang.Exception;
import java.sql.ResultSet;

/**
 *
 * @author Ann-Kathrin Hillig, Benjamin Laws, Tristan Simon
 */
public class WorkingClass implements Runnable {

    int threadId;
    Connection conni = null;
    
    int accId, tellerId, branchId, delta;
    int countTx = 0, countFail = 0;
    
    public WorkingClass (int id) {
        this.threadId = id;
    }
    
    // getBalance-method
    private int getBalance() throws Exception {
        
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conni.createStatement();
        
        accId = (int) (1 + Math.random() * 10_000_000);     // 1 - 1.000
        
        int accBal = 77777;
        
        rs = stmt.executeQuery(
                "SELECT balance FROM accounts WHERE accid = " + accId + ";");
        
        rs.next();
        accBal = rs.getInt(1);
        conni.commit();
        
        stmt.close();
        return accBal;
    }

    private int deposit() throws Exception {
        
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conni.createStatement();
        
        accId = (int) (1 + Math.random() * 10_000_000); // 1 - 10.000.000
        tellerId = (int) (1 + Math.random() * 1_000);   // 1 - 1.000
        branchId = (int) (1 + Math.random() * 100);     // 1 - 100
        delta = (int) (1 + Math.random() * 10_000);     // 1 - 1.000 EUR
        
        int newBal = 88888;
        
        // Updates 1-4
        stmt.executeUpdate("UPDATE branches SET balance = balance + " + delta +
                            " WHERE branchid = " + branchId + ";");
        
        stmt.executeUpdate("UPDATE tellers SET balance = balance + " + delta +
                            " WHERE tellerid = " + tellerId + ";");
        
        stmt.executeUpdate("UPDATE accounts SET balance = balance + " + delta +
                             " WHERE accid = " + accId + ";");
        
        stmt.executeUpdate("INSERT INTO history VALUES (" + accId + "," +
                            tellerId + "," + delta + "," + branchId +
                            ", (SELECT balance FROM accounts WHERE accid = " +
                            accId + ") + " + delta +
                            ",'100.000LeuchtendeSterneGesehen');");
  
        rs = stmt.executeQuery("SELECT balance FROM accounts WHERE accid = " +
                            accId + ";");

        rs.next();
 
        if (rs != null)
            newBal = rs.getInt(1) + delta;      // new balance not yet committed
                                                // -> read old one and add delta
        conni.commit();
        
        stmt.close();
        return newBal;
    }

    private int analyze() throws Exception {
        
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conni.createStatement();
        
        delta = (int) (1 + Math.random() * 1000);   // 1 - 1000
        
        int sameAmount = 99999;
        
        rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM history WHERE delta = " + delta + ";");
        rs.next();
        
        sameAmount = rs.getInt(1);
        
        conni.commit();
        
        stmt.close();
        return sameAmount;
    }

    @Override
    public void run() {
        try {
            
            Statement stmt = null;
            
            conni = DriverManager.getConnection(
                    "jdbc:mysql://192.168.122.46/bank", "dbi", "dbi_pass");
            
            // no suitable English translation :-/            
            System.out.println("Sehr verbunden :)");
            
            // isolation level
            conni.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conni.setAutoCommit(false);
            
            stmt = conni.createStatement();
            
            // clear history in case thread id is 0
            if (this.threadId == 0) {
                
                // only one thread will clear the history
                stmt.executeUpdate("DELETE FROM history");
                conni.commit();
                stmt.close();
            }

            int methodNo;

            while (UpperClass.measure) {

                // "roll" for randomized method call
                methodNo = (int) (1 + Math.random() * 100);
                
                // 35 / 50 / 15 %
                if (methodNo > 0 && methodNo <= 35)
                    methodNo = 1;
                
                else if (methodNo > 35 && methodNo <= 85)
                    methodNo = 2;
                
                else if (methodNo > 85 && methodNo <= 100)
                    methodNo = 3;
                
                try {
                    
                    switch (methodNo) {     // method switch/case

                        case 1:
                            getBalance();
                            break;

                        case 2:
                            deposit();
                            break;

                        case 3:
                            analyze();
                            break;

                        default:
                            System.out.println("*** Error ***");
                            break;
                    }

                    Thread.sleep(50);   // "think time"

                    if (UpperClass.timeToCount)
                        countTx++;      // count TXs during correct time frame
                    
                }
                catch (Exception e1) {

                    if (UpperClass.timeToCount)
                        countFail++;    // count failures during correct time frame

                    System.out.println("tx failed");
                    
                    try {
                        conni.rollback();   // try rollback if failed
                    }
                    catch (Exception e2) {
                        
                        System.out.println("rollback failed");
                        conni.rollback();   // try rollback-rollback if failed

                    }
                }
            }
            
            // write tx count result to array
            UpperClass.countArr[threadId] = countTx;
            
            // write failure count result to array
            UpperClass.failArr[threadId] = countFail;
            
            // close connection
            conni.close();
 
        }
        // catch failures from run()
        catch (Exception e) {
            System.err.println("* Error *");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
