package controller.milestone1;

import model.Commit;
import model.Ticket;
import model.Version;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommitRetriever {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static Git git;

    //this is a utility class anc cannot be instanced
    private CommitRetriever(){}

    public static List<Commit> retrieveCommits(Git git, List<Ticket> tickets, List<Version> versions) throws JSONException {
        String output = String.format("Retrieving commits%n");
        LOGGER.info(output);

        try {
        CommitRetriever.git = git;
        List<Commit> commits = new ArrayList<>();
        Iterable<RevCommit> log = git.log().call();



            for (RevCommit rev : log) {
                //author of the commit
                PersonIdent authorIdent = rev.getAuthorIdent();
                String author = authorIdent.getName();
                //creation date of the commit
                Date creationTime = authorIdent.getWhen();

                //version when the commit was created
                Version version = VersionRetriever.findVersion(creationTime, versions);
                if (version == null) continue;

                //create the commit
                Commit commit = createCommit(rev, author, version, creationTime, tickets);
                commits.add(commit);
            }

            commits.sort(Comparator.comparing(Commit::getDate).reversed());
            return commits;

        }catch (JSONException | IOException|GitAPIException ex) {
            LOGGER.log(Level.SEVERE, "Error while retrieving commits", ex);
        }
        return Collections.emptyList();
    }

    private static Commit createCommit(RevCommit rev, String author, Version version, Date creationTime, List<Ticket> tickets) throws JSONException, IOException {
        //List of classes modified by the commit
        List<String> classes = getFilesCommit(rev);
        //List of tickets tagged buggy by the commit
        List<Ticket> buggyTickets = getBuggyTickets(rev, tickets);

        return new Commit(rev, author, version, creationTime, classes, buggyTickets);
    }

    private static List<String> getFilesCommit(RevCommit commit) throws  IOException {
        List<String> listOfFiles = new ArrayList<>();
        //navigate the files with the tree
        ObjectId treeId = commit.getTree().getId();
        TreeWalk treeWalk = new TreeWalk(git.getRepository());
        treeWalk.reset(treeId);
        //explore the tree till the leaf
        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
            } else {
                //get the path of the java class
                if (treeWalk.getPathString().endsWith(".java")) {
                    String file = treeWalk.getPathString();
                   listOfFiles.add(file);
                }
            }
        }
        return listOfFiles;
    }
    private static List<Ticket> getBuggyTickets(RevCommit commit, List<Ticket> tickets){
        List<Ticket> listOfBuggyTickets = new ArrayList<>();
        //get the message from the commit
        String message = commit.getFullMessage();
        //if the commit contains the key "buggy", add it to the list
        for(Ticket ticket : tickets) {
            if(message.contains(ticket.getKey())) {
                listOfBuggyTickets.add(ticket);
            }
        }
        return listOfBuggyTickets;
    }

}
