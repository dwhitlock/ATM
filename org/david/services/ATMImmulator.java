package org.david.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import org.david.common.DBHelper;
/**
 * Created by dwhitlock on 9/26/2015.
 */
public class ATMImmulator {
    public DBHelper db;
    private Properties properties;
    private boolean systemCommErr = false;

    public ATMImmulator() throws IOException {
        InputStream fins = getClass().getResourceAsStream("env.properties");
        properties = new Properties();
        properties.load(fins);
        String url = properties.getProperty("jdbcUrl");
        String user = properties.getProperty("username");
        String password = properties.getProperty("password");
        String driver = properties.getProperty("driverName");
        db = new DBHelper();
        try {
            db.connectToDB(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        db.setDriverClass(driver);

        System.out.println("Driver class: " + db.getDriverClass());
    }

    public void printActionableItems(){
        System.out.println("Please select from the following action items:");
        System.out.println("To Deposit cash: Press '1'");
        System.out.println("To Withdraw cash: Press '2'");
        System.out.println("To see your account balance: Press '3'");
        System.out.println("To Exit: Press '0'");
    }

    public void ccNotFound(String cc) {
        System.out.println("Your debit card " + cc + " is not an authorized card to this ATM.\nWould you like to swipe another card? (Y/N)");
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        switch (input.toUpperCase()) {
            case "Y":
                break;
            case "N":
                break;
            default:
        }
    }

    public String getInput(){
        //System.out.println("Please enter your 4 to 8 digit pin: ");
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        return input;
    }

    public int invalidPinPrompt(int cnt){
        int rtnCode = 0;
        if (cnt == 3) {
            System.out.println("You have exceeded the maximum allowable attempts. Please contact customer service to resolve your issue.");
            rtnCode = -1;
        }
        else
            System.out.println("The pin you provided is invalid, please provide your pin, or type 'Cancel' to exit.");

        return rtnCode;
    }

    /*public Date setStringToDate(String date){
        SimpleDateFormat formatter = new SimpleDateFormat("mm/dd/yyyy");
        String exp = formatter.parse(date);
        Calendar c = Calendar.getInstance();
        int day = new Integer(date.split("/")[0]);
        int month = new Integer(date.split("/")[1]);
        int year =  new Integer(date.split("/")[2]);
        c.set(year, month, day);
        return c.getTime();
    }*/

    public boolean validatePin(String pin){
        return pin.matches("[0-9]{4,8}");
    }

    public String convertExpirationDate(String expDate){
        int month = new Integer(expDate.split("/")[0]);
        int year = 2000 +  new Integer(expDate.split("/")[1]);
        int lastday = 30;
        switch(month) {
            case 4:
            case 6:
            case 9:
            case 11:
                lastday = 30;
                break;
            default:
                if (year / 4 == 0)
                    lastday = 29;
                else
                    lastday = 28;
        }


        return month +"/" + lastday + "/" + year;
    }

    public boolean isAmountValid(String input, String type){
        boolean inputValid = true;
        if (input.matches("[0-9]{1,3}\\.[0-9]+")) {
            System.out.println("This system can not accept change, please enter amount to nearest dollar.");
            inputValid = false;
        }

        if (input.matches(".*[a-zA-Z]+.*")) {
            System.out.println("This is not a valid dollar amount.");
            inputValid = false;
        }

        if (type.equals("withdrawl")){
            //will not allow more than $500 to be withdrawn
            if (new Integer(input) > 500) {
                System.out.println("This ATM can only dispense up to $500.");
                inputValid = false;
            }
        }
        return inputValid;
    }

    public void activateCard(String ccnum){
        try {
            db.activateCard(ccnum);
            System.out.println("Your card has been activated!");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void withdrawAmt(String ccnum, float currentBalance, float withdrawlAmt, boolean applyOverdraft ) throws SQLException, ClassNotFoundException {
        withdrawlAmt = applyOverdraft ? withdrawlAmt + 35 : withdrawlAmt;
        float newBalance = currentBalance - withdrawlAmt;
        int activityId = db.createActivityHistoryRecord(ccnum, "withdrawl", withdrawlAmt);
        db.updateAccountBalance(ccnum, newBalance, activityId);
        db.updateCCActiviity(ccnum, activityId);
    }

    public float getAccountBalance(String ccnum){
        float balance = 0;
        boolean found = false;
        try {
            balance = new Float(db.getAccountInfo(ccnum).split("!")[1]);
            found = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (!found) {
                System.out.println("Unable to communicate to database. Please try another time.");
                systemCommErr = true;
            }
        }

        return balance;
    }

    public static void main(String[] args) {
        ATMImmulator atm = null;
        try {
            atm = new ATMImmulator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thank you for using XYZ Banking. Please swipe <enter> your debit card");
        String ccnum = atm.getInput();
        String ccid = null, acctid = null, pin = null;
        String ccStatus = null, expireDate = null;
        boolean isRestricted = false;
        boolean isActive;
        try {
            String[] ccInfo = atm.db.getCCInfo(ccnum).split("!");
            System.out.println("CC INFO " + ccInfo[0]);
            if (ccInfo[0].trim().length() > 0) {//no entry found in DB
                ccid = ccInfo[0];
                acctid = ccInfo[1];
                ccStatus = ccInfo[4];
                isActive = new Boolean(ccInfo[5]);
                expireDate = ccInfo[6];
                pin = ccInfo[7];
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally{
            //if cc does not exist
            if (acctid == null){
                atm.db.closeDBConnection();
                System.out.println("Unable to communicate to database. Please try another time.");
                System.exit(1);
            }
        }

        //verify pin
        int verified = 0;
        int counter = 0;
        int rtnCode = 0;
        do {
            counter += 1;
            System.out.println("Enter your 4 to 8 digit pin, then press Enter");
            String inputPin = atm.getInput();
            if (atm.validatePin(inputPin)) {
                if (pin.equals(inputPin))
                    verified = 1;
                else
                    rtnCode = atm.invalidPinPrompt(counter);
            }
            else
                rtnCode = atm.invalidPinPrompt(counter);
            verified = rtnCode == -1 ? -1 : verified;
        }while(verified == 0);
        //verify expiration date
        if (verified == 1){
            String expdate = atm.convertExpirationDate(expireDate);
            SimpleDateFormat formatter = new SimpleDateFormat("mm/dd/yyyy");
            Date expirationDate = null, today = null;
            try {
                expirationDate = formatter.parse(expdate);
                String hoy = formatter.format(Calendar.getInstance().getTime());
                today = formatter.parse(hoy);
            } catch (ParseException e) {
                e.printStackTrace();
            }


            if (! today.before(expirationDate)){
                System.out.println("This card is expired on " + expirationDate);
                System.out.println("Please contact customer support to get a new card");
                System.exit(0);
            }
        }
        else
            System.exit(0);

        //validate cc for exp and credit card status
        String msg = null;
        switch(ccStatus){
            case "Not Activated":
                try {
                    atm.activateCard(ccnum);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "On Hold":
                msg = "Your card has been placed on hold and can not be used until it has its restrition removed.";
                isRestricted = true;
                break;
            case "Temoporary Deactive":
                msg = "Your card has been placed on temporary and can not be used until it has its restrition removed.";
                isRestricted = true;
        }

        if (isRestricted){

            System.out.println(msg);
            System.out.println("Please call customer assistance on the back of your card to have it removed.");
            System.out.println("Thank you for using XYZ Banking Automated Teller Service.");
            System.exit(0);
        }
        //allow transactions
        boolean hasMoreTransactions = true;
        while (hasMoreTransactions){
            atm.printActionableItems();
            String input = atm.getInput();
            boolean isInputValid = input.matches("0|1|2|3");
            switch (input) {
                case "1": //deposit
                    System.out.println("Please enter the amount you want to deposit.");
                    input = atm.getInput();
                    boolean inputValid = atm.isAmountValid(input, "deposit");

                    if (inputValid){
                        int activityId = 0;
                        try {
                            float currentBalance = new Float(atm.db.getAccountInfo(ccnum).split("!")[1]);
                            activityId = atm.db.createActivityHistoryRecord(ccnum, "deposit", new Float(input));
                            atm.db.updateAccountBalance(ccnum, currentBalance + new Float(input), activityId);
                            atm.db.updateCCActiviity(ccnum, activityId);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        System.out.println(input + " is not valid");
                    }
                    break;
                case "2": //withdrawl
                    input = atm.getInput();
                    boolean isAmountValid = atm.isAmountValid(input, "withdrawl");
                    float withdrawlAmt = new Float(input);
                    if (isAmountValid) {
                        try {
                            boolean applyOverdraft = false;
                            float currentBalance = new Float(atm.db.getAccountInfo(ccnum).split("!")[1]);
                            boolean hasSufficientFunds = currentBalance >= withdrawlAmt;
                            if (!hasSufficientFunds){
                                System.out.println("You have insufficient funds to withdraw $" + input + ".");
                                System.out.println("If you would like to proceed, a $35 overdraft fee will be applied");
                                System.out.println("Would you like to proceed? y/n");
                                applyOverdraft = atm.getInput().matches("Y|y") ? true : false;
                            }

                            if (hasSufficientFunds | (!hasSufficientFunds & applyOverdraft)){
                                atm.withdrawAmt(ccnum, currentBalance, withdrawlAmt, applyOverdraft);
                            }

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case "3":
                    //view balance
                    try {
                        float currentBalance = new Float(atm.db.getAccountInfo(ccnum).split("!")[1]);
                        System.out.println("Your current balance is $" + currentBalance + ".");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    break;
                case "0":
                    hasMoreTransactions = false;
                    break;
            }

        }
        System.out.println("Thank you for using XYZ Banking Automated Teller Service.");

    }
}
