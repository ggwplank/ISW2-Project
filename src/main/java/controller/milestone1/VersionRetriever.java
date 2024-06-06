package controller.milestone1;

import utils.Properties;
import model.Version;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static utils.Properties.OUTPUT_DIRECTORY;


public class VersionRetriever {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static Map<LocalDateTime, String> releaseNames;
    private static Map<LocalDateTime, String> releaseID;
    private static List<LocalDateTime> releases;

    private VersionRetriever() {                                                                                        //With this is impossible to create instances of this class, this is a utility class
    }

    public static List<Version> retrieveVersions(String projectName){
        String output = String.format("Retrieving versions%n");
        LOGGER.info(output);

        List<Version> versions = null;
        try{
            getReleaseInfo(projectName);
            versions = getVersions(OUTPUT_DIRECTORY + projectName + "VersionInfo.csv");
        }catch (IOException | InterruptedException | ParseException e){
            LOGGER.log(Level.SEVERE, "Error while retrieving versions", e);
            Thread.currentThread().interrupt();
        }
        return versions;
    }

    private static void getReleaseInfo(String projectName) throws IOException, InterruptedException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String jsonStr = response.body();

        JSONObject json = new JSONObject(jsonStr);
        JSONArray versions = json.getJSONArray("versions");


        //write the .csv file
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Index,Version ID,Version Name,Date\n");

        int counter = 0;

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            if (version.has("releaseDate")) {
                String releaseDate = version.getString("releaseDate");
                String name = version.optString("name", "");
                String id = version.optString("id", "");

                counter += 1;

                csvContent.append(counter).append(",");
                csvContent.append(id).append(",");
                csvContent.append(name).append(",");
                csvContent.append(releaseDate).append("\n");
            }
        }


        String fileName = OUTPUT_DIRECTORY + projectName + "VersionInfo.csv";
        Files.write(Paths.get(fileName), csvContent.toString().getBytes());
        sortCsvByReleaseDate(projectName);
    }

    private static void sortCsvByReleaseDate(String projectName) throws IOException {
        String fileName = OUTPUT_DIRECTORY + projectName + "VersionInfo.csv";
        List<String> lines = Files.readAllLines(Paths.get(fileName));

        // Extract header
        String header = lines.remove(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Sort lines by the release date (4th column)
        List<String[]> sortedLines = lines.stream()
                .map(line -> line.split(","))
                .sorted(Comparator.comparing(parts -> LocalDate.parse(parts[3], formatter)))
                .collect(Collectors.toList());

        // Update indices
        for (int i = 0; i < sortedLines.size(); i++) {
            sortedLines.get(i)[0] = String.valueOf(i + 1);
        }

        // Write sorted data back to the CSV file
        List<String> outputLines = new ArrayList<>();
        outputLines.add(header); // Re-add header
        outputLines.addAll(sortedLines.stream()
                .map(parts -> String.join(",", parts))
                .collect(Collectors.toList()));

        Files.write(Paths.get(fileName), outputLines);
    }


    private static List<Version> getVersions(String pathVersion) throws IOException, ParseException {
        List<Version> versions = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d = null;

        try (BufferedReader in = new BufferedReader(new FileReader(pathVersion))) {
            String line;

            //skip the header
            String header = in.readLine();
            if (header == null){
                LOGGER.log(Level.SEVERE, "No header in the csv file");
            }

            while ((line = in.readLine()) != null) {
                String[] x = line.split(",");
                d = sdf.parse(x[3]);
                versions.add(new Version(Long.parseLong(x[1]), x[2], d));
            }
        }



        //set the end date for every version with the start of the next version in the file
        for(int i=0; i < versions.size() -1; i++ ){
            versions.get(i).setEndDate(versions.get(i+1).getStartDate());
        }

        //we do not consider the last version that is the current version of the project
        versions.removeLast();

        return versions;
    }

    public static Version findVersion(Date date, List<Version> versions){
        for (Version version : versions) {
            if (!version.getEndDate().before(date)){
                return version;
            }
        }
        return null;
    }

}



