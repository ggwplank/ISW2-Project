package model;

public class Acume {
    private int id;
    private double size;
    private double predicted;
    private boolean actual;

    public Acume(int id, double size, double predicted, boolean actual) {
        this.id = id;
        this.size = size;
        this.predicted = predicted;
        this.actual = actual;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getPredicted() {
        return predicted;
    }

    public void setPredicted(double predicted) {
        this.predicted = predicted;
    }

    public boolean isActual() {
        return actual;
    }

    public void setActual(boolean actual) {
        this.actual = actual;
    }

    public String getActualStringValue() {
        return actual ? "Yes" : "No";
    }
}
