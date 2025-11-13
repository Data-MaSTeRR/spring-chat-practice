# Spring Chat Practice

Spring Boot와 WebSocket을 활용한 실시간 채팅 애플리케이션 연습 프로젝트입니다.

## 프로젝트 개요

두 사용자 간의 1:1 실시간 채팅을 구현한 학습용 프로젝트로, Spring Boot의 WebSocket과 STOMP 프로토콜을 활용하여 양방향 통신을 구현했습니다.

### 주요 기능

- 실시간 1:1 채팅
- 채팅방 생성 및 관리
- 과거 메시지 조회 (페이징 지원)
- 내 채팅방 목록 조회
- 채팅방 목록 UI

## 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA** - ORM 및 데이터 접근
- **Spring Web** - REST API
- **Spring WebSocket** - 실시간 양방향 통신
- **MySQL** - 데이터베이스
- **Lombok** - 코드 간소화

### Frontend
- **Thymeleaf** - 서버 사이드 템플릿 엔진
- **SockJS 1.5.1** - WebSocket 폴백 지원
- **STOMP 2.3.4** - WebSocket 메시징 프로토콜

### Build Tool
- **Gradle**

## ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│      USER       │
├─────────────────┤
│ *id (PK)        │
│  name           │
└─────────────────┘
        │
        │ hosts
        │ (1)
        │
        ├──────────────────┐
        │                  │
        │                  │ guests
       (1)                (1)
        │                  │
┌───────▼──────────────────▼───┐
│        CHAT_ROOM             │
├──────────────────────────────┤
│ *id (PK)                     │
│ +hostId (FK → USER.id)       │
│ +guestId (FK → USER.id)      │
└──────────────────────────────┘
        │
        │ contains
        │ (1)
        │
       (*)
        │
┌───────▼──────────────────────┐
│       CHAT_MESSAGE           │
├──────────────────────────────┤
│ *id (PK)                     │
│ +roomId (FK → CHAT_ROOM.id)  │
│ +senderId (FK → USER.id)     │
│  message                     │
│  createdAt                   │
└──────────────────────────────┘
        │
        │ sends
       (*)
        │
       (1)
        │
    (back to USER)

관계 표기법:
  *  = Primary Key
  +  = Foreign Key
  1  = 정확히 1개
  *  = 0개 이상
  ?  = 0개 또는 1개
  │  = 관계선
```

### 엔티티 설명

#### User (사용자)
- 채팅 사용자 정보를 저장
- 채팅방의 호스트 또는 게스트가 될 수 있음

#### ChatRoom (채팅방)
- 두 사용자 간의 1:1 채팅방을 나타냄
- `hostId`: 채팅방 개설자
- `guestId`: 채팅 상대방
- 동일한 사용자 조합에 대해 중복 생성 방지

#### ChatMessage (채팅 메시지)
- 채팅방 내에서 주고받은 메시지
- 발신자 정보와 생성 시간 자동 기록
- 메시지는 생성 시간 역순으로 조회 가능

## API 명세

### REST API

#### 1. 채팅방 생성
```http
POST /api/v1/chat/rooms
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| hostId | Long | O | 채팅방 개설자 ID |
| guestId | Long | O | 채팅 상대방 ID |

**Response**
```json
{
  "roomId": 1,
  "hostId": 1,
  "hostName": "user1",
  "guestId": 2,
  "guestName": "user2"
}
```

#### 2. 과거 메시지 조회
```http
GET /api/v1/chat/rooms/{roomId}/messages
```

**Path Parameters**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| roomId | Long | 채팅방 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|-------|------|
| page | int | X | 0 | 페이지 번호 (0부터 시작) |
| size | int | X | 10 | 페이지당 메시지 수 |

**Response**
```json
[
  {
    "messageId": 1,
    "roomId": 1,
    "senderId": 1,
    "senderName": "user1",
    "message": "안녕하세요",
    "createdAt": "2025-11-13T10:30:00"
  }
]
```

