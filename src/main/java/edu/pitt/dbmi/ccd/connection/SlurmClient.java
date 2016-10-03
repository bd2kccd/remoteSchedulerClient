package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStat;
import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 11:50 AM
 * Author : Chirayu (Kong) Wongchokprasitti, PhD 
 * Modified 6/21/16 2:21 PM
 */
public class SlurmClient implements SchedulerClient {

    final Logger logger = LoggerFactory.getLogger(SchedulerClient.class);

    final private VelocityEngine ve;
    
    final private SshConnection sshConn;
    
    public SlurmClient() {
    	ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, Configuration.getInstance().getTemplatePath());
        ve.init();
        
    	this.sshConn = SshConnection.getInstance();
    }
    
    /**
     * @param remoteOutput
     * @param localDestination
     * @throws Exception
     */
    public void downloadOutput(String remoteOutput, String localDestination) throws Exception {
    	sshConn.connect();
        sshConn.receiveFile(remoteOutput, localDestination);
     }
    
    /**
     * @param dataDirTemplateName - the velocity template filename that will be found in configuration's template path
     * @param dataProperties
     * @param remoteScriptFileName
     * @param localSource
     * @param remoteDestination
     * @throws Exception
     */
    public void uploadFile(String dataDirTemplateName, Properties dataProperties, 
    		String remoteScriptFileName, String localSource, String remoteDestination) throws Exception {
    	
    	//Script to create a user space if not existing
    	if(!remoteFileExistes(remoteScriptFileName)){

            // get the Template  */
            Template t = ve.getTemplate(dataDirTemplateName);

            // create context and add variables
            VelocityContext context = new VelocityContext();
            Enumeration e = dataProperties.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                context.put(key, dataProperties.getProperty(key));
            }

            // now render the template into a StringWriter */
            String outputFilename = Configuration.getInstance().getScratchDirectory() + File.separator + System.currentTimeMillis() + ".sh";
        	
            // save the output to a file
            FileWriter writer = new FileWriter(outputFilename);
            t.merge(context, writer);
            writer.flush();
            writer.close();

            // upload the check data space script file
            sshConn.connect();
            sshConn.sendFile(outputFilename, remoteScriptFileName);
        	
            // Delete a temp file
            try {
    			Files.deleteIfExists(Paths.get(outputFilename));
    		} catch (Exception e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
            
            // run a script
            sshConn.connect();
            String scriptResult = sshConn.executeCommand("sh " + remoteScriptFileName);
            logger.debug(scriptResult);
    	}
    	
        // upload the dataset
        sshConn.connect();
        sshConn.sendFile(localSource, remoteDestination);
    }
    
    /**
     * @param jobTemplateName - the velocity template filename that will be found in configuration's template path
     * @param jobProperties
     * @param remoteScriptFileName
     * @return job id from the remote slurm scheduler
     * @throws Exception
     */
    public long submitJob(String jobTemplateName, Properties jobProperties, String remoteScriptFileName) throws Exception {

        // get the Template  */
        Template t = ve.getTemplate(jobTemplateName);

        // create context and add variables
        VelocityContext context = new VelocityContext();
        Enumeration e = jobProperties.propertyNames();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            context.put(key, jobProperties.getProperty(key));
        }

        // now render the template into a StringWriter */
        String outputFilename = Configuration.getInstance().getScratchDirectory() + File.separator + System.currentTimeMillis() + ".sh";

        // save the output to a file
        FileWriter writer = new FileWriter(outputFilename);
        t.merge(context, writer);
        writer.flush();
        writer.close();

        // upload the job file
        sshConn.connect();
        sshConn.sendFile(outputFilename, remoteScriptFileName);

        // Delete a temp file
        try {
			Files.deleteIfExists(Paths.get(outputFilename));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        // submit job
        sshConn.connect();
        String sbatchResult = sshConn.executeCommand("sbatch " + remoteScriptFileName);
        String jobId = "";
        if (sbatchResult.contains("Submitted batch job ")) {
            jobId = sbatchResult.substring("Submitted batch job ".length(), sbatchResult.length() - 1);
        }

        logger.debug("Submitted script and assigned job id: " + jobId);

        return Long.parseLong(jobId);
    }

    /**
     * @param remoteFileName
     * @return True if file exists
     * @throws Exception
     */
    public boolean remoteFileExistes(String remoteFileName) {

        String lsResult;
		try {
	        sshConn.connect();
			lsResult = sshConn.executeCommand("ls " + remoteFileName);
			logger.debug("lsResult: " + lsResult);
	        return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return false;
		}
        
    }
    
    /**
     * @param remoteFileName
     * @return True if file exists
     * @throws Exception
     */
    public boolean deleteRemoteFile(String remoteFileName) {

		try {
	        sshConn.connect();
			sshConn.executeCommand("rm " + remoteFileName);
	        return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return false;
		}
        
    }
    
    /**
     * Retrieve a list of all jobs on the remote system
     *
     * @return list of Job Status objects
     * @throws Exception
     */
    public List<JobStatus> getQueueStatus() throws Exception {

        sshConn.connect();
        String squeueResult = sshConn.executeCommand("squeue");

        return str2JobStatuses(squeueResult);
    }

    /**
     * Retrieve a list of the user's all finished jobs on the remote system
     *
     * @return list of Job Status objects
     * @throws Exception
     */
    public List<JobStatus> getFinishedJobs() throws Exception {

        String sacctCmd = "sacct " +
        		" --format=JobID,Partition,JobName,User,state,Elapsed,NNodes,NodeList";
        sshConn.connect();
        String sacctResult = sshConn.executeCommand(sacctCmd);

        List<JobStatus> jobList = sacctResult2JobStatuses(sacctResult);
        List<JobStatus> finishedJobList = new ArrayList<>();
        for(JobStatus job : jobList){
        	if(!job.getState().equalsIgnoreCase("RUNNING") && !job.getState().equalsIgnoreCase("PENDING")){
        		finishedJobList.add(job);
        	}
        }
        
        return finishedJobList;
    }
    
    public JobStat getJobStat(long jobId) throws Exception {
    	
    	JobStat jobStat = new JobStat();
    	
        sshConn.connect();
        String sacctResult = sshConn.executeCommand("sacct "
        		+ "--format=JobID,Partition,state,Elapsed,Start,End,AllocCPUS,AllocNodes --job " + jobId);
        List<JobStat> jobStats = sacctResult2JobStats(sacctResult);
        if(jobStats != null && !jobStats.isEmpty()){
        	jobStat = jobStats.get(0);
        }
    	
    	return jobStat;
    }
    
    /**
     * Retrieve the status of a single job on the remote system
     *
     * @param jobId
     * @return Job Status on the remote system.  Null if doesn't exist
     * @throws Exception
     */
    public JobStatus getStatus(long jobId) throws Exception {

        JobStatus jobStatus = new JobStatus();

        sshConn.connect();
        String squeueResult = sshConn.executeCommand("squeue --job " + jobId);

        List<JobStatus> jobStatuses = str2JobStatuses(squeueResult);

        if (jobStatuses != null && jobStatuses.size() > 0) {

            jobStatus = jobStatuses.get(0);

        } else {

            // job might have finished so get exit status
            String sacctCmd = "sacct "
            		+ "--format=JobID,Partition,JobName,User,state,Elapsed,NNodes,NodeList --job " + jobId;
            String sacctResult = sshConn.executeCommand(sacctCmd);
            jobStatuses = sacctResult2JobStatuses(sacctResult);
            if (jobStatuses != null && !jobStatuses.isEmpty()) {
                jobStatus = jobStatuses.get(0);
            }

        }

        return jobStatus;

    }

    /**
     * Cancel a single job
     *
     * @param jobId the job id on the remote system
     * @throws Exception
     */
    public void cancelJob(long jobId) throws Exception {

        sshConn.connect();
        sshConn.executeCommand("scancel " + jobId);

    }

    /**
     * Parses a slurm string for job information from sacct command
     *
     * @param s
     * @return
     */
    private List<JobStat> sacctResult2JobStats(String s) {
    	/**
    	 *       JobID  Partition      State    Elapsed               Start                 End  AllocCPUS AllocNodes 
		 *------------ ---------- ---------- ---------- ------------------- ------------------- ---------- ----------
		 *138330               RM  COMPLETED   00:00:02 2016-06-08T16:50:17 2016-06-08T16:50:19         28          1 
		 *138330.batch             COMPLETED   00:00:02 2016-06-08T16:50:17 2016-06-08T16:50:19         28          1 
    	 * 
    	 */
    	
    	List<JobStat> JobStats = new ArrayList<>();
    	Scanner scanner = new Scanner(s);
        boolean secondLine = true;
        while (scanner.hasNextLine()) {
            if (secondLine) {
                secondLine = false;
                scanner.nextLine();
                scanner.nextLine();
                continue;
            }
            String line = scanner.nextLine();
            JobStat js = line2JobStat(line);
            if(js != null){
            	JobStats.add(js);
            }

        }
        scanner.close();
        
    	return JobStats;
    }


    /**
     * Parses a slurm string for job information from sacct command
     *
     * @param s
     * @return
     */
    private List<JobStatus> sacctResult2JobStatuses(String s) {
    	/**
    	 *       JobID  Partition    JobName      User      State    Elapsed   NNodes        NodeList 
		 *------------ ---------- ---------- --------- ---------- ---------- -------- --------------- 
		 *135524               RM causal-cm+   chirayu  COMPLETED   00:00:01        1            r017 
		 *135524.batch                 batch            COMPLETED   00:00:01        1            r017 
    	 * 
    	 */
    	
    	List<JobStatus> jobStatuses = new ArrayList<>();
    	Scanner scanner = new Scanner(s);
        boolean secondLine = true;
        while (scanner.hasNextLine()) {
            if (secondLine) {
                secondLine = false;
                scanner.nextLine();
                scanner.nextLine();
                continue;
            }
            String line = scanner.nextLine();
            JobStatus js = line2JobStatus(line);
            if(js != null){
                jobStatuses.add(js);
            }

        }
        scanner.close();
        
    	return jobStatuses;
    }

    /**
     * Parses a slurm string for job information from squeue command
     *
     * @param s
     * @return
     */
    private List<JobStatus> str2JobStatuses(String s) {
        /**
         *              JOBID PARTITION     NAME     USER ST       TIME  NODES NODELIST(REASON)
         *                     111189        LM run_RM2_  sczyrba  R    9:15:49      1 l004
         */

        List<JobStatus> jobStatuses = new ArrayList<>();
        Scanner scanner = new Scanner(s);
        boolean firstLine = true;
        while (scanner.hasNextLine()) {
            if (firstLine) {
                firstLine = false;
                scanner.nextLine();
                continue;
            }
            String line = scanner.nextLine();
            JobStatus js = line2JobStatus(line);
            if(js != null){
                jobStatuses.add(js);
            }

        }
        scanner.close();

        return jobStatuses;

    }

    /**
     * Parses a single line for job stat information
     *
     * @param line
     * @return
     */
    private JobStat line2JobStat(String line) {

        line = line.trim();
        String elements[] = line.split("\\s+");

        if(elements == null || elements.length < 8)
        	return null;
        
        JobStat jobStat = new JobStat();
        jobStat.setJobId(elements[0]);
        jobStat.setPartition(elements[1]);
        jobStat.setState(elements[2]);
        jobStat.setTime(elements[3]);
        jobStat.setStart(elements[4]);
        jobStat.setEnd(elements[5]);
        jobStat.setAllocCPUS(elements[6]);
        jobStat.setAllocNodes(elements[7]);

        return jobStat;
    }
    
    /**
     * Parses a single line for job status information
     *
     * @param line
     * @return
     */
    private JobStatus line2JobStatus(String line) {

        line = line.trim();
        String elements[] = line.split("\\s+");

        if(elements == null || elements.length < 8)
        	return null;
        
        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobId(elements[0]);
        jobStatus.setPartition(elements[1]);
        jobStatus.setName(elements[2]);
        jobStatus.setUser(elements[3]);
        jobStatus.setState(elements[4]);
        jobStatus.setTime(elements[5]);
        jobStatus.setNodes(elements[6]);
        jobStatus.setNodelist(elements[7]);

        return jobStatus;
    }
}
