from fastapi_mail import FastMail, MessageSchema, ConnectionConfig
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import EmailStr


class MailSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    MAIL_USERNAME: str
    MAIL_PASSWORD: str
    MAIL_FROM: EmailStr
    MAIL_PORT: int = 465
    MAIL_SERVER: str

    MAIL_STARTTLS: bool = False
    MAIL_SSL_TLS: bool = True
    USE_CREDENTIALS: bool = True
    VALIDATE_CERTS: bool = True


settings = MailSettings()


conf = ConnectionConfig(
    MAIL_USERNAME=settings.MAIL_USERNAME,
    MAIL_PASSWORD=settings.MAIL_PASSWORD,
    MAIL_FROM=settings.MAIL_FROM,
    MAIL_PORT=settings.MAIL_PORT,
    MAIL_SERVER=settings.MAIL_SERVER,
    MAIL_STARTTLS=settings.MAIL_STARTTLS,
    MAIL_SSL_TLS=settings.MAIL_SSL_TLS,
    USE_CREDENTIALS=settings.USE_CREDENTIALS,
    VALIDATE_CERTS=settings.VALIDATE_CERTS,
)

async def send_verification_email(email: str, token: str):
    verification_url = f"https://cm838.myasustor.com:5445/accounts/verify?token={token}"

    html_content = f"""
    <html>
      <body style="font-family: Arial, sans-serif; line-height:1.6;">
        <p>LectureVision에 계정을 등록하려면 아래 버튼을 클릭해 인증을 완료하세요:</p>
        <p style="text-align:center; margin: 30px 0;">
          <a href="{verification_url}"
             style="background-color:#4CAF50; color:white; padding:12px 24px;
                    text-decoration:none; border-radius:5px; font-weight:bold;">
            계정 인증하기
          </a>
        </p>
        <p>만약 회원 가입 시도를 하지 않았다면 이 메일을 무시하세요.</p>
      </body>
    </html>
    """

    message = MessageSchema(
        subject="GNU 계정 인증 메일",
        recipients=[email],
        body=html_content,
        subtype="html"
    )
    fm = FastMail(conf)
    await fm.send_message(message)