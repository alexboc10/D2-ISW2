package resources;

import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private final String key;
    private Release injectedVersion;
    private Release openingVersion;
    private Release fixedVersion;
    private List<Release> affectedVersions;
    private List<Commit> commits;
    private double p;

    public Ticket(String key) {
        this.key = key;
        this.injectedVersion = null;
        this.openingVersion = null;
        this.fixedVersion = null;
        this.affectedVersions = new ArrayList<>();
        this.commits = new ArrayList<>();
        this.p = 0.0;
    }

    public List<Commit> getCommits() {
        return this.commits;
    }

    public Release getInjectedVersion() {
        return this.injectedVersion;
    }

    public Release getFixedVersion() {
        return this.fixedVersion;
    }

    public Release getOpeningVersion() {
        return this.openingVersion;
    }

    public double getP() {
        return this.p;
    }

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public void computeP() {
        this.p = (double)(this.fixedVersion.getIndex() - this.injectedVersion.getIndex()) / (double) (this.fixedVersion.getIndex() - this.openingVersion.getIndex());
    }

    public void setFixedVersion(Release release) {
        this.fixedVersion = release;
    }

    public void setOpeningVersion(Release release) {
        this.openingVersion = release;
    }

    public void setInjectedVersion(Release version) {
        this.injectedVersion = version;
    }

    public List<Release> getAffectedVersions() {
        return this.affectedVersions;
    }

    public Commit getLastCommit() {
        return this.getCommits().get(this.getCommits().size() - 1);
    }

    public void clearAffectedVersions() {
        this.affectedVersions.clear();
    }

    public void addAffectedVersion(Release version) {
        this.affectedVersions.add(version);
    }

    public String getKey() {
        return this.key;
    }
}
