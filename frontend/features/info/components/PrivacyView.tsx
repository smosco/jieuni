import { CONTACT_EMAIL, EFFECTIVE_DATE, PRIVACY_OFFICER } from "../contact";
import { InfoPage, InfoSection, List, P } from "./InfoPage";

export function PrivacyView() {
  const mail = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

  return (
    <InfoPage label="PRIVACY" title="개인정보처리방침" effectiveDate={EFFECTIVE_DATE}>
      <P>
        공간의 지은이(이하 &ldquo;서비스&rdquo;)는 개인정보 보호법에 따라 이용자의 개인정보를 아래처럼 처리해요. 로그인하지
        않고 공간을 둘러보는 동안에는 개인정보를 받지 않아요.
      </P>

      <InfoSection title="1. 처리 목적">
        <List
          items={[
            "회원 식별과 로그인 유지",
            "공간 저장(가고 싶어요·다녀왔어요)과 만든 사람 팔로우",
            "이용자가 보낸 공간 소개·크레딧 제보의 검토",
            "문의와 삭제·정정 요청에 대한 답변",
          ]}
        />
      </InfoSection>

      <InfoSection title="2. 처리 항목">
        <List
          items={[
            "구글 로그인으로 받는 것: 이메일 주소, 이름(닉네임으로 쓰며 20자를 넘으면 자름), 프로필 사진 주소, 구글 계정 식별자",
            "서비스를 쓰며 생기는 것: 닉네임 변경, 저장한 공간, 팔로우한 만든 사람, 보낸 제보(공간 이름·이유·참고 링크·만든 사람 정보)",
            "문의 메일을 보내면: 보낸 메일 주소와 메일 내용",
            "자동으로 생기는 것: 로그인 유지용 쿠키(SESSION), 요청 위조 방지용 쿠키(XSRF-TOKEN), 서버 접속 기록(IP 주소, 요청 시각, 요청 주소)",
          ]}
        />
        <P>
          쿠키 두 개는 로그인에만 써요. 브라우저에서 쿠키를 막으면 로그인할 수 없지만 둘러보기는 그대로 돼요. 이용자를
          추적하는 분석 도구나 광고 쿠키는 쓰지 않아요.
        </P>
      </InfoSection>

      <InfoSection title="3. 보유 기간과 파기">
        <List
          items={[
            "회원 정보와 저장·팔로우: 탈퇴할 때까지. 탈퇴하면 계정, 로그인 세션, 저장, 팔로우를 바로 지워요.",
            "보낸 제보: 소개에 쓰일 수 있어 탈퇴 뒤에도 남지만, 누가 보냈는지는 탈퇴할 때 지워요.",
            "데이터베이스 백업: 매일 만들고 7일 보관 뒤 자동으로 지워요. 그래서 탈퇴한 사용자의 정보도 백업에는 최대 7일 남아 있어요.",
            "서버 접속 기록: 용량 한도(10MB씩 3개)를 넘으면 오래된 것부터 자동으로 지워요.",
            "문의 메일: 답변과 처리가 끝나면 지워요.",
          ]}
        />
        <P>지울 때는 데이터베이스와 저장소에서 다시 살릴 수 없게 삭제해요.</P>
      </InfoSection>

      <InfoSection title="4. 제3자 제공">
        <P>이용자의 개인정보를 다른 곳에 제공하지 않아요. 법령에 따른 요청이 있을 때만 예외예요.</P>
      </InfoSection>

      <InfoSection title="5. 처리 위탁">
        <List
          items={[
            "Amazon Web Services: 서버와 데이터베이스 운영(서울 리전에 저장)",
            "Vercel: 웹 화면 제공과 요청 전달",
            "Cloudflare: 도메인 운영, 데이터베이스 백업 보관, 문의 메일 전달",
            "Google: 로그인 인증, 문의 메일 수신(Gmail)",
          ]}
        />
      </InfoSection>

      <InfoSection title="6. 국외 이전">
        <P>
          아래 업체는 국외에서 개인정보를 처리해요. 서비스를 쓸 때마다 또는 정해진 때에 네트워크로 전송돼요. 이전을
          원하지 않으면 회원 가입을 하지 않거나 탈퇴하면 되고, 이 경우 로그인이 필요한 기능은 쓸 수 없어요.
        </P>
        <List
          items={[
            "Vercel Inc.(미국, privacy@vercel.com): 접속 기록과 쿠키가 담긴 요청을 화면 제공·전달을 위해 이용할 때마다 처리. 업체의 기록 보관 기간 동안",
            "Cloudflare, Inc.(미국, dpo@cloudflare.com): 데이터베이스 백업 전체를 보관을 위해 매일 전송, 7일 보관. 문의 메일을 받은 편지함으로 전달할 때 메일 주소와 내용 처리, 보관하지 않음",
            "Google LLC(미국, support.google.com/policies): 문의 메일의 주소와 내용을 수신·보관. 처리가 끝나면 삭제",
          ]}
        />
      </InfoSection>

      <InfoSection title="7. 만 14세 미만">
        <P>만 14세 미만은 가입할 수 없어요. 가입할 때 만 14세 이상인지 확인해요.</P>
      </InfoSection>

      <InfoSection title="8. 이용자의 권리">
        <P>
          이용자는 자기 개인정보의 열람, 정정, 삭제, 처리 정지를 요청할 수 있어요. 닉네임 변경과 탈퇴는 내 공간의
          &ldquo;설정&rdquo;에서 바로 할 수 있고, 그 밖의 요청은 {mail}로 보내 주세요. 지체 없이, 늦어도 10일 안에 답해요.
        </P>
      </InfoSection>

      <InfoSection title="9. 안전성 확보 조치">
        <List
          items={[
            "모든 통신은 HTTPS로 암호화해요.",
            "로그인 쿠키는 스크립트로 읽을 수 없고(HttpOnly) HTTPS로만 보내요(Secure).",
            "관리자 기능은 정해진 관리자 계정만 쓸 수 있어요.",
            "백업은 공개되지 않는 저장소에 두고, 접근 권한을 최소로 둬요.",
          ]}
        />
      </InfoSection>

      <InfoSection title="10. 개인정보 보호책임자">
        <P>
          {PRIVACY_OFFICER} · {mail}
        </P>
      </InfoSection>

      <InfoSection title="11. 권익 침해 구제">
        <P>개인정보 침해에 대한 상담이나 신고는 아래 기관에도 할 수 있어요.</P>
        <List
          items={[
            "개인정보침해신고센터: 118 (privacy.kisa.or.kr)",
            "개인정보분쟁조정위원회: 1833-6972 (www.kopico.go.kr)",
            "대검찰청: 1301 (www.spo.go.kr)",
            "경찰청: 182 (ecrm.police.go.kr)",
          ]}
        />
      </InfoSection>

      <InfoSection title="12. 변경">
        <P>
          이 방침을 바꾸면 시행 7일 전에 이 페이지에 알려요. 이 방침은 {EFFECTIVE_DATE}부터 시행해요.
        </P>
      </InfoSection>
    </InfoPage>
  );
}
