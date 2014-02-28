config({
    mail: {
        store: {
            protocol: "imap"
        },
        user: "edward",
        host: "mail.example.com",
        port: 143,
        imap: {starttls: {enable: true}}
    },
    password: "xxxxxx",
    minage: "3 months",
    operations: 300
});

move("Inbox").to("archive");
split("archive");

var subjects = ['subject1', 'subject2'];
subjects.forEach(function(s){
    deleteFrom("Inbox").ifIt(matchesSubject('.*' + s + '.*'));
});

function repeatedWordSubject(msg) {
    var myRE = /(\w+)\W+\1/;
    return myRE.test(msg.subject);
}

move("test").to("repeated").ifIt(repeatedWordSubject);

function spamscore(msg) {
  try {
  var hdr = msg.getHeader("X-Spam-Score");
  if(hdr.length > 0) {
    var score = parseFloat(hdr[0]);
    return score >= 2.5;
  }
  } catch(e) {}
  return false;
}

move("spamtest").to("spamscore").ifIt(spamscore);