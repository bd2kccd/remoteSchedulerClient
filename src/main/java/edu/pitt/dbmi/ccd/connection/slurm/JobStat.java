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
package edu.pitt.dbmi.ccd.connection.slurm;

/**
 *
 * Jun 8, 2016 6:17:44 PM
 *
 * @author Chirayu (Kong) Wongchokprasitti
 *
 */
public class JobStat {

    String jobId;
    String partition;
    String state;
    String time;
    String start;
    String end;
    String allocCPUS;
    String allocNodes;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getAllocCPUS() {
        return allocCPUS;
    }

    public void setAllocCPUS(String allocCPUS) {
        this.allocCPUS = allocCPUS;
    }

    public String getAllocNodes() {
        return allocNodes;
    }

    public void setAllocNodes(String allocNodes) {
        this.allocNodes = allocNodes;
    }
}
