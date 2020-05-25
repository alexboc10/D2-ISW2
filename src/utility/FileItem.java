package utility;

public class FileItem {
    private final String name;
    private boolean buggy;
    private int size;
    private int touchingCommits;
    private int numOfAuthors;
    private int bugFixes;
    private long age;
    private int changeSetSize;
    private int maxChangeSetSize;
    private int avgChangeSetSize;
    private int addedLoc;

    public FileItem(String name) {
        this.name = name;
        this.buggy = false;
        this.size = 0;
        this.touchingCommits = 0;
        this.numOfAuthors = 0;
        this.bugFixes = 0;
        this.age = 0;
        this.changeSetSize = 0;
        this.maxChangeSetSize = 0;
        this.avgChangeSetSize = 0;
        this.addedLoc = 0;
    }

    public int getAddedLoc() {
        return this.addedLoc;
    }

    public int getAvgChangeSetSize() {
        return this.avgChangeSetSize;
    }

    public int getMaxChangeSetSize() {
        return this.maxChangeSetSize;
    }

    public int getChangeSetSize() {
        return this.changeSetSize;
    }

    public int getBugFixes() {
        return this.bugFixes;
    }

    public int getTouchingCommits() {
        return this.touchingCommits;
    }

    public void incrAddedLoc(int addedLoc) {
        this.addedLoc = this.addedLoc + addedLoc;
    }

    public long getAge() {
        return this.age;
    }

    public int getSize() {
        return this.size;
    }

    public int getNumOfAuthors() {
        return this.numOfAuthors;
    }

    public void addChangeSetSize(int changeSetSize) {
        this.changeSetSize = this.changeSetSize + changeSetSize;

        if (this.changeSetSize > this.maxChangeSetSize) {
            this.maxChangeSetSize = this.changeSetSize;
        }

        this.avgChangeSetSize = ((this.avgChangeSetSize * (this.touchingCommits - 1)) + this.changeSetSize) / (this.touchingCommits);
    }

    public void setAge(long age) {
        this.age = age;
    }

    public void setBugFixes(int bugFixes) {
        this.bugFixes = bugFixes;
    }

    public void setNumOfAuthors(int num) {
        this.numOfAuthors = num;
    }

    public void incrTouchingCommits() {
        this.touchingCommits++;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getName() {
        return this.name;
    }

    public boolean isBuggy() {
        return (this.buggy);
    }

    public void setBuggy() {
        this.buggy = true;
    }
}
