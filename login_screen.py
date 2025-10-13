# 로그인 
from kivy.uix.screenmanager import Screen
from kivy.uix.popup import Popup
from kivy.uix.label import Label
from utils.file_io import load_json
from main import LVApp

USERS_FILE = 'data/users.json'

class LoginScreen(Screen):
    """[로그인 기능]
    - 이메일, 비밀번호 입력 후 로그인
    - 로그인 성공 시 홈 화면 이동
    """
    def attempt_login(self, email, password):
        if not email or not password:
            Popup(title='오류', content=Label(text='이메일과 비밀번호를 입력하세요'),
                  size_hint=(.8,.3)).open()
            return
        users = load_json(USERS_FILE, [])
        found = None
        for u in users:
            if u.get('email') == email and u.get('password') == password:
                found = u
                break
        if found:
            LVApp.get_running_app().current_user = found
            LVApp.get_running_app().root.current = 'home'
        else:
            Popup(title='로그인 실패', content=Label(text='계정이 없거나 비밀번호가 틀립니다'),
                  size_hint=(.8,.3)).open()
