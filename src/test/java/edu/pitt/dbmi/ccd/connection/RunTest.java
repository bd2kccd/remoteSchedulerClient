/*
 * Copyright (C) 2015 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.pitt.dbmi.ccd.connection;

import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;

import edu.pitt.dbmi.ccd.connection.SlurmClient;
import edu.pitt.dbmi.ccd.connection.slurm.JobStatus;

/**
 * 
 * May 17, 2016 3:11:32 PM
 * 
 * @author Chirayu (Kong) Wongchokprasitti
 *
 */
public class RunTest {

	private static long jobId;

	@Autowired
    private static SlurmClient client;
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//SlurmClient client = new SlurmClient();

		Properties p = new Properties();

		System.out.println("===transferDataset===");
		
		p.setProperty("causalUser", "kong");
		try {
			client.uploadDataset(
					"/home/chirayu/slurm/job_templates/causalUserWorkspace.vm", 
					p,"~/checkUserDir.sh","/home/chirayu/slurm/Retention.txt", 
					"/pylon1/bi4s84p/chirayu/kong/Retention.txt");
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		System.out.println("===testSubmitJob===");

		p.setProperty("inputFile", "/pylon1/bi4s84p/chirayu/kong/Retention.txt");
		p.setProperty("outputFile", "fgs-retention-output");
		try {
			jobId = client.submitJob("/home/chirayu/slurm/job_templates/slurmCausalJob.vm", p, "~/testremotefile.sh");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("job id " + jobId);

		System.out.println("===testGetStatus===");

		JobStatus js;
		try {
			js = client.getStatus(jobId);
			if (js != null) {
				System.out.println(js.toString());
			} else {
				System.out.println("jobId: " + jobId + "'s status not found!");
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println("===getQueueStatus===");

		List<JobStatus> jobStatuses;
		try {
			jobStatuses = client.getQueueStatus();
			for (JobStatus j : jobStatuses) {
				System.out.println(j.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("===getFinishedJobs===");

		try {
			jobStatuses = client.getFinishedJobs();
			for (JobStatus j : jobStatuses) {
				System.out.println(j.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*System.out.println("===cancelJob===");

		try {
			client.cancelJob(jobId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

}
