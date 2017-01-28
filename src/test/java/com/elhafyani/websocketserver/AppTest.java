package com.elhafyani.websocketserver;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        byte[] byteBarray = ByteBuffer.allocate(2).putShort((short) 65535).array();
        System.out.println(byteBarray.length);
        short result =  ByteBuffer.wrap(byteBarray).getShort();

        assertEquals( result ,  (short)65535);
    }
}
