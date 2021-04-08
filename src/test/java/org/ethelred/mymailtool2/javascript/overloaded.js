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

var blacklist = ['banana@fruit.com', 'cheddar@cheese.com'];
deleteFrom("Inbox").ifIt(isFrom(blacklist));
