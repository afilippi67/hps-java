package org.hps.crawler;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.datacat.client.DatasetFileFormat;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * A file visitor that crawls directories for EVIO files and returns the information as a {@link RunSummaryMap}.
 * <p>
 * The {@link #addFilter(FileFilter)} method can be used to add a file filter.  Paths must pass all filters to
 * be accepted.
 *
 * @author Jeremy McCormick, SLAC
 */
final class CrawlerFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(CrawlerFileVisitor.class, new DefaultLogFormatter(), Level.FINE);

    /**
     * A list of file filters to apply.
     */
    private final List<FileFilter> filters = new ArrayList<FileFilter>();

    /**
     * The run log containing information about files from each run.
     */
    // FIXME: This should be replaced by a map of run summary to file set.
    private final RunSummaryMap runs = new RunSummaryMap();
    
    /**
     * Create a new file visitor.
     *
     * @param timestamp the timestamp which is used for date filtering
     */
    CrawlerFileVisitor() {
    }

    /**
     * Run the filters on the file to tell whether it should be accepted or not.
     *
     * @param file the EVIO file
     * @return <code>true</code> if file should be accepted
     */
    private boolean accept(final File file) {
        boolean accept = true;
        for (final FileFilter filter : this.filters) {
            accept = filter.accept(file);
            if (!accept) {
                LOGGER.finer(filter.getClass().getSimpleName() + " rejected " + file.getPath());
                break;
            }
        }
        return accept;
    }

    /**
     * Add a file filter.
     *
     * @param filter the file filter
     */
    void addFilter(final FileFilter filter) {
        this.filters.add(filter);
        LOGGER.config("added " + filter.getClass().getSimpleName() + " filter");
    }

    /**
     * Get the run log.
     *
     * @return the run log
     */
    RunSummaryMap getRunMap() {
        return this.runs;
    }

    /**
     * Visit a single file.
     *
     * @param path the file to visit
     * @param attrs the file attributes
     */
    @Override
    public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
                
        final File file = path.toFile();
        
        if (this.accept(file)) {            

            // Get the run number from the file name.
            final Integer run = CrawlerFileUtilities.getRunFromFileName(file);

            // Get the file format.
            DatasetFileFormat format = DatacatUtilities.getFileFormat(file);

            LOGGER.info("accepted file " + file.getPath() + " with run " + run);
            
            // Add file to run summary.
            this.runs.getRunSummary(run).addFile(format, file);
            
        } else {
            // File was rejected by one of the filters.
            LOGGER.info("file " + file.getPath() + " was rejected");
        }
        // Always continue crawling.
        return FileVisitResult.CONTINUE;
    }
}