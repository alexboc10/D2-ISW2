package resources;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class File {
    private final String name;
    private List<String> authors;
    private int bugFixes;
    private final LocalDate creationDate;
    private long age;

    public File(String name, LocalDate creationDate) {
        this.name = name;
        this.authors = new ArrayList<>();
        this.bugFixes = 0;
        this.creationDate = creationDate;
        this.age = 0;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public long getAge() {
        return this.age;
    }

    public LocalDate getCreationDate() {
        return this.creationDate;
    }

    public void incrBugFixes() {
        this.bugFixes++;
    }

    public int getBugFixes() {
        return this.bugFixes;
    }

    public List<String> getAuthors() {
        return this.authors;
    }

    public void addAuthor(String author) {
        if (!this.authors.contains(author)) {
            this.authors.add(author);
        }
    }

    public String getName() {
        return this.name;
    }
}
