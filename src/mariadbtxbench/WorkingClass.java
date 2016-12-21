/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mariadbtxbench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.lang.Exception;
import java.sql.ResultSet;

/**
 *
 * @author ben
 */
public class WorkingClass implements Runnable {

    int threadId;
    Connection conni = null;
    
    int accId, tellerId, branchId, delta;
    int countTx = 0, countFail = 0;
    
    public WorkingClass (int id) {
        this.threadId = id;
    }
    
    private int getBalance() throws Exception {
        
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conni.createStatement();
        
        accId = (int) (1 + Math.random() * 10_000_000);
        
        int accBal = 77777;
        
        rs = stmt.executeQuery("SELECT balance FROM accounts WHERE accid = " + accId + ";");
        
        rs.next();
//        System.out.println(rs.getString(1));

        accBal = rs.getInt(1);
        conni.commit();
        
        stmt.close();
        return accBal;
    }

    private int deposit() throws Exception {
        
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conni.createStatement();
        
        accId = (int) (1 + Math.random() * 10_000_000);
        tellerId = (int) (1 + Math.random() * 1_000);
        branchId = (int) (1 + Math.random() * 100);
        delta = (int) (-500 + Math.random() * 1_000);    // +/- 500 EUR
        
        int newBal = 88888;
        
        // Updates 1-4
        stmt.executeUpdate("UPDATE branches SET balance = balance + " + delta +
                            " WHERE branchid = " + branchId + ";");
        
        stmt.executeUpdate("UPDATE tellers SET balance = balance + " + delta +
                            " WHERE tellerid = " + tellerId + ";");
        
        stmt.executeUpdate("UPDATE accounts SET balance = balance + " + delta +
                             " WHERE accid = " + accId + ";");
        
        stmt.executeUpdate("INSERT INTO history VALUES (" + accId + "," + tellerId +
                            "," + delta + "," + branchId +
                            ", (SELECT balance FROM accounts WHERE accid = " +
                            accId + ") + " + delta + ",'100.000LeuchtendeSterneGesehen');");
  
        rs = stmt.executeQuery("SELECT balance FROM accounts WHERE accid = " + accId + ";");
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
        
        delta = (int) (-1000 + Math.random() * 1000);
        
        int sameAmount = 99999;
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM history WHERE delta = " + delta + ";");
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
            
            conni = DriverManager.getConnection("jdbc:mysql://localhost/bank", "dbi", "dbi_pass");
            conni.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conni.setAutoCommit(false);
            
            stmt = conni.createStatement();
            
            // clear history in case thread id is 0
            if (this.threadId == 0) {
                stmt.executeUpdate("DELETE FROM history");
                conni.commit();
                stmt.close();
            }
            
            System.out.println("Sehr verbunden :)");    // no suitable English translation :-/
            int methodNo;

            while (UpperClass.measure) {

                methodNo = (int) (1 + Math.random() * 100);     // for randomized method call
                
                // 35 / 50 / 15 %
                if (methodNo > 0 && methodNo <= 35)
                    methodNo = 1;
                
                else if (methodNo > 35 && methodNo <= 85)
                    methodNo = 2;
                
                else if (methodNo > 85 && methodNo <= 100)
                    methodNo = 3;
                
                try {
                    
                    switch (methodNo) {

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

                    Thread.sleep(50);                   // "think time"

                    if (UpperClass.timeToCount)
                        countTx++;                      // count TXs during correct time frame
                    
                }
                catch (Exception e1) {

                    if (UpperClass.timeToCount)
                        countFail++;                    // count failures during correct time frame

                    System.out.println("tx failed");
                    
                    try {
                        conni.rollback();
                    }
                    catch (Exception e2) {
                        
                        System.out.println("rollback failed");
                        conni.rollback();
                        
                    }
                }
            }
            
            UpperClass.countArr[threadId] = countTx;    // write count result to array
            UpperClass.failArr[threadId] = countFail;   // count failures
            
            conni.close();
 
        }
        catch (Exception e) {
            System.err.println("* Error *");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
