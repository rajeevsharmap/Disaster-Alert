S4b mock data (authority review, draft and publish screens; reporter History).
All shapes follow PROJECT_HANDOFF.md section 9 (S4b entries) and 14.5 D/E/F.
- report-detail-*.json: GET /api/reports/{id} in each state (pending review, needs clarification, verified without draft, verified with draft v3, published SENDING / COMPLETED / COMPLETED_WITH_FAILURES, resolved, rejected).
- decision-request-*.json / decision-response.json: POST /api/reports/{id}/decision.
- hazard-draft-put-*.json: PUT /api/reports/{id}/hazard-alert. publish-*.json: POST /api/hazard-alerts/{id}/publish. hazard-alert-get.json: GET /api/hazard-alerts/{id}.
- reports-mine-s4b.json: reporter History with clarification and alert filled.
- errors.json: example error bodies. The wording of the real error text can differ: always show the server's {"error"} text as it is.
instructions is ONE string, one bullet per line. Times are UTC, show IST.
