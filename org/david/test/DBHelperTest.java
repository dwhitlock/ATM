package org.david.test;

import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.david.common.DBHelper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Created by dwhitlock on 9/25/2015.
 */
public class DBHelperTest {
    public String driverClass;
    public String username;
    public String password;
    public String jdbcUrl;
    private Connection con;
    private DBHelper db;
    private static Logger logger = Logger.getLogger(DBHelper.class);

    @BeforeClass
    public void setUp() {
        logger.setLevel(Level.INFO);
        driverClass = "com.mysql.jdbc.Driver";
        jdbcUrl = "jdbc:mysql://localhost:3306/DBTEST";
        username = "root";
        password = "password";
        db = new DBHelper();
        db.setDriverClass(driverClass);
        try {
            con = db.connectToDB(jdbcUrl, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Parameters({ "ccid" })
    @Test
    public void getCustomerBalanceTest(String ccid) throws SQLException, ClassNotFoundException {
        float balance = new Float(db.getAccountInfo(ccid).split("!")[1]);
        System.out.println("Balance is " + balance);
        Assert.assertTrue("Expecting balance to be larger than 0", balance > 0);
    }

    @Parameters({ "ccid", "amount", "transType" })
    @Test
    public void updateAccountBalanceTest(String ccid, float amount, String transType) throws SQLException, ClassNotFoundException {
        System.out.println("CCID " + ccid + " Amount " + amount + " transType " + transType);
        float subtotal = transType.equalsIgnoreCase("withdrawl") ? amount * -1 : amount;
        float beforeUpdate = new Float(db.getAccountInfo(ccid).split("!")[1]);
        db.updateAccountBalance(ccid, subtotal, 1);
        float afterUpdate =  new Float(db.getAccountInfo(ccid).split("!")[1]);
        Assert.assertEquals("The subtotal was not added to the account balance", (subtotal + beforeUpdate), afterUpdate);
    }

    @Parameters({ "ccid", "amount", "transType" })
    @Test
    public void createActivityHistoryRecordTest(String ccid, float amount, String transType) throws SQLException, ClassNotFoundException {
        int beforeCount = db.getActivityHistoryCountForCC(ccid);
        db.createActivityHistoryRecord(ccid, transType, amount);
        int afterCount = db.getActivityHistoryCountForCC(ccid);

        Assert.assertEquals("No Activity History record inserted", (beforeCount + 1), afterCount);
    }

    @AfterClass
    public void teardown(){
        db.closeDBConnection();
        con = null;
    }
}
