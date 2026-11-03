// ReportJobLauncher(바깥 포트)의 Spring Batch 구현. 계약: 잡이 끝날 때까지 기다려 결과를 돌려주고, 못 띄우면 503 사유 예외.
@Override
public ReportJobResult runReportJob(Long targetKey) {
    try {
        // 기본 실행기가 동기라 잡이 끝날 때 돌아온다. 비동기 실행기를 붙이면 겹침 막기가 잡보다 먼저 풀린다.
        JobExecution execution = jobOperator.start(reportJob, reportParameters(targetKey));
        return ReportJobResult.of(execution.getId(), execution.getStatus().name());
    } catch (JobExecutionException e) {
        throw new ReportJobLaunchFailedException(e);
    }
}
