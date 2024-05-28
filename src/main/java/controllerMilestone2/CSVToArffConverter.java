package controllerMilestone2;

import utils.Properties;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class CSVToArffConverter {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private final String projectName;

    public CSVToArffConverter(String projectName){
        this.projectName = projectName;
    }


    public String convert() {

        String csvPath;
        String arffPath = "";

        try{
            String output = String.format("Converting to ARFF for %s%n", projectName);
            LOGGER.info(output);

            csvPath = Properties.OUTPUT_DIRECTORY + projectName + "dataset.csv";
            arffPath = Properties.OUTPUT_DIRECTORY  + projectName + "dataset.arff";

            // load CSV
            CSVLoader loader = new CSVLoader();


            loader.setSource(new File(csvPath));

            Instances data = loader.getDataSet();

            //get instances object
            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);//set the dataset we want to convert
            //and save as ARFF
            saver.setFile(new File(arffPath));
            saver.writeBatch();
            output = "File converted";
            LOGGER.info(output);

        } catch (IOException e){
            String output = String.format("Failed conversion to ARFF for %s%n", projectName);
            LOGGER.info(output);
        }

        return arffPath;
    }

}
