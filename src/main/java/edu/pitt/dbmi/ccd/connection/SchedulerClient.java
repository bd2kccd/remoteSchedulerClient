package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;

import java.util.List;
import java.util.Properties;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 11:41 AM
 */
public interface SchedulerClient {
    public void submitDirectJob(String cmd, String args) throws Exception;

    public int submitJob(String jobTemplateName, Properties jobProperties, String remoteFileName) throws Exception;

    public List<JobStatus> getQueueStatus() throws Exception;

    public JobStatus getStatus(int jobId) throws Exception;

}
