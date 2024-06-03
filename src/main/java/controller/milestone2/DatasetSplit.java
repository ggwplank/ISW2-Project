package controller.milestone2;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveWithValues;
import weka.core.Utils;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatasetSplit {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    public Instances getTrainingSet(Instances dataset, int trainingRelease, int releases) throws Exception {
        String output = String.format("Creating training set%n");
        LOGGER.info(output);

        RemoveWithValues removeWithValues = new RemoveWithValues();

        try {
            // Calculate the range of the test set
            int range = releases - trainingRelease;
            int[] testingReleases = new int[range];

            // Populate the array with the index of tests release
            for (int i = 0; i < range; i++) {
                testingReleases[i] = trainingRelease + i + 1;
            }

            // Configure the filter to remove the test releases
            StringBuilder nominalIndices = new StringBuilder();
            for (int i = 0; i < testingReleases.length; i++) {
                nominalIndices.append(testingReleases[i]);
                if (i < testingReleases.length - 1) {
                    nominalIndices.append(",");
                }
            }

            removeWithValues.setAttributeIndex("1");
            removeWithValues.setNominalIndices(nominalIndices.toString());
            removeWithValues.setInputFormat(dataset);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating training set", e);
            throw e;
        }

        // Apply the filter to create the training set
        return Filter.useFilter(dataset, removeWithValues);
    }

    public Instances getTestingSet(Instances dataset, int trainingRelease) throws Exception {
        String output = String.format("Creating testing set%n");
        LOGGER.info(output);

        RemoveWithValues removeWithValues = new RemoveWithValues();

        try {
            // Configure the filter to remove the instances of the training releases
            String options = String.format("-C 1 -L %d -V", trainingRelease + 1);
            removeWithValues.setOptions(Utils.splitOptions(options));
            removeWithValues.setInputFormat(dataset);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating testing set", e);
            throw e;
        }

        // Apply the filter to get the testing set
        return Filter.useFilter(dataset, removeWithValues);
    }
}
