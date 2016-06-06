package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;

import java.util.List;
import java.util.Properties;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 11:41 AM
 */
public interface SchedulerClient {

	public void downloadOutput(String remoteOutput, String localDestination) throws Exception;
	
	public void uploadDataset(String dataDirTemplateName, Properties dataProperties, 
    		String remoteScriptFileName, String localSource, String remoteDestination) throws Exception;
	
    public long submitJob(String jobTemplateName, Properties jobProperties, String remoteFileName) throws Exception;

    public boolean remoteFileExisted(String remoteFileName);
    
    public List<JobStatus> getQueueStatus() throws Exception;

    public JobStatus getStatus(long jobId) throws Exception;

    public void cancelJob(long jobId) throws Exception;
}
