# 설치 및 실행 방법

Docker와 Docker Compose가 설치된 환경에서 실행합니다. 로컬에서는 PostgreSQL과 로컬 파일 저장소를 사용하며, 클라우드 간 사진 복제는 비활성화됩니다.

### 1) Repository Clone

```bash
git clone https://github.com/lty02-ops/Photo-storage.git
cd Photo-storage
```

### 2) 환경 변수 설정

```bash
cp infra/.env.example infra/.env
```

Windows PowerShell에서는 `Copy-Item infra/.env.example infra/.env`를 사용합니다.

`infra/.env`에 데이터베이스 비밀번호와 각 소셜 로그인 서비스에서 발급받은 Client 정보를 입력합니다.

```env
DB_PASSWORD=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=

KAKAO_CLIENT_ID=
```

소셜 로그인 Callback URL은 다음과 같습니다.

```text
Google: http://localhost:3000/login/oauth2/code/google
Naver:  http://localhost:3000/login/oauth2/code/naver
Kakao:  http://localhost:3000/login/oauth2/code/kakao
```

### 3) Docker Compose 실행

```bash
cd infra
docker compose up --build
```

### 4) 서비스 접속 및 종료

- 프론트엔드: http://localhost:3000
- 백엔드: http://localhost:8081

종료할 때는 `infra` 디렉터리에서 다음 명령어를 실행합니다.

```bash
docker compose down
```
