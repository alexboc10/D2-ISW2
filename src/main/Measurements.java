package main;

import resources.Project;
import resources.Release;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
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
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Measurements {
    private static final Logger logger = Logger.getLogger(Measurements.class.getName());
    private static int numInstances;
    private static final String NO_SAMPLING = "No Sampling";
    private static final String UNDERSAMPLING = "Undersampling";
    private static final String OVERSAMPLING = "Oversampling";
    private static final String SMOTE = "SMOTE";
    private static final String NO_SELECTION = "No Selection";
    private static final String BACKWARD = "Backward Selection";
    private static final String RELEASES_PATH = System.getProperty("user.dir") + "/data/releaseSets/";

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

    private static Instances[] computeSelection(Instances training, Instances testing) {
        weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval subsetEval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        filter.setEvaluator(subsetEval);
        filter.setSearch(search);

        //Applying attributes filter to new training and testing sets
        try {
            filter.setInputFormat(training);

            Instances filteredTraining = Filter.useFilter(training, filter);
            Instances filteredTesting = Filter.useFilter(testing, filter);

            int numFilteredAttributes = filteredTraining.numAttributes();

            filteredTraining.setClassIndex(numFilteredAttributes - 1);
            filteredTesting.setClassIndex(numFilteredAttributes - 1);

            return new Instances[]{filteredTraining, filteredTesting};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Instances[0];
    }

    private static Classifier getClassifier(String classifier) {
        switch (classifier) {
            case "Naive Bayes":
                return new NaiveBayes();
            case "Random Forest":
                return new RandomForest();
            case "IBk":
                return new IBk();
            default:
                return null;
        }
    }

    private static void computeClassifier(String projectName, int numTraining, FileWriter csvWriter, Instances training, Instances testing, String classifier) {
        FilteredClassifier fc = new FilteredClassifier();
        Instances[] filteredData;
        Instances actTraining;
        Instances actTesting;
        Evaluation eval;
        String sampling = "";
        Classifier method = null;

        method = getClassifier(classifier);

        int[] stats = computeBuggy(training, testing);
        fc.setClassifier(method);

        for (int n = 1; n < 5; n++) {
            actTraining = training;
            actTesting = testing;

            try {
                switch (n) {
                    //No Sampling
                    case 1:
                        sampling = NO_SAMPLING;
                        break;
                    //Undersampling
                    case 2:
                        sampling = OVERSAMPLING;

                        Resample resample = new Resample();
                        resample.setNoReplacement(false);
                        String[] opt1 = new String[]{"-B", "1.0"};
                        String[] opt2 = new String[]{"-Z", Double.toString(computeRatio(stats[0], stats[1], training.size()) * 100 * 2)};
                        resample.setOptions(opt1);

                        resample.setOptions(opt2);

                        resample.setInputFormat(actTraining);
                        fc.setFilter(resample);

                        break;
                    //Oversampling
                    case 3:
                        sampling = UNDERSAMPLING;

                        SpreadSubsample spreadSubsample = new SpreadSubsample();
                        String[] opt = new String[]{"-M", "1.0"};
                        spreadSubsample.setOptions(opt);

                        spreadSubsample.setInputFormat(actTraining);
                        fc.setFilter(spreadSubsample);

                        break;
                    //SMOTE
                    case 4:
                        sampling = SMOTE;

                        SMOTE smote = new SMOTE();
                        smote.setInputFormat(actTraining);
                        fc.setFilter(smote);

                        break;
                    default:
                        System.exit(1);
                }

                for (int m = 1; m < 3; m++) {
                    csvWriter.append(projectName);
                    csvWriter.append(",");
                    csvWriter.append(Integer.toString(numTraining));
                    csvWriter.append(",");
                    csvWriter.append(String.format("%.3f", (double) training.size() / (double) numInstances));
                    csvWriter.append(",");
                    csvWriter.append(String.format("%.3f", (double) stats[0] / (double) training.size()));
                    csvWriter.append(",");
                    csvWriter.append(String.format("%.3f", (double) stats[2] / (double) training.size()));
                    csvWriter.append(",");
                    csvWriter.append(classifier);
                    csvWriter.append(",");
                    csvWriter.append(sampling);
                    csvWriter.append(",");

                    if (m == 1) {
                        //No Selection
                        csvWriter.append(NO_SELECTION);
                        csvWriter.append(",");
                    } else {
                        //Backward search
                        csvWriter.append(BACKWARD);
                        csvWriter.append(",");

                        filteredData = computeSelection(actTraining, actTesting);

                        actTraining = filteredData[0];
                        actTesting = filteredData[1];
                    }

                    if (sampling.equals(NO_SAMPLING)) {
                        method.buildClassifier(actTraining);
                        eval = new Evaluation(actTraining);
                        eval.evaluateModel(method, actTesting);
                    } else {
                        fc.buildClassifier(actTraining);
                        eval = new Evaluation(actTraining);
                        eval.evaluateModel(fc, actTesting);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static double computeRatio(double numBuggy, double numNotBuggy, double numInstances) {
        if (numNotBuggy >= numBuggy) {
            return numNotBuggy / numInstances;
        } else {
            return numBuggy / numInstances;
        }
    }

    private static int computeValidReleases(List<Release> releases) {
        int numReleases = 0;

        for (Release release : releases) {
            if (!release.isValid()) {
                break;
            }
            numReleases++;
        }

        return numReleases;
    }

    //The method returns an int array with
    //1. number of buggy testing instances
    //2. number of buggy training instances
    //3. number of not buggy training instances
    private static int[] computeBuggy(Instances training, Instances testing)  {
        int[] stats = new int[]{0,0,0};

        for (Instance instance : testing) {
            if (instance.stringValue(testing.attribute("Buggy").index()).equals("Yes")) {
                stats[2]++;
            }
        }

        for (Instance instance : training) {
            if (instance.stringValue(training.attribute("Buggy").index()).equals("Yes")) {
                stats[0]++;
            } else {
                stats[1]++;
            }
        }
        return stats;
    }

    public static void applyWeka(Project project) {
        int numAttributes;
        int numReleases;

        //Implementing WalkForward method
        numReleases = computeValidReleases(project.getReleases());

        try (FileWriter csvEvaluation = new FileWriter("/home/alex/code/intelliJ/projects/D2-ISW2/data/evaluation/" + project.getName() + "_Models.csv")) {
            csvEvaluation.append("Dataset");
            csvEvaluation.append(",");
            csvEvaluation.append("#TrainingRelease");
            csvEvaluation.append(",");
            csvEvaluation.append("%Training");
            csvEvaluation.append(",");
            csvEvaluation.append("%TrainDefective");
            csvEvaluation.append(",");
            csvEvaluation.append("%TestDefective");
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

            ConverterUtils.DataSource sourceTrain = new ConverterUtils.DataSource(RELEASES_PATH + project.getName() + "_release_1.arff");
            Instances training = sourceTrain.getDataSet();
            numAttributes = training.numAttributes();

            ConverterUtils.DataSource sourceTest = new ConverterUtils.DataSource(RELEASES_PATH + project.getName() + "_release_2.arff");
            Instances testing = sourceTest.getDataSet();

            for (Release release : project.getReleases()) {
                if (release.getIndex() >= numReleases) {
                    continue;
                }

                if (release.getIndex() > 1) {
                    training.addAll(new ConverterUtils.DataSource(RELEASES_PATH + project.getName() + "_release_" + release.getIndex() + ".arff").getDataSet());
                    testing = new ConverterUtils.DataSource(RELEASES_PATH + project.getName() + "_release_" + (release.getIndex() + 1) + ".arff").getDataSet();
                }

                training.setClassIndex(numAttributes - 1);
                testing.setClassIndex(numAttributes  - 1);

                computeClassifier(project.getName(), release.getIndex(), csvEvaluation, training, testing, "Naive Bayes");
                computeClassifier(project.getName(), release.getIndex(), csvEvaluation, training, testing, "Random Forest");
                computeClassifier(project.getName(), release.getIndex(), csvEvaluation, training, testing, "IBk");
            }

            csvEvaluation.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
