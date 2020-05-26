package resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utility.CommandLine;
import utility.FileItem;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.WEEKS;

public class Project {
    private static final String WRITING = "Writing on file";
    private static final Logger logger = Logger.getLogger(Project.class.getName());
    private final String name;
    private int totalTickets;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Ticket> tickets;
    private List<Release> releases;
    private List<Commit> commits;
    private List<File> files;
    private Release lastValidRelease;
    private static final String CLASS = "{Yes,No}";

    public Project(String name) {
        this.name = name;
        this.totalTickets = 0;
        this.tickets = new ArrayList<>();
        this.startDate = LocalDate.MAX;
        this.endDate = LocalDate.MIN;
        this.lastValidRelease = null;
        this.commits = new ArrayList<>();
        this.releases = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public List<Release> getReleases() {
        return this.releases;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private static JSONObject readJsonFromUrl (String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();

        try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private void csvToArff(String filename) {
        ArffSaver saver = new ArffSaver();
        String[] options = new String[2];
        options[0] = "-R";
        options[1] = "1-2";

        Remove remove = new Remove();
        try {
            remove.setOptions(options);

            CSVLoader source = new CSVLoader();
            source.setSource(new java.io.File(filename));
            Instances instances = source.getDataSet();

            remove.setInputFormat(instances);
            instances = Filter.useFilter(instances, remove);

            saver.setInstances(instances);
            saver.setFile(new java.io.File(filename.replace(".csv", ".arff")));
            saver.writeBatch();

            Path path = Paths.get(filename.replace(".csv", ".arff"));
            Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path));
            content = content.replaceAll("[{]No,Yes[}]", CLASS);
            content = content.replaceAll("[{]No[}]", CLASS);
            content = content.replaceAll("[{]Yes[}]", CLASS);
            Files.write(path, content.getBytes(charset));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int prepareCSV(FileWriter csvFeatures, Release release) {
        int count = 0;

        try(FileWriter csvRelease = new FileWriter("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + this.name + "_release_" + release.getIndex() + ".csv")) {
            csvRelease.append("Release");
            csvRelease.append(",");
            csvRelease.append("File");
            csvRelease.append(",");
            csvRelease.append("NAuth");
            csvRelease.append(",");
            csvRelease.append("NR");
            csvRelease.append(",");
            csvRelease.append("Age");
            csvRelease.append(",");
            csvRelease.append("Size");
            csvRelease.append(",");
            csvRelease.append("NFix");
            csvRelease.append(",");
            csvRelease.append("LOC_Added");
            csvRelease.append(",");
            csvRelease.append("ChgSetSize");
            csvRelease.append(",");
            csvRelease.append("Max_ChgSetSize");
            csvRelease.append(",");
            csvRelease.append("Avg_ChgSetSize");
            csvRelease.append(",");
            csvRelease.append("Buggy");
            csvRelease.append("\n");

            for (FileItem item : release.getFileItems()) {
                count++;

                csvFeatures.append(Integer.toString(release.getIndex()));
                csvFeatures.append(",");
                csvFeatures.append(item.getName());
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getNumOfAuthors()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getTouchingCommits()));
                csvFeatures.append(",");
                csvFeatures.append(Long.toString(item.getAge()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getSize()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getBugFixes()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getAddedLoc()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getChangeSetSize()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getMaxChangeSetSize()));
                csvFeatures.append(",");
                csvFeatures.append(Integer.toString(item.getAvgChangeSetSize()));
                csvFeatures.append(",");
                if (item.isBuggy()) {
                    csvFeatures.append("Yes");
                } else {
                    csvFeatures.append("No");
                }
                csvFeatures.append("\n");

                csvRelease.append(Integer.toString(release.getIndex()));
                csvRelease.append(",");
                csvRelease.append(item.getName());
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getNumOfAuthors()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getTouchingCommits()));
                csvRelease.append(",");
                csvRelease.append(Long.toString(item.getAge()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getSize()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getBugFixes()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getAddedLoc()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getChangeSetSize()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getMaxChangeSetSize()));
                csvRelease.append(",");
                csvRelease.append(Integer.toString(item.getAvgChangeSetSize()));
                csvRelease.append(",");
                if (item.isBuggy()) {
                    csvRelease.append("Yes");
                } else {
                    csvRelease.append("No");
                }
                csvRelease.append("\n");
            }

            csvRelease.flush();
            csvToArff("/home/alex/code/intelliJ/projects/D2-ISW2/data/releaseSets/" + this.name + "_release_" + release.getIndex() + ".csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    public int writeBugginess() {
        int count = 0;

        try(FileWriter csvFeatures = new FileWriter("/home/alex/code/intelliJ/projects/D2-ISW2/data/bugginess/" + this.name + "_Bugginess.csv")) {

            logger.log(Level.FINE, WRITING);

            csvFeatures.append("Release");
            csvFeatures.append(",");
            csvFeatures.append("File");
            csvFeatures.append(",");
            csvFeatures.append("NAuth");
            csvFeatures.append(",");
            csvFeatures.append("NR");
            csvFeatures.append(",");
            csvFeatures.append("Age");
            csvFeatures.append(",");
            csvFeatures.append("Size");
            csvFeatures.append(",");
            csvFeatures.append("NFix");
            csvFeatures.append(",");
            csvFeatures.append("LOC_Added");
            csvFeatures.append(",");
            csvFeatures.append("ChgSetSize");
            csvFeatures.append(",");
            csvFeatures.append("Max_ChgSetSize");
            csvFeatures.append(",");
            csvFeatures.append("Avg_ChgSetSize");
            csvFeatures.append(",");
            csvFeatures.append("Buggy");
            csvFeatures.append("\n");

            for (Release release : this.releases) {
                if (release.getIndex() > this.lastValidRelease.getIndex() || release.getFileItems().size() == 0) {
                    continue;
                }

                count = count + prepareCSV(csvFeatures, release);
            }

            csvFeatures.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    private void writeReleases() {
        logger.log(Level.FINE, WRITING);

        try (FileWriter fileWriter = new FileWriter("/home/alex/code/intelliJ/projects/D2-ISW2/data/releases/" + this.name + "_Releases.csv")) {
            fileWriter.append("Index,Version ID,Version Name,Date");
            fileWriter.append("\n");

            for (Release release : this.releases) {
                fileWriter.append(Integer.toString(release.getIndex()));
                fileWriter.append(",");
                fileWriter.append(Integer.toString(release.getId()));
                fileWriter.append(",");
                fileWriter.append(release.getName());
                fileWriter.append(",");
                fileWriter.append(release.getDate().toString());
                fileWriter.append("\n");
            }

            fileWriter.flush();
        } catch (Exception e) {
            logger.log(Level.FINE, WRITING);
            e.printStackTrace();
        }
    }

    private File getFileByName(String name) {
        for (File file : this.files) {
            if (name.equals(file.getName())) {
                //File already exists
                return file;
            }
        }
        //File does not exist
        return null;
    }

    private Release getNextRelease(LocalDate date) {
        for (Release release : this.releases) {
            if (date.isBefore(release.getDate())) {
                return release;
            }
        }
        return null;
    }

    private Release getReleaseByName(String name) {
        for (Release release : releases) {
            if (release.getName().equals(name)) {
                return release;
            }
        }
        return null;
    }

    public Release getReleaseByIndex(int index) {
        if (index < 1) {
            index = 1;
        }
        for (Release release : releases) {
            if (release.getIndex() == index) {
                return release;
            }
        }
        return null;
    }

    private void findChangedFiles(Release myRelease, Commit commit) throws IOException {
        CommandLine command = new CommandLine();
        String output;
        FileItem myFileItem;
        File myFile;
        String[] lines;
        String[] items;

        command.setCommand("./getChanges.sh " + this.name  + " " + commit.getHash(), "/home/alex/code/intelliJ/projects/D2-ISW2/script");
        output = command.executeCommand();

        lines = output.split("END", 0);

        //Modified files in the considered commit
        for (String line : lines) {
            items = line.split("\t", 0);

            if (items.length != 3 || (myFile = getFileByName(items[2])) == null || (myFileItem = myRelease.getFileItemByName(items[2])) == null) {
                continue;
            }

            myFile.incrBugFixes();

            myFileItem.incrTouchingCommits();
            myFileItem.setBugFixes(myFile.getBugFixes());
            myFileItem.addChangeSetSize(lines.length - 1);
            myFileItem.incrAddedLoc(Integer.parseInt(items[0]));

            for (Release av : commit.getTicket().getAffectedVersions()) {
                myFileItem = av.getFileItemByName(items[2]);

                if (myFileItem == null) {
                    continue;
                }

                myFileItem.setBuggy();
            }
        }
    }

    private void findExistingFiles(Release myRelease, Commit commit) throws IOException {
        CommandLine command = new CommandLine();
        String output;
        FileItem myFileItem;
        File myFile;
        String[] lines;

        //Listing all the files existing in the considered commit
        command.setCommand("./getFiles.sh " + this.name  + " " + commit.getHash(), "/home/alex/code/intelliJ/projects/D2-ISW2/script");
        output = command.executeCommand();

        lines = output.split("END", 0);

        //Existing files in the considered commit
        for (String filename : lines) {
            myFile = getFileByName(filename);
            if (myFile == null) {
                myFile = new File(filename, commit.getDate());
                this.files.add(myFile);
            }

            myFile.setAge(WEEKS.between(myFile.getCreationDate(), commit.getDate()));
            myFile.addAuthor(commit.getAuthor());

            //Searching for the fileItem, if it exists
            myFileItem = myRelease.getFileItemByName(filename);
            if (myFileItem == null) {
                myFileItem = new FileItem(filename);
                myRelease.addFileItem(myFileItem);
            }

            myFileItem.setAge(myFile.getAge());
            myFileItem.setNumOfAuthors(myFile.getAuthors().size());

            command.setCommand("cat " + filename + " | wc -l", "/home/alex/code/ISW2/" + this.name);
            output = command.executeCommand();

            myFileItem.setSize(Integer.parseInt(output));
        }
    }

    public void extractFiles() throws IOException {
        Release myRelease;
        int index = 0;

        logger.log(Level.FINE, "EXTRACTING FILES");

        //Commits ordered by increasing date
        for (Commit commit : this.commits) {
            index++;

            if (index == 1) {
                findExistingFiles(this.releases.get(0), commit);
            }

            logger.log(Level.FINE, "{}", index + "/" + this.commits.size());

            //Searching for corresponding release
            //Release ordered by increasing date
            myRelease = getNextRelease(commit.getDate());
            if (myRelease == null) {
                continue;
            }

            findExistingFiles(myRelease, commit);
            findChangedFiles(myRelease, commit);
        }

        for (Release release : this.releases) {
            if (release.getFileItems().size() == 0) {
                for (FileItem fileItem : getReleaseByIndex(release.getIndex() - 1).getFileItems()) {
                    release.addFileItem(fileItem);
                }
            }
        }
    }

    public void extractCommits() throws IOException {
        CommandLine command = new CommandLine();
        String output;
        String[] lines;
        String[] info;
        String newHash;
        String newDate;
        String newAuthor;
        Commit newCommit;
        int index = 0;

        logger.log(Level.FINE, "EXTRACTING COMMITS");

        for (Ticket ticket : this.tickets) {
            index++;

            logger.log(Level.FINE, "{}", index + "/" + this.commits.size());

            command.setCommand("git log --date=iso-strict --grep=" + ticket.getKey() + " -F --until=" + this.releases.get(this.releases.size() - 1).getDate().toString() + " --pretty=format:'%H'BREAK'%cd'BREAK'%an'END | sort", "/home/alex/code/ISW2/" + this.name);
            output = command.executeCommand();

            lines = output.split("END", 0);
            for (String line : lines) {
                info = line.split("BREAK", 0);

                if (info.length == 3) {
                    newHash = info[0];
                    newDate = info[1].substring(0, 10);
                    newAuthor = info[2];
                } else {
                    continue;
                }

                newCommit = new Commit(newHash, newAuthor, LocalDate.parse(newDate));
                ticket.addCommit(newCommit);
                this.commits.add(newCommit);
                newCommit.setTicket(ticket);
            }
        }

        logger.log(Level.FINE, "Sorting commits by date");
        //@Override
        this.commits.sort(Comparator.comparing(Commit::getDate));
    }

    private boolean isValidTicket(Ticket ticket, JSONObject field) {
        Release newOpeningVersion;
        Release newFixedVersion;

        LocalDate resolutionDate = LocalDate.parse(field.getString("resolutiondate").substring(0,10));
        LocalDate created = LocalDate.parse(field.getString("created").substring(0,10));

        if (ticket.getFixedVersion() == null) {
            newFixedVersion = getNextRelease(resolutionDate);

            if (newFixedVersion == null) {
                return false;
            }

            ticket.setFixedVersion(newFixedVersion);
        }

        //Setting opening version as the next release to ticket creation date
        newOpeningVersion = getNextRelease(created);
        if (newOpeningVersion == null) {
            return false;
        }

        ticket.setOpeningVersion(newOpeningVersion);

        return ticket.getOpeningVersion().getIndex() < ticket.getFixedVersion().getIndex();
    }

    private void computeIV(Ticket ticket) {
        if (ticket.getAffectedVersions().size() > 0 && ticket.getOpeningVersion().getIndex() > 1) {
            ticket.setInjectedVersion(ticket.getAffectedVersions().get(0));
            if (ticket.getInjectedVersion().getIndex() >= ticket.getOpeningVersion().getIndex()) {
                ticket.setInjectedVersion(null);
                ticket.clearAffectedVersions();
            } else {
                ticket.computeP();
            }
        }
    }

    private void computeVersions(Ticket ticket, JSONObject field) {
        Release newFixedVersion;
        Release newAffectedVersion;

        JSONArray fixVersion = field.getJSONArray("fixVersions");
        JSONArray versions = field.getJSONArray("versions");

        //Adding, if it exists, fixed version
        for (int k=0; k<fixVersion.length();k++) {
            newFixedVersion = getReleaseByName(fixVersion.getJSONObject(k).get("name").toString());
            if (newFixedVersion == null) {
                continue;
            }
            if (ticket.getFixedVersion() == null || newFixedVersion.getIndex() < ticket.getFixedVersion().getIndex()) {
                ticket.setFixedVersion(newFixedVersion);
            }
        }

        if (!isValidTicket(ticket, field)) {
            return;
        }

        this.tickets.add(ticket);

        //Adding, if they exist, affected versions
        for (int k=0; k<versions.length();k++) {
            newAffectedVersion = getReleaseByName(versions.getJSONObject(k).get("name").toString());
            if (newAffectedVersion == null) {
                continue;
            }
            ticket.addAffectedVersion(newAffectedVersion);
        }

        //Sorting affected versions for every ticket
        if (ticket.getAffectedVersions().size() > 1) {
            //@Override
            ticket.getAffectedVersions().sort(Comparator.comparing(Release::getDate));
        }

        computeIV(ticket);
    }

    private void computeProportion() {
        int newInjectedIndex;
        int avItems;
        double incrP;
        avItems = 0;
        incrP = 0.0;
        for (Ticket ticket : this.tickets) {
            if (ticket.getOpeningVersion().getIndex() == 1) {
                continue;
            }
            //Ticket with known affected versions and injected version
            if (ticket.getAffectedVersions().size() > 0) {
                //P computation
                incrP = ((incrP * avItems) + ticket.getP()) / (avItems + 1);
                avItems++;
            } else {
                if (incrP == 0.0 || avItems == 0) {
                    //Simple method
                    ticket.setInjectedVersion(getReleaseByIndex(ticket.getOpeningVersion().getIndex() - 1));
                } else {
                    //Incremental proportion method
                    newInjectedIndex = (int) (ticket.getFixedVersion().getIndex() - (ticket.getFixedVersion().getIndex() - ticket.getOpeningVersion().getIndex()) * incrP);
                    ticket.setInjectedVersion(getReleaseByIndex(newInjectedIndex));
                }

                for (int m = ticket.getInjectedVersion().getIndex(); m < ticket.getFixedVersion().getIndex(); m++) {
                    ticket.addAffectedVersion(getReleaseByIndex(m));
                }
            }
        }
    }

    public void extractTickets() throws IOException {
        Ticket newTicket;
        int j;
        int i = 0;

        logger.log(Level.FINE, "EXTRACTING TICKETS");
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + this.name;
            url += "%22AND%22issueType%22=%22Bug%22";
            url += "AND(%22status%22=%22resolved%22OR%22status%22=%22closed%22)";
            url += "AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt=" + i + "&maxResults=" + j;

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");

            this.totalTickets = json.getInt("total");

            for (; i < this.totalTickets && i < j; i++) {

                logger.log(Level.FINE, "{}", i+1 + "/" + this.totalTickets);

                String key = issues.getJSONObject(i%1000).get("key").toString();
                JSONObject field = issues.getJSONObject(i%1000).getJSONObject("fields");

                newTicket = new Ticket(key);

                computeVersions(newTicket, field);
            }
        } while (i < this.totalTickets);

        logger.log(Level.FINE, "Sorting tickets by opening date");
        //@Override
        this.tickets.sort(Comparator.comparing(t -> t.getOpeningVersion().getDate()));

        logger.log(Level.FINE, "Computing Simple, AV and Proportion");
        computeProportion();
    }

    public void extractReleases() throws IOException {
        LocalDate date;
        LocalDate lastValidDate;
        String versionName = "";
        int id = 0;
        int index = 1;

        logger.log(Level.FINE, "EXTRACTING RELEASES");

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + this.name.toUpperCase();
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        //Adding all the releases with complete information
        for (int i = 0; i < versions.length(); i++ ) {
            logger.log(Level.FINE, "{}", i+1 + "/" + versions.length());
            if(versions.getJSONObject(i).has("releaseDate")) {
                date = LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString());
                if (versions.getJSONObject(i).has("name"))  {
                    versionName = versions.getJSONObject(i).get("name").toString();
                }
                if (versions.getJSONObject(i).has("id")) {
                    id = Integer.parseInt(versions.getJSONObject(i).get("id").toString());
                }
                this.releases.add(new Release(id, versionName, date));
            }
        }

        logger.log(Level.FINE, "Sorting releases by date");
        //@Override
        this.releases.sort(Comparator.comparing(Release::getDate));

        this.startDate = this.releases.get(0).getDate();
        this.endDate = this.releases.get(this.releases.size() - 1).getDate();

        long projectTime = DAYS.between(this.startDate, this.endDate);
        lastValidDate = this.startDate.plusDays(projectTime/2);

        //Setting as valid releases in the first half of the project time period
        for (Release release : this.releases) {
            if (release.getDate().isBefore(lastValidDate)) {
                release.setValid();
                this.lastValidRelease = release;
            } else {
                release.setInvalid();
            }
            release.setIndex(index);
            index++;
        }

        writeReleases();
    }
}
