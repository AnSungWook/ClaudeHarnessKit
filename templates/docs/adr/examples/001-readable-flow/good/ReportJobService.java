public ReportJobResult startReportJob(Long targetKey) {
    runningFlag.markStartedOrReject();
    try {
        return reportJobLauncher.runReportJob(targetKey);
    } finally {
        runningFlag.markFinished();
    }
}
