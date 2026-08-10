import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError } from '../../api/errors';
import {
  getKakaoLinkStatus,
  issueKakaoConnectionCode,
  revokeKakaoLink,
} from '../../api/kakao';

const linkKey = ['kakao-link'] as const;

export function KakaoLinkPanel({ showTitle = true }: { showTitle?: boolean }) {
  const queryClient = useQueryClient();
  const status = useQuery({
    queryKey: linkKey,
    queryFn: getKakaoLinkStatus,
    refetchInterval: (query) => (query.state.data?.linked ? false : 3000),
  });
  const [copied, setCopied] = useState(false);
  const issue = useMutation({ mutationFn: issueKakaoConnectionCode });
  const revoke = useMutation({
    mutationFn: revokeKakaoLink,
    onSuccess: () => {
      issue.reset();
      queryClient.setQueryData(linkKey, { linked: false, linkedAt: null });
    },
  });
  const error = issue.error ?? revoke.error ?? status.error;

  if (status.isPending) {
    return <p className="kakao-link-loading">카카오톡 연동 상태를 확인하고 있어요.</p>;
  }

  const isLinked = Boolean(status.data?.linked);
  const copy = async () => {
    if (!issue.data) return;
    await navigator.clipboard.writeText(issue.data.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 900);
  };

  return (
    <section className="kakao-link-panel">
      {showTitle ? <h3 className="settings-section-title">카카오톡 연동</h3> : null}

      <div className="kakao-link-state">
        <span className={isLinked ? 'status-dot is-linked' : 'status-dot'} />
        <div>
          <strong>{isLinked ? '카카오톡이 연동되어 있어요' : '아직 연동이 되어있지 않아요'}</strong>
          <p>
            {isLinked
              ? '일정 확인, 등록, 수정이 카카오톡으로 바로 연결돼요.'
              : '우측 버튼으로 연결 코드를 발급해 카카오톡 대화창에 붙여넣으세요.'}
          </p>
        </div>
      </div>

      {issue.data && !isLinked ? (
        <div className="connection-code-card" aria-live="polite">
          <span>발급된 연결 코드</span>
          <strong>{issue.data.code.slice(0, 4)} {issue.data.code.slice(4)}</strong>
          <small>5분 뒤 자동 만료</small>
          <p>코드는 5분 뒤 만료되며, 최초 1회만 사용 가능합니다.</p>
          <button
            type="button"
            className="text-button"
            onClick={copy}
            disabled={issue.isPending}
          >
            {copied ? '복사 완료' : '코드 복사'}
          </button>
        </div>
      ) : null}

      {error ? (
        <p className="field-error" role="alert">
          {error instanceof ApiError ? error.message : '요청에 실패했습니다.'}
        </p>
      ) : null}

      {isLinked ? (
        <button
          className="danger-button"
          type="button"
          disabled={revoke.isPending}
          onClick={() => revoke.mutate()}
        >
          {revoke.isPending ? '연결 해제 중...' : '카카오톡 연동 해제'}
        </button>
      ) : (
        <button
          className="primary-button"
          type="button"
          disabled={issue.isPending}
          onClick={() => {
            setCopied(false);
            issue.mutate();
          }}
        >
          {issue.isPending ? '코드 발급 중...' : issue.data ? '연결 코드 다시 받기' : '연결 코드 발급'}
        </button>
      )}
    </section>
  );
}
