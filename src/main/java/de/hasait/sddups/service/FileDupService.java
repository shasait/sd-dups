package de.hasait.sddups.service;

import de.hasait.util.dup.DupObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
public class FileDupService {

    private static final Logger log = LoggerFactory.getLogger(FileDupService.class);

    private final CopyOnWriteArrayList<FileDupListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicLong remainingScans = new AtomicLong();

    private final AtomicReference<FileDupRepo> repoHolder = new AtomicReference<>();

    private final TaskExecutor taskExecutor;

    public FileDupService(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        clear();
    }

    public void addListener(FileDupListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FileDupListener listener) {
        listeners.remove(listener);
    }

    public void clear() {
        this.repoHolder.set(new FileDupRepo());
    }

    public void scan(String startPathLines, String excludePathRegexLines) {
        FileDupRepo repo = repoHolder.get();

        List<Pattern> excludePathPatterns = new ArrayList<>();
        excludePathRegexLines.lines().forEach(line -> {
            excludePathPatterns.add(Pattern.compile(line));
        });

        startPathLines.lines().forEach(line -> {
            if (!line.isEmpty()) {
                File path = new File(line);
                scanInternal(repo, path, excludePathPatterns);
            }
        });

    }

    private void scanInternal(FileDupRepo repo, File path, List<Pattern> excludePathPatterns) {
        if (remainingScans.getAndIncrement() == 0) {
            listeners.forEach(FileDupListener::scanStarted);
        }
        taskExecutor.execute(() -> {
            try {
                if (!excludePathPatterns.isEmpty() && excludePathPatterns.stream().anyMatch(it -> it.matcher(path.getPath()).matches())) {
                    log.debug("scanInternal: EXCLUDED {}", path);
                } else {
                    if (path.isFile()) {
                        log.debug("scanInternal: FILE {}", path);
                        repo.add(path);
                    } else if (path.isDirectory()) {
                        log.info("scanInternal: DIR {}", path);
                        for (File child : path.listFiles()) {
                            scanInternal(repo, child, excludePathPatterns);
                        }
                    } else {
                        log.info("scanInternal: OTHER {}", path);
                    }
                }
            } catch (Exception e) {
                log.warn("Cannot scan: {}", path, e);
            } finally {
                if (remainingScans.decrementAndGet() == 0) {
                    listeners.forEach(FileDupListener::scanFinished);
                }
            }
        });
    }

    public Collection<DupObject<File>> collectObjects() {
        return repoHolder.get().collectObjects();
    }

    public void deleteObject(DupObject<File> selection) {
        selection.getObject().delete();
        selection.remove();
    }

}
