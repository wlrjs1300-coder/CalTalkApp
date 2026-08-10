# KakaoTalk integration

## Product goal

CalTalk must let a linked user query, create, and update their schedules from a KakaoTalk Channel conversation. Kakao social login alone does not satisfy this goal.

The conversational AI routing, safety policy, and current top-priority definition are maintained in [`AI_SCHEDULING_ASSISTANT.md`](AI_SCHEDULING_ASSISTANT.md). That document is normative for natural-language work.

## Delivery status

- [x] Phase 1: Kakao chatbot skill webhook contract and protected connection endpoint
- [x] Phase 2: Kakao chatbot user to CalTalk account linking
- [x] Phase 3: Ollama-first natural-language provider and structured command output
- [x] Phase 4: Free-form schedule queries and follow-up questions
- [x] Phase 5: Schedule creation/update/delete with confirmation
- [x] Phase 6: Optional OpenAI fallback for sufficiently specified but complex requests
- [ ] Phase 7: Production monitoring, replay protection, stable HTTPS deployment, and broader end-to-end channel tests

Verified KakaoTalk E2E flows include account linking, query, create, update, delete,
ambiguous candidate selection, confirmation, cancellation, and callback delivery.
Natural-language requests receive a direct response when processing completes within
2.5 seconds; slower requests switch to the Kakao Callback API. Fast follow-up commands
such as confirmation and cancellation always use a direct response.

## Phase 1 endpoint

The chatbot skill server endpoint is:

```text
POST /api/v1/kakao/skill
```

The Kakao Chatbot Admin Center skill must send this header:

```text
X-CalTalk-Skill-Secret: <KAKAO_CHATBOT_SKILL_SECRET value>
```

Required backend environment variables:

```properties
KAKAO_CHATBOT_ENABLED=true
KAKAO_CHATBOT_SKILL_SECRET=<generate-a-long-random-secret>
```

The URL registered with Kakao must be a publicly reachable HTTPS URL. `localhost` cannot receive requests from Kakao. For production, use:

```text
https://<backend-domain>/api/v1/kakao/skill
```

For a temporary development tunnel, use the tunnel's HTTPS origin with the same path.

The local stack and temporary tunnel can be restored from the repository root with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1 -CopySkillUrl
```

The script prints the complete Kakao skill URL. A Cloudflare Quick Tunnel URL may change
after restart, so update both Kakao `URL` and `Test URL`, preserve the secret headers,
save, and deploy when the printed URL differs. A stable production domain remains the
preferred deployment target.

On Render, register the public frontend URL plus `/api/v1/kakao/skill`. The frontend
Nginx proxy forwards that path to the private backend; the private service itself has
no public URL. Render Key Value stores pending confirmations, candidate selections,
and the OpenAI fallback budget.

## Kakao administrator setup

1. Create a KakaoTalk Channel in KakaoTalk Channel Admin Center.
2. Create a KakaoTalk chatbot in Chatbot Admin Center.
3. Connect the chatbot to the development channel under bot settings.
4. Create a skill named `CalTalk 일정 비서`.
5. Enter the public HTTPS endpoint above as the Test URL.
6. Add `X-CalTalk-Skill-Secret` and the configured secret as the test header.
7. Run the skill test. A successful Phase 1 response says the connection is ready and account linking is the next step.
8. Connect the skill to a fallback or schedule-assistant block and select skill data as the response.
9. Enable Callback on every block that can invoke the skill, including the fallback block.

The secret is independent from Kakao REST API keys and OAuth client secrets. Never commit it.

## Security boundary

The webhook is public because Kakao, not a browser session, calls it. Browser CSRF and CalTalk session authentication therefore do not apply to this route. Instead, the endpoint requires a dedicated high-entropy header secret. Account-level schedule access remains unavailable until Phase 2 verifies and stores the Kakao chatbot identity mapping.
