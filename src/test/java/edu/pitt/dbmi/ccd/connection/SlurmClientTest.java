package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Author : Jeremy Espino MD
 * Created  4/6/16 2:06 PM
 */
public class SlurmClientTest {

    private static long jobId;
    
    //@Autowired
    //private SlurmClient client;

    @Test
    public void testSubmitJob() throws Exception {

        SlurmClient client = new SlurmClient();

        Properties p = new Properties();

        p.setProperty("inputFile", "Retention.txt");
        jobId = client.submitJob(
        		"/Users/chw20/Documents/DBMI/remoteSchedulerClient/dist/job_templates/simpleFgs.vm", 
        		p, "~/testremotefile.sh");

        System.out.println("job id " + jobId);


    }


    @Test
    public void testGetQueueStatus() throws Exception {

        SlurmClient client = new SlurmClient();
        List<JobStatus> jobStatuses = client.getQueueStatus();
        for (JobStatus j : jobStatuses) {
            System.out.println(j.toString());
        }


    }

    @Test
    public void testGetStatus() throws Exception {

        SlurmClient client = new SlurmClient();

        JobStatus js = client.getStatus(jobId);

        assertNotNull(js);

        if (js != null) {
            System.out.println(js.toString());
        }
    }

    @Test
    public void testCancelJob() throws Exception {

        SlurmClient client = new SlurmClient();
        client.cancelJob(jobId);

        //assertNull(client.getStatus(jobId));


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