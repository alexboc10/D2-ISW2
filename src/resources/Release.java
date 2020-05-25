package resources;

import utility.FileItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Release {
    private int index;
    private final int id;
    private final String name;
    private final LocalDate date;
    private boolean valid;
    private List<FileItem> fileItems;

    public Release(int id, String name, LocalDate date) {
        this.index = 0;
        this.id = id;
        this.name = name;
        this.date = date;
        this.valid = false;
        this.fileItems = new ArrayList<>();
    }

    public FileItem getFileItemByName(String name) {
        for (FileItem item : this.fileItems) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public List<FileItem> getFileItems() {
        return this.fileItems;
    }

    public void addFileItem(FileItem fileItem) {
        for (FileItem item : this.fileItems) {
            if (item.getName().equals(fileItem.getName())) {
                return;
            }
        }
        this.fileItems.add(fileItem);
    }

    public int getIndex() {
        return this.index;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isValid() {
        return this.valid;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setValid() {
        this.valid = true;
    }

    public void setInvalid() {
        this.valid = false;
    }
}
