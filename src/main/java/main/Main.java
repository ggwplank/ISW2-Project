package main;

import controller.milestone1.ControllerMilestone1;
import controller.milestone2.ControllerMilestone2;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    public static void main(String[] args){

        ControllerMilestone1 bookkeeperControllerMilestone1 = new ControllerMilestone1("BOOKKEEPER");
        bookkeeperControllerMilestone1.createDataset();
        bookkeeperControllerMilestone1.cutVersions();

        /*
                ControllerMilestone1 bookkeeperControllerMilestone1 = new ControllerMilestone1("BOOKKEEPER");
        bookkeeperControllerMilestone1.createDataset();
        bookkeeperControllerMilestone1.cutVersions();

        ControllerMilestone2 bookkeeperControllerMilestone2 = new ControllerMilestone2("BOOKKEEPER");

        try {
            bookkeeperControllerMilestone2.modelPerformanceEvaluation();
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error while evaluating model performance on BOOKKEEPER", e);
        }



        ControllerMilestone1 stormController = new ControllerMilestone1("STORM");
        stormController.createDataset();

        ControllerMilestone2 stormControllerMilestone2 = new ControllerMilestone2("STORM");
        try {
            stormControllerMilestone2.modelPerformanceEvaluation();
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error while evaluating model performance on STORM", e);
        }

         */
    }
}
