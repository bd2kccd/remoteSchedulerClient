package edu.pitt.dbmi.ccd.connection;

import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 11:50 AM
 */
public class SlurmClient implements SchedulerClient {

    final Logger logger = LoggerFactory.getLogger(SchedulerClient.class);

    
    /**
     * @param remoteOutput
     * @param localDestination
     * @throws Exception
     */
    public void downloadOutput(String remoteOutput, String localDestination) throws Exception {
        // upload the check data space script file
        SshConnection sshConn = SshConnection.getInstance();
        sshConn.connect();
        sshConn.receiveFile(remoteOutput, localDestination);
        sshConn.close();
    	
     }
    
    /**
     * @param dataDirTemplateName - the velocity template filename that will be found in configuration's template path
     * @param dataProperties
     * @param remoteScriptFileName
     * @param localSource
     * @param remoteDestination
     * @throws Exception
     */
    public void uploadDataset(String dataDirTemplateName, Properties dataProperties, 
    		String remoteScriptFileName, String localSource, String remoteDestination) throws Exception {
    	
    	//Script to create a user space if not existing
        // create the job file needed for the run
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, Configuration.getInstance().getTemplatePath());
        ve.init();

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
        SshConnection sshConn = SshConnection.getInstance();
        sshConn.connect();
        sshConn.sendFile(outputFilename, remoteScriptFileName);
        sshConn.close();
    	
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
        System.out.println(scriptResult);
        sshConn.close();

        // upload the dataset
        sshConn.connect();
        sshConn.sendFile(localSource, remoteDestination);
        sshConn.close();

    }
    
    /**
     * @param jobTemplateName - the velocity template filename that will be found in configuration's template path
     * @param jobProperties
     * @param remoteScriptFileName
     * @return job id from the remote slurm scheduler
     * @throws Exception
     */
    public long submitJob(String jobTemplateName, Properties jobProperties, String remoteScriptFileName) throws Exception {

        // create the job file needed for the run
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, Configuration.getInstance().getTemplatePath());
        ve.init();

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
        SshConnection sshConn = SshConnection.getInstance();
        sshConn.connect();
        sshConn.sendFile(outputFilename, remoteScriptFileName);
        sshConn.close();

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
        sshConn.close();

        logger.info("Submitted script and assigned job id: " + jobId);

        return Long.parseLong(jobId);
    }

    public boolean remoteFileExisted(String remoteFileName) {
    	SshConnection sshConn = SshConnection.getInstance();

        String lsResult;
		try {
	        sshConn.connect();
			lsResult = sshConn.executeCommand("ls " + remoteFileName);
	        logger.info("lsResult: " + lsResult);
	        sshConn.close();
	        return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			sshConn.close();
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
        SshConnection sshConn = SshConnection.getInstance();

        sshConn.connect();
        String squeueResult = sshConn.executeCommand("squeue");
        sshConn.close();

        return str2JobStatuses(squeueResult);

    }

    /**
     * Retrieve a list of the user's all finished jobs on the remote system
     *
     * @return list of Job Status objects
     * @throws Exception
     */
    public List<JobStatus> getFinishedJobs() throws Exception {
    	SshConnection sshConn = SshConnection.getInstance();

        sshConn.connect();
        String sacctCmd = "sacct " +
        		" --format=JobID,Partition,JobName,User,state,Elapsed,NNodes,NodeList";
        sshConn.connect();
        String sacctResult = sshConn.executeCommand(sacctCmd);

        sshConn.close();

        return sacctResult2JobStatuses(sacctResult);
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

        SshConnection sshConn = SshConnection.getInstance();

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
            if (jobStatuses != null && jobStatuses.size() > 0) {
                jobStatus = jobStatuses.get(0);
            }

        }

        sshConn.close();
        return jobStatus;

    }

    /**
     * Cancel a single job
     *
     * @param jobId the job id on the remote system
     * @throws Exception
     */
    public void cancelJob(long jobId) throws Exception {
        SshConnection sshConn = SshConnection.getInstance();

        sshConn.connect();
        sshConn.executeCommand("scancel " + jobId);

        sshConn.close();

    }
    
    private List<JobStatus> sacctResult2JobStatuses(String s) {
    	/**
    	 *       JobID  Partition    JobName      User      State    Elapsed   NNodes        NodeList 
		 *------------ ---------- ---------- --------- ---------- ---------- -------- --------------- 
		 *135524               RM causal-cm+   chirayu  COMPLETED   00:00:01        1            r017 
		 *135524.batch                 batch            COMPLETED   00:00:01        1            r017 
    	 * 
    	 */
    	
    	ArrayList<JobStatus> jobStatuses = new ArrayList<JobStatus>();
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
     * Parses a slurm string for job information
     *
     * @param s
     * @return
     */
    private List<JobStatus> str2JobStatuses(String s) {
        /**
         *              JOBID PARTITION     NAME     USER ST       TIME  NODES NODELIST(REASON)
         *                     111189        LM run_RM2_  sczyrba  R    9:15:49      1 l004
         */

        ArrayList<JobStatus> jobStatuses = new ArrayList<JobStatus>();
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
     * Parses a single line for job status information
     *
     * @param line
     * @return
     */
    private JobStatus line2JobStatus(String line) {

        //System.out.println(line);
        //System.out.flush();
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
