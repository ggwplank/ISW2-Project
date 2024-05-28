package controllerMilestone1;

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
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


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
            versions = getVersions(Properties.OUTPUT_DIRECTORY + projectName + "VersionInfo.csv");
        }catch (IOException | InterruptedException | ParseException e){
            LOGGER.log(Level.SEVERE, "Error while retrieving versions", e);
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

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            if (version.has("releaseDate")) {
                String releaseDate = version.getString("releaseDate");
                String name = version.optString("name", "");
                String id = version.optString("id", "");

                csvContent.append(i + 1).append(",");
                csvContent.append(id).append(",");
                csvContent.append(name).append(",");
                csvContent.append(releaseDate).append("\n");
            }
        }

        String fileName = Properties.OUTPUT_DIRECTORY + projectName + "VersionInfo.csv";
        Files.write(Paths.get(fileName), csvContent.toString().getBytes());
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

        //don't read the first line
        boolean firstIteration = true;
        //assign the starter date as the end date of the previous version in the list
        for (Version version : versions) {
            if(firstIteration){
                version.setStartDate(null);
                firstIteration = false;
            } else{
                version.setStartDate(d);
            }
            d = version.getEndDate();
        }

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



