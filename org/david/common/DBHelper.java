package org.david.common;

/**
 * Created by dwhitlock on 9/25/2015.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;;

import javafx.beans.property.SimpleMapProperty;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class DBHelper {
    private static Logger logger = Logger.getLogger(DBHelper.class);

    public Connection con = null;
    private String query;
    private String driverClass;

    public DBHelper(){
        String log4jConfPath = "C:/Users/dwhitlock/IdeaProjects/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);
    }

    //establish connection with the DB
    public Connection connectToDB(String connectionURL, String connectionUser, String connectionPass)
            throws SQLException {
        System.out.println("connectionURL: " + connectionURL);
        System.out.println("connectionUser: " + connectionUser);
        System.out.println("connectionPass: " + connectionPass);

        if (connectionUser.trim().length() > 0)
            con = DriverManager.getConnection(connectionURL, connectionUser,
                    connectionPass);
        else
            con = DriverManager.getConnection(connectionURL);

        return con;

    }
    //close the DB connection
    public void closeDBConnection() {
        try {
            if (con != null) {
                con.close();
                System.out.println("DB connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getActivityRef(String name) throws ClassNotFoundException, SQLException {
        Class.forName(driverClass);
        Statement stmt1 = null;
        String id = "";
        try {
            stmt1 = con.createStatement();
            String query = "Select ACTIVITY_TYPE_REF_ID from ACTIVITY_TYPE_REF where Name = '" + name + "'";
            ResultSet rs = stmt1.executeQuery(query);
            if (rs.next()) {
                id = rs.getString("ACTIVITY_TYPE_REF_ID");
            }
        }catch (SQLException se){

        }
        finally {
            if (stmt1 != null)
                stmt1.close();
        }
        return id;

    }

    public String getAccountInfo(String ccnum) throws ClassNotFoundException, SQLException {
        Class.forName(driverClass);
        Statement stmt1 = null;
        float balance = 0;
        String acctid=null;
        String contactid=null;
        try {
            stmt1 = con.createStatement();
            String query = "SELECT a.account_master_id, a.balance, a.contact_id from dbtest.account_master a, dbtest.credit_card_master cm" +
                    " where cm.cc_number = '" + ccnum + "' and cm.account_master_id = a.account_master_id";
            ResultSet rs = stmt1.executeQuery(query);
            if (rs.next()) {
                acctid = rs.getString("account_master_id");
                balance = rs.getFloat("balance");
                contactid = rs.getString("contact_id");
            }
        }catch (SQLException se){

        }
        finally {
            if (stmt1 != null)
                stmt1.close();
        }

        logger.warn("Account info : " + acctid + "!" + balance + "!" + contactid);
        return acctid + "!" + balance + "!" + contactid;

    }

    public String getCCInfo(String ccnum) throws ClassNotFoundException, SQLException {
        Class.forName(driverClass);
        Statement stmt1 = null;
        boolean active = false;
        String acctid=null, contactid=null, ccid=null, acttivityId=null, status_ref=null;
        String expire=null, pin=null;
        try {
            stmt1 = con.createStatement();
            String query = "SELECT c.cc_master_id, c.account_master_id, c.contact_id, c.activity_history_master_id, c.is_active, c.expiration_date, c.pin, s.cc_status_ref_name " +
                    "from credit_card_master c join credit_card_status_ref s on c.cc_status_ref_id = s.cc_status_ref_id " +
                    "where c.cc_number = '" + ccnum + "'";
            ResultSet rs = stmt1.executeQuery(query);
            if (rs.next()) {
                ccid = rs.getString("cc_master_id");
                acctid = rs.getString("account_master_id");
                contactid = rs.getString("contact_id");
                acttivityId = rs.getString("activity_history_master_id");
                status_ref = rs.getString("cc_status_ref_name");
                active = rs.getBoolean("is_active");
                expire = rs.getString("expiration_date");
                pin = rs.getString("pin");

            }
        }catch (SQLException se){

        }
        finally {
            if (stmt1 != null)
                stmt1.close();
        }
        logger.warn("CC info : CC: " +ccid + "\nAcct Id: " + acctid  + "\nContactId: " + contactid + "\nActiviityId: " + acttivityId + "\nStatus: " + status_ref + "\nIsActive: " + active + "!ExpirationDate: " + expire + "\nPIN:" + pin);
        return ccid + "!" + acctid  + "!" + contactid + "!" + acttivityId + "!" + status_ref + "!" + active + "!" + expire + "!" + pin;

    }

    public void updateAccountBalance(String ccnum, float balance, int activityId) throws SQLException, ClassNotFoundException {
        String query = "update account_master a set a.balance = " + balance +
                " where a.account_master_id = (select account_master_id from credit_card_master where cc_number = '" + ccnum + "')";

        executePreparedStatement(query);
    }

    public void activateCard(String ccnum) throws SQLException, ClassNotFoundException {
        String query = "update credit_card_master c set c.is_Active = 1 where c.cc_number = '" + ccnum + "'";
        executePreparedStatement(query);
    }

    public void updateCCActiviity(String ccnum, int activityId){
        String query = "update credit_card_master c set c.activity_history_master_id = " + activityId + " where c.cc_number = '" + ccnum +"'";
        executePreparedStatement(query);
    }

    public int createActivityHistoryRecord(String ccnum, String activityType, float amount)
            throws SQLException, ClassNotFoundException {
        int id = getMaxIdOfTable("ACTIVITY_HISTORY_MASTER_ID", "ACTIVITY_HISTORY_MASTER");
        String activityRefId = getActivityRef(activityType.toUpperCase());
        String ccid = getCCInfo(ccnum).split("!")[0];
        String today = getCurrentTimeStamp();
        String query = "INSERT INTO ACTIVITY_HISTORY_MASTER" +
                "(ACTIVITY_HISTORY_MASTER_ID, CC_MASTER_ID, ACTIVITY_TYPE_REF_ID, AMOUNT, ACTIVITY_DATE, ATM_LOCATION_MASTER_ID) " +
                "values (" + ++id + ", " + ccid + ", " + activityRefId + "," + amount + ",'" + today + "', 1)";

        executePreparedStatement(query);
        return id;
    }

    //gets the max id of the specified table
    public int getMaxIdOfTable(String column, String table) throws ClassNotFoundException, SQLException {
        Class.forName(driverClass);
        Statement stmt1 = null;
        int id = 0;
        try {
            stmt1 = con.createStatement();
            String query = "SELECT max(" + column + ") as Id from " + table;
            ResultSet rs = stmt1.executeQuery(query);
            if (rs.next()) {
                id = rs.getInt("Id");
            }
        }catch (SQLException se){

        }
        finally {
            if (stmt1 != null)
                stmt1.close();
        }
        return id;
    }

    public int getActivityHistoryCountForCC(String ccnum) throws ClassNotFoundException, SQLException {
        Class.forName(driverClass);
        Statement stmt1 = null;
        int cnt = -1;
        try {
            stmt1 = con.createStatement();
            String query = "SELECT count(ACTIVITY_HISTORY_MASTER_ID) AS CNT FROM ACTIVITY_HISTORY_MASTER WHERE CC_MASTER_ID = " +
                    "(Select CC_MASTER_ID FROM CREDIT_CARD_MASTER WHERE CC_NUMBER = '" + ccnum + "')";
            ResultSet rs = stmt1.executeQuery(query);
            if (rs.next()) {
                cnt = rs.getInt("cnt");
            }
        }catch (SQLException se){

        }
        finally {
            if (stmt1 != null)
                stmt1.close();
        }
        return cnt;
    }

    //executes a prepared statement
    public boolean executePreparedStatement(String stmt){
        boolean executed = false;
        System.out.println("Prepared Statement: " + stmt);
        try {
            PreparedStatement p = con.prepareStatement(stmt);
            executed = p.execute();
            p.close();
        } catch (SQLException e) {
            System.err.println("Exception in " + Thread.currentThread().getStackTrace()[1].getMethodName() + ":" + e.getMessage());
            e.printStackTrace();
        }
        return executed;
    }

    public String getCurrentTimeStamp(){

        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return formater.format(new Timestamp(new Date().getTime()));
    }

    public void setDriverClass(String driverClass){
        this.driverClass = driverClass;
    }

    public String getDriverClass(){
        return driverClass;
    }
}
