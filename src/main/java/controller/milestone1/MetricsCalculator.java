package controller.milestone1;

import model.ClassInstance;
import model.Commit;
import model.Ticket;
import model.Version;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MetricsCalculator {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static Git git;
    private static List<ClassInstance> instances = new ArrayList<>();

    //this is a utility class
    private MetricsCalculator(){

    }


    public static List<ClassInstance> getInstances(Git gitInstance, List<Commit> commits, List<Version> versions, Map<String, List<Integer>> mapInst) {
        String output = String.format("Calculating metrics %n");
        LOGGER.info(output);

        git = gitInstance;
        Version version = versions.getFirst();
        try {
            for (Commit commit : commits) {

                //set the boolean if there are or not buggy tickets
                boolean fixCommit = !commit.getBuggyTickets().isEmpty();

                //verify if the version is changed from the previous version
                if (!version.equals(commit.getVersion())) {
                    //update the instances age
                    updateInstances(mapInst);
                    version = commit.getVersion();
                    instances.forEach(ClassInstance::increaseAge);
                }

                manageFiles(commit, version, fixCommit);

            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving versions", e);
        }
        updateInstances(mapInst);

        setBuggy(commits, mapInst);
        return instances;
    }

    private static void manageFiles(Commit commit, Version version, boolean fixCommit) throws IOException {
        //check if the commit has any parent
        if (commit.getRev().getParentCount() == 0) {
            return;
        }
        RevCommit prevCommit = commit.getRev().getParent(0);
        //get differences from this commit and the previous
        List<DiffEntry> diffs = diff(prevCommit, commit.getRev());

        for (String file : commit.getClasses()) {
            //check if the commit has edited some files
            List<Edit> editedFiles = getEdits(diffs, file);
            if (editedFiles.isEmpty()) continue;

            //add the files to the edited by the commit
            commit.addTouchedClass(file);

            ClassInstance instance = findOrCreateInstance(file, version, commit.getDate());
            //update the metadata of the instance
            editedFiles.forEach(edit -> instance.updateRevisionsLoc(edit.getEndB() - edit.getBeginB(), edit.getEndA() - edit.getBeginA()));
            instance.updateAvgChurnAuthors(commit.getAuthor(), fixCommit);
        }
    }

    private static List<DiffEntry> diff(RevCommit oldCommit, RevCommit newCommit) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
            diffFormatter.setRepository(git.getRepository());

            if (oldCommit != null) {
                //check the differences between the old and the new commit
                return diffFormatter.scan(oldCommit.getTree(), newCommit.getTree());
            } else {
                //create a new tree to represent a new file or a directory and check the differences
                ObjectReader reader = git.getRepository().newObjectReader();
                AbstractTreeIterator newTree = new CanonicalTreeParser(null, reader, newCommit.getTree());
                AbstractTreeIterator oldTree = new EmptyTreeIterator();
                return diffFormatter.scan(oldTree, newTree);
            }
        }
    }

    private static List<Edit> getEdits(List<DiffEntry> diffs, String file) {
        try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDetectRenames(true);

            return diffs.stream()
                    //check if in the differences there is the specified file
                    .filter(diff -> diff.getNewPath().equals(file))
                    //map the modifies
                    .flatMap(diff -> {
                        try {
                            return diffFormatter.toFileHeader(diff).toEditList().stream();
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    //concatenate in a list
                    .toList();
        }
    }

    //search in the stream the name of file, if there is stop else create the istance with that file.
    private static ClassInstance findOrCreateInstance(String file, Version version, Date commitDate) {
        return instances.stream()
                .filter(inst -> inst.getName().equals(file))
                .findFirst()
                .orElseGet(() -> {
                    ClassInstance newInstance = new ClassInstance(file, version, commitDate);
                    instances.add(newInstance);
                    return newInstance;
                });
    }

    private static void updateInstances(Map<String, List<Integer>> mapInst) {
        int size = instances.size();
        instances.forEach(inst -> mapInst.computeIfAbsent(inst.getName(), k -> new ArrayList<>()).add(size));
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
