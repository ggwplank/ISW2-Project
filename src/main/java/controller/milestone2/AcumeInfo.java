package controller.milestone2;

import model.Acume;
import utils.Properties;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class AcumeInfo {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");


    private AcumeInfo(){}

    public static void buildAcumeCSV(Instances testingSet, Classifier cls) {

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
                double size = instance.value(0);

                acumeObject = new Acume(i, size, predictedProbability, actual);
                acumeList.add(acumeObject);
            }
        }catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error in ACUME.csv writer", e);
        }

        String output = String.format("Assembling CSV file fo ACUME%n");
        LOGGER.info(output);

        try (FileWriter fileWriter = new FileWriter(Properties.OUTPUT_DIRECTORY+"Acume.csv")) {

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

        System.exit(1);

    }

    private static String getString(Acume acume){
        return String.format("%s,%s,%s,%s%n",acume.getId(),acume.getSize(),acume.getPredicted(),acume.getActualStringValue());
    }


}
