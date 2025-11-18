import sys

import psutil
import time
from datetime import datetime

# 검사하고 싶은 기본 앱(따로 추가 가능)
APPS = [
    "chrome.exe",
    "kakao.exe",
    "discord.exe"
]


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
    def display_app_stop(app_name, pid, start_time, end_time, duration):
        formatted_start = start_time.strftime('%H:%M:%S')
        formatted_end = end_time.strftime('%H:%M:%S')

        print(f"\n [STOP] '{app_name}' (PID: {pid}) - 종료 시간: {formatted_end}")
        print(f"   👉 총 사용 시간: {duration}")
        # 서버로 전송할 데이터를 시각적으로 보여줍니다.
        print(f"   [Data] 시작: {formatted_start}, 종료: {formatted_end}, 사용 시간(초): {duration.total_seconds()}")
        print("-" * 50)
        sys.stdout.flush()



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
            if p_name and p_name.lower() in [app.lower() for app in APPS]:
                current_process[p_info['pid']] = {
                    'name': p_name,
                    # create_time은 timestamp 임. datetime 객체로 변환
                    'start_time': datetime.fromtimestamp(p_info['create_time'])
                }
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess): continue

    return current_process


def track_running_apps():
    Outputview.display_start_message(APPS)

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
            #출력
            Outputview.display_app_start(info['name'], pid, info['start_time'])
            #추천 리스트에 등록
            running_instances[pid] = info


def check_stop(current_snapshot, pids_to_check):
    for pid in pids_to_check:
        if(pid not in current_snapshot):
            app_data = running_instances[pid]
            #삭제된 내용의 정보들
            end_time = datetime.now()
            start_time = app_data['start_time']
            duration = end_time - start_time

            Outputview.display_app_stop(app_data['name'], pid, start_time, end_time, duration)

            del running_instances[pid]




if __name__ == "__main__":
    try:
        track_running_apps()
    except KeyboardInterrupt:
        print("종료.")