TODO
====

* audit security impl of unix domian sockets
  http://www.thomasstover.com/uds.html

  Our use of abstract domain sockets is worrying. A rogue application could
  "squat" on our socket and intercept the passphrase.

  We should implement SO_PEERCRED 
  http://welz.org.za/notes/on-peer-cred.html
