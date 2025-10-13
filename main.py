from kivy.app import App
from kivy.uix.screenmanager import ScreenManager, NoTransition

# 화면 import
from login_screen import LoginScreen
from signup_screen import SignupScreen
from measure_screen import MeasureScreen
from compare_screen import CompareScreen
from record_screen import RecordScreen
from timer_screen import TimerScreen


class LectureScreenManager(ScreenManager):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # 전환 효과 (필요 시 FadeTransition, SlideTransition 등 변경 가능)
        self.transition = NoTransition()

        # 화면 등록
        self.add_widget(LoginScreen(name='login'))
        self.add_widget(SignupScreen(name='signup'))
        self.add_widget(MeasureScreen(name='measure'))
        self.add_widget(CompareScreen(name='compare'))
        self.add_widget(RecordScreen(name='record'))
        self.add_widget(TimerScreen(name='timer'))

        # 초기 화면 설정
        self.current = 'login'


class LectureApp(App):
    def build(self):
        self.title = "LectureVision - 인원 측정 시스템"
        return LectureScreenManager()


if __name__ == '__main__':
    LectureApp().run()
