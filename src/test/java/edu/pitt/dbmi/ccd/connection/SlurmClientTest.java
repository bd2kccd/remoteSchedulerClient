package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Author : Jeremy Espino MD
 * Created  4/6/16 2:06 PM
 */
public class SlurmClientTest {

    @Test
    public void testSubmitJob() throws Exception {

    }

    @Test
    public void testSubmitDirectJob() throws Exception {

    }

    @Test
    public void testGetQueueStatus() throws Exception {

        SlurmClient client = new SlurmClient();
        List<JobStatus> jobStatuses = client.getQueueStatus();
        for (JobStatus j: jobStatuses ) {
            System.out.println(j.toString());


        }



    }

    @Test
    public void testGetStatus() throws Exception {

        SlurmClient client = new SlurmClient();
        System.out.println(client.getStatus(111407).toString());

    }

    @Before
    public void setUp() throws Exception {

        SshConnection.getInstance();

    }

    @After
    public void tearDown() throws Exception {

        SshConnection.getInstance().close();
    }
}