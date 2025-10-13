# 타이머/재측정 
from kivy.uix.screenmanager import Screen
from kivy.clock import Clock
from utils.file_io import load_json, save_json
from datetime import datetime
import time

RECORDS_FILE = 'data/records.json'

class TimerScreen(Screen):
    """[재측정 / 타이머 기능]
    - 지정한 시간마다 자동 측정 시뮬레이션
    - 상태 표시
    """
    timer_ev = None
    status_text = '정지'

    def start_timer(self, minutes_text):
        try:
            m = int(minutes_text)
            if m <= 0: return
        except:
            return self.stop_timer()
        self.status_text = f'실행중 ({m}분)'
        self.timer_ev = Clock.schedule_interval(lambda dt: self._on_tick(), m*60)

    def stop_timer(self):
        if self.timer_ev:
            self.timer_ev.cancel()
            self.timer_ev = None
        self.status_text = '정지'

    def _on_tick(self):
        # 자동 측정 시뮬레이션
        simulated_current = max(0, int(10 + (time.time()%10)))
        simulated_total = simulated_current + 2
        simulated_absent = simulated_total - simulated_current
