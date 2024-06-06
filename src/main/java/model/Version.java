package model;

import java.util.Date;

public class Version {
    private long id;
    private String name;
    private Date endDate;
    private Date startDate;
    private int numRel;

    public Version(long id, String name, Date startDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.numRel = -1;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getNumRel() {
        return numRel;
    }

    public void setNumRel(int numRel) {
        this.numRel = numRel;
    }

    public boolean isBefore(Version v) {
        return this.endDate.before(v.getEndDate());
    }

    public boolean isEqual(Version v) {
        return this.endDate.equals(v.getEndDate());
    }
}
