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

    public static String getNPofB20(Instances testingSet, Classifier cls ) {

        String output = String.format("Calculating NPofB20%n");
        LOGGER.info(output);
        List<Acume> acumeList = new ArrayList<>();

        Acume acumeObject;

        try {
            // Print predictions for each instance in the testing set
            for (int i = 0; i < testingSet.numInstances(); i++) {


                Instance instance = testingSet.instance(i);

                boolean actual = false;
                int actualClass = (int) instance.classValue(); //actual
                if (actualClass == 1) {
                    actual = true;
                }

                double[] distribution = cls.distributionForInstance(instance);
                double predictedProbability = distribution[1]; //predicted
                // Truncate predictedProbability to three decimal places
                BigDecimal bd = new BigDecimal(predictedProbability).setScale(3, RoundingMode.DOWN);
                predictedProbability = bd.doubleValue();


                double size = instance.value(instance.attribute(1)); //size
                acumeObject = new Acume(i, size, predictedProbability, actual);
                acumeList.add(acumeObject);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in ACUME.csv writer", e);
        }

        writeAcumeCsv(acumeList);
        String NPofB20 = evaluateNPofB20();
        eliminateGeneratedFiles();
        return NPofB20;
    }


        private static void writeAcumeCsv(List<Acume> acumeList){
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
        }

    private static String getString(Acume acume){
        return String.format("%s,%s,%s,%s%n",acume.getId(),(int)acume.getSize(),acume.getPredicted(),acume.getActualStringValue());
    }

    private static String evaluateNPofB20(){
        try {
            // work directory
            File directory = new File(Properties.ACUME_DIRECTORY);

            // create the command
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "main.py", "NPofB");
            processBuilder.directory(directory);

            // start the process
            Process process = processBuilder.start();

            // wait the result
            process.waitFor();

            //extract data from csv
            return extractNPofB();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while evaluating NPofB", e);
            return null;
        }
    }

    private static String extractNPofB() {
        String csvFile = Properties.ACUME_DIRECTORY+"/EAM_NEAM_output.csv"; // Search for the output file
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            // ignore the first line
            reader.readNext();

            // read the second line with the data
            String[] nextLine = reader.readNext();
            //return the fourth column in (NPofB20)
            return nextLine[3];
        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Error while evaluating NPofB", e);
            return null;
        }
    }

    private static void eliminateGeneratedFiles(){
        File file1 = new File(Properties.ACUME_DIRECTORY+"csv/Acume.csv");
        File file2 = new File(Properties.ACUME_DIRECTORY+"EAM_NEAM_output.csv");
        File file3 = new File(Properties.ACUME_DIRECTORY+"norm_EAM_NEAM_output.csv");

        if(file1.exists()){
            file1.delete();
        }
        if(file2.exists()){
            file2.delete();
        }
        if(file3.exists()){
            file3.delete();
        }
    }

}
