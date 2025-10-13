
<LoginScreen>:
    BoxLayout:
        orientation: 'vertical'
        padding: 12
        spacing: 12
        LabelBig:
            text: 'Lecture Vision'
            size_hint_y: None
            height: '40dp'
        TextInput:
            id: email
            hint_text: '이메일'
            multiline: False
            size_hint_y: None
            height: '44dp'
        TextInput:
            id: password
            hint_text: '비밀번호'
            password: True
            multiline: False
            size_hint_y: None
            height: '44dp'
        Button:
            text: '로그인'
            on_release: root.attempt_login(email.text, password.text)
            size_hint_y: None
            height: '44dp'
        Button:
            text: '회원가입'
            on_release: app.root.current = 'signup'
            size_hint_y: None
            height: '44dp'
