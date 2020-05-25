package main;

import resources.Project;
import resources.Release;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

import java.io.FileWriter;
import java.util.Scanner;
import java.util.logging.Logger;

public class Measurements {
    private static final Logger logger = Logger.getLogger(Measurements.class.getName());
    private static int numInstances;

    public static void main(String[] args) throws Exception {

        Project myProject = new Project(askForProjectName());

        //Extracting and validating project releases, sorted by date
        myProject.extractReleases();

        //Extracting tickets and setting IV, OV and FV.
        myProject.extractTickets();

        //Extracting commits with corresponding ticket, sorted by date.
        //Removed not computable tickets
        myProject.extractCommits();

        //Extracting all the files for every valid release
        myProject.extractFiles();
        numInstances = myProject.writeBugginess();
        applyWeka(myProject);
    }

    private static Instances[] computeSelection(Instances training, Instances testing) throws Exception {
        weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval subsetEval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        filter.setEvaluator(subsetEval);
        filter.setSearch(search);

        //Applying attributes filter to new training and testing sets
        filter.setInputFormat(training);
        Instances filteredTraining = Filter.useFilter(training, filter);
        Instances filteredTesting = Filter.useFilter(testing, filter);

        int numFilteredAttributes = filteredTraining.numAttributes();

        filteredTraining.setClassIndex(numFilteredAttributes - 1);
        filteredTesting.setClassIndex(numFilteredAttributes - 1);

        return new Instances[]{filteredTraining, filteredTesting};
    }

