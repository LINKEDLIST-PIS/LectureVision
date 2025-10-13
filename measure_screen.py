# 인원수 측정
from kivy.uix.screenmanager import Screen
from kivy.uix.popup import Popup
from kivy.uix.label import Label
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from utils.file_io import load_json, save_json
from utils.api import measure_via_api
import time, os
from datetime import datetime

RECORDS_FILE = 'data/records.json'
DATA_DIR = 'data/'

class MeasureScreen(Screen):
    """[인원수 측정 기능]
    - 수동 입력
    - 카메라 촬영 후 서버 업로드
    """
    def manual_measure(self):
        box = BoxLayout(orientation='vertical', spacing=8)
        ti_cur = TextInput(hint_text='현재 인원', input_filter='int', multiline=False)
        ti_total = TextInput(hint_text='총 인원', input_filter='int', multiline=False)
        ti_abs = TextInput(hint_text='결석', input_filter='int', multiline=False)
        btn = Button(text='저장', size_hint_y=None, height='40dp')
        box.add_widget(ti_cur); box.add_widget(ti_total); box.add_widget(ti_abs); box.add_widget(btn)
        p = Popup(title='수동 측정', content=box, size_hint=(.9,.6))

        def on_save(instance):
            try:
                cur = int(ti_cur.text or '0'); tot = int(ti_total.text or '0'); absn = int(ti_abs.text or '0')
            except:
                return
            recs = load_json(RECORDS_FILE, [])
            rec = {'id': int(time.time()*1000), 'time': datetime.now().isoformat(),
                   'current': cur, 'total': tot, 'absent': absn}
            recs.insert(0, rec)
            save_json(RECORDS_FILE, recs)
            p.dismiss()
        btn.bind(on_release=on_save)
        p.open()

    def camera_measure(self, filepath):
        """카메라 촬영 후 API 측정"""
        rv = measure_via_api(filepath)
        if rv and isinstance(rv, dict):
            recs = load_json(RECORDS_FILE, [])
            rec = {'id': int(time.time()*1000), 'time': datetime.now().isoformat(),
                   'current': int(rv.get('current',0)),
                   'total': int(rv.get('total',0)),
                   'absent': int(rv.get('absent',0))}
            recs.insert(0, rec)
            save_json(RECORDS_FILE, recs)
