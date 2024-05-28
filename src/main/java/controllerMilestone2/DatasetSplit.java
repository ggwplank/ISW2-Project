package controllerMilestone2;

import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetSplit {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    public Instances getTrainingSet(Instances dataset, int trainingRelease, int releases) throws Exception {
        String output = String.format("Creating training set%n");
        LOGGER.info(output);

        RemoveWithValues removeWithValues = null;

        try {
            // Calculate the range of the test set
            int range = releases - trainingRelease;
            int[] testingReleases = new int[range];

            // Populate the array with the index of tests release
            for (int i = 0; i < range - 1; i++) {
                testingReleases[i] = trainingRelease + i + 1;
            }
            output = String.format(String.valueOf(range));
            LOGGER.info(output);
            // Configure the filter to remove the test releases
            removeWithValues = new RemoveWithValues();
            removeWithValues.setAttributeIndex("1");
            removeWithValues.setNominalIndicesArr(testingReleases);
            removeWithValues.setInputFormat(dataset);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating training set", e);
        }
        // Apply the filter to create the training set
        assert removeWithValues != null;
        return Filter.useFilter(dataset, removeWithValues);
    }


    //Get the testing set based on the training releases
    public Instances getTestingSet(Instances dataset, int trainingRelease) throws Exception {
        String output = String.format("Creating testing set%n");
        LOGGER.info(output);
        RemoveWithValues removeWithValues = null;
        try {
            // Configure the filter to remove the instances of the training releases
            String options = String.format("-C 1 -L %d -V", trainingRelease +1);
            removeWithValues = new RemoveWithValues();
            removeWithValues.setOptions(Utils.splitOptions(options));
            removeWithValues.setInputFormat(dataset);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating testing set", e);
        }

        // Apply the filter to get the testing set
        assert removeWithValues != null;
        return Filter.useFilter(dataset, removeWithValues);
    }
}
