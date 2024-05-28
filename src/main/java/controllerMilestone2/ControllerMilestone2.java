package controllerMilestone2;

import utils.Properties;
import model.MLProfile;
import model.ModelEvaluation;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControllerMilestone2 {

    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    List<ModelEvaluation> modelEvaluations = new ArrayList<>();
    private final String projectName;
    int numberOfVersions;

    public ControllerMilestone2(String projectName) {
        this.projectName = projectName;
    }

    public void modelPerformanceEvaluation() throws Exception {
        String output = String.format("Starting performance evaluation for %s%n",projectName);
        LOGGER.info(output);

        //convert csv to arf
        CSVToArffConverter csvToArffConverter = new CSVToArffConverter(projectName);
        String datasetARFF = csvToArffConverter.convert();

        // Get the dataset
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(datasetARFF);
        Instances dataset = source.getDataSet();
        dataset.deleteStringAttributes();

        DataFilter dataFilter = new DataFilter();
        DatasetSplit datasetSplit = new DatasetSplit();
        int numberOfAttributes = dataset.numAttributes();
        numberOfVersions = dataset.attribute(0).numValues();
        modelEvaluations = new ArrayList<>();

        // Iterate for the feature selection
        for (MLProfile.FEATURE_SELECTION featureSelection : MLProfile.FEATURE_SELECTION.values()) {
            // Iterate for balancing
            for (MLProfile.BALANCING balancing : MLProfile.BALANCING.values()) {
                // Iterate for teh cost sensitive
                for (MLProfile.SENSITIVITY sensitivity : MLProfile.SENSITIVITY.values()) {
                    // Walk forward through the versions
                    for (int i = 1; i < numberOfVersions; i++) {
                        Instances training = datasetSplit.getTrainingSet(dataset, i, numberOfVersions);
                        Instances testing = datasetSplit.getTestingSet(dataset, i);

                        // Remove the version attribute and set the class attribute
                        training.deleteAttributeAt(0);
                        testing.deleteAttributeAt(0);
                        training.setClassIndex(numberOfAttributes - 2);
                        testing.setClassIndex(numberOfAttributes - 2);

                        // Apply feature selection and sampling
                        dataFilter.trainingData = training;
                        dataFilter.testingData = testing;
                        dataFilter.applyFeatureSelection(featureSelection);
                        dataFilter.applySampling(balancing);

                        training = dataFilter.trainingData;
                        testing = dataFilter.testingData;

                        // Evaluate the model for different classifier
                        evaluate(training,testing,sensitivity,featureSelection,balancing);
                    }
                }
            }
        }
        output = String.format("Performance evaluation for %s terminated%n",projectName);
        LOGGER.info(output);

        writeCSV();
    }


    private void evaluate(Instances training, Instances testing, MLProfile.SENSITIVITY sensitivity, MLProfile.FEATURE_SELECTION featureSelection, MLProfile.BALANCING balancing ) {
        for (MLProfile.CLASSIFIER classifier : MLProfile.CLASSIFIER.values()) {
            if (classifier == MLProfile.CLASSIFIER.NAIVE_BAYES && training.classAttribute().isNumeric()) {
                try {
                    // Naive bayes cannot handle numeric classes, so we convert them into nominal classes
                        NumericToNominal convert = new NumericToNominal();
                        convert.setAttributeIndices("" + (training.classIndex() + 1));
                        convert.setInputFormat(training);
                        training = Filter.useFilter(training, convert);
                        testing = Filter.useFilter(testing, convert);
                } catch (Exception e){
                    LOGGER.log(Level.SEVERE, "Error while converting to nominal classes for NAIVE BAYES", e);
                }
            }
            Evaluation evaluation = Analyzer.analyze(training, testing, classifier, sensitivity);
            modelEvaluations.add(new ModelEvaluation(classifier, featureSelection, balancing, sensitivity, evaluation));
        }
    }



    private void writeCSV(){

        String output = String.format("Writing results to CSV for %s%n",projectName);
        LOGGER.info(output);

        String path = Properties.OUTPUT_DIRECTORY + projectName + "evaluationResults.csv";

        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.append("Dataset,#TrainingRelease,Classifier,Feature Selection,Balancing,Sensitivity,Accuracy,Precision,Recall,AUC,Kappa\n");
            int numberOfTrainingRelease = 1;
            int counter = 0;

            for (ModelEvaluation modelEvaluation : modelEvaluations) {
                // After iterating over the three classifier reset the numberOfReleases
                if (counter >= 3) {
                    if (numberOfTrainingRelease >= numberOfVersions - 1) {
                        numberOfTrainingRelease = 1;
                    } else {
                        numberOfTrainingRelease++;
                    }
                    counter = 0;
                }

                // Extract the names of the attributes
                String classifier = modelEvaluation.getClassifier().toString();
                String featureSelection = modelEvaluation.getFeatureSelection().toString();
                String balancing = modelEvaluation.getBalancing().toString();
                String sensitivity = modelEvaluation.getSensitivity().toString();

                // Calculate the evaluation metrics
                Evaluation evaluation = modelEvaluation.getEvaluation();
                String accuracy = String.format(Locale.US, "%.3f", evaluation.pctCorrect());
                String precision = String.format(Locale.US, "%.3f", evaluation.precision(1));
                String recall = String.format(Locale.US, "%.3f", evaluation.recall(1));
                String auc = String.format(Locale.US, "%.3f", evaluation.areaUnderROC(1));
                String kappa = String.format(Locale.US, "%.3f", evaluation.kappa());

                // Create the sting to add
                String line = String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        projectName, numberOfTrainingRelease, classifier, featureSelection, balancing, sensitivity,
                        accuracy, precision, recall, auc, kappa);

                if (!precision.equals("NaN") && !auc.equals("NaN")) {
                    fileWriter.append(line);
                }
                counter++;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while writing results to CSV", e);
        }
        output = String.format("CSV file written for %s%n",projectName);
        LOGGER.info(output);
    }

}
