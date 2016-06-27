package com.mercateo.ops.tools.springbootstatus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StatusBeanUnit0Test {

    private StatusBean uut;

    @Test
    public void testIsUp_FailStatusNull() throws Exception {
        uut = new StatusBean(null);

        assertFalse(uut.isUp());
    }

    @Test
    public void testIsUp_FailStatusEmpty() throws Exception {
        uut = new StatusBean(" ");

        assertFalse(uut.isUp());
    }

    @Test
    public void testIsUp_FailStatusNotPresent() throws Exception {
        uut = new StatusBean("{some=key}");

        assertFalse(uut.isUp());
    }

    @Test
    public void testIsUp_SuccessSingleStatusUp() throws Exception {
        uut = new StatusBean("{status=UP}");

        assertTrue(uut.isUp());
    }

    @Test
    public void testIsUp_SuccessSingleStatusUnknown() throws Exception {
        uut = new StatusBean("{status=UNKNOWN}");

        assertTrue(uut.isUp());
    }

    @Test
    public void testIsUp_FailSingleStatusDown() throws Exception {
        uut = new StatusBean("{status=DOWN}");

        assertFalse(uut.isUp());
    }

    @Test
    public void testIsUp_SuccessMultiStatusUpUp() throws Exception {
        uut = new StatusBean("{status=UP, sub={status=UP}}");

        assertTrue(uut.isUp());
    }

    @Test
    public void testIsUp_SuccessMultiStatusUpUnknown() throws Exception {
        uut = new StatusBean("{status=UP, sub={status=UNKNOWN}}");

        assertTrue(uut.isUp());
    }

    @Test
    public void testIsUp_FailMultiStatusDownUp() throws Exception {
        // likely not possible in this constellation
        uut = new StatusBean("{status=DOWN, sub={status=OK}}");

        assertFalse(uut.isUp());
    }

    @Test
    public void testIsUp_FailMultiStatusDownRealWorld() throws Exception {
        uut = new StatusBean(
                "{status=DOWN, my={status=DOWN, errorCode=1}, discoveryComposite={description=Spring Cloud Eureka Discovery Client, status=UP, discoveryClient={description=Spring Cloud Eureka Discovery Client, status=UP, services=[this.server, that.server]}, eureka={description=Remote status from Eureka server, status=UP, applications={THIS.SERVER=3, THAT.SERVER=2}}}, diskSpace={status=UP, total=190073262080, free=44522237952, threshold=10485760}, refreshScope={status=UP}, configServer={status=UP, propertySources=[ssh://...]}, hystrix={status=UP}}");

        assertFalse(uut.isUp());
    }

}