    private static void computeNaiveBayes(String projectName, int numTraining, FileWriter csvWriter, Instances training, Instances testing, double ratio) throws Exception {
        NaiveBayes naiveBayes = new NaiveBayes();
        FilteredClassifier fc = new FilteredClassifier();
        Instances[] filteredData;
        Instances actTraining;
        Instances actTesting;
        Evaluation eval;
        String sampling = "";

        fc.setClassifier(naiveBayes);

        for (int n=1; n<5; n++) {
            actTraining = training;
            actTesting = testing;

            switch (n) {
                //No Sampling
                case 1:
                    sampling = "No Sampling";
                    break;
                //Undersampling
                case 2:
                    sampling = "Oversampling";

                    Resample resample = new Resample();
                    resample.setNoReplacement(false);
                    String[] opt1 = new String[]{ "-B", "1.0"};
                    String[] opt2 = new String[]{ "-Z", Double.toString(ratio*100*2)};
                    resample.setOptions(opt1);
                    resample.setOptions(opt2);

                    resample.setInputFormat(actTraining);
                    fc.setFilter(resample);

                    break;
                //Oversampling
                case 3:
                    sampling = "Undersampling";

                    SpreadSubsample spreadSubsample = new SpreadSubsample();
                    String[] opt = new String[]{ "-M", "1.0"};
                    spreadSubsample.setOptions(opt);

                    spreadSubsample.setInputFormat(actTraining);
                    fc.setFilter(spreadSubsample);

                    break;
                //SMOTE
                case 4:
                    sampling = "SMOTE";

                    SMOTE smote = new SMOTE();
                    smote.setInputFormat(actTraining);
                    fc.setFilter(smote);

                    break;
            }

            for (int m=1;m<3;m++) {
                csvWriter.append(projectName);
                csvWriter.append(",");
                csvWriter.append(Integer.toString(numTraining));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f",(double)training.size()/(double)numInstances));
                csvWriter.append(",");
                csvWriter.append("Naive Bayes");
                csvWriter.append(",");
                csvWriter.append(sampling);
                csvWriter.append(",");

                switch (m) {
                    //No Selection
                    case 1:
                        csvWriter.append("No Selection");
                        csvWriter.append(",");
                        break;
                    //Backward search
                    case 2:
                        csvWriter.append("Backward Search");
                        csvWriter.append(",");

                        filteredData = computeSelection(actTraining, actTesting);
                        actTraining = filteredData[0];
                        actTesting = filteredData[1];
                        break;
                }

                if (!sampling.equals("No Sampling")) {
                    fc.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(fc, actTesting);
                } else {
                    naiveBayes.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(naiveBayes, actTesting);
                }

                csvWriter.append(String.format("%.3f", eval.numTruePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalsePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numTrueNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalseNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.precision(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.recall(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.areaUnderROC(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.kappa()));
                csvWriter.append("\n");
            }

        }
    }

    private static void computeRandomForest(String projectName, int numTraining, FileWriter csvWriter, Instances training, Instances testing, double ratio) throws Exception {
        RandomForest randomForest = new RandomForest();
        FilteredClassifier fc = new FilteredClassifier();
        Instances[] filteredData;
        Instances actTraining;
        Instances actTesting;
        Evaluation eval;
        String sampling = "";

        fc.setClassifier(randomForest);

        for (int n=1; n<5; n++) {
            actTraining = training;
            actTesting = testing;

            switch (n) {
                //No Sampling
                case 1:
                    sampling = "No Sampling";
                    break;
                //Undersampling
                case 2:
                    sampling = "Oversampling";

                    Resample resample = new Resample();
                    resample.setNoReplacement(false);
                    String[] opt1 = new String[]{ "-B", "1.0"};
                    String[] opt2 = new String[]{ "-Z", Double.toString(ratio*100*2)};
                    resample.setOptions(opt1);
                    resample.setOptions(opt2);

                    resample.setInputFormat(actTraining);
                    fc.setFilter(resample);

                    break;
                //Oversampling
                case 3:
                    sampling = "Undersampling";

                    SpreadSubsample spreadSubsample = new SpreadSubsample();
                    String[] opts = new String[]{ "-M", "1.0"};
                    spreadSubsample.setOptions(opts);
                    spreadSubsample.setInputFormat(actTraining);
                    fc.setFilter(spreadSubsample);

                    break;
                //SMOTE
                case 4:
                    sampling = "SMOTE";

                    SMOTE smote = new SMOTE();
                    smote.setInputFormat(actTraining);
                    fc.setFilter(smote);

                    break;
            }

            for (int m=1;m<3;m++) {
                csvWriter.append(projectName);
                csvWriter.append(",");
                csvWriter.append(Integer.toString(numTraining));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f",(double)training.size()/(double)numInstances));
                csvWriter.append(",");
                csvWriter.append("Random Forest");
                csvWriter.append(",");
                csvWriter.append(sampling);
                csvWriter.append(",");

                switch (m) {
                    //No Selection
                    case 1:
                        csvWriter.append("No Selection");
                        csvWriter.append(",");
                        break;
                    //Backward search
                    case 2:
                        csvWriter.append("Backward Search");
                        csvWriter.append(",");

                        filteredData = computeSelection(actTraining, actTesting);
                        actTraining = filteredData[0];
                        actTesting = filteredData[1];
                        break;
                }

                if (!sampling.equals("No Sampling")) {
                    fc.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(fc, actTesting);
                } else {
                    randomForest.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(randomForest, actTesting);
                }

                csvWriter.append(String.format("%.3f", eval.numTruePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalsePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numTrueNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalseNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.precision(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.recall(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.areaUnderROC(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.kappa()));
                csvWriter.append("\n");
            }

        }
    }

    private static void computeIbk(String projectName, int numTraining, FileWriter csvWriter, Instances training, Instances testing, double ratio) throws Exception {
        IBk ibk = new IBk();
        FilteredClassifier fc = new FilteredClassifier();
        Instances[] filteredData;
        Instances actTraining;
        Instances actTesting;
        Evaluation eval;
        String sampling = "";

        fc.setClassifier(ibk);

        for (int n=1; n<5; n++) {
            actTraining = training;
            actTesting = testing;

            switch (n) {
                //No Sampling
                case 1:
                    sampling = "No Sampling";
                    break;
                //Undersampling
                case 2:
                    sampling = "Oversampling";

                    Resample resample = new Resample();
                    resample.setNoReplacement(false);
                    String[] opt1 = new String[]{ "-B", "1.0"};
                    String[] opt2 = new String[]{ "-Z", Double.toString(ratio*100*2)};
                    resample.setOptions(opt1);
                    resample.setOptions(opt2);

                    resample.setInputFormat(actTraining);
                    fc.setFilter(resample);

                    break;
                //Oversampling
                case 3:
                    sampling = "Undersampling";

                    SpreadSubsample spreadSubsample = new SpreadSubsample();
                    String[] opts = new String[]{ "-M", "1.0"};
                    spreadSubsample.setOptions(opts);
                    spreadSubsample.setInputFormat(actTraining);
                    fc.setFilter(spreadSubsample);

                    break;
                //SMOTE
                case 4:
                    sampling = "SMOTE";

                    SMOTE smote = new SMOTE();
                    smote.setInputFormat(actTraining);
                    fc.setFilter(smote);

                    break;
            }

            for (int m=1;m<3;m++) {
                csvWriter.append(projectName);
                csvWriter.append(",");
                csvWriter.append(Integer.toString(numTraining));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f",(double)training.size()/(double)numInstances));
                csvWriter.append(",");
                csvWriter.append("IBk");
                csvWriter.append(",");
                csvWriter.append(sampling);
                csvWriter.append(",");

                switch (m) {
                    //No Selection
                    case 1:
                        csvWriter.append("No Selection");
                        csvWriter.append(",");
                        break;
                    //Backward search
                    case 2:
                        csvWriter.append("Backward Search");
                        csvWriter.append(",");

                        filteredData = computeSelection(actTraining, actTesting);
                        actTraining = filteredData[0];
                        actTesting = filteredData[1];
                        break;
                }

                if (!sampling.equals("No Sampling")) {
                    fc.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(fc, actTesting);
                } else {
                    ibk.buildClassifier(actTraining);
                    eval = new Evaluation(actTraining);
                    eval.evaluateModel(ibk, actTesting);
                }

                csvWriter.append(String.format("%.3f", eval.numTruePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalsePositives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numTrueNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.numFalseNegatives(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.precision(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.recall(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.areaUnderROC(0)));
                csvWriter.append(",");
                csvWriter.append(String.format("%.3f", eval.kappa()));
                csvWriter.append("\n");
            }

        }
    }

    public static void applyWeka(Project project) throws Exception {
        FileWriter csvEvaluation;
        int numAttributes;
        int numReleases = 0;
        int numBuggy;
        int numNotBuggy;
        double ratio;

        //Implementing WalkForward method
        for (Release release : project.getReleases()) {
            if (!release.isValid()) {
                break;
            }
            numReleases++;
        }

        csvEvaluation = new FileWriter("/home/alex/code/intelliJ/projects/D2-ISW2/data/evaluation/" + project.getName() + "_Models.csv");
        csvEvaluation.append("Dataset");
        csvEvaluation.append(",");
        csvEvaluation.append("#TrainingRelease");
        csvEvaluation.append(",");
        csvEvaluation.append("%Training");
        csvEvaluation.append(",");
        csvEvaluation.append("Classifier");
        csvEvaluation.append(",");
        csvEvaluation.append("Sampling");
        csvEvaluation.append(",");
        csvEvaluation.append("Feature Selection");
        csvEvaluation.append(",");
        csvEvaluation.append("TP");
        csvEvaluation.append(",");
        csvEvaluation.append("FP");
        csvEvaluation.append(",");
        csvEvaluation.append("TN");
        csvEvaluation.append(",");
        csvEvaluation.append("FN");
        csvEvaluation.append(",");
        csvEvaluation.append("Precision");
        csvEvaluation.append(",");
        csvEvaluation.append("Recall");
        csvEvaluation.append(",");
        csvEvaluation.append("AUC");
        csvEvaluation.append(",");
        csvEvaluation.append("Kappa");
        csvEvaluation.append("\n");

        ConverterUtils.DataSource sourceTrain = new ConverterUtils.DataSource("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + project.getName() + "_release_1.arff");
        Instances training = sourceTrain.getDataSet();
        numAttributes = training.numAttributes();

        ConverterUtils.DataSource sourceTest = new ConverterUtils.DataSource("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + project.getName() + "_release_2.arff");
        Instances testing = sourceTest.getDataSet();

        Instances actTraining = training;
        numBuggy = numNotBuggy = 0;
        for (Release release : project.getReleases()) {
            if (release.getIndex() >= numReleases) {
                continue;
            }

            if (release.getIndex() > 1) {
                actTraining = new ConverterUtils.DataSource("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + project.getName() + "_release_" + release.getIndex() + ".arff").getDataSet();
                testing = new ConverterUtils.DataSource("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + project.getName() + "_release_" + (release.getIndex() + 1) + ".arff").getDataSet();
            }

            for (Instance instance : actTraining) {
                if (release.getIndex() > 1) {
                    training.add(instance);
                }
                if (instance.stringValue(actTraining.attribute("Buggy").index()).equals("Yes")) {
                    numBuggy++;
                } else {
                    numNotBuggy++;
                }
            }

            training.setClassIndex(numAttributes - 1);
            testing.setClassIndex(numAttributes  - 1);

            if (numNotBuggy >= numBuggy) {
                ratio = (double)numNotBuggy / ((double)training.size());
            } else {
                ratio = (double)numBuggy / ((double)training.size());
            }

            computeNaiveBayes(project.getName(), release.getIndex(), csvEvaluation, training, testing, ratio);
            computeRandomForest(project.getName(), release.getIndex(), csvEvaluation, training, testing, ratio);
            computeIbk(project.getName(), release.getIndex(), csvEvaluation, training, testing, ratio);
        }
        csvEvaluation.flush();
    }

    public static String askForProjectName() {
        Scanner sc = new Scanner(System.in);
        boolean exit = false;
        String projectName = "";

        while (!exit) {
            logger.info("Type the project name: ");
            projectName = sc.nextLine();

            if (projectName.length() > 2) {
                exit = true;
            } else {
                logger.info("Project name requires at least 3 characters. Try Again!");
            }
        }

        return projectName.toLowerCase().trim();
    }
}
