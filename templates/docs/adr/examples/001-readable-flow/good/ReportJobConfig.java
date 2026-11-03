@Configuration
public class ReportJobConfig {

    @Bean
    public Job reportJob(JobRepository jobRepository, Step reportStep) {
        return new JobBuilder("reportJob", jobRepository).start(reportStep).build();
    }

    @Bean
    public Step reportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
        PendingReportReader pendingReportReader, ReportSummaryProcessor reportSummaryProcessor,
        ReportSummaryWriter reportSummaryWriter) {
        return new StepBuilder("reportStep", jobRepository)
            .<Report, ReportSummary>chunk(100)
            .transactionManager(transactionManager)
            .reader(pendingReportReader)
            .processor(reportSummaryProcessor)
            .writer(reportSummaryWriter)
            .build();
    }
}
