package model;

public class MLProfile {
    private MLProfile() {
        throw new IllegalStateException("ProfileML class must not be instantiated");
    }

    public enum CLASSIFIER {
        RANDOM_FOREST,
        NAIVE_BAYES,
        IBK;
    }

    public enum FEATURE_SELECTION {
        NO_SELECTION,
        BEST_FIRST;
    }

    public enum BALANCING {
        NO_SAMPLING,
        OVERSAMPLING,
        UNDERSAMPLING,
        SMOTE;
    }

    public enum SENSITIVITY {
        NO_COST_SENSITIVE,
        SENSITIVE_THRESHOLD,
        SENSITIVE_LEARNING;
    }
}

