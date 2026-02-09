package me.timwastaken.infectedmanhunt.common;

import java.util.ArrayList;
import java.util.List;

public abstract class Observer<T> {
    private final List<Subject<T>> subjects;

    public Observer() {
        this.subjects = new ArrayList<>();
    }

    public void register(Subject<T> subject) {
        subjects.add(subject);
    }

    public void unregister(Subject<T> subject) {
        subjects.remove(subject);
    }

    private void update(T value) {
        for (Subject<T> subject : subjects) {
            subject.notify(value);
        }
    }
}
