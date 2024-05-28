package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Commit {
    private RevCommit rev;
    private String author;
    private Version version;
    private Date date;
    private List<String> classes;
    private List<String> touchedClasses;
    private List<Ticket> buggyTickets;

    public Commit(RevCommit rev, String author, Version version, Date date, List<String> classes, List<Ticket> buggyTickets) {
        this.rev = rev;
        this.author = author;
        this.version = version;
        this.date = date;
        this.classes = classes;
        this.touchedClasses = new ArrayList<>();
        this.buggyTickets = buggyTickets;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    public RevCommit getRev() {
        return rev;
    }

    public void setRev(RevCommit rev) {
        this.rev = rev;
    }

    public List<String> getTouchedClasses() {
        return touchedClasses;
    }

    public void setTouchedClasses(List<String> touchedClasses) {
        this.touchedClasses = touchedClasses;
    }

    public List<Ticket> getBuggyTickets() {
        return buggyTickets;
    }

    public void setBuggyTickets(List<Ticket> buggyTickets) {
        this.buggyTickets = buggyTickets;
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void addTouchedClass(String file) {
        this.touchedClasses.add(file);
    }
}
