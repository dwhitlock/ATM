package org.david.test;
import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.david.common.DBHelper;
import org.david.services.ATMImmulator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Created by dwhitlock on 9/27/2015.
 */
public class ATMImmulatorTest {
    ATMImmulator atm;
    @BeforeClass
    public void setUp(){
        try {
            atm = new ATMImmulator();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void validatePinTest(){
        boolean passed = atm.validatePin("1234");
        Assert.assertTrue("This pin should have been valid", passed);
    }

    @Test
    public void convertExpirationDateTest(){
        String rtnDate = atm.convertExpirationDate("09/16");
        System.out.println("Returned date is " + rtnDate);
    }

    @Test
    public void isAmountValidDepositTest(){
        boolean isValid = atm.isAmountValid("123", "deposit");
        Assert.assertTrue("123 should be valid for deposit", isValid);
    }

    @Test
    public void isAmountValidWithdrawTest(){
        boolean isValid = atm.isAmountValid("123", "withdraw");
        Assert.assertTrue("123 should be valid for deposit", isValid);
    }

    @Parameters({"ccid", "amount"})
    @Test
    public void withdrawAmtTest(String ccnum, float withdrawlAmt) throws SQLException, ClassNotFoundException {
        boolean applyOverdraft = false;
        float currentBalance = new Float(atm.db.getAccountInfo(ccnum).split("!")[1]);
        boolean hasSufficientFunds = currentBalance >= withdrawlAmt;
        if (!hasSufficientFunds){
            System.out.println("You have insufficient funds to withdraw $" + withdrawlAmt + ".");
            System.out.println("If you would like to proceed, a $35 overdraft fee will be applied");
            System.out.println("Would you like to proceed?");
            applyOverdraft = atm.getInput().matches("Y|y") ? true : false;
        }
        if (hasSufficientFunds | (!hasSufficientFunds & applyOverdraft)){
            atm.withdrawAmt(ccnum, currentBalance, withdrawlAmt, applyOverdraft);
        }
    }
}
