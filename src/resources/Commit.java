package resources;

import java.time.LocalDate;

public class Commit {
    private final String hash;
    private final String author;
    private final LocalDate date;
    private Ticket ticket;

    public Commit(String hash, String author, LocalDate date) {
        this.hash = hash;
        this.author = author;
        this.date = date;
        this.ticket = null;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getHash() {
        return this.hash;
    }
}
