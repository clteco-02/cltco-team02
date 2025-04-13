from flask import Flask, request, jsonify
from flask_cors import CORS
import redis
import mysql.connector
import json
from datetime import datetime
import os

app = Flask(__name__)
CORS(app)

# MariaDB 연결 함수
def get_db_connection():
    return mysql.connector.connect(
        host=os.getenv('MYSQL_HOST', 'my-mariadb'),
        user=os.getenv('MYSQL_USER', 'testuser'),
        password=os.getenv('MYSQL_PASSWORD'),
        database="testdb",
        connect_timeout=30
    )

# Redis 연결 함수
def get_redis_connection():
    return redis.Redis(
        host=os.getenv('REDIS_HOST', 'my-redis-master'),
        port=6379,
        password=os.getenv('REDIS_PASSWORD'),
        decode_responses=True,
        db=0
    )

# 로깅 함수
def log_to_redis(action, details):
    try:
        redis_client = get_redis_connection()
        log_entry = {
            'timestamp': datetime.now().isoformat(),
            'action': action,
            'details': details
        }
        redis_client.lpush('api_logs', json.dumps(log_entry))
        redis_client.ltrim('api_logs', 0, 99)  # 최근 100개 로그만 유지
        redis_client.close()
    except Exception as e:
        print(f"Redis logging error: {str(e)}")

# MariaDB 엔드포인트
@app.route('/db/message', methods=['POST'])
def save_to_db():
    try:
        db = get_db_connection()
        data = request.json
        cursor = db.cursor()
        sql = "INSERT INTO messages (message, created_at) VALUES (%s, %s)"
        cursor.execute(sql, (data['message'], datetime.now()))
        db.commit()
        cursor.close()
        db.close()
        
        # 로깅
        log_to_redis('db_insert', f"Message saved: {data['message'][:30]}...")
        
        return jsonify({"status": "success"})
    except Exception as e:
        log_to_redis('db_insert_error', str(e))
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/db/messages', methods=['GET'])
def get_from_db():
    try:
        db = get_db_connection()
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM messages ORDER BY created_at DESC")
        messages = cursor.fetchall()
        cursor.close()
        db.close()
        
        # 로깅
        log_to_redis('db_select', f"Retrieved {len(messages)} messages")
        
        return jsonify(messages)
    except Exception as e:
        log_to_redis('db_select_error', str(e))
        return jsonify({"status": "error", "message": str(e)}), 500

# Redis 로그 조회
@app.route('/logs/redis', methods=['GET'])
def get_redis_logs():
    try:
        redis_client = get_redis_connection()
        logs = redis_client.lrange('api_logs', 0, -1)
        redis_client.close()
        return jsonify([json.loads(log) for log in logs])
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True) 