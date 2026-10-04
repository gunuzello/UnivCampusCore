#!/usr/bin/env python3
"""Start and stop only the local server processes owned by this runner."""
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import time
from urllib.request import urlopen

root = Path(__file__).resolve().parents[1]
children = []

def cleanup(*_):
    for process in reversed(children):
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
    for process in reversed(children):
        try:
            process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
    children.clear()

for port in (8080, 5173):
    with socket.socket() as probe:
        if probe.connect_ex(('127.0.0.1', port)) == 0:
            sys.exit(f'{port} 포트를 이미 사용 중입니다. 실행 중인 UCC 서버를 종료한 뒤 다시 실행하세요.')

signal.signal(signal.SIGINT, lambda *_: sys.exit(0))
signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
try:
    subprocess.run(['docker', 'compose', 'up', '-d', '--wait'], cwd=root, check=True)
    subprocess.run(['npm', 'ci'], cwd=root / 'frontend', check=True)
    children.append(subprocess.Popen(['./mvnw', '-q', 'clean', 'spring-boot:run', '-Dspring-boot.run.profiles=local'], cwd=root / 'backend', start_new_session=True))
    deadline = time.monotonic() + 120
    while True:
        if children[0].poll() is not None:
            raise RuntimeError('백엔드 실행에 실패했습니다. 위의 오류 내용을 확인하세요.')
        try:
            with urlopen('http://127.0.0.1:8080/api/v1/health', timeout=1) as response:
                if response.status == 200:
                    break
        except OSError:
            pass
        if time.monotonic() > deadline:
            raise RuntimeError('백엔드가 120초 안에 준비되지 않았습니다.')
        time.sleep(0.5)
    children.append(subprocess.Popen(['npm', 'run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173', '--strictPort'], cwd=root / 'frontend', start_new_session=True))
    print('\nUCC: http://127.0.0.1:5173/ · 종료: Ctrl+C', flush=True)
    while all(process.poll() is None for process in children):
        time.sleep(0.5)
    raise RuntimeError('서버가 종료되었습니다. 위의 오류 내용을 확인하세요.')
except (RuntimeError, subprocess.CalledProcessError) as error:
    print(error, file=sys.stderr)
    sys.exit(1)
finally:
    cleanup()
