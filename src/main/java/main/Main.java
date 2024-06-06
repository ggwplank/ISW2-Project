package main;

import controller.milestone1.ControllerMilestone1;
import controller.milestone2.ControllerMilestone2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    private static final Logger LOGGER = Logger.getLogger("Analyzer");

    public static void main(String[] args) {


        ControllerMilestone1 controllerMilestone1 = new ControllerMilestone1("STORM");
        controllerMilestone1.createDataset();
        //controllerMilestone1.cutVersions();
    }
}





/*
        ControllerMilestone1 controllerMilestone1 = new ControllerMilestone1("STORM");
        controllerMilestone1.createDataset();
       controllerMilestone1.cutVersions();



        ControllerMilestone2 controllerMilestone2 = new ControllerMilestone2("STORM");

        try {
            controllerMilestone2.modelPerformanceEvaluation();
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error while evaluating model performance", e);
        }

 */




