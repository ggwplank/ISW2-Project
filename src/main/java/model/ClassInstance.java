package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassInstance {
    private String name;
    private Version version;
    private Date creationDate;
    private int numberOfRevisions;                                 //number of revisions form release 0
    private List<String> authors;                    //from release 0
    private int numberOfFix;                        //from release 0
    private int size;                               //lines of code from release 0
    private int averageLocAdded;                    //average loc added over revisions from release 0
    private int maxLocAdded;                        //maximum loc added over revisions from release 0
    private int churn;                              //sum over revisions of added and deleted LOC
    private int averageChurn;                       //average over revisions of added and deleted LOC
    private int maxChurn;                           //max over revisions of added and deleted LOC
    private int age;
    private boolean buggy;



    private int totalLocAdded;                      //this is not a metric for the course

    public ClassInstance(String name, Version version, Date creationDate) {
        this.name = name;
        this.version = version;
        this.creationDate = creationDate;
        this.numberOfRevisions = 0;
        this.authors = new ArrayList<>();
        this.numberOfFix = 0;
        this.size = 0;
        this.averageLocAdded = 0;
        this.maxLocAdded = 0;
        this.churn = 0;
        this.averageChurn = 0;
        this.maxChurn = 0;
        this.age = 0;
        this.buggy = false;

        this.totalLocAdded = 0;
    }

    public ClassInstance(ClassInstance instance) {
        this.name = instance.getName();
        this.version = instance.getVersion();
        this.creationDate = instance.getCreationDate();
        this.numberOfRevisions = instance.getNumberOfRevisions();
        this.authors = instance.authors;
        this.numberOfFix = instance.getNumberOfFix();
        this.size = instance.getSize();
        this.averageLocAdded = instance.getAverageLocAdded();
        this.maxLocAdded = instance.getMaxLocAdded();
        this.churn = instance.getChurn();
        this.averageChurn = instance.getAverageChurn();
        this.maxChurn = instance.getMaxChurn();
        this.age = instance.getAge();
        this.buggy = instance.isBuggy();

        this.totalLocAdded = instance.getTotalLocAdded();
    }

    public int getTotalLocAdded() {
        return totalLocAdded;
    }

    public void setTotalLocAdded(int totalLocAdded) {
        this.totalLocAdded = totalLocAdded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public int getNumberOfRevisions() {
        return numberOfRevisions;
    }

    public void setNumberOfRevisions(int numberOfRevisions) {
        this.numberOfRevisions = numberOfRevisions;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public int getNumberOfFix() {
        return numberOfFix;
    }

    public void setNumberOfFix(int numberOfFix) {
        this.numberOfFix = numberOfFix;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getAverageLocAdded() {
        return averageLocAdded;
    }

    public void setAverageLocAdded(int averageLocAdded) {
        this.averageLocAdded = averageLocAdded;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(int averageChurn) {
        this.averageChurn = averageChurn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }


    public void addAge(){
        this.age++;
    }

    public void updateLoc(int added, int deleted){

        // Calculate the net change in lines
        int netChange = added - deleted;

        // Update the total size
        this.size += netChange;

        //update total loc added
        this.totalLocAdded += added;

        // Update the maximum number of lines added
        this.maxLocAdded = Math.max(this.maxLocAdded, added);
    }

    public void updateChurn(int added, int deleted){
        // Calculate the net change in lines
        int netChange = added - deleted;

        // Update the churn (total change in lines)
        this.churn += netChange;

        // Update the maximum change in lines
        this.maxChurn = Math.max(this.maxChurn, netChange);
    }

    public void updateAvgChurn(){
        // Calculate the average churn per revision
        if (numberOfRevisions != 0) {
            this.averageChurn = this.churn / this.numberOfRevisions;
        } else {
            this.averageChurn = 0; // In case NR is zero, set the average churn to zero to avoid division by zero
        }

    }

    public void updateAvgLocAdded(){
        //Calculate the average loc added
        if(this.numberOfRevisions != 0){
            this.averageLocAdded = this.totalLocAdded/this.numberOfRevisions;
        } else {
            this.averageLocAdded = 0;
        }
    }


    public void addAuthor(String author) {
        // If the author of the revision is new, add the author to the list of authors
        if (!authors.contains(author)) {
            this.authors.add(author);
        }
    }

     public void addRevision(){
            // Update the total number of revisions
            numberOfRevisions++;
    }

    public void addFixCommit(){
        // update the total number of fixes
            numberOfFix++;
    }



    //check if the version is in the affected versions
    public boolean insideAV(Version iv, Version fv) {
        return (!version.isBefore(iv) || version.isEqual(iv)) && version.isBefore(fv);
    }
}
