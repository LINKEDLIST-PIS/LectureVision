# 측정 수 비교 
from kivy.uix.screenmanager import Screen
from utils.file_io import load_json

RECORDS_FILE = 'data/records.json'

class CompareScreen(Screen):
    """[측정 인원 비교 기능]
    - 최신 측정값과 이전 기록 비교
    - 차이 표시, 알림 가능
    """
    def get_last_measure(self):
        recs = load_json(RECORDS_FILE, [])
        if recs:
            return recs[0]  # 최신 기록
        return None

    def compare_with_previous(self):
        recs = load_json(RECORDS_FILE, [])
        if len(recs) < 2:
            return None
        latest = recs[0]
        prev = recs[1]
        diff = {
            'current_diff': latest['current'] - prev['current'],
            'total_diff': latest['total'] - prev['total'],
            'absent_diff': latest['absent'] - prev['absent']
        }
        return diff
