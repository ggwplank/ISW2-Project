package ControllerMilestone2;

import Utils.Properties;
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

        String CSVPath;
        String ARFFPath = "";

        try{
            String output = String.format("Converting to ARFF for %s%n", projectName);
            LOGGER.info(output);

            CSVPath = Properties.OUTPUT_DIRECTORY + projectName + "dataset.csv";
            ARFFPath = Properties.OUTPUT_DIRECTORY  + projectName + "dataset.arff";

            // load CSV
            CSVLoader loader = new CSVLoader();


            loader.setSource(new File(CSVPath));

            Instances data = loader.getDataSet();

            //get instances object
            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);//set the dataset we want to convert
            //and save as ARFF
            saver.setFile(new File(ARFFPath));
            saver.writeBatch();
            output = "File converted";
            LOGGER.info(output);

        } catch (IOException e){
            String output = String.format("Failed conversion to ARFF for %s%n", projectName);
            LOGGER.info(output);
        }

        return ARFFPath;
    }

}
