package controller.milestone1;

import model.Ticket;
import model.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicketRetriever {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static String projName;
    private static List<Ticket> tickets = new ArrayList<>();


    //this is a utility class and cannot be instanced
    private TicketRetriever() {
    }

    public static List<Ticket> retrieveTickets(List<Version> allVersions, String projectName) {
        String output = String.format("Retrieving tickets%n");
        LOGGER.info(output);

        projName = projectName;

        try {
            //get the tickets from jira
            getTickets(allVersions);

            //check the tickets
            checkTickets(tickets);

            //apply proportion
            proportion(allVersions, tickets);

            return tickets;
        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving Tickets", e);
        }
        return Collections.emptyList();
    }

    private static void getTickets(List<Version> allVersions) throws JSONException, ParseException, IOException {
        //starting position
        int startAt = 0;
        int maxResults = 1000;
        //number of problem found
        int total = 1;


        do {
            //create the url
            String url = buildUrl(startAt, maxResults);
            //take the JSON object
            JSONObject json = readJsonObject(url);
            assert json != null;
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            for (int i = 0; i < issues.length(); i++) {
                //extract the information from the JSON
                JSONObject issue = issues.getJSONObject(i);
                JSONObject fields = issue.getJSONObject("fields");

                String key = issue.getString("key");
                Date resolved = parseDate(fields.getString("resolutiondate"));
                Date created = parseDate(fields.getString("created"));
                JSONArray versions = fields.getJSONArray("versions");

                //get the affected versions, opening versions and fixed versions
                Version av = getAffectedVersion(versions);
                Version ov = VersionRetriever.findVersion(created, allVersions);
                Version fv = VersionRetriever.findVersion(resolved, allVersions);

                if (ov != null && fv != null) {
                    //add the tickets to the list
                    tickets.add(new Ticket(issue.getLong("id"), key, created, resolved, av, ov, fv));
                }
            }
            startAt += maxResults;
        } while (startAt < total);
    }

    private static String buildUrl(int startAt, int maxResults) {
        //create the URL
        String baseUrl = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt=";
        return baseUrl + startAt + "&maxResults=" + maxResults;
    }

    private static Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return sdf.parse(dateString);
    }

    private static Version getAffectedVersion(JSONArray versions) throws ParseException {
        if (!versions.isEmpty()) {
            JSONObject v = versions.getJSONObject(0);
            if (!v.isNull("releaseDate")) {
                SimpleDateFormat sdfSimple = new SimpleDateFormat("yyyy-MM-dd");
                Date dateAv = sdfSimple.parse(v.getString("releaseDate"));
                return new Version(v.getLong("id"), v.getString("name"), dateAv);
            }
        }
        return null;
    }


    private static JSONObject readJsonObject(String urlString) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        HttpURLConnection connection = null;

        try {
            //create the URI and convert to URL
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            //get the connection and send the request
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            //add to the buffer reader the output
            try (InputStream is = connection.getInputStream();
                 BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            }

            return new JSONObject(sb.toString());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error while reading the JSON file", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void checkTickets(List<Ticket> tickets) {
        int i = 0;
        while (i < tickets.size()) {
            Ticket ticket = tickets.get(i);
            //check if there is an affected version
            if (ticket.getAv() == null) {
                i++;
                continue;
            }

            //TODO check the null values
            //check if the ov's endDate comes before the av's endDate
System.out.println(ticket.getOv().getName());
            if (ticket.getOv().getEndDate().before(ticket.getAv().getEndDate())) {
                tickets.remove(i);
            } else {
                i++;
            }
        }
    }

    private static void proportion(List<Version> allVersions, List<Ticket> tickets) {

        float avSum = 0;
        float ovSum = 0;
        float fvSum = 0;
        float proportion = 0;

        for (Ticket ticket : tickets) {
            //check if the ticked hasn't an av
            if (ticket.getAv() != null) {
                //check if the ticked has already a fixed version
                if (ticket.getOv().getName().contains(ticket.getFv().getName())) continue;
                //calculate the proportion
                avSum += ticket.getAv().getNumRel();
                ovSum += ticket.getOv().getNumRel();
                fvSum += ticket.getFv().getNumRel();
                proportion = (fvSum - avSum) / (fvSum - ovSum);
            } else {
                //if the ticket hasn't an av, assign it with the proportion
                ticket.setAvByProportion(proportion, allVersions);
            }


        }
    }
}
