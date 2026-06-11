export const meta = {
  name: 'dev-pipeline',
  description: '문서 검증 → PLAN 구현 → 테스트/컴플라이언스 병렬 검사 전체 파이프라인',
  phases: [
    { title: '문서 정합성 검증', detail: 'doc-consistency-verifier: 문서 간 충돌·중복 탐지' },
    { title: 'PLAN 구현',       detail: 'ai-action-implementer: PLAN 항목 순서대로 코드 구현' },
    { title: '병렬 검증',       detail: 'test-verifier + compliance-verifier 동시 실행' },
    { title: '최종 리포트',     detail: '전체 결과 종합 및 Go/No-Go 판정' },
  ],
};

// ── 스키마 정의 ─────────────────────────────────────────────

const DOC_SCHEMA = {
  type: 'object',
  properties: {
    scanned_files:  { type: 'array', items: { type: 'string' } },
    conflicts:      { type: 'array' },
    duplications:   { type: 'array' },
    summary: {
      type: 'object',
      properties: {
        total_conflicts:   { type: 'number' },
        total_duplications:{ type: 'number' },
        passed:            { type: 'boolean' },
        verdict:           { type: 'string' },
        message:           { type: 'string' },
      },
      required: ['verdict', 'passed'],
    },
  },
  required: ['summary'],
};

const IMPL_SCHEMA = {
  type: 'object',
  properties: {
    plan_items:    { type: 'array' },
    changed_files: { type: 'array' },
    summary: {
      type: 'object',
      properties: {
        total_items: { type: 'number' },
        done:        { type: 'number' },
        failed:      { type: 'number' },
        verdict:     { type: 'string' },
        message:     { type: 'string' },
      },
      required: ['verdict'],
    },
  },
  required: ['summary'],
};

const TEST_SCHEMA = {
  type: 'object',
  properties: {
    test_suites: { type: 'array' },
    summary: {
      type: 'object',
      properties: {
        total_tests:   { type: 'number' },
        total_passed:  { type: 'number' },
        total_failed:  { type: 'number' },
        verdict:       { type: 'string' },
        build_status:  { type: 'string' },
        message:       { type: 'string' },
      },
      required: ['verdict', 'build_status'],
    },
  },
  required: ['summary'],
};

const COMPLIANCE_SCHEMA = {
  type: 'object',
  properties: {
    plan_requirements:    { type: 'array' },
    over_implementations: { type: 'array' },
    summary: {
      type: 'object',
      properties: {
        total_requirements: { type: 'number' },
        satisfied:          { type: 'number' },
        compliance_rate:    { type: 'string' },
        verdict:            { type: 'string' },
        message:            { type: 'string' },
      },
      required: ['verdict', 'compliance_rate'],
    },
  },
  required: ['summary'],
};

// ── Phase 1: 문서 정합성 검증 ───────────────────────────────

phase('문서 정합성 검증');
log('SubAgent1: 프로젝트 문서 파일 스캔 중...');

const docResult = await agent(
  `프로젝트 루트(C:\\reviewer\\json-poc)의 모든 문서 파일(*.md, PLAN*, *.txt)을 읽고
   문서 간 충돌(서로 다른 설명)과 중복(같은 내용 반복)을 검증하라.
   현재 문서 목록: README.md, .claude/agents/*.md, .claude/workflows/*.js
   입력된 args(PLAN 경로): ${args && args.planPath ? args.planPath : '없음'}`,
  { label: 'SubAgent1: 문서 정합성', agentType: 'doc-consistency-verifier', schema: DOC_SCHEMA }
);

if (!docResult) {
  log('SubAgent1 실패 - 파이프라인 중단');
  return { verdict: 'ABORTED', reason: 'SubAgent1 실패' };
}

log(`문서 검증 결과: ${docResult.summary.verdict} — ${docResult.summary.message}`);

if (!docResult.summary.passed) {
  log(`충돌 ${docResult.summary.total_conflicts}건, 중복 ${docResult.summary.total_duplications}건 발견`);
  log('문서 정합성 문제가 있습니다. 계속 진행하려면 planPath에 PLAN 경로를 지정하세요.');
}

