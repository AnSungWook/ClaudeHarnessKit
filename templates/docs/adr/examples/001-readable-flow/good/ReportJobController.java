@PostMapping("/jobs/report")
public CommonResponse<ReportJobResponse> startReportJob(@RequestParam(required = false) @Positive Long targetKey) {
    return CommonResponse.success(ReportJobResponse.of(reportJobService.startReportJob(targetKey)));
}
