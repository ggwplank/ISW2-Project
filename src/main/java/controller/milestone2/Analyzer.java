package controller.milestone2;

import model.MLProfile;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Analyzer {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");
    private static String nPofB20 = null;

    private Analyzer(){}


    public static Evaluation analyze(Instances trainingSet, Instances testingSet, MLProfile.CLASSIFIER classifier, MLProfile.SENSITIVITY sensitivity) {

        String output = String.format("Analyzing %s, with %s%n", classifier, sensitivity);
        LOGGER.info(output);

        // Initialize the classifier
        Classifier cls = initializeClassifier(classifier);

        Evaluation evaluation = null;
        try {
            if (sensitivity == MLProfile.SENSITIVITY.NO_COST_SENSITIVE) {
                // Classifier non cost sensitive

                // Train the classifier
                cls.buildClassifier(trainingSet);

                // Evaluate the model
                evaluation = new Evaluation(testingSet);
                evaluation.evaluateModel(cls, testingSet);

                //calculate NPofB
                nPofB20 = AcumeInfo.getNPofB20(testingSet,cls);

            } else {
                // Classifier cost-sensitive
                CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                CostMatrix costMatrix = createCostMatrix(1.0, 10.0);

                costSensitiveClassifier.setClassifier(cls);
                costSensitiveClassifier.setCostMatrix(costMatrix);
                costSensitiveClassifier.setMinimizeExpectedCost(sensitivity == MLProfile.SENSITIVITY.SENSITIVE_THRESHOLD);

                // Train the cost sensitive classifier
                costSensitiveClassifier.buildClassifier(trainingSet);

                // Evaluate the cost-sensitive model
                evaluation = new Evaluation(testingSet, costMatrix);
                evaluation.evaluateModel(costSensitiveClassifier, testingSet);

                //calculate NPofB
                nPofB20 = AcumeInfo.getNPofB20(testingSet,costSensitiveClassifier);
            }

            output = String.format("Analysis terminated%n");
            LOGGER.info(output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while analyzing", e);
        }
        return evaluation;
    }

    private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    private static Classifier initializeClassifier(MLProfile.CLASSIFIER classifier) {
        return switch (classifier) {
            case RANDOM_FOREST -> new RandomForest();
            case NAIVE_BAYES -> new NaiveBayes();
            default -> new IBk();
        };
    }

    public static String getnPofB20(){
        return nPofB20;
    }

}

