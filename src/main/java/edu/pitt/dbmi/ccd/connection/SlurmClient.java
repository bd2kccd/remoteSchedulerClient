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
import java.util.*;

/**
 * Author : Jeremy Espino MD
 * Created  4/1/16 11:50 AM
 */
public class SlurmClient implements SchedulerClient {

    final Logger logger = LoggerFactory.getLogger(SchedulerClient.class);

    /**
     * @param jobTemplateName - the velocity template filename that will be found in configuration's template path
     * @param jobProperties
     * @param remoteFileName
     * @return job id from the remote slurm scheduler
     * @throws Exception
     */
    public int submitJob(String jobTemplateName, Properties jobProperties, String remoteFileName) throws Exception {

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
        sshConn.sendFile(outputFilename, remoteFileName);
        sshConn.close();

        // submit job
        sshConn.connect();
        String sbatchResult = sshConn.executeCommand("sbatch " + remoteFileName);
        String jobId = "";
        if (sbatchResult.contains("Submitted batch job ")) {
            jobId = sbatchResult.substring("Submitted batch job ".length(), sbatchResult.length() - 1);
        }
        sshConn.close();

        logger.info("Submitted script and assigned job id: " + jobId);

        return Integer.parseInt(jobId);
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
     * Retrieve the status of a single job on the remote system
     *
     * @param jobId
     * @return Job Status on the remote system.  Null if doesn't exist
     * @throws Exception
     */
    public JobStatus getStatus(int jobId) throws Exception {

        JobStatus jobStatus = new JobStatus();

        SshConnection sshConn = SshConnection.getInstance();

        sshConn.connect();
        String squeueResult = sshConn.executeCommand("squeue --job " + jobId);


        List<JobStatus> jobStatuses = str2JobStatuses(squeueResult);

        if (jobStatuses != null && jobStatuses.size() > 0) {

            jobStatus = jobStatuses.get(0);

        } else {

            // job might have finished so get exit status
            sshConn.connect();
            String sacctResult = sshConn.executeCommand("sacct --job " + jobId);

            System.out.println(sacctResult);
            System.out.flush();

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
    public void cancelJob(int jobId) throws Exception {
        SshConnection sshConn = SshConnection.getInstance();

        sshConn.connect();
        sshConn.executeCommand("scancel " + jobId);

        sshConn.close();

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
            jobStatuses.add(js);

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

        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobId(elements[0]);
        jobStatus.setPartition(elements[1]);
        jobStatus.setName(elements[2]);
        jobStatus.setUser(elements[3]);
        jobStatus.setString(elements[4]);
        jobStatus.setTime(elements[5]);
        jobStatus.setNodes(elements[6]);
        jobStatus.setNodelist(elements[7]);

        return jobStatus;
    }
}
