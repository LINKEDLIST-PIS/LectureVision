import logging
import time
from fastapi import Request

logging.basicConfig(level=logging.INFO)

async def log_exceptions(request: Request, call_next):
    start = time.time()
    try:
        response = await call_next(request)
    except Exception as e:
        logging.exception(f"ERROR | path={request.url.path} | error={e}")
        raise
    duration = (time.time() - start) * 1000
    logging.info(
        f"REQUEST | {request.method} {request.url.path} | "
        f"status={response.status_code} | duration={duration:.2f}ms"
    )
    return response