@PostMapping("/jobs/report")
public void report(@RequestParam(required = false) Long targetKey, HttpServletResponse response) throws IOException {
    stream(runs.report(targetKey), response);   // ADR-001 위반: 무엇이 실행되는지 이름으로 안 보인다(runs, stream)
}

private static void stream(BatchRun run, HttpServletResponse response) throws IOException {
    try {
        run.execute(new RunLines(new ResponseLines(response.getWriter())));   // ADR-001 위반: 위로 돌아와서야 실행된다
    } finally {
        run.release().run();
    }
}
