package ControllerMilestone1;

import Utils.Properties;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import model.*;
import org.eclipse.jgit.api.Git;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



public class ControllerMilestone1 {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private final String projectName;
    String path;
    private Git git;
    private List<Version> versions;

    private static int buggyCounter = 0;
    private static int nonBuggyCounter = 0;


    public void createDataset() {
        List<Commit> commits;
        List<Ticket> tickets;

        List<ClassInstance> instances;
        Map<String, List<Integer>> mapInst = new HashMap<>();

        initializeProject();
        String output = String.format("Creating csv file for %s%n", projectName);
        LOGGER.info(output);

        //get the versions
        versions = VersionRetriever.retrieveVersions(projectName);
        versions.sort(Comparator.comparing(Version::getEndDate));

        //get the tickets
        tickets = TicketRetriever.retrieveTickets(versions, projectName);

        //get the commits
        commits = CommitRetriever.retrieveCommits(git, tickets, versions);

        //get the metrics
        assert commits != null;
        instances = MetricsCalculator.getInstances(git, commits, versions, mapInst);

        output = String.format("Assembling CSV file%n");
        LOGGER.info(output);

        try (FileWriter fileWriter = new FileWriter(Properties.OUTPUT_DIRECTORY + projectName + "dataset.csv")) {

            fileWriter.append("Version,Name,Size,Average loc added,Max loc added,Churn,MaxChurn,Average churn,Number of revisions,Number of fix,Number of authors,Age,Buggy");
            fileWriter.append("\n");

            for (ClassInstance instance : instances) {
                String line = getString(instance);
                fileWriter.append(line);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in dataset.csv writer", e);
        }

        output = String.format("Number of buggy classes is:%s%n", buggyCounter);
        LOGGER.info(output);
        output = String.format("Number of NON buggy classes is:%s%n", nonBuggyCounter);
        LOGGER.info(output);

        output = String.format("Dataset created!%n");
        LOGGER.info(output);
    }

    public ControllerMilestone1(String projectName) {
        this.projectName = projectName;
    }

    private void initializeProject() {
        try {
            System.setProperty("user.home", Properties.APACHE_DIRECTORY);

            String folderName = this.projectName.toLowerCase();
            this.path = System.getProperty("user.home");
            File dir = new File(this.path, folderName);
            this.git = Git.open(dir);
            this.git.pull();
            this.git.checkout();
        } catch (IOException var3) {
            LOGGER.log(Level.SEVERE, "Error in initialization phase", var3);
        }
    }


    private static String getString(ClassInstance instance) {
        int buggy = instance.isBuggy() ? 1 : 0;
        if (buggy == 1) {
            buggyCounter++;
        } else {
            nonBuggyCounter++;
        }

        // Create line for CSV file
        return String.format("%s,%s,%s,%s,%s,%s,%S,%s,%s,%s,%s,%s,%s%n",
                instance.getVersion().getName(),
                instance.getName(),

                instance.getSize(),
                instance.getAverageLocAdded(),
                instance.getMaxLocAdded(),
                instance.getChurn(),
                instance.getMaxChurn(),
                instance.getAverageChurn(),
                instance.getNumberOfRevisions(),
                instance.getNumberOfFix(),
                instance.getAuthors().size(),
                instance.getAge(),

                buggy);
    }


    public void cutVersions(){
        buggyCounter = 0;
        nonBuggyCounter = 0;

        List<Version> versionsCopy = versions;


        //take all the versions in the list and maintain only the second half

        int newLength = versionsCopy.size()/2;
        List<String> cuttedListWithID = new ArrayList<>();

        for(int i = 0; i < newLength; i++ ){
            cuttedListWithID.add(versionsCopy.getLast().getName());
            versionsCopy.removeLast();
        }

        //delete the versions that are not in the new list
        try {
            // delete the CSV
            CSVReader reader = new CSVReader(new FileReader(Properties.OUTPUT_DIRECTORY + projectName + "dataset.csv"));
            List<String[]> everyRow= reader.readAll();
            reader.close();

            List<String[]> filteredRow = new ArrayList<>();
            for (String[] row : everyRow) {
                if (!cuttedListWithID.contains(row[0])) {
                    filteredRow.add(row);

                    //count buggy and non-buggy instances
                    if(row[12].equals("1")){
                        buggyCounter++;
                    } else {
                        nonBuggyCounter++;
                    }
                }
            }

            // rewrite the CSV
            CSVWriter writer = new CSVWriter(new FileWriter(Properties.OUTPUT_DIRECTORY + "temp.csv"));
            writer.writeAll(filteredRow);
            writer.close();

            // replace the file

            Path originalFIle = Paths.get(Properties.OUTPUT_DIRECTORY + projectName + "dataset.csv");
            Path tempPath = Paths.get(Properties.OUTPUT_DIRECTORY + "temp.csv");
            Files.move(tempPath, originalFIle, StandardCopyOption.REPLACE_EXISTING);


            String output = String.format("Number of buggy classes is:%s%n", buggyCounter);
            LOGGER.info(output);
            output = String.format("Number of NON buggy classes is:%s%n", nonBuggyCounter);
            LOGGER.info(output);

        } catch (IOException | CsvException e) {
            LOGGER.log(Level.SEVERE, "Error while cutting the dataset");
        }
    }
}

