import socket
import sys


class Redis:
    def __init__(self, host='127.0.0.1', port=6379, timeout=3):
        self.sock = socket.create_connection((host, port), timeout)
        self.f = self.sock.makefile('rb')

    def _read(self):
        line = self.f.readline()
        if not line:
            return None
        t, rest = line[:1], line[1:].strip()
        if t == b'+':
            return rest.decode('utf-8', 'ignore')
        if t == b'-':
            return 'ERR ' + rest.decode('utf-8', 'ignore')
        if t == b':':
            return int(rest)
        if t == b'$':
            n = int(rest)
            if n == -1:
                return None
            data = self.f.read(n)
            self.f.read(2)
            return data.decode('utf-8', 'ignore')
        if t == b'*':
            n = int(rest)
            if n == -1:
                return None
            return [self._read() for _ in range(n)]
        return line

    def cmd(self, *args):
        payload = ('*%d\r\n' % len(args)).encode()
        for a in args:
            b = str(a).encode()
            payload += b'$%d\r\n' % len(b) + b + b'\r\n'
        self.sock.sendall(payload)
        return self._read()

    def close(self):
        try:
            self.f.close()
            self.sock.close()
        except Exception:
            pass


if __name__ == '__main__':
    r = Redis()
    for line in sys.argv[1:]:
        parts = line.split()
        result = r.cmd(*parts)
        print('>', line)
        if isinstance(result, list):
            if not result:
                print('  (空)')
            else:
                print('  共 %d 条:' % len(result))
                for item in result[:20]:
                    print('   ', item)
        else:
            print(' ', result)
    r.close()
