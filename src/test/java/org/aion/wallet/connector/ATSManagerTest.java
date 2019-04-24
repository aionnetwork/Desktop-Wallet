package org.aion.wallet.connector;

import org.aion.api.IAionAPI;
import org.aion.wallet.config.Env;
import org.aion.wallet.exception.ValidationException;
import org.junit.After;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class ATSManagerTest {
    private ATSManager manager;
    private IAionAPI api;

    public ATSManagerTest(){
        api = IAionAPI.init();
        api.connect(Env.KERNEL_URI);
        manager = new ATSManager(api);
    }

    @After
    public void after(){
        api.destroyApi();
    }



    @Test
    public void getName() throws ValidationException {
        assertEquals( "SantaCoin" ,manager.getName("a0236a8bbe698c3293cb75cdb5bdbbae18f17507878778d42686e61d6410d643", "0xa0398f98c7f8b8509d84949b0dcafd26b75cf0c9d13f6f9879b51a2030852278"));
    }

    @Test
    public void getSymbol() throws ValidationException {
        assertEquals( "SNC",manager.getSymbol("a0236a8bbe698c3293cb75cdb5bdbbae18f17507878778d42686e61d6410d643", "0xa0398f98c7f8b8509d84949b0dcafd26b75cf0c9d13f6f9879b51a2030852278"));

    }

    @Test
    public void getGranularity() throws ValidationException {
        assertEquals( 1L, manager.getGranularity("a0236a8bbe698c3293cb75cdb5bdbbae18f17507878778d42686e61d6410d643", "0xa0398f98c7f8b8509d84949b0dcafd26b75cf0c9d13f6f9879b51a2030852278") );

    }

    @Test
    public void getBalance() throws ValidationException {
        assertEquals(BigInteger.valueOf(42L) ,manager.getBalance("a0236a8bbe698c3293cb75cdb5bdbbae18f17507878778d42686e61d6410d643", "0xa0398f98c7f8b8509d84949b0dcafd26b75cf0c9d13f6f9879b51a2030852278"));

    }
}