#### 3. 내 채팅방 목록 조회
```http
GET /api/v1/chat/rooms/my
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Long | O | 조회할 사용자 ID |

**Response**
```json
[
  {
    "roomId": 1,
    "hostId": 1,
    "hostName": "user1",
    "guestId": 2,
    "guestName": "user2"
  }
]
```

### WebSocket API

#### 연결 엔드포인트
```
ws://localhost:8080/ws-stomp
```

- SockJS 지원으로 WebSocket을 사용할 수 없는 환경에서도 동작

#### 메시지 전송 (STOMP)

**Subscribe (수신)**
```
/topic/chat/room/{roomId}
```
- 특정 채팅방의 메시지를 실시간으로 수신

**Send (송신)**
```
/app/chat/message
```

**Payload**
```json
{
  "roomId": 1,
  "senderId": 1,
  "message": "안녕하세요"
}
```

**Broadcast Response**
```json
{
  "messageId": 1,
  "roomId": 1,
  "senderId": 1,
  "senderName": "user1",
  "message": "안녕하세요",
  "createdAt": "2025-11-13T10:30:00"
}
```

### View Pages

#### 채팅방 목록
```http
GET /chat-rooms
```
- 전체 채팅방 목록을 보여주는 페이지

#### 채팅 페이지
```http
GET /chat-room/{roomId}?sender={userId}
```

**Path Parameters**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| roomId | Long | 채팅방 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| sender | Long | O | 현재 사용자 ID |

## 프로젝트 구조

```
src/main/java/com/example/chat/
├── ChatApplication.java              # 메인 애플리케이션
├── controller/
│   ├── ChatController.java           # WebSocket 메시지 처리
│   ├── ChatRoomController.java       # 채팅방 REST API
│   └── ChatViewController.java       # 뷰 컨트롤러
├── domain/                            # JPA 엔티티
│   ├── ChatMessage.java
│   ├── ChatRoom.java
│   └── User.java
├── dto/                               # Data Transfer Objects
│   ├── ChatMessageDto.java
│   └── ChatRoomDto.java
├── service/                           # 비즈니스 로직
│   ├── ChatService.java
│   ├── ChatRoomService.java
│   └── UserService.java
├── repository/                        # 데이터 접근 계층
│   ├── ChatMessageRepository.java
│   ├── ChatRoomRepository.java
│   └── UserRepository.java
└── config/                            # 설정
    ├── WebSocketConfig.java          # WebSocket 설정
    └── DataInitializer.java          # 초기 데이터 생성
```

## 설치 및 실행

### 사전 요구사항

- Java 17 이상
- MySQL 8.0 이상
- Gradle 7.x 이상

### 데이터베이스 설정

1. MySQL에 데이터베이스 생성
```sql
CREATE DATABASE chat_db;
```

2. `src/main/resources/application.properties` 또는 `application.yml` 설정
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/chat_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### 실행 방법

1. 프로젝트 클론
```bash
git clone <repository-url>
cd chat
```

2. 빌드 및 실행
```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/chat-0.0.1-SNAPSHOT.jar
```

3. 브라우저에서 접속
```
http://localhost:8080
```

### 초기 데이터

애플리케이션 시작 시 `DataInitializer`가 자동으로 다음을 생성합니다:
- 테스트 사용자 3명 (user1, user2, user3)
- 테스트 채팅방 2개
- 샘플 메시지

## 주요 기능 흐름

### 1. 실시간 메시지 전송
```
클라이언트 (WebSocket)
  → /app/chat/message (STOMP)
  → ChatController.sendMessage()
  → ChatService.processAndSaveMessage()
  → DB 저장 + 발신자 이름 조회
  → /topic/chat/room/{roomId} (브로드캐스트)
  → 해당 채팅방 구독 클라이언트 수신
```

### 2. 채팅방 생성
```
클라이언트
  → POST /api/v1/chat/rooms
  → ChatRoomService.createRoom()
  → 중복 체크 (findByTwoUsers)
  → 새 채팅방 생성 또는 기존 채팅방 반환
```

### 3. 과거 메시지 조회
```
클라이언트
  → GET /api/v1/chat/rooms/{roomId}/messages
  → ChatRoomService.getMessages()
  → 페이징된 메시지 조회 + 발신자 이름 매핑
```

## 설계 특징

### 1. 양방향 채팅방 지원
- `ChatRoomRepository.findByTwoUsers()` 메서드가 hostId/guestId 순서와 관계없이 동일한 채팅방을 반환
- 중복 채팅방 생성 방지

### 2. DTO 패턴 활용
- 엔티티와 별도로 DTO를 사용하여 클라이언트에 필요한 정보 전달
- 엔티티에 없는 필드(senderName, hostName, guestName) 추가 가능

### 3. 페이징 지원
- Spring Data JPA의 `Pageable`을 활용한 효율적인 메시지 조회
- 과거 메시지를 필요한 만큼만 로드

### 4. 자동 타임스탬프
- `@CreationTimestamp`를 사용하여 메시지 생성 시간 자동 기록

### 5. WebSocket 폴백
- SockJS를 통해 WebSocket을 지원하지 않는 환경에서도 동작 가능

## 학습 포인트

이 프로젝트를 통해 다음을 학습할 수 있습니다:

- Spring Boot와 WebSocket을 활용한 실시간 통신 구현
- STOMP 프로토콜을 이용한 메시지 브로커 패턴
- Spring Data JPA를 활용한 효율적인 데이터 접근
- REST API와 WebSocket API의 조합
- 엔티티와 DTO를 분리한 계층화 아키텍처
- 페이징을 통한 대용량 데이터 처리

## 라이선스

이 프로젝트는 학습 목적으로 제작되었습니다.
