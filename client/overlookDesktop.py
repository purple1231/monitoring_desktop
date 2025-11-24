import sys

import psutil
import time
from datetime import datetime
import requests
import json
import threading
import subprocess
from flask import Flask, request, jsonify

app = Flask(__name__)

CODE_APPS = [
    "code.exe",      # VS Code 예시
    "pycharm64.exe",
    "idea64.exe",
    "eclipse.exe",
    "cmd.exe",
    "powershell.exe"
]

GAME_APPS = [
    "leagueoflegends.exe",
    "steam.exe",
    "Battle.net.exe",
    "Origin.exe",
    "EpicGamesLauncher.exe",

]

MUSIC_APPS = [
    "spotify.exe",
    "youtubemusic.exe",
    "iTunes.exe",
    "foobar2000.exe",
    "melon.exe"
]

PRODUCT_APPS = [
    "chrome.exe",
    "kakao.exe",
    "powerpnt.exe",
    "EXCEL.EXE",
    "WINWORD.EXE",
    "Notion.exe",
    "Discord.exe",
    "slack.exe"
]

APP_CATEGORIES = {
    "CODE": CODE_APPS,
    "GAME": GAME_APPS,
    "MUSIC": MUSIC_APPS,
    "PRODUCT": PRODUCT_APPS
}

ALL_APPS = list(set(CODE_APPS + GAME_APPS + MUSIC_APPS + PRODUCT_APPS))

SERVER_URL = "http://localhost:8080/api/v1/game/log"

SERVER_CATEGORY_URL = "http://localhost:8080/api/v1/game/category"



running_instances = {}


class Outputview():
    @staticmethod
    def display_start_message(apps_list):
        print("=" * 50)
        print("데스크탑 앱 모니터링을 시작합니다.")
        print(f"감시 대상 앱: {', '.join(apps_list)}")
        print("=" * 50)
        sys.stdout.flush()

    @staticmethod
    def display_app_start(app_name, pid, start_time):
        formatted_time = start_time.strftime('%Y-%m-%d %H:%M:%S')
        print(f"\n [START] '{app_name}' (PID: {pid}) - 시작 시간: {formatted_time}")
        sys.stdout.flush()

    @staticmethod
    def display_app_stop(app_name, pid, end_time):
        formatted_end = end_time.strftime('%H:%M:%S')
        print(f"\n [STOP] '{app_name}' (PID: {pid}) - 종료 시간: {formatted_end}")
        print("-" * 50)
        sys.stdout.flush()

    @staticmethod
    def failed_send(response):
        print(f"   [Server] 전송 실패: HTTP {response.status_code}")

    @staticmethod
    def succeed_send(response):
        print(f"   [Server] 전송 성공: HTTP {response.status_code}")

    @staticmethod
    def error_send(e):
        print(f"   [Server] 전송 중 알 수 없는 오류 발생: {e}")

    @staticmethod
    def failed_add_category(app_name):
        print(f"   앱 넣기 실패: {app_name}")

    @staticmethod
    def display_command_log(command, result):
        print(f"\n [Command] 수신: {command}")
        print(f"           결과: {result}")

    @staticmethod
    def succeed_category_send(app_name, category, response):
        print(f"   [Category/Server] '{app_name}' 전송 성공: HTTP {response.status_code}")

    @staticmethod
    def failed_category_send(app_name, category, response):
        print(f"   [Category/Server] '{app_name}' 전송 실패: HTTP {response.status_code}")

    @staticmethod
    def error_category_send(e):
        print(f"   [Category/Server] 전송 중 알 수 없는 오류 발생: {e}")

def get_app_category(app_name):
    app_name_lower = app_name.lower()

    # 각 리스트를 체크하여 카테고리 반환.
    if app_name_lower in [app.lower() for app in CODE_APPS]:
        return "CODE"
    elif app_name_lower in [app.lower() for app in GAME_APPS]:
        return "GAME"
    elif app_name_lower in [app.lower() for app in MUSIC_APPS]:
        return "MUSIC"
    elif app_name_lower in [app.lower() for app in PRODUCT_APPS]:
        return "PRODUCT"
    else:
        return "UNKNOWN"


# 현재 실행 중인 모든 프로세스 스캔(APPS안에 있는것들만)
def get_running_apps():

    #지금 켜진 현재 앱들 정보
    current_process = {}

    # 현재 실행 중인 모든 프로세스를 반복함(성능을 위해 필요한 정보만 갖고옴)
    for process in psutil.process_iter(['pid', 'name', 'create_time']):
        try:
            p_info = process.info
            p_name = p_info['name']

            # 프로세스 이름이 감시 대상 목록에 있는지 확인
            if get_app_category(p_name) != "UNKNOWN":
                current_process[p_info['pid']] = {
                    'name': p_name,
                    # create_time은 timestamp 임. datetime 객체로 변환
                    'start_time': datetime.fromtimestamp(p_info['create_time'])
                }
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess): continue

    return current_process

