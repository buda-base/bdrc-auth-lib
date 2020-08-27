package io.bdrc.auth;

import java.util.Date;

public class ConnectionJob {

    public String jobId;
    public Date date;
    public String state;
    public String location;

    public ConnectionJob(String jobId, Date date, String state, String location) {
        super();
        this.jobId = jobId;
        this.date = date;
        this.state = state;
        this.location = location;
    }

    public String getJobId() {
        return jobId;
    }

    public Date getDate() {
        return date;
    }

    public String getState() {
        return state;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "ConnectionJob [jobId=" + jobId + ", date=" + date + ", state=" + state + ", location=" + location + "]";
    }

}
