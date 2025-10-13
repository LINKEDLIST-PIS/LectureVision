# 기록 조회
from kivy.uix.screenmanager import Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.scrollview import ScrollView
from utils.file_io import load_json, save_json
import csv, os

RECORDS_FILE = 'data/records.json'
DATA_DIR = 'data/'

class RecordScreen(Screen):
    """[인원수 기록 조회 기능]
    - 과거 기록 확인
    - 삭제, 전체 삭제
    - CSV 내보내기
    """
    def render(self):
        grid = self.ids.recgrid
        grid.clear_widgets()
        recs = load_json(RECORDS_FILE, [])
        for r in recs:
            b = BoxLayout(orientation='horizontal')
            b.add_widget(Label(text=f"{r['time']}\n현재:{r['current']} 총:{r['total']} 결석:{r['absent']}"))
            btn = Button(text='삭제', size_hint_x=None, width=80)
            btn.bind(on_release=lambda inst, rid=r['id']: self.delete_record(rid))
            b.add_widget(btn)
            grid.add_widget(b)

    def delete_record(self, id):
        recs = load_json(RECORDS_FILE, [])
        recs = [r for r in recs if r['id'] != id]
        save_json(RECORDS_FILE, recs)
        self.render()

    def clear_all(self):
        save_json(RECORDS_FILE, [])
        self.render()

    def export_csv(self):
        recs = load_json(RECORDS_FILE, [])
        if not recs: return
        path = os.path.join(DATA_DIR, 'lecture_records_export.csv')
        with open(path, 'w', newline='', encoding='utf-8') as f:
            w = csv.writer(f)
            w.writerow(['time','current','total','absent'])
            for r in recs:
                w.writerow([r['time'], r['current'], r['total'], r['absent']])
