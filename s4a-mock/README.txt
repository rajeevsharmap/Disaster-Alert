S4a mock data for the authority report detail screen.
- analysis-*.json are the value of the "analysis" field of GET /api/reports/{id} in each state.
- report-detail-analysis-completed.json is a whole detail response (status PENDING_REVIEW) with a COMPLETED analysis.
- retry-response.json is the body of POST /api/reports/{id}/analysis/retry (treat any 2xx as success).
- errors.json: bodies of the error cases. "error" text on FAILED/SKIPPED is free text: show it as is.
Reporters never receive "analysis". Only authority screens use it.
