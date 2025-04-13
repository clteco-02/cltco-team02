<template>
  <div id="app">
    <h1>K8s 마이크로서비스 데모</h1>
    
    <!-- 로그인 섹션 추가 -->
    <div class="login-section">
      <div v-if="!isLoggedIn">
        <input v-model="credentials.id" placeholder="ID" type="text" class="login-input">
        <input v-model="credentials.password" placeholder="비밀번호" type="password" class="login-input">
        <button @click="login" class="login-btn">로그인</button>
      </div>
      <div v-else class="logged-in-info">
        <span><strong>{{ userId }}</strong>님으로 로그인됨</span>
        <button @click="logout" class="logout-btn">로그아웃</button>
      </div>
    </div>
    
    <!-- 비로그인 사용자에게 보이는 메시지 -->
    <div class="container" v-if="!isLoggedIn">
      <div class="login-required-message">
        <p>서비스를 이용하려면 로그인이 필요합니다.</p>
        <p>ID와 비밀번호를 입력하여 로그인해 주세요.</p>
      </div>
    </div>
    
    <!-- 로그인한 사용자에게만 보이는 섹션 -->
    <div class="container" v-if="isLoggedIn">
      <div class="section">
        <h2>MariaDB 메시지 관리</h2>
        <input v-model="dbMessage" placeholder="저장할 메시지 입력">
        <button @click="saveToDb">DB에 저장</button>
        <button @click="getFromDb">DB에서 조회</button>
        <button @click="insertSampleData" class="sample-btn">샘플 데이터 저장</button>
        
        <!-- 검색 기능 추가 -->
        <div class="search-container">
          <input v-model="searchKeyword" placeholder="검색어 입력" class="search-input">
          <button @click="searchMessages" class="search-btn">검색</button>
          <button v-if="isSearchActive" @click="clearSearch" class="clear-btn">검색 취소</button>
        </div>
        
        <div v-if="loading" class="loading-spinner">
          <p>데이터를 불러오는 중...</p>
        </div>
        <div v-if="dbData.length && !loading">
          <h3>{{ isSearchActive ? '검색 결과:' : '저장된 메시지:' }}</h3>
          <ul>
            <li v-for="item in dbData" :key="item.id">{{ item.message }} ({{ formatDate(item.created_at) }})</li>
          </ul>
        </div>
        <div v-if="isSearchActive && dbData.length === 0 && !loading" class="no-results">
          <p>검색 결과가 없습니다.</p>
        </div>
      </div>

      <div class="section">
        <h2>Redis 로그</h2>
        <button @click="getRedisLogs">로그 조회</button>
        <div v-if="redisLogs.length">
          <h3>API 호출 로그:</h3>
          <ul>
            <li v-for="(log, index) in redisLogs" :key="index">
              [{{ formatDate(log.timestamp) }}] {{ log.action }}: {{ log.details }}
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

// nginx 프록시를 통해 요청하도록 수정
const API_BASE_URL = '/api';

