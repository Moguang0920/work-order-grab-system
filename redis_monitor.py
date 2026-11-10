import socket, sys, time
sec = float(sys.argv[1]); outfile = sys.argv[2]
s = socket.create_connection(('127.0.0.1', 6379), 3)
s.sendall(b'*1\r\n$7\r\nMONITOR\r\n')
s.settimeout(0.3)
end = time.time() + sec
buf = []
while time.time() < end:
    try:
        d = s.recv(65536)
        if d: buf.append(d.decode('utf-8', 'ignore'))
    except socket.timeout:
        pass
open(outfile, 'w', encoding='utf-8').write(''.join(buf))
s.close()
