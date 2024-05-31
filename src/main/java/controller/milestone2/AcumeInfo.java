package controller.milestone2;

import model.Acume;
import utils.Properties;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;


import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class AcumeInfo {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");


    private AcumeInfo(){}

    public static String buildAcumeCSV(Instances testingSet, Classifier cls ) {

        List<Acume> acumeList = new ArrayList<>();

        Acume acumeObject;

        try {
            // Print predictions for each instance in the testing set
            for (int i = 0; i < testingSet.numInstances(); i++) {


                Instance instance = testingSet.instance(i);

                boolean actual = false;
                int actualClass = (int) instance.classValue();
                if (actualClass == 1) {
                    actual = true;
                }

                double[] distribution = cls.distributionForInstance(instance);
                double predictedProbability = distribution[1]; // Assuming binary classification (class index 1 for positive class)
                // Truncate predictedProbability to three decimal places
                BigDecimal bd = new BigDecimal(predictedProbability).setScale(3, RoundingMode.DOWN);
                predictedProbability = bd.doubleValue();


                double size = instance.value(1);

                acumeObject = new Acume(i, size, predictedProbability, actual);
                acumeList.add(acumeObject);
            }
        }catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error in ACUME.csv writer", e);
        }

        String output = String.format("Assembling CSV file fo ACUME%n");
        LOGGER.info(output);

        try (FileWriter fileWriter = new FileWriter(Properties.ACUME_DIRECTORY+"csv/Acume.csv")) {

            fileWriter.append("ID,Size,Predicted,Actual");
            fileWriter.append("\n");

            for (Acume acume : acumeList) {
                String line = getString(acume);
                fileWriter.append(line);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in ACUME.csv writer", e);
        }

        output = String.format(".csv file created!%n");
        LOGGER.info(output);

        return evaluateNPofB20();

    }

    private static String getString(Acume acume){
        return String.format("%s,%s,%s,%s%n",acume.getId(),(int)acume.getSize(),acume.getPredicted(),acume.getActualStringValue());
    }

    private static String evaluateNPofB20(){
        try {
            // Imposta la directory di lavoro
            File directory = new File(Properties.ACUME_DIRECTORY); // Sostituisci con il percorso assoluto corretto

            // Costruisci il comando
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "main.py", "NPofB");
            processBuilder.directory(directory);

            // Avvia il processo
            Process process = processBuilder.start();

            // Attendi la fine del processo e ottieni il codice di uscita
            process.waitFor();

            //extract data from csv
            return extractNPofB();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while evaluating NPofB", e);
            return null;
        }
    }

    private static String extractNPofB() {
        String csvFile = Properties.ACUME_DIRECTORY+"/EAM_NEAM_output.csv"; // Sostituisci con il percorso corretto del tuo file CSV
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            // Leggi e ignora la prima riga (header)
            reader.readNext();

            // Leggi la seconda riga (dati)
            String[] nextLine = reader.readNext();
            return nextLine[3];
        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Error while evaluating NPofB", e);
            return null;
        }

    }
}
