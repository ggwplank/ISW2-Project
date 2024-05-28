package controllerMilestone2;


import model.MLProfile;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.supervised.instance.SMOTE;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataFilter {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    Instances trainingData;
    Instances testingData;

    // Apply the feature selection to the training and test set
    public void applyFeatureSelection(MLProfile.FEATURE_SELECTION featureSelection) {

        String output = String.format("Applying feature selection: %s%n", featureSelection);
        LOGGER.info(output);


        if (featureSelection.equals(MLProfile.FEATURE_SELECTION.BEST_FIRST)) {
            try {
                // Configure the feature selection with CfsSubsetEval and BestFirst
                AttributeSelection attributeSelection = new AttributeSelection();
                CfsSubsetEval evaluator = new CfsSubsetEval();
                BestFirst bestFirst = new BestFirst();

                attributeSelection.setEvaluator(evaluator);
                attributeSelection.setSearch(bestFirst);

                // Apply the feature selection to the training data
                attributeSelection.SelectAttributes(trainingData);

                // Get the indexes of the selected attributes
                int[] selectedAttributes = attributeSelection.selectedAttributes();

                // Configure the filter to remove the non-selected attributes
                Remove remove = new Remove();
                remove.setAttributeIndicesArray(selectedAttributes);
                remove.setInvertSelection(true);
                remove.setInputFormat(trainingData);

                // Apply the filter to the training and test set
                trainingData = Filter.useFilter(trainingData, remove);
                testingData = Filter.useFilter(testingData, remove);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while applying feature selection", e);
            }
        }
        // else if the selection is not BEST_FIRST, do nothing
    }


    //apply all the sampling techniques studied
    public void applySampling(MLProfile.BALANCING balancing) throws Exception {
        String output = String.format("Applying balancing: %s%n", balancing);
        LOGGER.info(output);

        // balancing cannot handle numeric classes, so we convert them into nominal classes
        if (trainingData.classAttribute().isNumeric()) {
            NumericToNominal convert = new NumericToNominal();
            convert.setAttributeIndices("" + (trainingData.classIndex() + 1));
            convert.setInputFormat(trainingData);
            trainingData = Filter.useFilter(trainingData, convert);
            testingData = Filter.useFilter(testingData, convert);
        }

        try {
            if (balancing.equals(MLProfile.BALANCING.OVERSAMPLING)) {
                // Filter Resample for the OVERSAMPLING
                Resample resample = new Resample();
                resample.setInputFormat(trainingData);

                //Set the options for the resamples
                //B is the bias (1.0 is balanced), Z is the oversampling with percentage calculated with the majorityClassPercentage method
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
                DecimalFormat decimalFormat = new DecimalFormat("#.##", symbols);
                String options = String.format("-B 1.0 -Z %s", decimalFormat.format(majorityClassPercentage()));
                resample.setOptions(Utils.splitOptions(options));

                // Apply the Resample to the training set
                trainingData = Filter.useFilter(trainingData, resample);

            } else if (balancing.equals(MLProfile.BALANCING.UNDERSAMPLING)) {
                // Filter Resample for the UNDERSAMPLING
                SpreadSubsample underSampling = new SpreadSubsample();
                underSampling.setInputFormat(trainingData);

                //Maintain a 1:1 ratio to the classes
                underSampling.setOptions(Utils.splitOptions("-M 1.0"));

                // Apply the under sampling filter to the training data
                trainingData = Filter.useFilter(trainingData, underSampling);

            } else if (balancing.equals(MLProfile.BALANCING.SMOTE)) {
                // Apply the SMOTE filter, synthetic minority oversampling
                SMOTE smote = new SMOTE();

                smote.setInputFormat(trainingData);

                // Apply the SMOTE filter to the training data
                trainingData = Filter.useFilter(trainingData, smote);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while applying balancing", e);
        }

    }


    private double majorityClassPercentage() {
        int numberOfBuggy = 0;

        // Create a copy of the training set
        Instances dataset = new Instances(trainingData);

        // Loop for counting the number of classes in the majority class
        for (Instance instance : dataset) {
            String buggy = instance.stringValue(dataset.numAttributes() - 1);
            if (buggy.equals("1")) {
                numberOfBuggy++;
            }
        }

        // Calculate the percentage
        double percentage = (100.0 * 2 * numberOfBuggy) / dataset.size();
        return (percentage >= 50) ? percentage : 100.0 - percentage;
    }
}