// ── Phase 2: PLAN 기반 구현 ─────────────────────────────────

phase('PLAN 구현');

const planPath = args && args.planPath ? args.planPath : null;
if (!planPath) {
  log('planPath가 없습니다. 구현 단계를 건너뜁니다.');
  return {
    doc_check: docResult.summary,
    verdict: 'SKIPPED',
    reason: 'planPath args가 없어 구현 단계를 건너뜀. args: { planPath: "경로" } 형식으로 전달하세요.',
  };
}

log(`SubAgent2: ${planPath} 기반으로 구현 시작...`);

const implResult = await agent(
  `다음 PLAN 파일을 읽고 지시된 모든 항목을 순서대로 구현하라: ${planPath}
   기존 코드 구조(model/parser/repository/service/ui 패키지)를 유지하고
   PLAN에 명시된 것만 구현한다. 추가 기능을 임의로 넣지 않는다.`,
  { label: 'SubAgent2: 구현', agentType: 'ai-action-implementer', schema: IMPL_SCHEMA }
);

if (!implResult) {
  log('SubAgent2 실패 - 파이프라인 중단');
  return { doc_check: docResult.summary, verdict: 'ABORTED', reason: 'SubAgent2 실패' };
}

log(`구현 결과: ${implResult.summary.verdict} — ${implResult.summary.message}`);
log(`완료: ${implResult.summary.done}개, 실패: ${implResult.summary.failed}개`);

if (implResult.summary.verdict === 'FAILED') {
  log('구현 실패 - 파이프라인 중단');
  return { doc_check: docResult.summary, impl: implResult.summary, verdict: 'ABORTED' };
}

// ── Phase 3: 테스트 + 컴플라이언스 병렬 실행 ───────────────

phase('병렬 검증');
log('SubAgent3(Test) + SubAgent4(Compliance) 병렬 실행 시작...');

const [testResult, complianceResult] = await parallel([
  () => agent(
    `구현된 코드의 전체 테스트를 실행하라.
     JAVA_HOME: C:\\Users\\User\\.jdks\\temurin-17.0.19
     프로젝트 경로: C:\\reviewer\\json-poc
     명령: .\\gradlew.bat test --rerun-tasks
     실패한 테스트가 있으면 원인을 분석하고 수정 후 재실행한다.`,
    { label: 'SubAgent3: 테스트', agentType: 'test-verifier', schema: TEST_SCHEMA }
  ),
  () => agent(
    `다음 PLAN 파일의 모든 요구사항이 구현 코드에서 충족되었는지 검증하라: ${planPath}
     구현된 파일 목록: ${JSON.stringify((implResult.changed_files || []).map(f => f.path))}
     각 요구사항을 코드와 1:1 대조하고 충족 여부를 판정한다.`,
    { label: 'SubAgent4: 컴플라이언스', agentType: 'compliance-verifier', schema: COMPLIANCE_SCHEMA }
  ),
]);

// ── Phase 4: 최종 리포트 ────────────────────────────────────

phase('최종 리포트');

const testVerdict       = testResult?.summary?.verdict ?? 'UNKNOWN';
const complianceVerdict = complianceResult?.summary?.verdict ?? 'UNKNOWN';
const overallPass = testVerdict === 'PASS' && complianceVerdict === 'PASS';

log(`[Test]        ${testVerdict} — ${testResult?.summary?.message ?? '결과 없음'}`);
log(`[Compliance]  ${complianceVerdict} (${complianceResult?.summary?.compliance_rate ?? '?'}) — ${complianceResult?.summary?.message ?? '결과 없음'}`);
log(`[최종 판정]   ${overallPass ? '✅ GO' : '❌ NO-GO'}`);

return {
  phases: {
    doc_check:   docResult.summary,
    impl:        implResult.summary,
    test:        testResult?.summary,
    compliance:  complianceResult?.summary,
  },
  verdict:  overallPass ? 'GO' : 'NO-GO',
  message: overallPass
    ? '모든 검증 통과. 배포 가능합니다.'
    : `검증 실패: Test=${testVerdict}, Compliance=${complianceVerdict}`,
};