#현재 시작됬거나 끝난 함수들 찾기
def track_running_apps():
    Outputview.display_start_message(ALL_APPS)

    while True:
        # 1. 현재 순간 실행 중인 타겟 프로세스들을 가져옴
        current_snapshot = get_running_apps()
        # 2. 새로운 프로세스가 생겼는지 확인
        check_start(current_snapshot)
        # 3. 추적 중이던 프로세스가 사라졌는지 확인
        pids_to_check = list(running_instances.keys())
        check_stop(current_snapshot, pids_to_check)
        time.sleep(1)


def check_start(current_snapshot):
    for pid, info in current_snapshot.items():
        if(pid not in running_instances):
            start_time = datetime.now()
            #출력 및 서버로 보내기
            Outputview.display_app_start(info['name'], pid, start_time)
            send_server(pid, info['name'], "START", start_time)
            #추천 리스트에 등록
            running_instances[pid] = info


def check_stop(current_snapshot, pids_to_check):
    for pid in pids_to_check:
        if(pid not in current_snapshot):
            app_data = running_instances[pid]
            end_time = datetime.now()
            # 출력 및 서버로 보내기
            Outputview.display_app_stop(app_data['name'], pid, end_time)
            send_server(pid, app_data['name'], "STOP", end_time)
            # 추천 리스트에서 제거
            del running_instances[pid]

#데이터를 서버로 전송하는 함수
def send_server(pid, app_name, event_type, event_time):

    event_time_tostr = event_time.isoformat()
    app_category = get_app_category(app_name)

    data = {
        'pid': pid,
        'appName': app_name,
        'eventType': event_type,
        'eventTime': event_time_tostr,
        'appCategory': app_category,
    }
    try:
        response = requests.post(SERVER_URL, json=data, timeout=2)
        if response.status_code >= 200 and response.status_code < 300:
            Outputview.succeed_send(response)
    except (requests.exceptions.RequestException, requests.exceptions.ConnectionError) as e:
        Outputview.error_send(e)



#카테고리 전체 이름을 로드하는 함수
def get_all_categories():
    return list(APP_CATEGORIES.keys())

def add_category(app_name, category):
    global ALL_APPS

    target_list = APP_CATEGORIES[category.upper()]
    # 이미 카테고리에 있을 때
    if app_name in target_list:
        Outputview.failed_send(category)
        return False

    target_list.append(app_name)
    ALL_APPS = list(set(ALL_APPS + [app_name]))

    return True, None


# 서버로 카테고리 추가 정보를 전송하는 함수
@app.route("/api/v1/game/category", methods=["GET"])
def get_all_category_info():
    return jsonify(APP_CATEGORIES), 200

# 서버가 카테고리 추가하는 함수
@app.route("/api/v1/game/category", methods=["POST"])
def add_category_from_spring():
    data = request.get_json()
    app_name = data.get("appName")
    category = data.get("categoryName")

    #파라미터 체크
    if not app_name or not category:
        return jsonify({"message": "appName, categoryName 둘 다 필요합니다."}), 400
    #카테고리 존재 여부 확인
    category_upper = category.upper()
    if category_upper not in APP_CATEGORIES:
        return jsonify({"message": f"존재하지 않는 카테고리: {category}"}), 400

    #추가 로직
    success, error = add_category(app_name, category_upper)

    if not success:
        return jsonify({"message": error or f"{category}에 이미 {app_name}이(가) 있습니다."}), 400
    return jsonify({"message": f"{app_name}을(를) {category_upper} 카테고리에 추가했습니다."}), 201


def run_executor(commands):
    try:
        cmd = commands[0]
        subprocess.Popen(cmd, shell=True)
        return f"명령어 '{cmd}' 실행 시작됨", True
    except Exception as e:
        return f"실행 중 오류 발생: {str(e)}", False

# Flask와는 별도의 스레드로 운영
def monitor_loop():
    Outputview.display_start_message(ALL_APPS)
    while True:
        current_snapshot = get_running_apps()
        check_start(current_snapshot)
        pids_to_check = list(running_instances.keys())
        check_stop(current_snapshot, pids_to_check)

        time.sleep(1)

if __name__ == "__main__":
    try:
        # 1. 감시 루프를 별도 스레드(데몬)로 실행 -> 백그라운드에서 계속 돔
        monitor_thread = threading.Thread(target=monitor_loop, daemon=True)
        monitor_thread.start()
        # 2. 메인 스레드는 Flask 서버 실행 -> 요청을 기다림 (Blocking)
        app.run(host="0.0.0.0", port=5000)

    except KeyboardInterrupt:
        print("종료.")
