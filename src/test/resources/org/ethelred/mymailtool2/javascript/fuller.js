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
    minage: "1 month",
    operations: 300
});

function matcherList(input) {
    return input.split(/,\s*/).map(function(s){return ".*" + s + ".*";});
}

var spamSenders = matcherList("Fake Spam Co, Another Spammer");

deleteFrom("Inbox").ifIt(isFrom(spamSenders));
deleteFrom("old-messages").ifIt(isFrom(spamSenders)).includeSubFolders();

move("Inbox").to("old-messages").ifIt(isOlderThan("4 days"));

move("Inbox").to("Currency").ifIt(isFrom(".*Currency Converter.*")).and(matchesSubject(".*Currency Update.*"));
