package model;

import java.util.Date;
import java.util.List;

public class Ticket {
    private long id;
    private String key;
    private Date created;
    private Date resolDate;
    private Version av;
    private Version ov;
    private Version fv;


    public Ticket(long id, String key, Date created, Date resolDate, Version av, Version ov, Version fv) {
        this.id = id;
        this.key = key;
        this.created = created;
        this.resolDate = resolDate;
        this.av = av;
        this.ov = ov;
        this.fv = fv;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getResolDate() {
        return resolDate;
    }

    public void setResolDate(Date resolDate) {
        this.resolDate = resolDate;
    }

    public Version getAv() {
        return av;
    }

    public void setAv(Version av) {
        this.av = av;
    }

    public Version getOv() {
        return ov;
    }

    public void setOv(Version ov) {
        this.ov = ov;
    }

    public Version getFv() {
        return fv;
    }

    public void setFv(Version fv) {
        this.fv = fv;
    }
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setAvByProportion(float proportion, List<Version> allVersions) {
        //get ov e fv release date
        float ovRd =  ov.getNumRel();
        float fvRd =  fv.getNumRel();

        //calculate the number of release to set av
        float posFloat = fvRd - (fvRd - ovRd) * proportion;
        int pos = Math.round(posFloat);
        if(pos < 1) pos = 1;

        //set av at pos-1
        av = allVersions.get(pos-1);
        av.setNumRel(pos);
    }
}
