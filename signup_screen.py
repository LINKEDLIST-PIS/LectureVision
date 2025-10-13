# 회원가입
from kivy.uix.screenmanager import Screen
from kivy.uix.popup import Popup
from kivy.uix.label import Label
from utils.file_io import load_json, save_json
import time
from main import LVApp

USERS_FILE = 'data/users.json'

class SignupScreen(Screen):
    """[회원가입 기능]
    - 이름, 전화번호, 이메일, 비밀번호 입력
    - JSON에 저장 후 로그인 화면 이동
    """
    def signup(self, name, phone, email, password):
        if not (name and phone and email and password):
            Popup(title='오류', content=Label(text='모든 항목을 입력하세요'),
                  size_hint=(.8,.3)).open()
            return
        users = load_json(USERS_FILE, [])
        users.append({'id': int(time.time()*1000), 'name': name, 'phone': phone,
                      'email': email, 'password': password})
        save_json(USERS_FILE, users)
        Popup(title='완료', content=Label(text='회원가입 완료'), size_hint=(.8,.3)).open()
        LVApp.get_running_app().root.current = 'login'
