public class FailureDetector implements Runnable {
    private final MonitorCore core;
    private final Logger logger;
    private final long intervalMs;

    public FailureDetector(MonitorCore core, Logger logger, long intervalMs) {
        this.core = core;
        this.logger = logger;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run() {
        while (core.isRunning()) {
            try {
                Thread.sleep(intervalMs);
                core.checkFailures();
            } catch (InterruptedException e) {
                logger.log("FailureDetector interrupted, stopping.");
                return;
            } catch (Exception e) {
                logger.log("FailureDetector error: " + e.getMessage());
            }
        }
    }
}
