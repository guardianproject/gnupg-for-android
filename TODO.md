TODO
====

* audit security impl of unix domian sockets
  http://www.thomasstover.com/uds.html

  Our use of abstract domain sockets is worrying. A rogue application could
  "squat" on our socket and intercept the passphrase.
