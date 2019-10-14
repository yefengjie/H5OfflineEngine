#! /bin/bash
echo 'please wait...'
python -m http.server 8080 LIGHTTPD_THROTTLE
echo 'start server at http://localhost:8080/'