export default {
  name: 'App',
  data() {
    return {
      // 로그인 관련 데이터 추가
      credentials: {
        id: '',
        password: ''
      },
      isLoggedIn: false,
      userId: '',
      // 기존 데이터
      dbMessage: '',
      dbData: [],
      redisLogs: [],
      sampleMessages: [
        '안녕하세요! 테스트 메시지입니다.',
        'K8s 데모 샘플 데이터입니다.',
        '마이크로서비스 테스트 중입니다.',
        '샘플 메시지 입니다.'
      ],
      offset: 0,
      limit: 20,
      loading: false,
      hasMore: true,
      searchKeyword: '',
      isSearchActive: false
    }
  },
  methods: {
    // 세션 관리 메소드 추가
    setupAxiosInterceptors() {
      // 요청 인터셉터
      axios.interceptors.request.use(
        config => {
          const sessionId = localStorage.getItem('sessionId');
          if (sessionId && !config.url.includes('/auth/login')) {
            config.headers['X-Session-ID'] = sessionId;
          }
          return config;
        },
        error => {
          return Promise.reject(error);
        }
      );

      // 응답 인터셉터
      axios.interceptors.response.use(
        response => {
          return response;
        },
        error => {
          if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.log('인증 세션이 만료되었습니다.');
            localStorage.removeItem('sessionId');
            this.isLoggedIn = false;
            this.userId = '';
            
            // 로그인 페이지로 리디렉션하거나 다시 로그인하도록 유도할 수 있음
            // 여기서는 단순히 상태만 변경
          }
          return Promise.reject(error);
        }
      );
    },
    
    // 로그인 관련 메소드
    async login() {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/login`, this.credentials);
        if (response.data && response.data.sessionId) {
          localStorage.setItem('sessionId', response.data.sessionId);
          this.isLoggedIn = true;
          this.userId = this.credentials.id;
          this.credentials.password = ''; // 보안을 위해 비밀번호 초기화
        }
      } catch (error) {
        console.error('로그인 실패:', error);
        // 401, 403 상태 코드 처리
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
          localStorage.removeItem('sessionId');
          this.isLoggedIn = false;
          this.userId = '';
        }
        alert('로그인에 실패했습니다. ID와 비밀번호를 확인해주세요.');
      }
    },
    
    async logout() {
      try {
        const sessionId = localStorage.getItem('sessionId');
        if (sessionId) {
          await axios.post(`${API_BASE_URL}/auth/logout`, {}, {
            headers: { 'X-Session-ID': sessionId }
          });
        }
      } catch (error) {
        console.error('로그아웃 실패:', error);
        // 401, 403 상태 코드는 이미 세션이 만료된 것으로 간주
      } finally {
        // 로그아웃 시도 후에는 항상 로컬 세션 정보 초기화
        localStorage.removeItem('sessionId');
        this.isLoggedIn = false;
        this.userId = '';
      }
    },
    
    async checkLoginStatus() {
      const sessionId = localStorage.getItem('sessionId');
      if (sessionId) {
        try {
          const response = await axios.get(`${API_BASE_URL}/auth/validate`, {
            headers: { 'X-Session-ID': sessionId }
          });
          if (response.data && response.data.valid) {
            this.isLoggedIn = true;
            this.userId = response.data.userId;
          } else {
            // 세션이 유효하지 않으면 로컬 스토리지에서 제거
            localStorage.removeItem('sessionId');
            this.isLoggedIn = false;
            this.userId = '';
          }
        } catch (error) {
          console.error('세션 검증 실패:', error);
          // 401, 403 상태 코드 처리 (인증 관련 오류)
          if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.log('인증 세션이 만료되었습니다.');
          }
          localStorage.removeItem('sessionId');
          this.isLoggedIn = false;
          this.userId = '';
        }
      }
    },
    
    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleString();
    },
    
    async saveToDb() {
      try {
        await axios.post(`${API_BASE_URL}/db/message`, {
          message: this.dbMessage
        });
        this.dbMessage = '';
        this.getFromDb();
        this.getRedisLogs();
      } catch (error) {
        console.error('DB 저장 실패:', error);
      }
    },

    async getFromDb() {
      try {
        this.loading = true;
        const response = await axios.get(`${API_BASE_URL}/db/messages?offset=${this.offset}&limit=${this.limit}`);
        this.dbData = response.data;
        this.hasMore = response.data.length === this.limit;
      } catch (error) {
        console.error('DB 조회 실패:', error);
      } finally {
        this.loading = false;
      }
    },

    async insertSampleData() {
      const randomMessage = this.sampleMessages[Math.floor(Math.random() * this.sampleMessages.length)];
      try {
        await axios.post(`${API_BASE_URL}/db/message`, {
          message: randomMessage
        });
        this.getFromDb();
        this.getRedisLogs();
      } catch (error) {
        console.error('샘플 데이터 저장 실패:', error);
      }
    },

    async getRedisLogs() {
      try {
        const response = await axios.get(`${API_BASE_URL}/logs/redis`);
        this.redisLogs = response.data;
      } catch (error) {
        console.error('Redis 로그 조회 실패:', error);
      }
    },

    async loadMore() {
      this.offset += this.limit;
      await this.getFromDb();
    },

    async searchMessages() {
      if (!this.searchKeyword.trim()) {
        this.clearSearch();
        return;
      }
      
      try {
        this.loading = true;
        const response = await axios.get(`${API_BASE_URL}/db/search?keyword=${encodeURIComponent(this.searchKeyword)}`);
        this.dbData = response.data;
        this.isSearchActive = true;
      } catch (error) {
        console.error('메시지 검색 실패:', error);
        alert('검색 중 오류가 발생했습니다.');
      } finally {
        this.loading = false;
      }
    },

    clearSearch() {
      this.searchKeyword = '';
      this.isSearchActive = false;
      this.getFromDb();
    }
  },
  // 컴포넌트가 마운트될 때 설정
  mounted() {
    this.setupAxiosInterceptors();
    this.checkLoginStatus();
  }
}
</script>

<style>
// 로그인 관련 스타일 추가
.login-section {
  max-width: 800px;
  margin: 0 auto 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 5px;
  display: flex;
  justify-content: center;
}

.login-input {
  margin-right: 10px;
  padding: 8px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  width: 180px;
}

.login-btn {
  background-color: #28a745;
  padding: 8px 15px;
}

.logout-btn {
  background-color: #dc3545;
  margin-left: 15px;
}

.logged-in-info {
  display: flex;
  align-items: center;
}

.login-required-message {
  text-align: center;
  padding: 40px 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin: 40px auto;
  max-width: 500px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.login-required-message p {
  margin: 10px 0;
  color: #6c757d;
  font-size: 18px;
}

.login-required-message p:first-child {
  font-weight: bold;
  color: #495057;
  font-size: 20px;
}

// 기존 스타일
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 5px;
}

input {
  margin-right: 10px;
  padding: 5px;
  width: 300px;
}

button {
  margin-right: 10px;
  padding: 5px 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

.sample-btn {
  background-color: #28a745;
}

.sample-btn:hover {
  background-color: #218838;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  margin: 5px 0;
  padding: 5px;
  border-bottom: 1px solid #eee;
}

.pagination {
  text-align: center;
  margin-top: 10px;
}

.pagination button {
  padding: 5px 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}

.pagination button:hover {
  background-color: #0056b3;
}

.pagination button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading-spinner {
  text-align: center;
  margin-top: 20px;
  font-size: 16px;
  color: #555;
}

.search-container {
  margin-top: 10px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.search-input {
  padding: 8px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  width: 200px;
}

.search-btn {
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 15px;
  margin-left: 10px;
  cursor: pointer;
}

.search-btn:hover {
  background-color: #0056b3;
}

.clear-btn {
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 15px;
  margin-left: 10px;
  cursor: pointer;
}

.clear-btn:hover {
  background-color: #c82333;
}

.no-results {
  text-align: center;
  margin-top: 10px;
  color: #dc3545;
  font-size: 16px;
}
</style> 