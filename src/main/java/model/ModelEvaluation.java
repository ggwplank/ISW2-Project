package model;

import weka.classifiers.Evaluation;

public class ModelEvaluation {
    private MLProfile.CLASSIFIER classifier;
    private MLProfile.FEATURE_SELECTION featureSelection;
    private MLProfile.BALANCING balancing;
    private MLProfile.SENSITIVITY sensitivity;
    private String nPofB20;
    private Evaluation evaluation;

    public ModelEvaluation(MLProfile.CLASSIFIER classifier, MLProfile.FEATURE_SELECTION featureSelection, MLProfile.BALANCING balancing, MLProfile.SENSITIVITY sensitivity, Evaluation evaluation, String nPofB20) {
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.balancing = balancing;
        this.sensitivity = sensitivity;
        this.evaluation = evaluation;
        this.nPofB20 = nPofB20;
    }

    public MLProfile.CLASSIFIER getClassifier() {
        return classifier;
    }

    public void setClassifier(MLProfile.CLASSIFIER classifier) {
        this.classifier = classifier;
    }

    public MLProfile.FEATURE_SELECTION getFeatureSelection() {
        return featureSelection;
    }

    public void setFeatureSelection(MLProfile.FEATURE_SELECTION featureSelection) {
        this.featureSelection = featureSelection;
    }

    public MLProfile.BALANCING getBalancing() {
        return balancing;
    }

    public void setBalancing(MLProfile.BALANCING balancing) {
        this.balancing = balancing;
    }

    public MLProfile.SENSITIVITY getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(MLProfile.SENSITIVITY sensitivity) {
        this.sensitivity = sensitivity;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public String getnPofB20() {
        return nPofB20;
    }

    public void setnPofB20(String nPofB20) {
        this.nPofB20 = nPofB20;
    }
}
