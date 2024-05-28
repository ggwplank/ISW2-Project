package controller.milestone1;

import model.ClassInstance;
import model.Commit;
import model.Ticket;
import model.Version;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricsCalculator {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static Git git;
    private static List<ClassInstance> instances = new ArrayList<>();

    //this is a utility class
    private MetricsCalculator(){

    }

    public static List<ClassInstance> getInstances(Git gitInstance, List<Commit> commits, List<Version> versions,
                                                   Map<String, List<Integer>> instancesMap) {

        String output = String.format("Calculating metrics%n");
        LOGGER.info(output);

        git = gitInstance;

        ArrayList<ClassInstance> tempList = new ArrayList<>();
        Map<String, Integer> tempMap = new HashMap<>();

        Version currentVersion = versions.getFirst();
        RevCommit previousCommit = null;

        // iteration through the commit list
        for (Commit commit : commits) {
            String author = commit.getAuthor();

            // version update if necessary
            if (!currentVersion.getName().equals(commit.getVersion().getName())) {
                updateInstances(instancesMap, tempList, tempMap);
                currentVersion = commit.getVersion();

                for (ClassInstance temp : tempList) {
                    temp.setVersion(currentVersion);
                    temp.addAge();
                }
            }

            // check if the commit is a fix commit
            boolean isFixCommit = !commit.getBuggyTickets().isEmpty();

            try {
                // manage file changes for the commit
                manageFile(commit, previousCommit, tempList, tempMap, currentVersion, author, isFixCommit);
            }catch(IOException e){
                LOGGER.log(Level.SEVERE, "Error while getting instances", e);
            }

            previousCommit = commit.getRev();
        }

        updateInstances(instancesMap, tempList, tempMap);

        setBuggy(commits, instancesMap);

        return instances;
    }


    private static void manageFile(Commit commit, RevCommit previousCommit, ArrayList<ClassInstance> tempList,
                                   Map<String, Integer> tempMap, Version version, String author, boolean isFixCommit) throws IOException {

        // list of file changes for the commit
        List<DiffEntry> diffEntryList = computeCommitDiff(previousCommit, commit.getRev());

        // iteration through each file in the commit
        for (String file : commit.getClasses()) {
            // extraction of file edits
            List<Edit> editList = extractFileEdits(diffEntryList, file);

            // if no edit is found for the file, then skip to the next one
            if (editList.isEmpty())
                continue;
            commit.addTouchedClass(file);

            // new Java class instance or get the existing one (if any)
            Integer isPresent = tempMap.get(file);
            ClassInstance classInstance = isPresent != null ? tempList.get(tempMap.get(file))
                    : new ClassInstance(file, version, commit.getDate());

            // update metrics for each edit in the file
            for (Edit edit : editList) {
                int deletedLines = edit.getEndA() - edit.getBeginA();
                int addedLines = edit.getEndB() - edit.getBeginB();

                classInstance.updateLoc(addedLines,deletedLines);
                classInstance.updateChurn(addedLines,deletedLines);

            }


            classInstance.addRevision();
            if(isFixCommit){
                classInstance.addFixCommit();
            }
            classInstance.updateAvgChurn();
            classInstance.updateAvgLocAdded();
            classInstance.addAuthor(author);

            // if it's new, the Java class instance is added to the list
            if (isPresent == null)
                tempList.add(classInstance);

            // Update the map with the file name and its index
            tempMap.computeIfAbsent(file, k -> tempList.size() - 1);
        }
    }


    private static void updateInstances(Map<String, List<Integer>> instancesMap, List<ClassInstance> tempList,
                                          Map<String, Integer> tempMap) {

        int currentSize = instances.size();

        // Update of stringListMap with new indexes for class names in temporaryJCIList
        for (ClassInstance instance : tempList) {
            String instanceName = instance.getName();

            // index in the combined list computed by adding current size to the original index
            instancesMap.computeIfAbsent(instanceName, k -> new ArrayList<>()).add(tempMap.get(instanceName) + currentSize);
        }

        // cloning instances from temporaryJCIList to javaClassInstances
        for (ClassInstance instance : tempList) {
            instances.add(new ClassInstance(instance));
        }
    }

    private static List<DiffEntry> computeCommitDiff(RevCommit oldCommit, RevCommit newCommit) throws IOException {
        List<DiffEntry> diffEntryList = null;

        // diff formatter to compute the differences
        DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream());
        diffFormatter.setRepository(git.getRepository());

        // difference between the old and new commits
        if (oldCommit != null) {
            // if there's an old commit, its tree is compared with the one associated to the new commit
            diffEntryList = diffFormatter.scan(oldCommit.getTree(), newCommit.getTree());
        } else {
            // if there's no old commit (initial commit), tree associated with the new commit is compared with an empty one
            ObjectReader objectReader = git.getRepository().newObjectReader();
            AbstractTreeIterator newCommitTree = new CanonicalTreeParser(null, objectReader, newCommit.getTree());
            AbstractTreeIterator oldCommitTree = new EmptyTreeIterator();
            diffEntryList = diffFormatter.scan(oldCommitTree, newCommitTree);
        }
        return diffEntryList;
    }

    private static List<Edit> extractFileEdits(List<DiffEntry> diffEntryList, String file) throws IOException {
        ArrayList<Edit> editArrayList = new ArrayList<>();

        DiffFormatter diffFormatter = new DiffFormatter(null);
        diffFormatter.setRepository(git.getRepository());

        for (DiffEntry diffEntry : diffEntryList) {
            if (diffEntry.toString().contains(file)) {
                // the diff entry is for the specified file, file header parsing to obtain edit info
                diffFormatter.setDetectRenames(true);
                EditList editList = diffFormatter.toFileHeader(diffEntry).toEditList();

                // each edit is added to the list
                for (Edit edit : editList)
                    editArrayList.add(edit);
                // forse è meglio editArrayList.addAll(editList); ?

            } else {
                // the diff entry is not for the specified file
                diffFormatter.setDetectRenames(false);
            }
        }

        return editArrayList;
    }


    private static void setBuggy(List<Commit> commits, Map<String, List<Integer>> mapInst) {
        for (Commit commit : commits) {
            for (Ticket ticket : commit.getBuggyTickets()) {
                for (String file : commit.getTouchedClasses()) {
                    // Indexes of the map
                    List<Integer> indexes = mapInst.getOrDefault(file, Collections.emptyList());
                    for (Integer index : indexes) {
                        checkIndexes(index,ticket);
                    }
                }
            }
        }
    }

    private static void checkIndexes(int index, Ticket ticket){
        if (index >= 0 && index < instances.size()) {
            ClassInstance instance = instances.get(index);
            if (instance.insideAV(ticket.getAv(), ticket.getFv())) {
                instance.setBuggy(true);
            }
        }
    }

}