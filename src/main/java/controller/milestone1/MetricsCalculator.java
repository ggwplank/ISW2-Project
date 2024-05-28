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
    private static final Logger LOGGER = Logger.getLogger(MetricsCalculator.class.getName());
    private static Git git;
    private static List<ClassInstance> instances = new ArrayList<>();

    public static List<ClassInstance> getInstances(Git gitInstance, List<Commit> commits, List<Version> versions,
                                                   Map<String, List<Integer>> instancesMap) {
        // Log the start of metrics calculation
        LOGGER.info("Calculating metrics");

        git = gitInstance;

        ArrayList<ClassInstance> tempList = new ArrayList<>();
        Map<String, Integer> tempMap = new HashMap<>();

        Version currentVersion = versions.get(0); // Get the first version
        RevCommit previousCommit = null;

        // Iterate through the commit list
        for (Commit commit : commits) {
            String author = commit.getAuthor();

            // Update version if necessary
            if (!currentVersion.getName().equals(commit.getVersion().getName())) {
                updateInstances(instancesMap, tempList, tempMap);
                currentVersion = commit.getVersion();

                // Update the version and age for each class instance
                for (ClassInstance temp : tempList) {
                    temp.setVersion(currentVersion);
                    temp.addAge();
                }
            }

            // Check if the commit is a fix commit
            boolean isFixCommit = !commit.getBuggyTickets().isEmpty();

            try {
                // Manage file changes for the commit
                manageFile(commit, previousCommit, tempList, tempMap, currentVersion, author, isFixCommit);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while getting instances", e);
            }

            previousCommit = commit.getRev();
        }

        // Final update for instances
        updateInstances(instancesMap, tempList, tempMap);
        setBuggy(commits, instancesMap);

        return instances;
    }

    private static void manageFile(Commit commit, RevCommit previousCommit, ArrayList<ClassInstance> tempList,
                                   Map<String, Integer> tempMap, Version version, String author, boolean isFixCommit) throws IOException {

        // List of file changes for the commit
        List<DiffEntry> diffEntryList = computeCommitDiff(previousCommit, commit.getRev());

        // Iterate through each file in the commit
        for (String file : commit.getClasses()) {
            // Extract file edits
            List<Edit> editList = extractFileEdits(diffEntryList, file);

            // If no edit is found for the file, skip to the next one
            if (editList.isEmpty()) continue;
            commit.addTouchedClass(file);

            // Get existing Java class instance or create a new one
            Integer isPresent = tempMap.get(file);
            ClassInstance classInstance = isPresent != null ? tempList.get(isPresent)
                    : new ClassInstance(file, version, commit.getDate());

            // Update metrics for each edit in the file
            for (Edit edit : editList) {
                int deletedLines = edit.getEndA() - edit.getBeginA();
                int addedLines = edit.getEndB() - edit.getBeginB();

                classInstance.updateLoc(addedLines, deletedLines);
                classInstance.updateChurn(addedLines, deletedLines);
            }

            classInstance.addRevision();
            if (isFixCommit) {
                classInstance.addFixCommit();
            }
            classInstance.updateAvgChurn();
            classInstance.updateAvgLocAdded();
            classInstance.addAuthor(author);

            // If it's new, add the Java class instance to the list and map
            if (isPresent == null) {
                tempList.add(classInstance);
                tempMap.put(file, tempList.size() - 1);
            }
        }
    }

    private static void updateInstances(Map<String, List<Integer>> instancesMap, List<ClassInstance> tempList,
                                        Map<String, Integer> tempMap) {

        int currentSize = instances.size();

        // Update instancesMap with new indexes for class names in tempList
        for (ClassInstance instance : tempList) {
            String instanceName = instance.getName();

            // Index in the combined list computed by adding current size to the original index
            instancesMap.computeIfAbsent(instanceName, k -> new ArrayList<>())
                    .add(tempMap.get(instanceName) + currentSize);
        }

        // Clone instances from tempList to instances
        for (ClassInstance instance : tempList) {
            instances.add(new ClassInstance(instance));
        }
    }

    private static List<DiffEntry> computeCommitDiff(RevCommit oldCommit, RevCommit newCommit) throws IOException {
        List<DiffEntry> diffEntryList;

        // Diff formatter to compute the differences
        try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
            diffFormatter.setRepository(git.getRepository());

            // Difference between the old and new commits
            if (oldCommit != null) {
                // Compare old commit's tree with new commit's tree
                diffEntryList = diffFormatter.scan(oldCommit.getTree(), newCommit.getTree());
            } else {
                // If no old commit (initial commit), compare new commit's tree with an empty tree
                ObjectReader objectReader = git.getRepository().newObjectReader();
                AbstractTreeIterator newCommitTree = new CanonicalTreeParser(null, objectReader, newCommit.getTree());
                AbstractTreeIterator oldCommitTree = new EmptyTreeIterator();
                diffEntryList = diffFormatter.scan(oldCommitTree, newCommitTree);
            }
        }
        return diffEntryList;
    }

    private static List<Edit> extractFileEdits(List<DiffEntry> diffEntryList, String file) throws IOException {
        ArrayList<Edit> editArrayList = new ArrayList<>();

        try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
            diffFormatter.setRepository(git.getRepository());

            for (DiffEntry diffEntry : diffEntryList) {
                if (diffEntry.toString().contains(file)) {
                    // The diff entry is for the specified file; parse file header to obtain edit info
                    diffFormatter.setDetectRenames(true);
                    EditList editList = diffFormatter.toFileHeader(diffEntry).toEditList();
                    editArrayList.addAll(editList);
                } else {
                    // The diff entry is not for the specified file
                    diffFormatter.setDetectRenames(false);
                }
            }
        }
        return editArrayList;
    }

    private static void setBuggy(List<Commit> commits, Map<String, List<Integer>> instancesMap) {
        for (Commit commit : commits) {
            for (Ticket ticket : commit.getBuggyTickets()) {
                for (String file : commit.getTouchedClasses()) {
                    // Indexes of the map
                    List<Integer> indexes = instancesMap.getOrDefault(file, Collections.emptyList());
                    for (Integer index : indexes) {
                        checkIndexes(index, ticket);
                    }
                }
            }
        }
    }

    private static void checkIndexes(int index, Ticket ticket) {
        if (index >= 0 && index < instances.size()) {
            ClassInstance instance = instances.get(index);
            if (instance.insideAV(ticket.getAv(), ticket.getFv())) {
                instance.setBuggy(true);
            }
        }
    }
}
