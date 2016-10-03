package edu.pitt.dbmi.ccd.connection.slurm;

/**
 * Author : Jeremy Espino MD Created 4/6/16 4:03 PM
 */
public class JobStatus {

    String jobId;
    String partition;
    String name;
    String user;
    String state;
    String time;
    String nodes;
    String nodelist;

    @Override
    public String toString() {
        return "JobStatus{"
                + "jobId=" + jobId
                + ", partition='" + partition + '\''
                + ", name='" + name + '\''
                + ", user='" + user + '\''
                + ", state='" + state + '\''
                + ", time='" + time + '\''
                + ", nodes=" + nodes
                + ", nodelist='" + nodelist + '\''
                + '}';
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodelist() {
        return nodelist;
    }

    public void setNodelist(String nodelist) {
        this.nodelist = nodelist;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
